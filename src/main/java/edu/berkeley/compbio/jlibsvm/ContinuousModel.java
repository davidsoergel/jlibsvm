package edu.berkeley.compbio.jlibsvm;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface ContinuousModel<P>
	{
// -------------------------- OTHER METHODS --------------------------

	Float predictValue(P x);
	}
