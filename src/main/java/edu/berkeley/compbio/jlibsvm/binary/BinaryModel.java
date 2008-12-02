package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.ContinuousModel;
import edu.berkeley.compbio.jlibsvm.DiscreteModel;
import edu.berkeley.compbio.jlibsvm.SigmoidProbabilityModel;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import org.apache.log4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BinaryModel<L extends Comparable, P> extends AlphaModel<L, P>
		implements DiscreteModel<L, P>, ContinuousModel<P>
	{
	private static final Logger logger = Logger.getLogger(BinaryModel.class);

	public float obj;
	public float upperBoundPositive;
	public float upperBoundNegative;

	public SigmoidProbabilityModel sigmoid;

	L trueLabel;
	L falseLabel;

	public float r;// for Solver_NU.  I wanted to factor this out as SolutionInfoNu, but that was too much hassle

	public BinaryModel(KernelFunction<P> kernel, SvmParameter<L> param)
		{
		super(kernel, param);
		}

	public BinaryModel(Properties props)
		{
		super(props);
		}

	public void printSolutionInfo(BinaryClassificationProblem<L, P> problem)
		{
		logger.info("obj = " + obj + ", rho = " + rho);

		// output SVs

		int nBSV = 0;
		for (Map.Entry<P, Double> entry : supportVectors.entrySet())
			{
			Double alpha = entry.getValue();
			P point = entry.getKey();
			if (Math.abs(alpha) > 0)
				{
				if (problem.getTargetValue(point).equals(trueLabel))
					{
					if (Math.abs(alpha) >= upperBoundPositive)
						{
						++nBSV;
						}
					}
				else
					{
					if (Math.abs(alpha) >= upperBoundNegative)
						{
						++nBSV;
						}
					}
				}
			}

		logger.info("nSV = " + supportVectors.size() + ", nBSV = " + nBSV);
		}

	public L predictLabel(P x)
		{
		return predictValue(x) > 0 ? trueLabel : falseLabel;
		}

	public Float predictValue(P x)
		{
		float sum = 0;

		for (Map.Entry<P, Double> entry : supportVectors.entrySet())
			{
			float kvalue = (float) kernel.evaluate(x, entry.getKey());
			sum += entry.getValue() * kvalue;
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

	public float getSumAlpha()
		{
		float result = 0;
		for (Double aFloat : supportVectors.values())
			{
			result += aFloat;
			}
		return result;
		}
	}
