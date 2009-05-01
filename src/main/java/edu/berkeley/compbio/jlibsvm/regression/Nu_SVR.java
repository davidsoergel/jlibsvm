package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.BooleanInvertingKernelQMatrix;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
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
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(Nu_SVR.class);


// --------------------------- CONSTRUCTORS ---------------------------

	public Nu_SVR(KernelFunction<P> kernel, ScalingModelLearner<P> scalingModelLearner, SvmParameter param)
		{
		super(kernel, scalingModelLearner, param);
		if (param.nu <= 0 || param.nu > 1)
			{
			throw new SvmException("nu <= 0 or nu > 1");
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

		float sum = param.C * param.nu * problem.getNumExamples() / 2f;

		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>();

		for (Map.Entry<P, Float> example : problem.getExamples().entrySet())
			{
			float initAlpha = Math.min(sum, param.C);
			sum -= initAlpha;

			SolutionVector<P> sv;

			sv = new SolutionVector<P>(example.getKey(), true, -example.getValue(), initAlpha);
			solutionVectors.add(sv);
			sv.id = problem.getId(example.getKey());

			sv = new SolutionVector<P>(example.getKey(), false, example.getValue(), initAlpha);
			solutionVectors.add(sv);
			sv.id = -problem.getId(example.getKey());
			}

		QMatrix<P> qMatrix =
				new BooleanInvertingKernelQMatrix<P>(kernel, problem.getNumExamples(), param.getCacheRows());
		RegressionSolverNu<P> s =
				new RegressionSolverNu<P>(solutionVectors, qMatrix, param.C, param.eps, param.shrinking);


		RegressionModel<P> model = s.solve(); //new RegressionModel<P>(binaryModel);
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
