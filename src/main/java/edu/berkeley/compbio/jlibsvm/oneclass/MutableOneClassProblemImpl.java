package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.MutableSvmProblem;
import edu.berkeley.compbio.jlibsvm.regression.RegressionProblem;

import java.util.HashMap;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MutableOneClassProblemImpl<L, P> extends OneClassProblemImpl<L, P>
		implements MutableSvmProblem<Float, P, RegressionProblem<P>>
	{
	public MutableOneClassProblemImpl(int numExamples, L label)
		{
		super(new HashMap<P, Float>(numExamples), new HashMap<P, Integer>(numExamples), label);

		//	targetValues = new Float[numExamples];
		}

	public void addExample(P point, Float label)
		{
		examples.put(point, label);
		exampleIds.put(point, exampleIds.size());
		}

	public void addExampleFloat(P point, Float x)
		{
		addExample(point, x);
		//putTargetValue(i, x);
		}
	}
