package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.Solver_NU;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BinarySolverNu<L extends Comparable, P> extends Solver_NU<L, P>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(BinarySolverNu.class);


// --------------------------- CONSTRUCTORS ---------------------------

	public BinarySolverNu(List<SolutionVector<P>> solutionVectors, QMatrix<P> Q, float Cp, float Cn, float eps,
	                      boolean shrinking)
		{
		super(solutionVectors, Q, Cp, Cn, eps, shrinking);
		}

// -------------------------- OTHER METHODS --------------------------

	public BinaryModel<L, P> solve() //ImmutableSvmParameter<L, P> param)
		{
		int iter = optimize();

		BinaryModel<L, P> model = new BinaryModel<L, P>();

		// calculate rho

		calculate_rho(model);

		// calculate objective value

		float v = 0;
		for (SolutionVector svC : active)
			{
			v += svC.alpha * (svC.G + svC.linearTerm);
			}

		model.obj = v / 2;


		// put the solution, mapping the alphas back to their original order

		model.supportVectors = new HashMap<P, Double>();
		for (SolutionVector<P> svC : allExamples)
			{
			model.supportVectors.put(svC.point, svC.alpha);
			}

		// note at this point the solution includes _all_ vectors, even if their alphas are zero

		// we can't do this yet because in the regression case there are twice as many alphas as vectors
		// model.compact();

		model.upperBoundPositive = Cp;
		model.upperBoundNegative = Cn;

		logger.info("optimization finished, #iter = " + iter);

		return model;
		}
	}
