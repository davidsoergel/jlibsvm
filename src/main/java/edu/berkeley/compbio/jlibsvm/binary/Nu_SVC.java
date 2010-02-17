package edu.berkeley.compbio.jlibsvm.binary;

import com.google.common.collect.Multiset;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
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
public class Nu_SVC<L extends Comparable, P> extends BinaryClassificationSVM<L, P>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(Nu_SVC.class);


// -------------------------- OTHER METHODS --------------------------

	@Override
	public BinaryModel<L, P> trainOne(BinaryClassificationProblem<L, P> problem, float Cp, float Cn,
	                                  @NotNull ImmutableSvmParameterPoint<L, P> param)
		{
		if (Cp != 1f || Cn != 1f)
			{
			logger.warn("Nu_SVC ignores Cp and Cn, provided values " + Cp + " and " + Cn + " + not used");
			}

		if (!isFeasible(problem, param))
			{
			throw new SvmException("Nu_SVM is not feasible for this problem");
			}


		int l = problem.getNumExamples();

		float nu = param.nu;

		float sumPos = nu * l / 2;
		float sumNeg = nu * l / 2;


		Map<P, Boolean> examples = problem.getBooleanExamples();

		float linearTerm = 0f;
		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>();

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
			SolutionVector<P> sv =
					new SolutionVector(problem.getId(entry.getKey()), entry.getKey(), entry.getValue(), linearTerm,
					                   initAlpha);
			//sv.id = problem.getId(entry.getKey());

			solutionVectors.add(sv);
			}

		QMatrix<P> qMatrix =
				new BooleanInvertingKernelQMatrix<P>(param.kernel, problem.getNumExamples(), param.getCacheRows());
		BinarySolverNu<L, P> s =
				new BinarySolverNu<L, P>(solutionVectors, qMatrix, 1.0f, 1.0f, param.eps, param.shrinking);


		BinaryModel<L, P> model = s.solve();
		//model.kernel = kernel;
		model.param = param;
		model.trueLabel = problem.getTrueLabel();
		model.falseLabel = problem.getFalseLabel();
		model.setSvmType(getSvmType());

		float r = model.r;

		logger.info("C = " + 1 / r);

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

	public boolean isFeasible(BinaryClassificationProblem problem, @NotNull ImmutableSvmParameter<L, P> param)
		{
		Multiset<Boolean> counts = problem.getExampleCounts();

		int n1 = counts.count(problem.getTrueLabel());
		int n2 = counts.count(problem.getFalseLabel());

		if (param.nu * (n1 + n2) / 2 > Math.min(n1, n2))
			{
			return false; //"specified nu is infeasible";
			}

		return true;
		}

	public String getSvmType()
		{
		return "nu_svc";
		}

	public void validateParam(@NotNull ImmutableSvmParameter<L, P> param)
		{
		super.validateParam(param);
		if (param.nu <= 0 || param.nu > 1)
			{
			throw new SvmException("nu <= 0 or nu > 1");
			}
		}
	}
