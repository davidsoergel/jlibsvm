package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.SvmProblem;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface RegressionProblem<P, R> extends SvmProblem<Float, P, R>
	{
	R getScaledCopy(ScalingModelLearner<P> scalingModelLearner);
	}

