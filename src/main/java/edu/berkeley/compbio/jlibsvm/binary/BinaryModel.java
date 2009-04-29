package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.ContinuousModel;
import edu.berkeley.compbio.jlibsvm.DiscreteModel;
import edu.berkeley.compbio.jlibsvm.SigmoidProbabilityModel;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BinaryModel<L extends Comparable, P> extends AlphaModel<L, P>
		implements DiscreteModel<L, P>, ContinuousModel<P>
	{
	private static final Logger logger = Logger.getLogger(BinaryModel.class);

	//private cv;

	public CrossValidationResults newCrossValidationResults(int i, int tt, int ft, int tf, int ff)
		{
		return new CrossValidationResults(i, tt, ft, tf, ff);
		//return cv;
		}

	public class CrossValidationResults
		{
		int numExamples;
		int tt, tf, ft, ff;

		public CrossValidationResults(int numExamples, int tt, int tf, int ft, int ff)
			{
			this.numExamples = numExamples;
			this.tt = tt;
			this.tf = tf;
			this.ft = ft;
			this.ff = ff;
			}

		float TrueTrueRate()
			{
			return (float) tt / (float) numExamples;
			}

		float TrueFalseRate()
			{
			return (float) tf / (float) numExamples;
			}

		float FalseTrueRate()
			{
			return (float) ft / (float) numExamples;
			}

		float FalseFalseRate()
			{
			return (float) ff / (float) numExamples;
			}
		}

	public float obj;
	public float upperBoundPositive;
	public float upperBoundNegative;

	public SigmoidProbabilityModel sigmoid;


	public ScalingModel<P> scalingModel = new NoopScalingModel<P>();

	@NotNull
	public ScalingModel<P> getScalingModel()
		{
		return scalingModel;
		}

	public void setScalingModel(@NotNull ScalingModel<P> scalingModel)
		{
		this.scalingModel = scalingModel;
		}

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

	public L getTrueLabel()
		{
		return trueLabel;
		}

	public L getFalseLabel()
		{
		return falseLabel;
		}

	public float getTrueProbability(P x)
		{
		return sigmoid.predict(predictValue(x));  // NPE if no sigmoid
		}

	public float getTrueProbability(float[] kvalues, int[] svIndexMap)
		{
		return sigmoid.predict(predictValue(kvalues, svIndexMap));  // NPE if no sigmoid
		}

	public void printSolutionInfo(BinaryClassificationProblem<L, P> problem)
		{
		if (logger.isDebugEnabled())
			{
			logger.debug("obj = " + obj + ", rho = " + rho);

			// output SVs

			int nBSV = 0;
			for (int i = 0; i < numSVs; i++)
				{
				Double alpha = alphas[i];
				P point = SVs[i];
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

			logger.debug("nSV = " + SVs.length + ", nBSV = " + nBSV);
			}
		}

	public L predictLabel(P x)
		{
		return predictValue(x) > 0 ? trueLabel : falseLabel;
		}

	public Float predictValue(P x)
		{
		float sum = 0;

		P scaledX = scalingModel.scaledCopy(x);

		for (int i = 0; i < numSVs; i++)
			{
			float kvalue = (float) kernel.evaluate(scaledX, SVs[i]);
			sum += alphas[i] * kvalue;
			}

		sum -= rho;
		return sum;
		}


	public L predictLabel(float[] kvalues, int[] svIndexMap)
		{
		return predictValue(kvalues, svIndexMap) > 0 ? trueLabel : falseLabel;
		}

	public Float predictValue(float[] kvalues, int[] svIndexMap)
		{
		float sum = 0;

		for (int i = 0; i < numSVs; i++)
			{
			//float kvalue = (float) kernel.evaluate(x, SVs[i]);
			sum += alphas[i] * kvalues[svIndexMap[i]];
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
