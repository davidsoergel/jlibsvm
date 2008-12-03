package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.Solver;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class RegressionSolver<P> extends Solver<Float, P>
	{
	private static final Logger logger = Logger.getLogger(RegressionSolver.class);

	public RegressionSolver(List<SolutionVector<P>> solutionVectors, QMatrix<P> Q, float C, float eps,
	                        boolean shrinking)
		{
		super(solutionVectors, Q, C, C, eps, shrinking);
		}


	public RegressionModel<P> Solve()
		{
		int iter = optimize();

		RegressionModel<P> model = new RegressionModel<P>();

		// calculate rho

		//		si.rho =
		calculate_rho(model);

		// calculate objective value


		/*		{
		float v = 0;
		for (SolutionVector svC : activeSet)
			{
			v += svC.alpha * (svC.G + svC.linearTerm);
			}

		model.obj = v / 2;
		}*/

		// put the solution, mapping the alphas back to their original order

		// note the swapping process applied to the Q matrix as well, so we have to map that back too

		//	model.alpha = new float[numExamples];		//	model.supportVectors = new SparseVector[numExamples];

		/*	float[] alpha = new float[l];
		  float sumAlpha = 0;
		  for (i = 0; i < l; i++)
			  {
			  alpha[i] = model.alpha[i] - model.alpha[i + l];
			  sumAlpha += Math.abs(alpha[i]);
			  }
  */		//model.alpha = alpha;

		float sumAlpha = 0;

		model.supportVectors = new HashMap<P, Double>();
		for (SolutionVector<P> svC : allExamples)
			{			// the examples contain both a true and a false SolutionVector for each P.			// we want the difference of their alphas

			Double alphaDiff = model.supportVectors.get(svC.point);
			if (alphaDiff == null)
				{
				alphaDiff = 0.;
				}
			alphaDiff += (svC.targetValue ? 1. : -1.) * svC.alpha;

			model.supportVectors.put(svC.point, alphaDiff);
			}

		for (Double alphaDiff : model.supportVectors.values())
			{
			sumAlpha += Math.abs(alphaDiff);
			}

		logger.info("nu = " + sumAlpha / (Cp * allExamples.size()));  //Cp == Cn == C

		// note at this point the solution includes _all_ vectors, even if their alphas are zero

		// we can't do this yet because in the regression case there are twice as many alphas as vectors		// model.compact();

		//	model.upperBoundPositive = Cp;		//	model.upperBoundNegative = Cn;

		logger.info("optimization finished, #iter = " + iter);

		return model;
		}
	}
