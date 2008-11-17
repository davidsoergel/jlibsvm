package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.Solver;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.SVR_Q;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class EpsilonSVR extends RegressionSVM
	{


	public EpsilonSVR(KernelFunction kernel, SvmParameter param)
		{
		super(kernel, param);
		if (param.p < 0)
			{
			throw new SvmException("p < 0");
			}
		if (param.C <= 0)
			{
			throw new SvmException("C <= 0");
			}
		}

	public RegressionModel train(RegressionProblem problem)
		{
		float laplaceParameter = RegressionModel.NO_LAPLACE_PARAMETER;
		if (param.probability)
			{
			laplaceParameter = laplaceParameter(problem);
			}


		int l = problem.examples.length;
		float[] linearTerm = new float[2 * l];
		boolean[] y = new boolean[2 * l];
		int i;

		for (i = 0; i < l; i++)
			{
			linearTerm[i] = param.p - problem.getTargetValue(i);
			y[i] = true;

			linearTerm[i + l] = param.p + problem.getTargetValue(i);
			y[i + l] = false;
			}

		Solver s = new Solver(new SVR_Q(problem, kernel, param.cache_size), linearTerm, y, param.C, param.C, param.eps,
		                      param.shrinking);
		BinaryModel binaryModel = s.Solve();
		binaryModel.kernel = kernel;
		binaryModel.param = param;

		RegressionModel model = new RegressionModel(binaryModel);
		model.setSvmType(getSvmType());
		model.laplaceParameter = laplaceParameter;

		float[] alpha = new float[l];
		float sumAlpha = 0;
		for (i = 0; i < l; i++)
			{
			alpha[i] = model.alpha[i] - model.alpha[i + l];
			sumAlpha += Math.abs(alpha[i]);
			}
		System.out.print("nu = " + sumAlpha / (param.C * l) + "\n");

		model.alpha = alpha;
		model.compact();

		return model;
		}

	public String getSvmType()
		{
		return "epsilon_svr";
		}
	}
