package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.SVM;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class RegressionSVM<P, R extends RegressionProblem<P, R>> extends SVM<Float, P, R>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(RegressionSVM.class);

	private final float SQRT_2 = (float) Math.sqrt(2);


// -------------------------- OTHER METHODS --------------------------

	// Return parameter of a Laplace distribution

	protected float laplaceParameter(RegressionProblem<P, R> problem, @NotNull ImmutableSvmParameter<Float, P> param)
		//,   final TreeExecutorService execService)
		{
		int i;
		float mae = 0;

		Map<P, Float> ymv = continuousCrossValidation(problem, param); //, execService);


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

	public abstract RegressionModel<P> train(R problem, @NotNull ImmutableSvmParameter<Float, P> param);
	//,final TreeExecutorService execService);

	@Override
	public void validateParam(@NotNull ImmutableSvmParameter<Float, P> param)
		{
		super.validateParam(param);
		}


	public RegressionCrossValidationResults<P, R> performCrossValidation(R problem,
	                                                                     @NotNull ImmutableSvmParameter<Float, P> param)
		//,final TreeExecutorService execService)
		{
		Map<P, Float> decisionValues = continuousCrossValidation(problem, param); //, execService);

		RegressionCrossValidationResults<P, R> cv = new RegressionCrossValidationResults<P, R>(problem, decisionValues);
		return cv;
		}
	}
