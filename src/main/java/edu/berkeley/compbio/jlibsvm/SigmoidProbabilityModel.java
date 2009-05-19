package edu.berkeley.compbio.jlibsvm;

import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SigmoidProbabilityModel
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(SigmoidProbabilityModel.class);
	float A, B;


// --------------------------- CONSTRUCTORS ---------------------------


	// Platt's binary SVM Probablistic Output: an improvement from Lin et al.
	// protected void sigmoidTrain(float[] decisionValues, boolean[] labels)

	public SigmoidProbabilityModel(float[] decisionValues, boolean[] labels)
		{
		int l = decisionValues.length;

		float prior1 = 0, prior0 = 0;		//int i;

		for (boolean b : labels)
			{
			if (b)
				{
				prior1 += 1;
				}
			else
				{
				prior0 += 1;
				}
			}

		int maximumIterations = 100;// Maximal number of iterations
		float minStep = 1e-10f;// Minimal step taken in line search
		float sigma = 1e-12f;// For numerically strict PD of Hessian
		float eps = 1e-5f;
		float hiTarget = (prior1 + 1.0f) / (prior1 + 2.0f);
		float loTarget = 1 / (prior0 + 2.0f);
		float[] t = new float[l];
		double p, q;
		float fApB, h11, h22, h21, g1, g2, det, dA, dB, gd, stepsize;
		float newA, newB, newf, d1, d2;		//	int iter;

		// Initial Point and Initial Fun Value
		A = 0.0f;
		B = (float) Math.log((prior0 + 1.0f) / (prior1 + 1.0f)); // PERF use approximateLog?
		float fval = 0.0f;

		for (int i = 0; i < l; i++)
			{
			if (labels[i])
				{
				t[i] = hiTarget;
				}
			else
				{
				t[i] = loTarget;
				}
			fApB = decisionValues[i] * A + B;
			if (fApB >= 0)
				{
				fval += t[i] * fApB + Math.log(1 + Math.exp(-fApB));
				}
			else
				{
				fval += (t[i] - 1) * fApB + Math.log(1 + Math.exp(fApB));
				}
			}


		int iter;
		for (iter = 0; iter < maximumIterations; iter++)
			{			// Update Gradient and Hessian (use H' = H + sigma I)
			h11 = sigma;// numerically ensures strict PD
			h22 = sigma;
			h21 = 0.0f;
			g1 = 0.0f;
			g2 = 0.0f;
			for (int i = 0; i < l; i++)
				{
				fApB = decisionValues[i] * A + B;

				// PERF

				if (fApB >= 0)
					{
					final double expfApB = Math.exp(-fApB);
					p = expfApB / (1.0f + expfApB);
					q = 1.0f / (1.0f + expfApB);
					}
				else
					{
					final double expfApB = Math.exp(fApB);
					p = 1.0f / (1.0f + expfApB);
					q = expfApB / (1.0f + expfApB);
					}
				d2 = (float) (p * q);
				h11 += decisionValues[i] * decisionValues[i] * d2;
				h22 += d2;
				h21 += decisionValues[i] * d2;
				d1 = (float) (t[i] - p);
				g1 += decisionValues[i] * d1;
				g2 += d1;
				}

			// Stopping Criteria
			if (Math.abs(g1) < eps && Math.abs(g2) < eps)
				{
				break;
				}

			// Finding Newton direction: -inv(H') * g
			det = h11 * h22 - h21 * h21;
			dA = -(h22 * g1 - h21 * g2) / det;
			dB = -(-h21 * g1 + h11 * g2) / det;
			gd = g1 * dA + g2 * dB;


			stepsize = 1;

			// Line Search
			while (stepsize >= minStep)
				{
				newA = A + stepsize * dA;
				newB = B + stepsize * dB;

				// New function value
				newf = 0.0f;
				for (int i = 0; i < l; i++)
					{
					fApB = decisionValues[i] * newA + newB;
					if (fApB >= 0)
						{
						newf += t[i] * fApB + Math.log(1 + Math.exp(-fApB));
						}
					else
						{
						newf += (t[i] - 1) * fApB + Math.log(1 + Math.exp(fApB));
						}
					}
				// Check sufficient decrease
				if (newf < fval + 0.0001 * stepsize * gd)
					{
					A = newA;
					B = newB;
					fval = newf;
					break;
					}
				else
					{
					stepsize = stepsize / 2.0f;
					}
				}

			if (stepsize < minStep)
				{
				logger.error("Line search fails in two-class probability estimates");
				break;
				}
			}

		if (iter >= maximumIterations)
			{
			logger.error("Reaching maximal iterations in two-class probability estimates");
			}
		}

// -------------------------- OTHER METHODS --------------------------

	public float predict(float decisionValue)
		{
		float fApB = decisionValue * A + B;
		if (fApB >= 0)
			{
			final double expMinusfApB = Math.exp(-fApB);
			return (float) (expMinusfApB / (1.0 + expMinusfApB));
			}
		else
			{
			return (float) (1.0 / (1 + Math.exp(fApB)));
			}
		}
	}
