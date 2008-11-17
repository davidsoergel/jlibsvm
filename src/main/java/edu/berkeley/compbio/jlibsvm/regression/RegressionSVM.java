package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.util.Iterator;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class RegressionSVM extends SVM<Float, RegressionProblem>
	{
	protected RegressionSVM(KernelFunction kernel, SvmParameter param)
		{
		super(kernel, param);
		}

	public abstract RegressionModel train(RegressionProblem problem);

	@Override
	public Class getLabelClass()
		{
		return Float.class;
		}


	// Stratified cross validation


	protected Float[] foldPredict(RegressionProblem subprob, Iterator<SvmPoint> foldIterator, int length)
		{
		RegressionModel model = train(subprob);
		Float[] result = new Float[length];

		int i = 0;
		while (foldIterator.hasNext())
			{
			result[i] = model.predictValue(foldIterator.next());
			i++;
			}
		return result;
		}

	private final float SQRT_2 = (float) Math.sqrt(2);

	// Return parameter of a Laplace distribution
	protected float laplaceParameter(RegressionProblem problem)
		{
		int i;
		int numberOfFolds = 5;
		float mae = 0;

		boolean paramProbability = param.probability;
		param.probability = false;

		Float[] ymv = crossValidation(problem, numberOfFolds);

		param.probability = paramProbability;

		for (i = 0; i < problem.examples.length; i++)
			{
			ymv[i] = problem.getTargetValue(i) - ymv[i];
			mae += Math.abs(ymv[i]);
			}
		mae /= problem.examples.length;
		//float std = (float) Math.sqrt(2 * mae * mae);  // PERF
		float std = SQRT_2 * mae;
		int count = 0;
		mae = 0;
		for (i = 0; i < problem.examples.length; i++)
			{
			if (Math.abs(ymv[i]) > 5 * std)
				{
				count = count + 1;
				}
			else
				{
				mae += Math.abs(ymv[i]);
				}
			}
		mae /= (problem.examples.length - count);
		System.err
				.print("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma="
						+ mae + "\n");
		return mae;
		}
	}
