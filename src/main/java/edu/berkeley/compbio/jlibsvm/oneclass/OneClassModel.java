package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.DiscreteModel;
import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.regression.RegressionModel;

import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class OneClassModel extends RegressionModel implements DiscreteModel<Boolean>
	{
	public OneClassModel(BinaryModel binaryModel)
		{
		super(binaryModel);
		}


	public OneClassModel(Properties props)
		{
		super(props);
		}

	//** Hmmm does this make sense?
	public Float predictValue(SvmPoint x)
		{
		return predictLabel(x) ? 1f : -1f;
		}


	public Boolean predictLabel(SvmPoint x)
		{
		return super.predictValue(x) > 0;
		}
	}
