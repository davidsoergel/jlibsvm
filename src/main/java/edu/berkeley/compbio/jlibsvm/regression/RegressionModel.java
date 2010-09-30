package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.ContinuousModel;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.binary.AlphaModel;
import edu.berkeley.compbio.ml.CrossValidationResults;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class RegressionModel<P> extends AlphaModel<Float, P> implements ContinuousModel<P>
	{
	public ImmutableSvmParameterPoint<Float, P> param;
// ------------------------------ FIELDS ------------------------------

	public static final float NO_LAPLACE_PARAMETER = -1;

	public float laplaceParameter = NO_LAPLACE_PARAMETER;

	public float r;// for Solver_NU.  I wanted to factor this out as SolutionInfoNu, but that was too much hassle

	public Collection<Float> getLabels()
		{
		return param.getLabels();
		}

	@Override
	public String getKernelName()
		{
		return param.kernel.toString();
		}


	public RegressionCrossValidationResults crossValidationResults;

	public CrossValidationResults getCrossValidationResults()
		{
		return crossValidationResults;
		}
// --------------------------- CONSTRUCTORS ---------------------------

	public RegressionModel()
		{
		super();
		}
/*
	public RegressionModel(Properties props, LabelParser<Float> labelParser)
		{
		super(props, labelParser);
		laplaceParameter = Float.parseFloat(props.getProperty("laplace"));
		}
*/

// --------------------- GETTER / SETTER METHODS ---------------------

	public float getLaplaceParameter()
		{
		if (laplaceParameter == NO_LAPLACE_PARAMETER)
			{
			throw new SvmException("Model doesn't contain information for SVR probability inference\n");
			}
		return laplaceParameter;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ContinuousModel ---------------------

	public Float predictValue(P x)
		{
		float sum = 0;
		for (int i = 0; i < numSVs; i++)
			{
			sum += alphas[i] * param.kernel.evaluate(x, SVs[i]);
			}
		sum -= rho;
		return sum;
		}

// -------------------------- OTHER METHODS --------------------------

	public boolean supportsLaplace()
		{
		return laplaceParameter != NO_LAPLACE_PARAMETER;
		}

	public void writeToStream(DataOutputStream fp) throws IOException
		{
		super.writeToStream(fp);
		fp.writeBytes("probA " + laplaceParameter + "\n");

		//these must come after everything else
		writeSupportVectors(fp);

		fp.close();
		}
	}
