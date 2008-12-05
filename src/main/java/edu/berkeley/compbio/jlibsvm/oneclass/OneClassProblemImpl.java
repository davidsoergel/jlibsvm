package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.regression.RegressionProblemImpl;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class OneClassProblemImpl<L, P> extends RegressionProblemImpl<P> implements OneClassProblem<L, P>
	{
	public OneClassProblemImpl(Map<P, Float> examples, L label)  // set<P> examples
		{

		//Map<P, Float> exampleMap = new HashMap<P,Float>(examples.size());

		super(examples);
		this.label = label;
		}

	public OneClassProblemImpl(int numExamples, L label)
		{
		super(numExamples);
		this.label = label;
		//	targetValues = new Float[numExamples];
		}

	L label;

	public L getLabel()
		{
		return label;
		}
	}
