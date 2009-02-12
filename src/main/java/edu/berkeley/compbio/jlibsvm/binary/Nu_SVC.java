package edu.berkeley.compbio.jlibsvm.binary;

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
public class Nu_SVC<L extends Comparable, P> extends BinaryClassificationSVM<L, P>
	{
	private static final Logger logger = Logger.getLogger(Nu_SVC.class);

	public Nu_SVC(KernelFunction<P> kernel, SvmParameter param)
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
	public BinaryModel<L, P> trainOne(BinaryClassificationProblem<L, P> problem, float Cp, float Cn)
		{	//private static void solve_nu_svc(svm_problem prob, float[] alpha, SolutionInfo si)

		if (Cp != 1f || Cn != 1f)
			{
			logger.warn("Nu_SVC ignores Cp and Cn, provided values " + Cp + " and " + Cn + " + not used");
			}

		if (!isFeasible(problem))
			{
			throw new SvmException("Nu_SVM is not feasible for this problem");
			}


		//	int i;
		int l = problem.getExamples().size();
		float nu = param.nu;

		//	boolean[] y;

		//	y = MathSupport.toPrimitive(problem.getTargetValues());

		float sumPos = nu * l / 2;
		float sumNeg = nu * l / 2;

		/*	float[] initAlpha = new float[l];

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
		   }*/
		Map<P, Boolean> examples = problem.getBooleanExamples()
				;		//Map<P, Float> initAlpha = new HashMap<P, Float>();


		float linearTerm = 0f;
		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>();
		int c = 0;
		for (Map.Entry<P, Boolean> entry : examples.entrySet())
			{
			float initAlpha;
			if (entry.getValue())
				{
				initAlpha = Math.min(1.0f, sumPos);
				sumPos -= initAlpha;
				}
			else
				{
				initAlpha = Math.min(1.0f, sumNeg);
				sumNeg -= initAlpha;
				}
			SolutionVector<P> sv = new SolutionVector(entry.getKey(), entry.getValue(), linearTerm, initAlpha);
			sv.id = problem.getId(entry.getKey());

			c++;
			solutionVectors.add(sv);
			}


		BinarySolverNu<L, P> s =
				new BinarySolverNu<L, P>(solutionVectors, qMatrix, 1.0f, 1.0f, param.eps, param.shrinking);


		BinaryModel<L, P> model = s.Solve();
		model.kernel = kernel;
		model.param = param;
		model.trueLabel = problem.getTrueLabel();
		model.falseLabel = problem.getFalseLabel();
		model.setSvmType(getSvmType());

		float r = model.r;

		logger.info("C = " + 1 / r);

		/*	float[] alpha = model.alpha;

	   for (int i = 0; i < l; i++)
		   {
		   alpha[i] *= (y[i] ? 1f : -1f) / r;
		   }*/

		for (Map.Entry<P, Double> entry : model.supportVectors.entrySet())
			{
			entry.setValue((examples.get(entry.getKey()) ? 1. : -1.) / r);
			}


		model.rho /= r;
		model.obj /= r * r;
		model.upperBoundPositive = 1 / r;
		model.upperBoundNegative = 1 / r;

		model.compact();

		return model;
		}

	public boolean isFeasible(BinaryClassificationProblem problem)
		{
		Map<Boolean, Integer> counts = problem.getExampleCounts();

		int n1 = counts.get(problem.getTrueLabel());
		int n2 = counts.get(problem.getFalseLabel());

		if (param.nu * (n1 + n2) / 2 > Math.min(n1, n2))
			{
			return false; //"specified nu is infeasible";
			}

		return true;
		}
	}
