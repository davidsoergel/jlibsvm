package edu.berkeley.compbio.jlibsvm.scaler;

import java.util.Collection;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class NoopScalingModelLearner<P> implements ScalingModelLearner<P>
	{
	// default implementation returns the identity ScalingModel; override for interesting behavior

	public ScalingModel<P> learnScaling(Collection<P> examples)
		{
		return new NoopScalingModel<P>();
		}
	}
