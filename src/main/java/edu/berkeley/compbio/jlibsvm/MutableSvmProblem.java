package edu.berkeley.compbio.jlibsvm;

/**
 * An SVM problem to which training examples may be added.
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface MutableSvmProblem<L extends Comparable, P, R extends SvmProblem<L, P, R>>
		extends ExplicitSvmProblem<L, P, R>
	{
// -------------------------- OTHER METHODS --------------------------

	void addExample(P point, L label);

	void addExampleFloat(P point, Float x);
	}
