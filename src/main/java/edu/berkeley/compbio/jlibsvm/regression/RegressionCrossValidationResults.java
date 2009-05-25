package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.ml.CrossValidationResults;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class RegressionCrossValidationResults<P, R extends RegressionProblem<P, R>> extends CrossValidationResults
	{
	double meanSquaredError;
	double squaredCorrCoeff;

	public RegressionCrossValidationResults(RegressionProblem<P, R> problem, Map<P, Float> decisionValues)
		{
		double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
		double total_error = 0;
		int numExamples = problem.getNumExamples();

		for (Map.Entry<P, Float> entry : problem.getExamples().entrySet())
			{
			P p = entry.getKey();
			Float y = entry.getValue();
			Float v = decisionValues.get(p);
			total_error += (v - y) * (v - y);
			sumv += v;
			sumy += y;
			sumvv += v * v;
			sumyy += y * y;
			sumvy += v * y;
			}
		meanSquaredError = total_error / numExamples;
		squaredCorrCoeff = ((numExamples * sumvy - sumv * sumy) * (numExamples * sumvy - sumv * sumy)) / (
				(numExamples * sumvv - sumv * sumv) * (numExamples * sumyy - sumy * sumy));

		System.out.print("Cross Validation Mean squared error = " + meanSquaredError + "\n");

		System.out.print("Cross Validation Squared correlation coefficient = " + squaredCorrCoeff + "\n");
		}

	public float accuracy()
		{
		return 0;
		}

	public float accuracyGivenClassified()
		{
		return 0;
		}

	public float unknown()
		{
		return 0;
		}
	}
