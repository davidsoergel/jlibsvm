package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.MathSupport;
import edu.berkeley.compbio.jlibsvm.Solver_NU;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.SVC_Q;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class Nu_SVC extends BinaryClassificationSVM
	{
	public Nu_SVC(KernelFunction kernel, SvmParameter param)
		{
		super(kernel, param);
		if (param.nu <= 0 || param.nu > 1)
			{
			throw new SvmException("nu <= 0 or nu > 1");
			}
		}

	public String getSvmType()
		{
		return "nu_svc";
		}

	@Override
	public BinaryModel trainOne(BinaryClassificationProblem problem, float Cp, float Cn)
		{	//private static void solve_nu_svc(svm_problem prob, float[] alpha, SolutionInfo si)


		if (!isFeasible(problem))
			{
			throw new SvmException("Nu_SVM is not feasible for this problem");
			}


		//	int i;
		int l = problem.examples.length;
		float nu = param.nu;

		boolean[] y;

		y = MathSupport.toPrimitive(problem.getTargetValues());

		float sumPos = nu * l / 2;
		float sumNeg = nu * l / 2;

		float[] initAlpha = new float[l];

		for (int i = 0; i < l; i++)
			{
			if (y[i])
				{
				initAlpha[i] = Math.min(1.0f, sumPos);
				sumPos -= initAlpha[i];
				}
			else
				{
				initAlpha[i] = Math.min(1.0f, sumNeg);
				sumNeg -= initAlpha[i];
				}
			}

		float[] zeros = new float[l];

		for (int i = 0; i < l; i++)
			{
			zeros[i] = 0;
			}

		Solver_NU s = new Solver_NU(new SVC_Q(problem, kernel, param.cache_size, y), zeros, y, initAlpha, 1.0f, 1.0f,
		                            param.eps, param.shrinking);
		BinaryModel model = s.Solve();
		model.kernel = kernel;
		model.param = param;
		model.setSvmType(getSvmType());
		model.compact();

		float r = model.r;

		System.out.print("C = " + 1 / r + "\n");

		float[] alpha = model.alpha;

		for (int i = 0; i < l; i++)
			{
			alpha[i] *= (y[i] ? 1f : -1f) / r;
			}

		model.rho /= r;
		model.obj /= r * r;
		model.upperBoundPositive = 1 / r;
		model.upperBoundNegative = 1 / r;

		return model;
		}

	public boolean isFeasible(BinaryClassificationProblem problem)
		{
		Map<Boolean, Integer> counts = problem.getExampleCounts();

		int n1 = counts.get(true);
		int n2 = counts.get(false);

		if (param.nu * (n1 + n2) / 2 > Math.min(n1, n2))
			{
			return false; //"specified nu is infeasible";
			}

		return true;
		}
	}
