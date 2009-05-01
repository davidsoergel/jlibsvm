package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.BooleanInvertingKernelQMatrix;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import edu.berkeley.compbio.jlibsvm.regression.RegressionSVM;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class OneClassSVC<L, P> extends RegressionSVM<P, OneClassProblem<L, P>>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(OneClassSVC.class);


// --------------------------- CONSTRUCTORS ---------------------------

	public OneClassSVC(KernelFunction<P> kernel, ScalingModelLearner<P> scalingModelLearner, SvmParameter param)
		{
		super(kernel, scalingModelLearner, param);


		if (param.probability)
			{
			throw new SvmException("one-class SVM probability output not supported yet");
			}
		if (param.nu <= 0 || param.nu > 1)
			{
			throw new SvmException("nu <= 0 or nu > 1");
			}
		}

// -------------------------- OTHER METHODS --------------------------

	public OneClassModel<L, P> train(OneClassProblem<L, P> problem)
		{
		if (param.C != 1f)
			{
			logger.warn("OneClassSVC ignores param.C, provided value " + param.C + " + not used");
			}

		float remainingAlpha = param.nu * problem.getNumExamples();
		;
		float linearTerm = 0f;
		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>();
		int c = 0;
		for (Map.Entry<P, Float> example : problem.getExamples().entrySet())
			{
			float initAlpha = remainingAlpha > 1f ? 1f : remainingAlpha;
			remainingAlpha -= initAlpha;

			SolutionVector<P> sv;

			sv = new SolutionVector<P>(example.getKey(), true, linearTerm, initAlpha);
			sv.id = problem.getId(example.getKey());
			c++;
			solutionVectors.add(sv);
			}

		QMatrix<P> qMatrix =
				new BooleanInvertingKernelQMatrix<P>(kernel, problem.getNumExamples(), param.getCacheRows());
		OneClassSolver<L, P> s = new OneClassSolver<L, P>(solutionVectors, qMatrix, 1.0f, param.eps, param.shrinking);


		OneClassModel<L, P> model = s.solve(); //new RegressionModel<P>(binaryModel);
		model.kernel = kernel;
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
	}
