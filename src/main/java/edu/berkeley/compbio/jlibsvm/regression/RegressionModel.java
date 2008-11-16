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
		//float[] sv_coef = sv_coef[0];
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
		//svm_parameter param = model.param;

		//int nr_class = nr_class;
		//int l = model.l;
		fp.writeBytes("probA " + laplaceParameter + "\n");
/*
		if (probA != null)// regression has probA only
			{
			fp.writeBytes("probA");
			for (int i = 0; i < nr_class * (nr_class - 1) / 2; i++)
				{
				fp.writeBytes(" " + model.probA[i]);
				}
			fp.writeBytes("\n");
			}
		if (probB != null)
			{
			fp.writeBytes("probB");
			for (int i = 0; i < nr_class * (nr_class - 1) / 2; i++)
				{
				fp.writeBytes(" " + model.probB[i]);
				}
			fp.writeBytes("\n");
			}

		if (nSV != null)
			{
			fp.writeBytes("nr_sv");
			for (int i = 0; i < nr_class; i++)
				{
				fp.writeBytes(" " + model.nSV[i]);
				}
			fp.writeBytes("\n");
			}
*/
		//these must come after everything else
		writeSupportVectors(fp);

		/*
		  float[][] sv_coef = model.sv_coef;
		  svm_node[][] SV = model.SV;

		  for (int i = 0; i < l; i++)
			  {
			  for (int j = 0; j < nr_class - 1; j++)
				  {
				  fp.writeBytes(sv_coef[j][i] + " ");
				  }

			  svm_node[] p = SV[i];
			  if (param.kernel_type == svm_parameter.PRECOMPUTED)
				  {
				  fp.writeBytes("0:" + (int) (p[0].value));
				  }
			  else
				  {
				  for (int j = 0; j < p.length; j++)
					  {
					  fp.writeBytes(p[j].index + ":" + p[j].value + " ");
					  }
				  }
			  fp.writeBytes("\n");
			  }
  */
		fp.close();
		}


	public boolean supportsLaplace()
		{
		return laplaceParameter != NO_LAPLACE_PARAMETER;
		}

	}
