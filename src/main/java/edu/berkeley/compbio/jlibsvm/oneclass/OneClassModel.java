package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.DiscreteModel;
import edu.berkeley.compbio.jlibsvm.regression.RegressionModel;

import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class OneClassModel<P> extends RegressionModel<P> implements DiscreteModel<Boolean, P>
	{
	/*public OneClassModel(BinaryModel binaryModel)
		{
		super(binaryModel);
		}
*/

	public OneClassModel()
		{
		super();
		}

	public OneClassModel(Properties props)
		{
		super(props);
		}

	//** Hmmm does this make sense?
	public Float predictValue(P x)
		{
		return predictLabel(x) ? 1f : -1f;
		}


	public Boolean predictLabel(P x)
		{
		return super.predictValue(x) > 0;
		}
	}
