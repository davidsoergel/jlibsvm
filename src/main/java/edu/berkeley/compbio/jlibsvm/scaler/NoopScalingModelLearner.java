package edu.berkeley.compbio.jlibsvm.scaler;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class NoopScalingModelLearner<P> implements ScalingModelLearner<P>
	{
	// default implementation returns the identity ScalingModel; override for interesting behavior

	public ScalingModel<P> learnScaling(Iterable<P> examples)
		{
		return new NoopScalingModel<P>();
		}
	}
