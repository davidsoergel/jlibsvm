package edu.berkeley.compbio.jlibsvm;

/**
 * An SVM solution which can classify unknown points using a set of discrete labels.
 *
 * The generic parameters are L: the label type, and P, the type of the objects to be classified.
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface DiscreteModel<L, P>
	{
// -------------------------- OTHER METHODS --------------------------

	L predictLabel(P x);
	}
