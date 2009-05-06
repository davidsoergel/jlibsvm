package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.MutableSvmProblem;

import java.util.HashMap;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MutableRegressionProblemImpl<P> extends RegressionProblemImpl<P, MutableRegressionProblemImpl<P>>
		implements MutableSvmProblem<Float, P, MutableRegressionProblemImpl<P>>
	{
// --------------------------- CONSTRUCTORS ---------------------------

	public MutableRegressionProblemImpl(int numExamples)
		{
		super(new HashMap<P, Float>(numExamples), new HashMap<P, Integer>(numExamples));
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface MutableSvmProblem ---------------------

	public void addExample(P point, Float label)
		{
		examples.put(point, label);
		exampleIds.put(point, exampleIds.size());
		}

	public void addExampleFloat(P point, Float x)
		{
		addExample(point, x);
		}
	}
