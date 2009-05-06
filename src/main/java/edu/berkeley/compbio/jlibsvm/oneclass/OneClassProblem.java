package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.regression.RegressionProblem;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface OneClassProblem<L, P> extends RegressionProblem<P, OneClassProblem<L, P>>
	{
// -------------------------- OTHER METHODS --------------------------

	L getLabel();

	OneClassProblem<L, P> getScaledCopy(ScalingModelLearner<P> scalingModelLearner);
	}
