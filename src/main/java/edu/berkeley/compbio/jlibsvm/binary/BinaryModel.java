package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.ContinuousModel;
import edu.berkeley.compbio.jlibsvm.DiscreteModel;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BinaryModel extends AlphaModel implements DiscreteModel<Boolean>, ContinuousModel
	{
	public float obj;
	public float upperBoundPositive;
	public float upperBoundNegative;


	public float r;// for Solver_NU.  I wanted to factor this out as SolutionInfoNu, but that was too much hassle

	public BinaryModel(KernelFunction kernel, SvmParameter param)
		{
		super(kernel, param);
		}

	public BinaryModel(Properties props)
		{
		super(props);
		}

	public void printSolutionInfo(BinaryClassificationProblem problem)
		{
		System.out.print("obj = " + obj + ", rho = " + rho + "\n");

		// output SVs

		int nBSV = 0;
		for (int i = 0; i < alpha.length; i++)
			{
			if (Math.abs(alpha[i]) > 0)
				{
				if (problem.getTargetValue(i))
					{
					if (Math.abs(alpha[i]) >= upperBoundPositive)
						{
						++nBSV;
						}
					}
				else
					{
					if (Math.abs(alpha[i]) >= upperBoundNegative)
						{
						++nBSV;
						}
					}
				}
			}

		System.out.print("nSV = " + alpha.length + ", nBSV = " + nBSV + "\n");
		}

	public Boolean predictLabel(SvmPoint x)
		{
		return predictValue(x) > 0;
		}

	public Float predictValue(SvmPoint x)
		{
		float sum = 0;
		for (int i = 0; i < alpha.length; i++)
			{
			float kvalue = kernel.evaluate(x, supportVectors[i]);
			sum += alpha[i] * kvalue;
			}
		sum -= rho;
		return sum;
		}

	public void writeToStream(DataOutputStream fp) throws IOException
		{
		super.writeToStream(fp);
		//svm_parameter param = model.param;

		//int nr_class = nr_class;
		//int l = model.l;
		fp.writeBytes("nr_class 2\n");
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
	}
