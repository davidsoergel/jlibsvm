package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblem;
import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.SvmProblem;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class RegressionSVM<P, R extends SvmProblem<Float, P>> extends SVM<Float, P, R>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(RegressionSVM.class);

	private final float SQRT_2 = (float) Math.sqrt(2);


// --------------------------- CONSTRUCTORS ---------------------------

	protected RegressionSVM(KernelFunction<P> kernel, ScalingModelLearner<P> scalingModelLearner,
	                        SvmParameter<Float> param)
		{
		super(kernel, scalingModelLearner, param);
		}

// -------------------------- OTHER METHODS --------------------------

	// Return parameter of a Laplace distribution

	protected float laplaceParameter(RegressionProblem<P> problem)
		{
		int i;
		int numberOfFolds = 5;
		float mae = 0;

		boolean paramProbability = param.probability;
		param.probability = false;

		Map<P, Float> ymv = continuousCrossValidation((ExplicitSvmProblem<Float, P, R>) problem, numberOfFolds);

		param.probability = paramProbability;

		for (Map.Entry<P, Float> entry : ymv.entrySet())
			{
			float newVal = problem.getTargetValue(entry.getKey()) - entry.getValue();
			entry.setValue(newVal);
			mae += Math.abs(newVal);
			}

		mae /= problem.getNumExamples();

		float std = SQRT_2 * mae;
		int count = 0;
		mae = 0;

		for (Map.Entry<P, Float> entry : ymv.entrySet())
			{
			float absVal = Math.abs(entry.getValue());
			if (absVal > 5 * std)
				{
				count = count + 1;
				}
			else
				{
				mae += absVal;
				}
			}
		mae /= (problem.getNumExamples() - count);
		logger.info("Prob. model for test data: target value = predicted value + z");
		logger.info("z: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma=" + mae);
		return mae;
		}

	public abstract RegressionModel<P> train(R problem);
	}
