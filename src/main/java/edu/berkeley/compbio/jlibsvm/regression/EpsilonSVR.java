package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.BooleanInvertingKernelQMatrix;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class EpsilonSVR<P> extends RegressionSVM<P, RegressionProblem<P>>
	{
// --------------------------- CONSTRUCTORS ---------------------------

	public EpsilonSVR(KernelFunction<P> kernel, ScalingModelLearner<P> scalingModelLearner, SvmParameter param)
		{
		super(kernel, scalingModelLearner, param);
		if (param.p < 0)
			{
			throw new SvmException("p < 0");
			}
		if (param.C <= 0)
			{
			throw new SvmException("C <= 0");
			}
		}

// -------------------------- OTHER METHODS --------------------------

	public RegressionModel<P> train(RegressionProblem<P> problem)
		{
		float laplaceParameter = RegressionModel.NO_LAPLACE_PARAMETER;
		if (param.probability)
			{
			laplaceParameter = laplaceParameter(problem);
			}

		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>();

		for (Map.Entry<P, Float> example : problem.getExamples().entrySet())
			{
			SolutionVector<P> sv;

			sv = new SolutionVector<P>(example.getKey(), true, param.p - example.getValue());
			sv.id = problem.getId(example.getKey());

			solutionVectors.add(sv);

			sv = new SolutionVector<P>(example.getKey(), false, param.p + example.getValue());
			sv.id = -problem.getId(example.getKey());

			solutionVectors.add(sv);
			}

		QMatrix<P> qMatrix =
				new BooleanInvertingKernelQMatrix<P>(kernel, problem.getNumExamples(), param.getCacheRows());
		RegressionSolver<P> s = new RegressionSolver<P>(solutionVectors, qMatrix, param.C, param.eps, param.shrinking);


		RegressionModel<P> model = s.solve();
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
