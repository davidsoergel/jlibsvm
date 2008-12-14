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
public class C_SVC<L extends Comparable, P> extends BinaryClassificationSVM<L, P>
	{
	private static final Logger logger = Logger.getLogger(C_SVC.class);

	public C_SVC(KernelFunction<P> kernel, SvmParameter param)
		{
		super(kernel, param);
		if (param.C <= 0)
			{
			throw new SvmException("C <= 0");
			}
		}


	@Override
	public BinaryModel<L, P> trainOne(BinaryClassificationProblem<L, P> problem, float Cp, float Cn)
		{		//	int l = problem.getNumExamples();		//	float[] minusOnes = new float[l];		//	boolean[] targetValues;

		//	Arrays.fill(minusOnes, -1);

		//	targetValues = MathSupport.toPrimitive(problem.getTargetValues());

		//Solver s = new Solver(new SVC_Q(problem, kernel, param.cache_size, targetValues), minusOnes, targetValues, Cp, Cn, param.eps,		//                      param.shrinking);

		float linearTerm = -1f;
		Map<P, Boolean> examples = problem.getBooleanExamples();

		List<SolutionVector<P>> solutionVectors = new ArrayList<SolutionVector<P>>(examples.size());
		int c = 0;
		for (Map.Entry<P, Boolean> example : examples.entrySet())
			{
			SolutionVector<P> sv = new SolutionVector<P>(example.getKey(), example.getValue(), linearTerm);
			sv.id = problem.getId(example.getKey());
			c++;
			solutionVectors.add(sv);
			}

		BinarySolver<L, P> s = new BinarySolver<L, P>(solutionVectors, qMatrix, Cp, Cn, param.eps, param.shrinking);

		BinaryModel<L, P> model = s.Solve();
		model.kernel = kernel;
		model.param = param;
		model.trueLabel = problem.getTrueLabel();
		model.falseLabel = problem.getFalseLabel();
		model.setSvmType(getSvmType());


		//System.err.println(qMatrix.perfString());

		/*float[] alpha = model.alpha;

		float sumAlpha = 0;
		for (int i = 0; i < alpha.length; i++)
			{
			sumAlpha += alpha[i];
			}
*/

		// ** logging output disabled for now
		/*	if (Cp == Cn)
			  {
			  logger.info("nu = " + model.getSumAlpha() / (Cp * problem.getExamples().size()));
			  }
  */
		for (Map.Entry<P, Double> entry : model.supportVectors.entrySet())
			{
			final P key = entry.getKey();
			final Boolean target = examples.get(key);
			if (!target)  // targetValue was false				//if(problem.getTargetValue(entry.getKey()).equals(falseLabel))
				{
				entry.setValue(entry.getValue() * -1);
				}
			}/*		for (int i = 0; i < alpha.length; i++)
			{
			if (!targetValues[i])
				{
				alpha[i] *= -1;
				}
			}
*/
		model.compact();

		return model;
		}

	public String getSvmType()
		{
		return "c_svc";
		}
	}
