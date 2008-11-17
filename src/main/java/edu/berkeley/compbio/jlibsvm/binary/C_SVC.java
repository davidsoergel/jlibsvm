package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.MathSupport;
import edu.berkeley.compbio.jlibsvm.Solver;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.SVC_Q;

import java.util.Arrays;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class C_SVC extends BinaryClassificationSVM
	{
	public C_SVC(KernelFunction kernel, SvmParameter param)
		{
		super(kernel, param);
		if (param.C <= 0)
			{
			throw new SvmException("C <= 0");
			}
		}

	@Override
	public BinaryModel trainOne(BinaryClassificationProblem problem, float Cp, float Cn)
		{
		int l = problem.examples.length;
		float[] minusOnes = new float[l];
		boolean[] y;

		Arrays.fill(minusOnes, -1);

		y = MathSupport.toPrimitive(problem.getTargetValues());

		Solver s = new Solver(new SVC_Q(problem, kernel, param.cache_size, y), minusOnes, y, Cp, Cn, param.eps,
		                      param.shrinking);

		BinaryModel model = s.Solve();
		model.kernel = kernel;
		model.param = param;
		model.setSvmType(getSvmType());


		float[] alpha = model.alpha;

		float sumAlpha = 0;
		for (int i = 0; i < alpha.length; i++)
			{
			sumAlpha += alpha[i];
			}

		if (Cp == Cn)
			{
			System.out.print("nu = " + sumAlpha / (Cp * problem.examples.length) + "\n");
			}

		for (int i = 0; i < alpha.length; i++)
			{
			if (!y[i])
				{
				alpha[i] *= -1;
				}
			}

		model.compact();

		return model;
		}

	public String getSvmType()
		{
		return "c_svc";
		}
	}
