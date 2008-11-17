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

		fp.writeBytes("nr_class 2\n");

		//these must come after everything else
		writeSupportVectors(fp);

		fp.close();
		}
	}
