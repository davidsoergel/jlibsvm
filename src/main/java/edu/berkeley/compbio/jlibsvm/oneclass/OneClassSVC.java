package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.Solver_NU;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.ONE_CLASS_Q;
import edu.berkeley.compbio.jlibsvm.regression.RegressionProblem;
import edu.berkeley.compbio.jlibsvm.regression.RegressionSVM;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class OneClassSVC extends RegressionSVM
	{
	public OneClassSVC(KernelFunction kernel, SvmParameter param)
		{
		super(kernel, param);


		if (param.probability)
			{
			throw new SvmException("one-class SVM probability output not supported yet");
			}
		if (param.nu <= 0 || param.nu > 1)
			{
			throw new SvmException("nu <= 0 or nu > 1");
			}
		}

	public OneClassModel train(RegressionProblem problem)
		{
		int l = problem.examples.length;
		float[] zeros = new float[l];
		boolean[] ones = new boolean[l];
		float[] initAlpha = new float[l];
		int i;

		int n = (int) (param.nu * problem.examples.length);// # of alpha's at upper bound

		for (i = 0; i < n; i++)
			{
			initAlpha[i] = 1;
			}
		if (n < problem.examples.length)
			{
			initAlpha[n] = param.nu * problem.examples.length - n;
			}
		for (i = n + 1; i < l; i++)
			{
			initAlpha[i] = 0;
			}

		for (i = 0; i < l; i++)
			{
			zeros[i] = 0;
			ones[i] = true;
			}

		Solver_NU s =
				new Solver_NU(new ONE_CLASS_Q(problem, kernel, param.cache_size), zeros, ones, initAlpha, 1.0f, 1.0f,
				              param.eps, param.shrinking);

		BinaryModel binaryModel = s.Solve();
		binaryModel.kernel = kernel;
		binaryModel.param = param;
		binaryModel.compact();
		OneClassModel model = new OneClassModel(binaryModel);
		model.setSvmType(getSvmType());
		return model;
		}


	public String getSvmType()
		{
		return "one_class_svc";
		}
	}
