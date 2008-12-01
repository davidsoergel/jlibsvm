package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblem;
import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class RegressionSVM<P> extends SVM<Float, P, RegressionProblem<P>>
	{
	protected RegressionSVM(KernelFunction<P> kernel, SvmParameter<Float> param)
		{
		super(kernel, param);
		}

	public abstract RegressionModel<P> train(RegressionProblem<P> problem);

/*	@Override
	public Class getLabelClass()
		{
		return Float.class;
		}*/


	// Stratified cross validation

	/*
	 protected Float[] foldPredict(RegressionProblem<P> subprob, Iterator<P> foldIterator, int length)
		 {
		 RegressionModel<P> model = train(subprob);
		 Float[] result = new Float[length];

		 int i = 0;
		 while (foldIterator.hasNext())
			 {
			 result[i] = model.predictValue(foldIterator.next());
			 i++;
			 }
		 return result;
		 }
 */
	private final float SQRT_2 = (float) Math.sqrt(2);

	// Return parameter of a Laplace distribution
	protected float laplaceParameter(RegressionProblem<P> problem)
		{
		int i;
		int numberOfFolds = 5;
		float mae = 0;

		boolean paramProbability = param.probability;
		param.probability = false;

		Map<P, Float> ymv =
				continuousCrossValidation((ExplicitSvmProblem<Float, P, RegressionProblem<P>>) problem, numberOfFolds);

		param.probability = paramProbability;

		for (Map.Entry<P, Float> entry : ymv.entrySet())
			{
			float newVal = problem.getTargetValue(entry.getKey()) - entry.getValue();
			entry.setValue(newVal);
			mae += Math.abs(newVal);
			}
		/*for (i = 0; i < problem.getNumExamples(); i++)
			{
			ymv[i] = problem.getTargetValue(i) - ymv[i];
			mae += Math.abs(ymv[i]);
			}*/
		mae /= problem.getExamples().size();
		//float std = (float) Math.sqrt(2 * mae * mae);  // PERF
		float std = SQRT_2 * mae;
		int count = 0;
		mae = 0;

		for (Map.Entry<P, Float> entry : ymv.entrySet())
			{
			//for (i = 0; i < problem.getNumExamples(); i++)
			//	{
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
		mae /= (problem.getExamples().size() - count);
		System.err
				.print("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma="
						+ mae + "\n");
		return mae;
		}
	}
