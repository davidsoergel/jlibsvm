package edu.berkeley.compbio.jlibsvm.scaler;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface ScalingModel<P>
	{
// -------------------------- OTHER METHODS --------------------------

	P scaledCopy(P example);
	}
