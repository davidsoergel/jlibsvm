package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.ContinuousModel;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.binary.AlphaModel;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class RegressionModel extends AlphaModel implements ContinuousModel
	{
	public static final float NO_LAPLACE_PARAMETER = -1;

	public float laplaceParameter = NO_LAPLACE_PARAMETER;

	public RegressionModel(BinaryModel binaryModel)
		{
		super(binaryModel);
		}

	public RegressionModel(Properties props)
		{
		super(props);
		laplaceParameter = Float.parseFloat(props.getProperty("laplace"));
		}

	public Float predictValue(SvmPoint x)
		{
		float sum = 0;
		for (int i = 0; i < alpha.length; i++)
			{
			sum += alpha[i] * kernel.evaluate(x, supportVectors[i]);
			}
		sum -= rho;
		return sum;
		}


	public float getLaplaceParameter()
		{
		if (laplaceParameter == NO_LAPLACE_PARAMETER)
			{
			throw new SvmException("Model doesn't contain information for SVR probability inference\n");
			}
		return laplaceParameter;
		}

	public void writeToStream(DataOutputStream fp) throws IOException
		{
		super.writeToStream(fp);
		fp.writeBytes("probA " + laplaceParameter + "\n");

		//these must come after everything else
		writeSupportVectors(fp);

		fp.close();
		}


	public boolean supportsLaplace()
		{
		return laplaceParameter != NO_LAPLACE_PARAMETER;
		}
	}
