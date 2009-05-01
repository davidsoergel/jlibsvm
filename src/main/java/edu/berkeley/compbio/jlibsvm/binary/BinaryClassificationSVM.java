package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblem;
import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SigmoidProbabilityModel;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Formatter;
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


// --------------------------- CONSTRUCTORS ---------------------------

	protected BinaryClassificationSVM(@NotNull KernelFunction<P> kernel,
	                                  @NotNull ScalingModelLearner<P> scalingModelLearner, SvmParameter<L> param)
		{
		super(kernel, scalingModelLearner, param);
		}

// -------------------------- OTHER METHODS --------------------------

	public BinaryModel<L, P> train(BinaryClassificationProblem<L, P> problem)
		{
		if (problem.getLabels().size() != 2)
			{
			throw new SvmException("Can't do binary classification; " + problem.getLabels().size() + " classes found");
			}

		// calculate weighted C

		float weightedCp = param.C;
		float weightedCn = param.C;

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
		BinaryModel<L, P> result = train(problem, weightedCp, weightedCn);

		// ** logging output disabled for now
		//if (logger.isDebugEnabled())
		//	{
		//result.printSolutionInfo(problem);
		//	}
		//logger.info(qMatrix.perfString());
		return result;
		}

	/**
	 * Train the classifier, and also prepare the probability sigmoid thing if requested.  Note that svcProbability will
	 * call this method in the course of cross-validation, but will first ensure that param.probability == false;
	 *
	 * @param problem
	 * @param Cp
	 * @param Cn
	 * @return
	 */
	public BinaryModel<L, P> train(BinaryClassificationProblem<L, P> problem, float Cp, float Cn)
		{
		if (scalingModelLearner != null && param.scaleBinaryMachinesIndependently)
			{
			// ** the examples are copied before scaling, not scaled in place
			// that way we don't need to worry that the same examples are being used in another thread, or scaled differently in different contexts, etc.
			// this may cause memory problems though

			problem = problem.getScaledCopy(scalingModelLearner);
			}
		BinaryModel<L, P> result = trainOne(problem, Cp, Cn);
		if (param.probability)
			{
			result.sigmoid = svcProbability(problem, Cp, Cn, result);
			}
		result.printSolutionInfo(problem);
		return result;
		}

	/**
	 * Normal training on the entire problem, with no cross-validation-based probability measure.
	 *
	 * @param problem
	 * @param Cp
	 * @param Cn
	 * @return
	 */
	protected abstract BinaryModel<L, P> trainOne(BinaryClassificationProblem<L, P> problem, float Cp, float Cn);


	/**
	 * Cross-validation decision values for probability estimates
	 *
	 * @param problem
	 * @param Cp
	 * @param Cn
	 * @param model
	 * @return
	 */

	private SigmoidProbabilityModel svcProbability(BinaryClassificationProblem<L, P> problem, float Cp, float Cn,
	                                               BinaryModel<L, P> model)
		{
		// ** Original implementation makes a point of not explicitly training if all of the examples are in one class anyway.  Does that matter?

		SvmParameter<L> subparam = new SvmParameter<L>(param);
		subparam.probability = false;
		subparam.C = 1.0f;

		subparam.putWeight(problem.getTrueLabel(), Cp);
		subparam.putWeight(problem.getFalseLabel(), Cn);


		// ugly hack to temporarily replace the parameters.  This only works because train() is ultimately a method on this very object.
		SvmParameter origParam = param;
		param = subparam;

		Map<P, Float> decisionValues =
				continuousCrossValidation((ExplicitSvmProblem<L, P, BinaryClassificationProblem<L, P>>) problem, 5);

		param = origParam;

		// convert to arrays

		int i = 0;
		float[] decisionValueArray = new float[decisionValues.size()];
		boolean[] labelArray = new boolean[decisionValues.size()];
		L trueLabel = problem.getTrueLabel();

		for (Map.Entry<P, Float> entry : decisionValues.entrySet())
			{
			decisionValueArray[i] = entry.getValue();
			labelArray[i] = problem.getTargetValue(entry.getKey()).equals(trueLabel);
			i++;
			}


		// while we're at it, since we've done a cross-validation anyway, we may as well report the accuracy.

		int tt = 0, ff = 0, ft = 0, tf = 0;
		for (int j = 0; j < i; j++)
			{
			if (decisionValueArray[j] > 0)
				{
				if (labelArray[j])
					{
					tt++;
					}
				else
					{
					ft++;
					}
				}
			else
				{
				if (labelArray[j])
					{
					tf++;
					}
				else
					{
					ff++;
					}
				}
			}

		BinaryModel.CrossValidationResults cv = model.newCrossValidationResults(i, tt, ft, tf, ff);

		Formatter f = new Formatter();
		f.format("Binary classifier for %s vs. %s: TP=%.2f FP=%.2f FN=%.2f TN=%.2f", trueLabel, problem.getFalseLabel(),
		         cv.TrueTrueRate(), cv.FalseTrueRate(), cv.TrueFalseRate(), cv.FalseFalseRate());


		//	logger.info("Binary classifier for " + trueLabel + " vs. " + problem.getFalseLabel() + ": TP="+((float)tp/i) + ": FP="
		//			+ ((float) fp / i) + ": FN=" + ((float) fn / i) + ": TN=" + ((float) tn / i) );

		logger.info(f.out().toString());

		return new SigmoidProbabilityModel(decisionValueArray, labelArray);
		}

	public Callable<BinaryModel<L, P>> trainCallable(BinaryClassificationProblem<L, P> problem, float Cp, float Cn)
		{
		return new BinarySvmTrainCallable(problem, Cp, Cn);
		}

// -------------------------- INNER CLASSES --------------------------

	private class BinarySvmTrainCallable implements Callable<BinaryModel<L, P>>
		{
// ------------------------------ FIELDS ------------------------------

		private BinaryClassificationProblem<L, P> problem;
		private float Cp, Cn;


// --------------------------- CONSTRUCTORS ---------------------------

		public BinarySvmTrainCallable(BinaryClassificationProblem<L, P> problem, float Cp, float Cn)
			{
			this.problem = problem;
			this.Cp = Cp;
			this.Cn = Cn;
			}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Callable ---------------------

		public BinaryModel<L, P> call() throws Exception
			{
			try
				{
				return train(problem, Cp, Cn);
				}
			catch (Exception e)
				{
				logger.error("Error", e);
				throw e;
				}
			}
		}
	}
