package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class EpsilonSVR<P> extends RegressionSVM<P>
	{

	public EpsilonSVR(KernelFunction<P> kernel, SvmParameter param)
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


	public RegressionModel<P> train(RegressionProblem<P> problem)
		{
		float laplaceParameter = RegressionModel.NO_LAPLACE_PARAMETER;
		if (param.probability)
			{
			laplaceParameter = laplaceParameter(problem);
			}

/*
		int l = problem.getNumExamples();
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
		                      param.shrinking);*/

		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>();
		int c = 0;
		for (Map.Entry<P, Float> example : problem.getExamples().entrySet())
			{
			SolutionVector<P> sv;

			sv = new SolutionVector<P>(example.getKey(), true, param.p - example.getValue());
			sv.id = problem.getId(example.getKey());
			c++;
			solutionVectors.add(sv);

			sv = new SolutionVector<P>(example.getKey(), false, param.p + example.getValue());
			sv.id = -problem.getId(example.getKey());
			c++;
			solutionVectors.add(sv);
			}

		RegressionSolver<P> s = new RegressionSolver<P>(solutionVectors, qMatrix, param.C, param.eps, param.shrinking);


		RegressionModel<P> model = s.Solve(); //new RegressionModel<P>(binaryModel);
		model.kernel = kernel;
		model.param = param;
		model.setSvmType(getSvmType());
		model.laplaceParameter = laplaceParameter;


		model.compact();

		return model;
		}

	public String getSvmType()
		{
		return "epsilon_svr";
		}
	}
