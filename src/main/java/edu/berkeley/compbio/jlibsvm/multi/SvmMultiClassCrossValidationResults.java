package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.ml.MultiClassCrossValidationResults;
import org.apache.log4j.Logger;

import java.util.Map;


/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SvmMultiClassCrossValidationResults<L extends Comparable, P> extends MultiClassCrossValidationResults<L>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(SvmMultiClassCrossValidationResults.class);

	/**
	 * if we did a grid search, keep track of which parameter set was used for these results
	 */
	public ImmutableSvmParameter<L, P> param;


	//private final Multiset<L> unknowns = new HashMultiset<L>();

// --------------------------- CONSTRUCTORS ---------------------------

	/*public CrossValidationResults(int numExamples, int tt, int tf, int ft, int ff)
		{
		this.numExamples = numExamples;
		this.tt = tt;
		this.tf = tf;
		this.ft = ft;
		this.ff = ff;
		}
*/

	public SvmMultiClassCrossValidationResults(MultiClassProblem<L, P> problem,
	                                           Map<P, L> predictions)//, Map<L, String> friendlyLabelMap)
		{
		super();
		//super(friendlyLabelMap);
		//numExamples = problem.getNumExamples();


		for (Map.Entry<P, L> entry : problem.getExamples().entrySet())
			{
			P point = entry.getKey();
			L realValue = entry.getValue();
			L predictedValue = predictions.get(point);

			// the confusionMatrix should count predictedValue==null (aka unknown) just like any other value

			//if (predictedValue != null)
			//	{
			addSample(realValue, predictedValue);
			//	}
			//else
			//	{
			//	unknowns.add(realValue);
			//	}
			}

		sanityCheck();
		}

// -------------------------- OTHER METHODS --------------------------


	public String getInfo()
		{
		if (param != null)
			{
			return param.toString();
			}
		return "";
		}
	}
