package edu.berkeley.compbio.jlibsvm;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface DiscreteModel<L, P>
	{
// -------------------------- OTHER METHODS --------------------------

	L predictLabel(P x);
	}
