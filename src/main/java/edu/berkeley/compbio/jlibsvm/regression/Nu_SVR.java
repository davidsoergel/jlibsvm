package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.Solver_NU;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.SVR_Q;
import edu.berkeley.compbio.jlibsvm.SvmParameter;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class Nu_SVR extends RegressionSVM
	{
	public Nu_SVR(KernelFunction kernel, SvmParameter param)
		{
		super(kernel, param);
		if (param.nu <= 0 || param.nu > 1)
			{
			throw new SvmException("nu <= 0 or nu > 1");
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

		//private static void solve_nu_svr(svm_problem prob, svm_parameter param, KernelFunction kernel, int cache_size, SolutionInfoNu si) // , float[] alpha,
		//	{
		int l = problem.examples.length;
		float C = param.C;
		float[] initAlpha = new float[2 * l];
		float[] linearTerm = new float[2 * l];
		boolean[] y = new boolean[2 * l];
		int i;

		float sum = C * param.nu * l / 2;
		for (i = 0; i < l; i++)
			{
			initAlpha[i] = initAlpha[i + l] = Math.min(sum, C);
			sum -= initAlpha[i];

			linearTerm[i] = -problem.getTargetValue(i);
			y[i] = true;

			linearTerm[i + l] = problem.getTargetValue(i);
			y[i + l] = false;
			}

		Solver_NU s =
				new Solver_NU(new SVR_Q(problem, kernel, param.cache_size), linearTerm, y, initAlpha, C, C, param.eps,
				              param.shrinking);

		BinaryModel binaryModel = s.Solve();
		binaryModel.kernel = kernel;
		binaryModel.param = param;

		System.out.print("epsilon = " + (-binaryModel.r) + "\n");

		RegressionModel model = new RegressionModel(binaryModel);
		model.setSvmType(getSvmType());
		model.laplaceParameter = laplaceParameter;

		float[] alpha = new float[l];
		for (i = 0; i < l; i++)
			{
			alpha[i] = model.alpha[i] - model.alpha[i + l];
			}
		model.alpha = alpha;
		model.compact();

		return model;
		}

	public String getSvmType()
		{
		return "nu_svr";
		}
	}
