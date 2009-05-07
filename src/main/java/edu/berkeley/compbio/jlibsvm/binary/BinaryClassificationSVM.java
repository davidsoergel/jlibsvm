package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterGrid;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.SVM;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class BinaryClassificationSVM<L extends Comparable, P>
		extends SVM<L, P, BinaryClassificationProblem<L, P>>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(BinaryClassificationSVM.class);


// -------------------------- OTHER METHODS --------------------------


	public BinaryModel<L, P> train(BinaryClassificationProblem<L, P> problem,
	                               @NotNull ImmutableSvmParameter<L, P> param)
		{
		validateParam(param);
		BinaryModel<L, P> result;
		if (param instanceof ImmutableSvmParameterGrid)  //  either the problem was binary to start with, or param.gridsearchBinaryMachinesIndependently
			{
			result = trainGrid(problem, (ImmutableSvmParameterGrid<L, P>) param);
			}
		else if (param.probability)
			{
			result = trainScaledWithCV(problem, (ImmutableSvmParameterPoint<L, P>) param);
			}
		else
			{
			result = trainScaled(problem, (ImmutableSvmParameterPoint<L, P>) param);
			}
		return result;
		}

	/**
	 * Try a bunch of different parameter sets, and return the model based on the one that produces the best
	 * class-normalized sensitivity.
	 *
	 * @param problem
	 * @param param
	 * @return
	 */


	private BinaryModel<L, P> trainGrid(BinaryClassificationProblem<L, P> problem,
	                                    @NotNull ImmutableSvmParameterGrid<L, P> param)
		{
		// PERF should be concurrent.  Doesn't matter for multiclass since that's already multithreaded, but each binary grid-search will be single-threaded here.

		ImmutableSvmParameterPoint<L, P> bestParam = null;
		BinaryCrossValidationResults<L, P> bestCrossValidationResults = null;
		float bestSensitivity = -1F;
		for (ImmutableSvmParameterPoint<L, P> gridParam : param.getGridParams())
			{
			// note we must use the CV variant in order to know which parameter set is best

			//float gridCp = Cp * gridParam.Cfactor;
			//float gridCn = Cn * gridParam.Cfactor;

			//MultiClassModel<L, P> result = trainScaledWithCV(problem, gridParam);
			BinaryCrossValidationResults<L, P> crossValidationResults = performCrossValidation(problem, gridParam);
			float sensitivity = crossValidationResults.classNormalizedSensitivity();
			if (sensitivity > bestSensitivity)
				{
				bestParam = gridParam;
				bestSensitivity = sensitivity;
				bestCrossValidationResults = crossValidationResults;
				}
			}

		logger.info("Chose grid point: " + bestParam);

		// finally train once on all the data (including rescaling)
		BinaryModel<L, P> result = trainScaled(problem, bestParam);
		result.crossValidationResults = bestCrossValidationResults;
		return result;
		}

	/**
	 * Train the classifier, and also prepare the probability sigmoid thing if requested.
	 *
	 * @param problem
	 * @return
	 */
	private BinaryModel<L, P> trainScaledWithCV(BinaryClassificationProblem<L, P> problem,
	                                            @NotNull ImmutableSvmParameterPoint<L, P> param)
		{
		// if scaling each binary machine is enabled, then each fold will be independently scaled also; so we don't need to scale the whole dataset prior to CV

		BinaryCrossValidationResults<L, P> cv = performCrossValidation(problem, param);

		// finally train once on all the data (including rescaling)
		BinaryModel<L, P> result = trainScaled(problem, param);
		result.crossValidationResults = cv;

		result.printSolutionInfo(problem);
		return result;
		}

	/**
	 * Cross-validation decision values for probability estimates
	 *
	 * @param problem
	 * @return
	 */
/*	private SigmoidProbabilityModel svcProbability(BinaryClassificationProblem<L, P> problem, float Cp, float Cn,
	                                               @NotNull ImmutableSvmParameter<L, P> param)
		{
		// ** Original implementation makes a point of not explicitly training if all of the examples are in one class anyway.  Does that matter?

		Map<P, Float> decisionValues = performCrossValidation(problem, Cp, Cn,param);

		CrossValidationResults<L,P> cv = new CrossValidationResults<L,P>(problem, decisionValues);

		return new SigmoidProbabilityModel(cv.decisionValueArray, cv.labelArray);
		}
*/
	public BinaryCrossValidationResults<L, P> performCrossValidation(BinaryClassificationProblem<L, P> problem,
	                                                                 @NotNull ImmutableSvmParameter<L, P> param)
		{
		Map<P, Float> decisionValues = continuousCrossValidation(problem, param);

		BinaryCrossValidationResults<L, P> cv =
				new BinaryCrossValidationResults<L, P>(problem, decisionValues, param.probability);
		return cv;
		}

	/**
	 * Normal training on the entire problem, with no scaling and no cross-validation-based probability measure.
	 *
	 * @param problem
	 * @param Cp
	 * @param Cn
	 * @return
	 */
	protected abstract BinaryModel<L, P> trainOne(BinaryClassificationProblem<L, P> problem, float Cp, float Cn,
	                                              @NotNull ImmutableSvmParameterPoint<L, P> param);


	private BinaryModel<L, P> trainScaled(BinaryClassificationProblem<L, P> problem,
	                                      @NotNull ImmutableSvmParameterPoint<L, P> param)
		{
		if (param.scalingModelLearner != null && param.scaleBinaryMachinesIndependently)
			{
			// the examples are copied before scaling, not scaled in place
			// that way we don't need to worry that the same examples are being used in another thread, or scaled differently in different contexts, etc.
			// this may cause memory problems though

			problem = problem.getScaledCopy(param.scalingModelLearner);
			}

		BinaryModel<L, P> result = trainWeighted(problem, param);

		result.printSolutionInfo(problem);
		return result;
		}

	private BinaryModel<L, P> trainWeighted(BinaryClassificationProblem<L, P> problem,
	                                        @NotNull ImmutableSvmParameterPoint<L, P> param)
		{
		// calculate weighted C

		float weightedCp = param.C;
		float weightedCn = param.C;

		if (param.redistributeUnbalancedC)
			{
			Float weightP = param.getWeight(problem.getTrueLabel());
			if (weightP != null)
				{
				weightedCp *= weightP;
				}

			Float weightN = param.getWeight(problem.getFalseLabel());
			if (weightN != null)
				{
				weightedCn *= weightN;
				}
			}

		// train using those
		BinaryModel<L, P> result = trainOne(problem, weightedCp, weightedCn, param);


		// ** logging output disabled for now
		//if (logger.isDebugEnabled())
		//	{
		//result.printSolutionInfo(problem);
		//	}
		//logger.info(qMatrix.perfString());
		return result;
		}

	public Callable<BinaryModel<L, P>> trainCallable(BinaryClassificationProblem<L, P> problem,
	                                                 @NotNull ImmutableSvmParameter<L, P> param)
		{
		return new BinarySvmTrainCallable(problem, param);
		}

// -------------------------- INNER CLASSES --------------------------

	private class BinarySvmTrainCallable implements Callable<BinaryModel<L, P>>
		{
// ------------------------------ FIELDS ------------------------------

		private BinaryClassificationProblem<L, P> problem;

		private ImmutableSvmParameter<L, P> param;


// --------------------------- CONSTRUCTORS ---------------------------

		public BinarySvmTrainCallable(BinaryClassificationProblem<L, P> problem,
		                              @NotNull ImmutableSvmParameter<L, P> param)
			{
			this.problem = problem;
			this.param = param;
			}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Callable ---------------------

		public BinaryModel<L, P> call() throws Exception
			{
			try
				{
				return train(problem, param);
				}
			catch (Exception e)
				{
				logger.error("Error", e);
				throw e;
				}
			}
		}
	}
