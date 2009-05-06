package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.CrossValidationResults;
import edu.berkeley.compbio.jlibsvm.SigmoidProbabilityModel;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BinaryCrossValidationResults<L extends Comparable, P> extends CrossValidationResults
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(BinaryCrossValidationResults.class);

	SigmoidProbabilityModel sigmoid;
	private int numExamples;
	private int tt, tf, ft, ff;


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

	public BinaryCrossValidationResults(BinaryClassificationProblem<L, P> problem, Map<P, Float> decisionValues,
	                                    boolean probability)
		{
		final float[] decisionValueArray;
		final boolean[] labelArray;

		// convert to arrays

		decisionValueArray = new float[decisionValues.size()];
		labelArray = new boolean[decisionValues.size()];
		L trueLabel = problem.getTrueLabel();

		for (Map.Entry<P, Float> entry : decisionValues.entrySet())
			{
			decisionValueArray[numExamples] = entry.getValue();
			labelArray[numExamples] = problem.getTargetValue(entry.getKey()).equals(trueLabel);
			numExamples++;
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

		// do this here so that we can forget the arrays
		if (probability)
			{
			sigmoid = new SigmoidProbabilityModel(decisionValueArray, labelArray);
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

	float trueTrueRate()
		{
		return (float) tt / (float) numExamples;
		}

	float falseTrueRate()
		{
		return (float) ft / (float) numExamples;
		}

	float trueFalseRate()
		{
		return (float) tf / (float) numExamples;
		}

	float falseFalseRate()
		{
		return (float) ff / (float) numExamples;
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	public SigmoidProbabilityModel getSigmoid()
		{
		return sigmoid;
		}

// -------------------------- OTHER METHODS --------------------------

	public float accuracy()
		{
		return (float) (tt + ff) / (float) numExamples;
		}

	public float accuracyGivenClassified()
		{
		// ** for now everything was classified
		return accuracy();
		}

	public float unknown()
		{
		// ** for now everything was classified
		return 0F;
		}

	float classNormalizedSensitivity()
		{
		return ((float) tt / (float) (tt + tf) + (float) ff / (float) (ff + ft)) / 2f;
		}
	}
