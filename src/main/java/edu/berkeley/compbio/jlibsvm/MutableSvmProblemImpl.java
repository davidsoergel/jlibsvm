package edu.berkeley.compbio.jlibsvm;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class MutableSvmProblemImpl<L extends Comparable, P, R extends SvmProblem<L, P>>
		extends ExplicitSvmProblemImpl<L, P, R> implements MutableSvmProblem<L, P, R>
	{
	public MutableSvmProblemImpl(int numExamples)
		{
		super();
		examples = new HashMap<P, L>(numExamples);
		exampleList = new HashMap<P, Integer>(numExamples);
		}

	public MutableSvmProblemImpl(Map<P, L> examples)
		{
		super();
		this.examples = examples;
		this.exampleList = new HashMap<P, Integer>();
		for (P point : examples.keySet())
			{
			exampleList.put(point, c);
			c++;
			}
		}

	int c = 0;

	public void addExample(P point, L label)
		{
		examples.put(point, label);
		exampleList.put(point, c);
		c++;
		}
	/*
	public void putTargetValue(int i, L x)
		{
		targetValues[i] = x;
		}
		*/
	}
