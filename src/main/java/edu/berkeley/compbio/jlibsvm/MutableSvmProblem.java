package edu.berkeley.compbio.jlibsvm;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface MutableSvmProblem<L extends Comparable, P, R extends SvmProblem<L, P>>
		extends ExplicitSvmProblem<L, P, R>
	{
// -------------------------- OTHER METHODS --------------------------

	void addExample(P point, L label);

	void addExampleFloat(P point, Float x);
	}
