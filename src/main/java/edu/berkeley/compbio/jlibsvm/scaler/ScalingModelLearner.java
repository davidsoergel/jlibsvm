package edu.berkeley.compbio.jlibsvm.scaler;

import java.util.Collection;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface ScalingModelLearner<P>
	{
	public ScalingModel<P> learnScaling(Collection<P> examples);
	}
