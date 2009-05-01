package edu.berkeley.compbio.jlibsvm.binary;

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
public class C_SVC<L extends Comparable, P> extends BinaryClassificationSVM<L, P>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(C_SVC.class);


// --------------------------- CONSTRUCTORS ---------------------------

	public C_SVC(KernelFunction<P> kernel, ScalingModelLearner<P> scalingModelLearner, SvmParameter param)
		{
		super(kernel, scalingModelLearner, param);
		if (param.C <= 0)
			{
			throw new SvmException("C <= 0");
			}
		}

// -------------------------- OTHER METHODS --------------------------

	@Override
	public BinaryModel<L, P> trainOne(BinaryClassificationProblem<L, P> problem, float Cp, float Cn)
		{
		float linearTerm = -1f;
		Map<P, Boolean> examples = problem.getBooleanExamples();

		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>(examples.size());

		for (Map.Entry<P, Boolean> example : examples.entrySet())
			{
			SolutionVector<P> sv = new SolutionVector<P>(example.getKey(), example.getValue(), linearTerm);
			sv.id = problem.getId(example.getKey());
			solutionVectors.add(sv);
			}

		QMatrix<P> qMatrix =
				new BooleanInvertingKernelQMatrix<P>(kernel, problem.getNumExamples(), param.getCacheRows());

		BinarySolver<L, P> s = new BinarySolver<L, P>(solutionVectors, qMatrix, Cp, Cn, param.eps, param.shrinking);

		BinaryModel<L, P> model = s.solve();
		model.kernel = kernel;
		model.param = param;
		model.trueLabel = problem.getTrueLabel();
		model.falseLabel = problem.getFalseLabel();
		model.setSvmType(getSvmType());
		model.setScalingModel(problem.getScalingModel());


		//System.err.println(qMatrix.perfString());


		if (Cp == Cn)
			{
			logger.debug("nu = " + model.getSumAlpha() / (Cp * problem.getNumExamples()));
			}

		for (Map.Entry<P, Double> entry : model.supportVectors.entrySet())
			{
			final P key = entry.getKey();
			final Boolean target = examples.get(key);
			if (!target)
				{
				entry.setValue(entry.getValue() * -1);
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
