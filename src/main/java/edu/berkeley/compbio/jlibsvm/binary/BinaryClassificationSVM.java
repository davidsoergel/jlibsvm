package edu.berkeley.compbio.jlibsvm.binary;

import com.davidsoergel.conja.Function;
import com.davidsoergel.conja.Parallel;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterGrid;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SvmException;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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


	public BinaryModel<L, P> train(@NotNull BinaryClassificationProblem<L, P> problem,
	                               @NotNull ImmutableSvmParameter<L, P> param)
		//,@NotNull final TreeExecutorService execService)
		{
		validateParam(param);
		BinaryModel<L, P> result;
		if (param instanceof ImmutableSvmParameterGrid)  //  either the problem was binary to start with, or param.gridsearchBinaryMachinesIndependently
			{
			result = trainGrid(problem, (ImmutableSvmParameterGrid<L, P>) param); //, execService);
			}
		else if (param.probability)  // this may already be a fold, but we have to sub-fold it to get probabilities
			{
			result = trainScaledWithCV(problem, (ImmutableSvmParameterPoint<L, P>) param); //, execService);
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


	private BinaryModel<L, P> trainGrid(@NotNull final BinaryClassificationProblem<L, P> problem,
	                                    @NotNull ImmutableSvmParameterGrid<L, P> param)
		//,  @NotNull final TreeExecutorService execService)
		{
		final GridTrainingResult gtresult = new GridTrainingResult();


		Parallel.forEach(param.getGridParams(), new Function<ImmutableSvmParameterPoint<L, P>, Void>()
		{
		public Void apply(final ImmutableSvmParameterPoint<L, P> gridParam)
			{// note we must use the CV variant in order to know which parameter set is best
			SvmBinaryCrossValidationResults<L, P> crossValidationResults =
					performCrossValidation(problem, gridParam); //, execService);
			logger.info("CV results for grid point " + gridParam + ": " + crossValidationResults);
			gtresult.update(gridParam, crossValidationResults);
			return null;
			}
		});

		// no need for the iterator version here; the set of params doesn't require too much memory
/*
		Set<Runnable> gridTasks = new HashSet<Runnable>();
		for (final ImmutableSvmParameterPoint<L, P> gridParam : param.getGridParams())
			{
			gridTasks.add(new Runnable()
			{
			public void run()
				{
				// note we must use the CV variant in order to know which parameter set is best
				SvmBinaryCrossValidationResults<L, P> crossValidationResults =
						performCrossValidation(problem, gridParam); //, execService);
				gtresult.update(gridParam, crossValidationResults);
				}
			});
			}

		execService.submitAndWaitForAll(gridTasks);
*/
		logger.info("Chose grid point: " + gtresult.bestParam);

		// finally train once on all the data (including rescaling)
		BinaryModel<L, P> result = trainScaled(problem, gtresult.bestParam);
		synchronized (gtresult)
			{
			result.crossValidationResults = gtresult.bestCrossValidationResults;
			}
		return result;
		}

	private class GridTrainingResult
		{
		ImmutableSvmParameterPoint<L, P> bestParam = null;
		SvmBinaryCrossValidationResults<L, P> bestCrossValidationResults = null;
		float bestSensitivity = -1F;

		synchronized void update(ImmutableSvmParameterPoint<L, P> gridParam,
		                         SvmBinaryCrossValidationResults<L, P> crossValidationResults)
			{
			float sensitivity = crossValidationResults.classNormalizedSensitivity();
			if (sensitivity > bestSensitivity)
				{
				bestParam = gridParam;
				bestSensitivity = sensitivity;
				bestCrossValidationResults = crossValidationResults;
				}
			}
		}

	/**
	 * Train the classifier, and also prepare the probability sigmoid thing if requested.
	 *
	 * @param problem
	 * @return
	 */
	private BinaryModel<L, P> trainScaledWithCV(@NotNull BinaryClassificationProblem<L, P> problem,
	                                            @NotNull ImmutableSvmParameterPoint<L, P> param)
		//,@NotNull final TreeExecutorService execService)
		{
		// if scaling each binary machine is enabled, then each fold will be independently scaled also; so we don't need to scale the whole dataset prior to CV

		SvmBinaryCrossValidationResults<L, P> cv = null;
		try
			{
			cv = performCrossValidation(problem, param); //, execService);
			}
		catch (SvmException e)
			{
			//ignore, probably there weren't enough points to make folds
			logger.debug("Could not perform cross-validation", e);
			}

		// finally train once on all the data (including rescaling)
		BinaryModel<L, P> result = trainScaled(problem, param);
		result.crossValidationResults = cv;  // careful later: this might be null

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
	public SvmBinaryCrossValidationResults<L, P> performCrossValidation(
			@NotNull BinaryClassificationProblem<L, P> problem, @NotNull ImmutableSvmParameter<L, P> param)
		//,	@NotNull final TreeExecutorService execService)
		{
		//there is no point in computing probabilities on these submodels (and that produces infinite recursion)
		ImmutableSvmParameterPoint<L, P> noProbParam = (ImmutableSvmParameterPoint<L, P>) param.noProbabilityCopy();

		final Map<P, Float> decisionValues = continuousCrossValidation(problem, noProbParam); //, execService);

		// but the CV may be used to compute probabilities at this level, if requested
		SvmBinaryCrossValidationResults<L, P> cv =
				new SvmBinaryCrossValidationResults<L, P>(problem, decisionValues, param.probability);
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
	protected abstract BinaryModel<L, P> trainOne(@NotNull BinaryClassificationProblem<L, P> problem, float Cp,
	                                              float Cn, @NotNull ImmutableSvmParameterPoint<L, P> param);


	private BinaryModel<L, P> trainScaled(@NotNull BinaryClassificationProblem<L, P> problem,
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

	private BinaryModel<L, P> trainWeighted(@NotNull BinaryClassificationProblem<L, P> problem,
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

/*	public Callable<BinaryModel<L, P>> trainCallable(BinaryClassificationProblem<L, P> problem,
	                                                 @NotNull ImmutableSvmParameter<L, P> param)
		{
		return new BinarySvmTrainCallable(problem, param);
		}
*/
// -------------------------- INNER CLASSES --------------------------
/*
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
		}*/
	}
