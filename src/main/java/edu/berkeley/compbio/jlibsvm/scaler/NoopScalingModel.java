package edu.berkeley.compbio.jlibsvm.scaler;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class NoopScalingModel<P> implements ScalingModel<P>
	{
// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ScalingModel ---------------------

	/**
	 * default implementation just returns the original object, but an overriding implementation that actually changes
	 * something should return a copy
	 *
	 * @param example
	 * @return
	 */
	public P scaledCopy(P example)
		{
		return example;
		}
	}
