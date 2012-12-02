package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterGrid;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.qmatrix.BooleanInvertingKernelQMatrix;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import edu.berkeley.compbio.jlibsvm.regression.RegressionModel;
import edu.berkeley.compbio.jlibsvm.regression.RegressionSVM;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class OneClassSVC<L extends Comparable, P> extends RegressionSVM<P, OneClassProblem<L, P>>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(OneClassSVC.class);


// -------------------------- OTHER METHODS --------------------------


	public RegressionModel<P> train(OneClassProblem<L, P> problem, @NotNull ImmutableSvmParameter<Float, P> param)
		//, final TreeExecutorService execService)
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


	private RegressionModel<P> trainScaled(OneClassProblem<L, P> problem,
	                                       @NotNull ImmutableSvmParameterPoint<Float, P> param)
		//,final TreeExecutorService execService)
		{
		if (param.scalingModelLearner != null && param.scaleBinaryMachinesIndependently)
			{
			// the examples are copied before scaling, not scaled in place
			// that way we don't need to worry that the same examples are being used in another thread, or scaled differently in different contexts, etc.
			// this may cause memory problems though

			problem = problem.getScaledCopy(param.scalingModelLearner);
			}


		float remainingAlpha = param.nu * problem.getNumExamples();

		float linearTerm = 0f;
		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>();
		int c = 0;
		for (Map.Entry<P, Float> example : problem.getExamples().entrySet())
			{
			float initAlpha = remainingAlpha > 1f ? 1f : remainingAlpha;
			remainingAlpha -= initAlpha;

			SolutionVector<P> sv;

			sv = new SolutionVector<P>(problem.getId(example.getKey()), example.getKey(), true, linearTerm, initAlpha);
			//sv.id = problem.getId(example.getKey());
			c++;
			solutionVectors.add(sv);
			}

		QMatrix<P> qMatrix =
				new BooleanInvertingKernelQMatrix<P>(param.kernel, solutionVectors.size(), param.getCacheRows());
		OneClassSolver<L, P> s = new OneClassSolver<L, P>(solutionVectors, qMatrix, 1.0f, param.eps, param.shrinking);


		OneClassModel<L, P> model = s.solve(); //new RegressionModel<P>(binaryModel);
		//model.kernel = kernel;
		model.param = param;
		model.label = problem.getLabel();
		model.setSvmType(getSvmType());
		model.compact();

		return model;
		}

	public String getSvmType()
		{
		return "one_class_svc";
		}

	public void validateParam(@NotNull ImmutableSvmParameterPoint<Float, P> param)
		{
		super.validateParam(param);

		if (param.C != 1f)
			{
			logger.warn("OneClassSVC ignores param.C, provided value " + param.C + " + not used");
			}
		if (param.probability)
			{
			throw new SvmException("one-class SVM probability output not supported yet");
			}
		if (param.nu <= 0 || param.nu > 1)
			{
			throw new SvmException("nu <= 0 or nu > 1");
			}
		}
	}
