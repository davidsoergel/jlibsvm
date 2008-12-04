package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.regression.RegressionProblemImpl;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class OneClassProblemImpl<L, P> extends RegressionProblemImpl<P> implements OneClassProblem<L, P>
	{
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
