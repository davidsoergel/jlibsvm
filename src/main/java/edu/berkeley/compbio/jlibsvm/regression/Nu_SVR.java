package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class Nu_SVR<P> extends RegressionSVM<P, RegressionProblem<P>>
	{
	private static final Logger logger = Logger.getLogger(Nu_SVR.class);

	public Nu_SVR(KernelFunction<P> kernel, SvmParameter param)
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

	public RegressionModel<P> train(RegressionProblem<P> problem)
		{
		float laplaceParameter = RegressionModel.NO_LAPLACE_PARAMETER;
		if (param.probability)
			{
			laplaceParameter = laplaceParameter(problem);
			}/*
		int l = problem.getNumExamples();
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
*/
		float sum = param.C * param.nu * problem.getExamples().size() / 2f;

		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>();
		int c = 0;
		for (Map.Entry<P, Float> example : problem.getExamples().entrySet())
			{
			float initAlpha = Math.min(sum, param.C);
			sum -= initAlpha;

			SolutionVector<P> sv;

			sv = new SolutionVector<P>(example.getKey(), true, -example.getValue(), initAlpha);
			solutionVectors.add(sv);
			sv.id = problem.getId(example.getKey());			//sv.id = c;
			c++;
			sv = new SolutionVector<P>(example.getKey(), false, example.getValue(), initAlpha);
			solutionVectors.add(sv);
			sv.id = -problem.getId(example.getKey());			//sv.id = c;
			c++;
			}

		RegressionSolverNu<P> s =
				new RegressionSolverNu<P>(solutionVectors, qMatrix, param.C, param.eps, param.shrinking);


		RegressionModel<P> model = s.Solve(); //new RegressionModel<P>(binaryModel);
		model.kernel = kernel;
		model.param = param;
		model.setSvmType(getSvmType());
		model.laplaceParameter = laplaceParameter;


		logger.info("epsilon = " + (-model.r));

		model.compact();

		return model;
		}

	public String getSvmType()
		{
		return "nu_svr";
		}
	}
