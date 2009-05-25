package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.SigmoidProbabilityModel;
import edu.berkeley.compbio.ml.BinaryCrossValidationResults;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SvmBinaryCrossValidationResults<L extends Comparable, P> extends BinaryCrossValidationResults
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(SvmBinaryCrossValidationResults.class);

	SigmoidProbabilityModel sigmoid;


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

	public SvmBinaryCrossValidationResults(BinaryClassificationProblem<L, P> problem,
	                                       final Map<P, Float> decisionValues, boolean probability)
		{
		// convert to arrays

		int totalExamples = decisionValues.size();

		final float[] decisionValueArray = new float[totalExamples];
		final boolean[] labelArray = new boolean[totalExamples];

		logger.debug("Collecting binary cross-validation results for " + totalExamples + " points");

		L trueLabel = problem.getTrueLabel();

		for (Map.Entry<P, Float> entry : decisionValues.entrySet())
			{
			decisionValueArray[numExamples] = entry.getValue();
			labelArray[numExamples] = problem.getTargetValue(entry.getKey()).equals(trueLabel);
			numExamples++;
			}

		// do this here so that we can forget the arrays
		if (probability)
			{
			//sigmoid = new SigmoidProbabilityModel(decisionValues, trueLabel);
			sigmoid = new SigmoidProbabilityModel(decisionValueArray, labelArray);
			}


		// while we're at it, since we've done a cross-validation anyway, we may as well report the accuracy.

		//	tt = 0, ff = 0, ft = 0, tf = 0;
		for (int j = 0; j < numExamples; j++)
			{
			if (decisionValueArray[j] > 0)
				{
				if (labelArray[j])
					{
					tt++;
					}
				else
					{
					ft++;
					}
				}
			else
				{
				if (labelArray[j])
					{
					tf++;
					}
				else
					{
					ff++;
					}
				}
			}


		//** debug output disabled for now

		/*
		Formatter f = new Formatter();
		f.format("Binary classifier for %s vs. %s: TP=%.2f FP=%.2f FN=%.2f TN=%.2f", trueLabel, problem.getFalseLabel(),
		         trueTrueRate(), falseTrueRate(), trueFalseRate(), falseFalseRate());


		//	logger.info("Binary classifier for " + trueLabel + " vs. " + problem.getFalseLabel() + ": TP="+((float)tp/i) + ": FP="
		//			+ ((float) fp / i) + ": FN=" + ((float) fn / i) + ": TN=" + ((float) tn / i) );

		logger.info(f.out().toString());*/
		}

	// --------------------- GETTER / SETTER METHODS ---------------------

	public SigmoidProbabilityModel getSigmoid()
		{
		return sigmoid;
		}

// -------------------------- OTHER METHODS --------------------------
	}
