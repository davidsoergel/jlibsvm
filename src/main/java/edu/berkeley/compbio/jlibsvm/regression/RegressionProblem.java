package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmProblem;

import java.util.List;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class RegressionProblem extends SvmProblem<Float, RegressionProblem>
	{
	public List<Float> getLabels()
		{
		throw new SvmException("Shouldn't try to get unique target values for a regression problem");
		}
/*
	public float[] targetValueAsFloat()
		{
		return ArrayUtils.toPrimitive(targetValues);
		}
*/
	public RegressionProblem(int numExamples)
		{
		super(numExamples);
		targetValues = new Float[numExamples];
		}


	public RegressionProblem newSubProblem(int length)
		{
		return new RegressionProblem(length);
		}

	public void putTargetFloat(int i, Float x)
		{
		putTargetValue(i, x);
		}
	}
