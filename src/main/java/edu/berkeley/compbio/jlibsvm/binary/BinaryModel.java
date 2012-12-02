package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.ContinuousModel;
import edu.berkeley.compbio.jlibsvm.DiscreteModel;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.LabelParser;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.kernel.LinearKernel;
import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BinaryModel<L extends Comparable, P> extends AlphaModel<L, P>
		implements DiscreteModel<L, P>, ContinuousModel<P>
	{
	// protected final would be nice, but the Solver constructs the Model without knowing about param so we have to set it afterwards.
	/**
	 * a thing that is confusing here: if a grid search was done, then the specific point that was the optimum should be
	 * recorded here.  That works for binary and multiclass models when the grid search is done at the top level.  But when
	 * param.gridsearchBinaryMachinesIndependently, there is no one point that makes sense.  Really we should just leave it
	 * null and refer to the subsidiary BinaryModels.
	 */
	public ImmutableSvmParameterPoint<L, P> param;

// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(BinaryModel.class);

	public float obj;
	public float upperBoundPositive;
	public float upperBoundNegative;

	//public SigmoidProbabilityModel sigmoid;


	public ScalingModel<P> scalingModel = new NoopScalingModel<P>();

	public float r;// for Solver_NU.  I wanted to factor this out as SolutionInfoNu, but that was too much hassle
	public SvmBinaryCrossValidationResults<L, P> crossValidationResults;

	public SvmBinaryCrossValidationResults<L, P> getCrossValidationResults()
		{
		return crossValidationResults;
		}

	L trueLabel;
	L falseLabel;


	public Collection<L> getLabels()
		{
		return param.getLabels();
		}

	@Override
	public String getKernelName()
		{
		return param.kernel.toString();
		}

	// --------------------------- CONSTRUCTORS ---------------------------

	/*(public BinaryModel(Properties props, LabelParser<L> labelParser)
		{
		super(props, labelParser);
		}
*/

	public BinaryModel()
		{
		super();
		}

/*	public BinaryModel(Properties props)
			{
			//super(null); //null, null);

			ImmutableSvmParameterPoint.Builder<L, P> builder = new ImmutableSvmParameterPoint.Builder<L, P>();
			try
				{
				//** test hack
				builder.kernel = (KernelFunction<P>) new LinearKernel(); //props);

			//	builder.kernel =
			//			(KernelFunction) Class.forName(props.getProperty("kernel_type")).getConstructor(Properties.class)
			//					.newInstance(props);
				}
			catch (Throwable e)
				{
				throw new SvmException(e);
				}

			param = builder.build();
			}*/

	public BinaryModel(Properties props, LabelParser<L> labelParser)
		{
		//super(null); //null, null);

		ImmutableSvmParameterPoint.Builder<L, P> builder = new ImmutableSvmParameterPoint.Builder<L, P>();
		try
			{
			//BAD test hack
			//builder.kernel = (KernelFunction<P>) new LinearKernel(); //props);
			builder.kernel =
					(KernelFunction) Class.forName(props.getProperty("kernel_type")).getConstructor(Properties.class)
							.newInstance(props);
			}
		catch (Throwable e)
			{
			throw new SvmException(e);
			}

		// param is only useful for training; when loading a trained model for testing, we can leave it null
		//param = new SvmParameter(props);

		// ... oops, except that we need the labels.

		//param = new ImmutableSvmParameter();

		StringTokenizer st = new StringTokenizer(props.getProperty("label"));
		while (st.hasMoreTokens())
			{
			builder.putWeight(labelParser.parse(st.nextToken()), null);
			}

		rho = Float.parseFloat(props.getProperty("rho"));
		numSVs = Integer.parseInt(props.getProperty("total_sv"));
//** test hack

		trueLabel = (L) (Object) "true";
		falseLabel = (L) (Object) "false";
		param = builder.build();
		}

	public BinaryModel(ImmutableSvmParameterPoint<L, P> param)
		{
		//super(param);
		this.param = param;
		}

/*	public BinaryModel(@NotNull ImmutableSvmParameter<L, P> param)
		{
		super(param);
		}
*/
// --------------------- GETTER / SETTER METHODS ---------------------

	public L getFalseLabel()
		{
		return falseLabel;
		}

	@NotNull
	public ScalingModel<P> getScalingModel()
		{
		return scalingModel;
		}

	public void setScalingModel(@NotNull ScalingModel<P> scalingModel)
		{
		this.scalingModel = scalingModel;
		}

	public L getTrueLabel()
		{
		return trueLabel;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DiscreteModel ---------------------

	public L predictLabel(P x)
		{
		return predictValue(x) > 0 ? trueLabel : falseLabel;
		}

// -------------------------- OTHER METHODS --------------------------

	public float getSumAlpha()
		{
		float result = 0;
		for (Double aFloat : supportVectors.values())
			{
			result += aFloat;
			}
		return result;
		}

	public float getTrueProbability(P x)
		{
		return crossValidationResults.sigmoid.predict(predictValue(x));  // NPE if no sigmoid
		}

	public float getProbability(P x, L l)
		{
		if (l.equals(trueLabel))
			{
			return getTrueProbability(x);
			}
		else if (l.equals(falseLabel))
			{
			return 1f - getTrueProbability(x);
			}
		else
			{
			throw new SvmException(
					"Can't compute probability: " + l + " is not one of the classes in this binary model (" + trueLabel
					+ ", " + falseLabel + ")");
			}
		}

	public Float predictValue(P x)
		{
		float sum = 0;

		P scaledX = scalingModel.scaledCopy(x);

		for (int i = 0; i < numSVs; i++)
			{
			float kvalue = (float) param.kernel.evaluate(scaledX, SVs[i]);
			sum += alphas[i] * kvalue;
			}

		sum -= rho;
		return sum;
		}

	public float getTrueProbability(float[] kvalues, int[] svIndexMap)
		{
		float pv = predictValue(kvalues, svIndexMap);
		if (crossValidationResults == null)
			{
			logger.error("Can't compute probability in binary model without crossvalidationresults");
			return pv > 0. ? 1f : 0f;
			}
		else if (crossValidationResults.sigmoid == null)
			{
			logger.error("Can't compute probability in binary model without sigmoid");
			return pv > 0. ? 1f : 0f;
			}
		else
			{
			return crossValidationResults.sigmoid.predict(pv);  // NPE if no sigmoid
			}
		}

	public Float predictValue(float[] kvalues, int[] svIndexMap)
		{
		float sum = 0;

		for (int i = 0; i < numSVs; i++)
			{
			sum += alphas[i] * kvalues[svIndexMap[i]];
			}

		sum -= rho;
		return sum;
		}

	/*public CrossValidationResults newCrossValidationResults(int i, int tt, int ft, int tf, int ff)
		{
		return new CrossValidationResults(i, tt, ft, tf, ff);
		}
*/

	public L predictLabel(float[] kvalues, int[] svIndexMap)
		{
		return predictValue(kvalues, svIndexMap) > 0 ? trueLabel : falseLabel;
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

	public void writeToStream(DataOutputStream fp) throws IOException
		{
		super.writeToStream(fp);

		fp.writeBytes("nr_class 2\n");

		//these must come after everything else
		writeSupportVectors(fp);

		fp.close();
		}
	}
