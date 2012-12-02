package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterGrid;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.qmatrix.BooleanInvertingKernelQMatrix;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class Nu_SVR<P, R extends RegressionProblem<P, R>> extends RegressionSVM<P, R>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(Nu_SVR.class);


// -------------------------- OTHER METHODS --------------------------

	public RegressionModel<P> train(R problem, @NotNull ImmutableSvmParameter<Float, P> param)
		//,final TreeExecutorService execService)
		{
		validateParam(param);
		RegressionModel<P> result;
		if (param instanceof ImmutableSvmParameterGrid && param.gridsearchBinaryMachinesIndependently)
			{
			throw new SvmException(
					"Can't do grid search without cross-validation, which is not implemented for regression SVMs.");
			}
		else
			{
			result = trainScaled(problem, (ImmutableSvmParameterPoint<Float, P>) param);//, execService);
			}
		return result;
		}


	private RegressionModel<P> trainScaled(R problem, @NotNull ImmutableSvmParameterPoint<Float, P> param)
		//, final TreeExecutorService execService)
		{
		if (param.scalingModelLearner != null && param.scaleBinaryMachinesIndependently)
			{
			// the examples are copied before scaling, not scaled in place
			// that way we don't need to worry that the same examples are being used in another thread, or scaled differently in different contexts, etc.
			// this may cause memory problems though

			problem = problem.getScaledCopy(param.scalingModelLearner);
			}

		float laplaceParameter = RegressionModel.NO_LAPLACE_PARAMETER;
		if (param.probability)
			{
			laplaceParameter = laplaceParameter(problem, param);//, execService);
			}

		float sum = param.C * param.nu * problem.getNumExamples() / 2f;

		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>();

		for (Map.Entry<P, Float> example : problem.getExamples().entrySet())
			{
			float initAlpha = Math.min(sum, param.C);
			sum -= initAlpha;

			SolutionVector<P> sv;

			sv = new SolutionVector<P>(problem.getId(example.getKey()), example.getKey(), true, -example.getValue(),
			                           initAlpha);
			solutionVectors.add(sv);
			//sv.id = problem.getId(example.getKey());

			sv = new SolutionVector<P>(-problem.getId(example.getKey()), example.getKey(), false, example.getValue(),
			                           initAlpha);
			solutionVectors.add(sv);
			//sv.id = -problem.getId(example.getKey());
			}

		QMatrix<P> qMatrix =
				new BooleanInvertingKernelQMatrix<P>(param.kernel, solutionVectors.size(), param.getCacheRows());
		RegressionSolverNu<P> s =
				new RegressionSolverNu<P>(solutionVectors, qMatrix, param.C, param.eps, param.shrinking);


		RegressionModel<P> model = s.solve(); //new RegressionModel<P>(binaryModel);
		//	model.kernel = kernel;
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

	public void validateParam(@NotNull ImmutableSvmParameterPoint<Float, P> param)
		{
		super.validateParam(param);
		if (param.nu <= 0 || param.nu > 1)
			{
			throw new SvmException("nu <= 0 or nu > 1");
			}
		if (param.C <= 0)
			{
			throw new SvmException("C <= 0");
			}
		}
	}
