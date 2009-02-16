package edu.berkeley.compbio.jlibsvm.binary;

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
public class BinarySolver<L extends Comparable, P> extends Solver<L, P>
	{
	private static final Logger logger = Logger.getLogger(BinarySolver.class);

	public BinarySolver(List<SolutionVector<P>> solutionVectors, QMatrix<P> Q, float Cp, float Cn, float eps,
	                    boolean shrinking)
		{
		super(solutionVectors, Q, Cp, Cn, eps, shrinking);
		}

	public BinaryModel<L, P> Solve()
		{
		int iter = optimize();

		BinaryModel<L, P> model = new BinaryModel<L, P>(null, null);

		// calculate rho

		//		si.rho =
		calculate_rho(model);

		// calculate objective value
		{
		float v = 0;
		for (SolutionVector svC : allExamples)
			{
			v += svC.alpha * (svC.G + svC.linearTerm);
			}

		model.obj = v / 2;
		}

		// put the solution, mapping the alphas back to their original order

		// note the swapping process applied to the Q matrix as well, so we have to map that back too

		//	model.alpha = new float[numExamples];		//	model.supportVectors = new SparseVector[numExamples];
		//	int posEval = 0, posNotEval = 0, negEval = 0, negNotEval = 0;

		model.supportVectors = new HashMap<P, Double>();
		for (SolutionVector<P> svC : allExamples)
			{
			model.supportVectors.put(svC.point, svC.alpha);

			/*if (svC.alpha != 0)
				{
				if (svC.wasEvaluated)
					{
					if (svC.targetValue)
						{
						posEval++;
						}
					else
						{
						negEval++;
						}
					}
				else
					{
					if (svC.targetValue)
						{
						posNotEval++;
						}
					else
						{
						negNotEval++;
						}
					}
				}*/
			}

		//	logger.trace("Learned binary model: " + posEval + " pos, " + negEval + " neg, " + posNotEval + " posNE!, "
		//			+ negNotEval + " negNE!");

		// make sure the optimizer actually considered every example
		//	assert posNotEval + negNotEval == 0;

		// note at this point the solution includes _all_ vectors, even if their alphas are zero

		// we can't do this yet because in the regression case there are twice as many alphas as vectors
		// model.compact();

		model.upperBoundPositive = Cp;
		model.upperBoundNegative = Cn;


		// ** logging output disabled for now
		//logger.info("optimization finished, #iter = " + iter);


		return model;
		}
	}
