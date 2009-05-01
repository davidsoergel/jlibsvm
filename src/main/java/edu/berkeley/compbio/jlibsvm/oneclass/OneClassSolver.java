package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.Solver;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;

import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class OneClassSolver<L, P> extends Solver<Float, P>
	{
// --------------------------- CONSTRUCTORS ---------------------------

	public OneClassSolver(List<SolutionVector<P>> solutionVectors, QMatrix<P> Q, float C, float eps, boolean shrinking)
		{
		super(solutionVectors, Q, C, C, eps, shrinking);
		}

// -------------------------- OTHER METHODS --------------------------

	public OneClassModel<L, P> solve()
		{
		optimize();

		OneClassModel<L, P> model = new OneClassModel<L, P>();

		calculate_rho(model);


		model.supportVectors = new HashMap<P, Double>();
		for (SolutionVector<P> svC : allExamples)
			{
			model.supportVectors.put(svC.point, svC.alpha);
			}

		// note at this point the solution includes _all_ vectors, even if their alphas are zero

		// we can't do this yet because in the regression case there are twice as many alphas as vectors		// model.compact();

		// ** logging output disabled for now
		//logger.info("optimization finished, #iter = " + iter);

		return model;
		}
	}
