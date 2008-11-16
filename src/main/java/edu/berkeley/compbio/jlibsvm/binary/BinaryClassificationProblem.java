package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.SvmProblem;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BinaryClassificationProblem extends SvmProblem<Boolean, BinaryClassificationProblem>
	{
	public float[] targetValueAsFloat()
		{
		float[] result =  new float[targetValues.length];
		int i = 0;
		for (Boolean targetValue : targetValues)
			{
			result[i] = targetValue? 1f : -1f;
			i++;
			}
		return result;
		}

	public BinaryClassificationProblem(int numExamples)
		{
		super(numExamples);
		targetValues = new Boolean[numExamples];
		}

	public BinaryClassificationProblem newSubProblem(int numExamples)
		{
		return new BinaryClassificationProblem(numExamples);
		}

	Float trueClass = null;
	public void putTargetFloat(int i, Float x)
		{
		if(trueClass == null)
			{
			trueClass = x;
			}
		putTargetValue(i, x.equals(trueClass));
		}
	}
