package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.binary.AlphaModel;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * An SMO algorithm in Fan et al., JMLR 6(2005), p. 1889--1918 Solves:
 * <p/>
 * min 0.5(\alpha^T Q \alpha) + p^T \alpha
 * <p/>
 * y^T \alpha = \delta y_i = +1 or -1 0 <= alpha_i <= Cp for y_i = 1 0 <= alpha_i <= Cn for y_i = -1
 * <p/>
 * Given:
 * <p/>
 * Q, p, y, Cp, Cn, and an initial feasible point \alpha l is the size of vectors and matrices eps is the stopping
 * tolerance
 * <p/>
 * solution will be put in \alpha, objective value will be put in obj
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */

public abstract class Solver<L extends Comparable, P>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(Solver.class);

	private static final int MAXITER = 50000;

	protected final static SolutionVector[] EMPTY_SV_ARRAY = new SolutionVector[0];


	QMatrix<P> Q;
	float[] Q_svA;
	float[] Q_svB;
	float[] Q_all;

	float eps;
	boolean unshrink = false;
	boolean shrinking;

	protected final List<SolutionVector<P>> allExamples;
	protected SolutionVector<P>[] active;
	protected SolutionVector<P>[] inactive;
	protected final float Cp, Cn;
	protected final int numExamples;


// --------------------------- CONSTRUCTORS ---------------------------

/*	protected Solver(QMatrix<P> Q, float Cp, float Cn, float eps, boolean shrinking)
		{
		if (eps <= 0)
			{
			throw new SvmException("eps <= 0");
			}

		this.Q = Q;
		this.Cp = Cp;
		this.Cn = Cn;
		this.eps = eps;
		this.shrinking = shrinking;
		}*/

	public Solver(@NotNull List<SolutionVector<P>> solutionVectors, @NotNull QMatrix<P> Q, float Cp, float Cn, float eps,
	              boolean shrinking)
		{
		//this(Q, Cp, Cn, eps, shrinking);

		if (eps <= 0)
			{
			throw new SvmException("eps <= 0");
			}

		this.Q = Q;
		this.Cp = Cp;
		this.Cn = Cn;
		this.eps = eps;
		this.shrinking = shrinking;



		this.allExamples = solutionVectors;

		this.numExamples = allExamples.size();
		Q_all = new float[numExamples];
		}

// -------------------------- OTHER METHODS --------------------------

	protected void calculate_rho(AlphaModel<L, P> si)
		{
		double r;
		int nr_free = 0;
		double ub = Double.POSITIVE_INFINITY, lb = Double.NEGATIVE_INFINITY, sum_free = 0;

		for (SolutionVector<P> sv : active)
			{
			double yG = (sv.targetValue ? 1f : -1f) * sv.G;

			if (sv.isLowerBound())
				{
				if (sv.targetValue)
					{
					ub = Math.min(ub, yG);
					}
				else
					{
					lb = Math.max(lb, yG);
					}
				}
			else if (sv.isUpperBound())
				{
				if (!sv.targetValue)
					{
					ub = Math.min(ub, yG);
					}
				else
					{
					lb = Math.max(lb, yG);
					}
				}
			else
				{
				++nr_free;
				sum_free += yG;
				}
			}

		if (nr_free > 0)
			{
			r = sum_free / nr_free;
			}
		else
			{
			r = (ub + lb) / 2;
			}

		si.rho = (float) r;
		}

	protected int optimize()
		{
		Q.initRanks(allExamples);

		for (SolutionVector svA : allExamples)			//	for (int i = 0; i < numExamples; i++)
			{
			svA.updateAlphaStatus(Cp, Cn);			//update_alpha_status(i);
			}


		// initialize active set (for shrinking)

		initActiveSet();

		// initialize gradient

		//	G = new float[numExamples];
		//	G_bar = new float[numExamples];
		for (SolutionVector svA : allExamples)
			{
			svA.G = svA.linearTerm;
			svA.G_bar = 0;
			}
		for (SolutionVector svA : allExamples)
			{
			if (!svA.isLowerBound()) //is_lower_bound(i))
				{
				//	float[] Q_i = Q.getQ(i, numExamples);
				// //	float alpha_i = shuffledAlpha[i];

				//float[] Q_svA =
				Q.getQ(svA, active, Q_svA);
				for (SolutionVector svB : allExamples)
					{
					//	assert Q_svA[svB.rank] == Q.evaluate(svA, svB);
					svB.G += svA.alpha * Q_svA[svB.rank];
					//Q.evaluate(svA, svB);
					//	svA.wasEvaluated = true;
					//	svB.wasEvaluated = true;
					}
				if (svA.isUpperBound()) //is_upper_bound(i))
					{
					for (SolutionVector svB : allExamples)
						{
						//		assert Q_svA[svB.rank] == Q.evaluate(svA, svB);
						svB.G_bar += svA.getC(Cp, Cn) * Q_svA[svB.rank];
						//Q.evaluate(svA, svB);
						//	svA.wasEvaluated = true;
						//	svB.wasEvaluated = true;
						}
					}
				}
			}


		// optimization step

		int iter = 0;
		int counter = Math.min(numExamples, 1000) + 1;		//int[] working_set = new int[2];

		SolutionVector<P> svA;
		SolutionVector<P> svB;

		//SolutionVectorPair pair, oldPair;

		while (true)
			{
			// show progress and do shrinking

			if (--counter == 0)
				{
				counter = Math.min(numExamples, 1000);
				if (shrinking)
					{
					do_shrinking();
					}

				// ** logging output disabled for now
				//logger.debug(".");
				}
			//oldPair = pair;
			SolutionVectorPair pair = selectWorkingPair();

			if (pair.isOptimal) // pair already optimal
				{
				// reconstruct the whole gradient
				reconstruct_gradient();

				// reset active set size and check
				resetActiveSet();


				// ** logging output disabled for now
				//logger.debug("*");
				// 			//svA = pair.svA;
				// 		//svB = pair.svB;

				pair = selectWorkingPair();
				if (pair.isOptimal) // pair already optimal
					{
					//svA = oldPair.svA;
					//svB = oldPair.svB;
					break;
					}
				else
					{
					counter = 1;
					// do shrinking next iteration
					// leave the working pair the same as before
					// pair = oldPair;
					}
				}
			svA = pair.svA;
			svB = pair.svB;
			// int i = working_set[0];
			// int j = working_set[1];

			++iter;

			if (iter > MAXITER)
				{
				logger.error("Solver reached maximum iterations, aborting");
				break;
				}

			// update alpha[i] and alpha[j], handle bounds carefully


			//float[] Q_svA =
			Q.getQ(svA, active, Q_svA);
			//float[] Q_svB =
			Q.getQ(svB, active, Q_svB);

			float C_i = svA.getC(Cp, Cn); //getC(i);
			float C_j = svB.getC(Cp, Cn); //getC(j);

			double old_alpha_i = svA.alpha;
			double old_alpha_j = svB.alpha;

			if (svA.targetValue != svB.targetValue)
				{
				//	assert Q_svA[svB.rank] == Q.evaluate(svA, svB);
				float quad_coef = Q.evaluateDiagonal(svA) + Q.evaluateDiagonal(svB)
						+ 2 * Q_svA[svB.rank]; // Q.evaluate(svA, svB);
				//	svA.wasEvaluated = true;
				//	svB.wasEvaluated = true;

				if (quad_coef <= 0)
					{
					quad_coef = 1e-12f;
					}
				double delta = (-svA.G - svB.G) / quad_coef;
				double diff = svA.alpha - svB.alpha;
				svA.alpha += delta;
				svB.alpha += delta;

				if (diff > 0)
					{
					if (svB.alpha < 0)
						{
						svB.alpha = 0;
						svA.alpha = diff;
						}
					}
				else
					{
					if (svA.alpha < 0)
						{
						svA.alpha = 0;
						svB.alpha = -diff;
						}
					}
				if (diff > C_i - C_j)
					{
					if (svA.alpha > C_i)
						{
						svA.alpha = C_i;
						svB.alpha = C_i - diff;
						}
					}
				else
					{
					if (svB.alpha > C_j)
						{
						svB.alpha = C_j;
						svA.alpha = C_j + diff;
						}
					}
				}
			else
				{
				//	assert Q_svA[svB.rank] == Q.evaluate(svA, svB);
				float quad_coef = Q.evaluateDiagonal(svA) + Q.evaluateDiagonal(svB)
						- 2 * Q_svA[svB.rank]; // Q.evaluate(svA, svB);
				//	svA.wasEvaluated = true;
				//	svB.wasEvaluated = true;

				if (quad_coef <= 0)
					{
					quad_coef = 1e-12f;
					}
				double delta = (svA.G - svB.G) / quad_coef;
				double sum = svA.alpha + svB.alpha;
				svA.alpha -= delta;
				svB.alpha += delta;

				if (sum > C_i)
					{
					if (svA.alpha > C_i)
						{
						svA.alpha = C_i;
						svB.alpha = sum - C_i;
						}
					}
				else
					{
					if (svB.alpha < 0)
						{
						svB.alpha = 0;
						svA.alpha = sum;
						}
					}
				if (sum > C_j)
					{
					if (svB.alpha > C_j)
						{
						svB.alpha = C_j;
						svA.alpha = sum - C_j;
						}
					}
				else
					{
					if (svA.alpha < 0)
						{
						svA.alpha = 0;
						svB.alpha = sum;
						}
					}
				}

			// update G

			double delta_alpha_i = svA.alpha - old_alpha_i;
			double delta_alpha_j = svB.alpha - old_alpha_j;

			if (delta_alpha_i == 0 && delta_alpha_j == 0)
				{
				// pair was already optimal, but selectWorkingPair() didn't realize it because the numeric precision of float is insufficient with respect to eps
				logger.error(
						"Pair is optimal within available numeric precision, but this is still larger than requested eps = "
								+ eps + ".");
				break;
				}

			// NO: loop over A first, then B (cache locality)
			//for (SolutionVector<P> svC : active)
			for (int i = 0; i < active.length; i++)
				{
				// i == svC.rank
				active[i].G += Q_svA[i] * delta_alpha_i + Q_svB[i] * delta_alpha_j;
				}
	// PERF test tradeoff

			/*
			for (SolutionVector<P> svC : active)
				{
				svC.G += Q.evaluate(svA, svC) * delta_alpha_i;
				svA.wasEvaluated = true;
				svC.wasEvaluated = true;
				}
			for (SolutionVector<P> svC : active)
				{
				svC.G += Q.evaluate(svB, svC) * delta_alpha_j;
				svB.wasEvaluated = true;
				//svC.wasEvaluated = true;
				}
				*/

			// update alpha_status and G_bar


			boolean ui = svA.isUpperBound(); //is_upper_bound(i);
			boolean uj = svB.isUpperBound(); //is_upper_bound(j);
			svA.updateAlphaStatus(Cp, Cn); //update_alpha_status(i);
			svB.updateAlphaStatus(Cp, Cn); //update_alpha_status(j);			//int k;


			if (ui != svA.isUpperBound()) //is_upper_bound(i))
				{
				//Q_i = Q.getQ(i, numExamples);
				Q.getQ(svA, active, inactive, Q_all);
				if (ui)
					{
					for (SolutionVector<P> svC : allExamples)
						{
						//		assert Q_all[svC.rank] == Q.evaluate(svA, svC);
						svC.G_bar -= C_i * Q_all[svC.rank]; //Q.evaluate(svA, svC);
						//		svA.wasEvaluated = true;
						//		svC.wasEvaluated = true;
						}
					}
				else
					{
					for (SolutionVector<P> svC : allExamples)
						{
						//		assert Q_all[svC.rank] == Q.evaluate(svA, svC);
						svC.G_bar += C_i * Q_all[svC.rank]; //Q.evaluate(svA, svC);
						//		svA.wasEvaluated = true;
						//		svC.wasEvaluated = true;
						}
					}
				}

			if (uj != svB.isUpperBound()) //is_upper_bound(j))
				{				//Q_j = Q.getQ(j, numExamples);
				Q.getQ(svB, active, inactive, Q_all);
				if (uj)
					{
					for (SolutionVector<P> svC : allExamples)
						{
						//		assert Q_all[svC.rank] == Q.evaluate(svB, svC);
						svC.G_bar -= C_j * Q_all[svC.rank]; //Q.evaluate(svB, svC);
						//		svB.wasEvaluated = true;
						//		svC.wasEvaluated = true;
						}
					}
				else
					{
					for (SolutionVector<P> svC : allExamples)
						{
						//		assert Q_all[svC.rank] == Q.evaluate(svB, svC);
						svC.G_bar += C_j * Q_all[svC.rank]; //Q.evaluate(svB, svC);
						//		svB.wasEvaluated = true;
						//		svC.wasEvaluated = true;
						}
					}
				}
			}

		logger.debug(Q.perfString());

		logger.debug("optimization finished, #iter = " + iter);
		return iter;		// activeSet;
		}

	protected void initActiveSet()
		{
		// initial sort order was provided by allExamples.  This is why allExamples must be a List or array, not just a Collection
		active = allExamples.toArray(EMPTY_SV_ARRAY);
		inactive = EMPTY_SV_ARRAY;
		Q_svA = new float[active.length];
		Q_svB = new float[active.length];
		}

	void do_shrinking()
		{
		int i;
		double Gmax1 = Float.NEGATIVE_INFINITY;// max { -y_i * grad(f)_i | i in I_up(\alpha) }
		double Gmax2 = Float.NEGATIVE_INFINITY;// max { y_i * grad(f)_i | i in I_low(\alpha) }

		// find maximal violating pair first

		for (SolutionVector<P> sv : active)
			{
			if (sv.targetValue)
				{
				if (!sv.isUpperBound())
					{
					if (-sv.G >= Gmax1)
						{
						Gmax1 = -sv.G;
						}
					}
				if (!sv.isLowerBound())
					{
					if (sv.G >= Gmax2)
						{
						Gmax2 = sv.G;
						}
					}
				}
			else
				{
				if (!sv.isUpperBound())
					{
					if (-sv.G >= Gmax2)
						{
						Gmax2 = -sv.G;
						}
					}
				if (!sv.isLowerBound())
					{
					if (sv.G >= Gmax1)
						{
						Gmax1 = sv.G;
						}
					}
				}
			}

		if (!unshrink && Gmax1 + Gmax2 <= eps * 10)
			{
			unshrink = true;
			reconstruct_gradient();
			resetActiveSet();			//activeSize = numExamples;
			}


		// There was an extremely messy iteration here before, but I think it served only to separate the shrinkable vectors from the unshrinkable ones.


		// This class is unfortunately entangled with the cache, because we want Q_get to return the kernel values in buf[] in the cache-ranked order.
		// Since we're going to be calling Q_get with the active and inactive arrays as arguments, we need to make sure to keep those in the cache-ranked order as well.
		// An intuitive reordering upon partitioning is to "compress" into the order active - newlyInactive - previouslyInactive.
		// However, that's not what Q.maintainCache does: it performs a minimal set of swaps to guarantee that all the active nodes are in the active range (the first n ranks)
		// and all the inactive nodes are in the inactive range, but makes no guarantees about the ordering within each of those regions.

		// Thus, we need to sort the arrays according to the ranks after Q.maintainCache is done with them.


		Collection<SolutionVector<P>> activeList = new ArrayList<SolutionVector<P>>(Arrays.asList(active));

		// start this off empty, knowing that it will eventually need to contain all the currently inactive elements
		Collection<SolutionVector<P>> inactiveList = new ArrayList<SolutionVector<P>>(inactive.length);

		for (Iterator<SolutionVector<P>> iter = activeList.iterator(); iter.hasNext();)
			{
			SolutionVector sv = iter.next();

			if (sv.isShrinkable(Gmax1, Gmax2))
				{
				iter.remove();
				inactiveList.add(sv);
				}
			}

		active = activeList.toArray(EMPTY_SV_ARRAY);

		Q_svA = new float[active.length];
		Q_svB = new float[active.length];

		SolutionVector<P>[] newlyInactive = inactiveList.toArray(EMPTY_SV_ARRAY);
		Q.maintainCache(active,
		                newlyInactive);  // note maintainCache doesn't need to know about the currently inactive elements

		inactiveList.addAll(Arrays.asList(inactive));  // but we do need them on the inactive list going forward
		inactive = inactiveList.toArray(EMPTY_SV_ARRAY);

		// these must happen after Q.maintainCache, since it modifies the ranks
		Arrays.sort(active); // SolutionVector.compareTo is based on the ranks!
		Arrays.sort(inactive); // SolutionVector.compareTo is based on the ranks!
		}

	/**
	 * reconstruct inactive elements of G from G_bar and free variables
	 */
	void reconstruct_gradient()
		{
		if (active.length == numExamples)
			{
			return;
			}

		int nr_free = 0;


		for (SolutionVector sv : inactive)
			{
			sv.G = sv.G_bar + sv.linearTerm;
			}

		for (SolutionVector sv : active)
			{
			if (sv.isFree())
				{
				nr_free++;
				}
			}

		int activeSize = active.length;


		// ** logging output disabled for now
		/*if (2 * nr_free < activeSize)
			{
			logger.info("using -h 0 may be faster");
			}
*/
		if (nr_free * numExamples > 2 * activeSize * (numExamples - activeSize))
			{
			for (SolutionVector svA : inactive)
				{
				//float[] Q_i = Q.getQ(i, activeSize);
				Q.getQ(svA, active, Q_svA);
				for (SolutionVector svB : active)
					{
					if (svB.isFree()) //is_free(j))
						{
						//assert Q_svA[svB.rank] == Q.evaluate(svA, svB);
						svA.G += svB.alpha * Q_svA[svB.rank];
						//[j];
						//Q.evaluate(svA, svB);
						//			svA.wasEvaluated = true;
						//			svB.wasEvaluated = true;
						}
					}
				}
			}
		else
			{
			for (SolutionVector svA : active)
				{
				if (svA.isFree()) //is_free(i))
					{
					//	float[] Q_i = Q.getQ(i, numExamples);
					//	float alpha_i = shuffledAlpha[i];
					Q.getQ(svA, active, inactive, Q_all);
					for (SolutionVector svB : inactive)
						{
						//assert Q_all[svB.rank] == Q.evaluate(svA, svB);
						svB.G += svA.alpha * Q_all[svB.rank];
						//Q.evaluate(svA, svB);
						//		svA.wasEvaluated = true;
						//		svB.wasEvaluated = true;
						}
					}
				}
			}
		}

	protected void resetActiveSet()
		{
		active = allExamples.toArray(EMPTY_SV_ARRAY);
		Arrays.sort(active);
		inactive = EMPTY_SV_ARRAY;
		Q_svA = new float[active.length];
		Q_svB = new float[active.length];
		}

	protected SolutionVectorPair selectWorkingPair()
		{
		/*
		  return i,j such that
		  i: maximizes -y_i * grad(f)_i, i in I_up(\alpha)
	      j: mimimizes the decrease of obj value
	      (if quadratic coefficeint <= 0, replace it with tau)
	      -y_j*grad(f)_j < -y_i*grad(f)_i, j in I_low(\alpha)
	    */

		double Gmax = Double.NEGATIVE_INFINITY;
		double Gmax2 = Double.NEGATIVE_INFINITY;
		SolutionVector GmaxSV = null; //-1;
		SolutionVector GminSV = null; //-1;
		double obj_diff_min = Double.POSITIVE_INFINITY;


		int l = active.length;
		for (int i = 0; i < l; i++)
			{
			SolutionVector<P> sv = active[i];
			if (sv.targetValue)
				{
				if (!sv.isUpperBound())
					{
					if (-sv.G >= Gmax)
						{
						Gmax = -sv.G;
						GmaxSV = sv;
						}
					}
				}
			else
				{
				if (!sv.isLowerBound())
					{
					if (sv.G >= Gmax)
						{
						Gmax = sv.G;
						GmaxSV = sv;
						}
					}
				}
			}


		// PERF this is where cache locality issues kick in big time.


		if (GmaxSV != null)
			{
			Q.getQ(GmaxSV, active, Q_svA);
			}

		for (int i = 0; i < l; i++)
			{
			SolutionVector<P> sv = active[i];
			if (sv.targetValue)
				{
				if (!sv.isLowerBound())
					{
					double grad_diff = Gmax + sv.G;
					if (sv.G >= Gmax2)
						{
						Gmax2 = sv.G;
						}
					if (grad_diff > 0)
						{
						double obj_diff;
						double quad_coef = Q.evaluateDiagonal(GmaxSV) + Q.evaluateDiagonal(sv)
								- 2.0f * (GmaxSV.targetValue ? 1f : -1f) * Q_svA[sv.rank]; //Q_GmaxSV[sv.rank];

						if (quad_coef > 0)
							{
							obj_diff = -(grad_diff * grad_diff) / quad_coef;
							}
						else
							{
							obj_diff = -(grad_diff * grad_diff) / 1e-12f;
							}

						if (obj_diff <= obj_diff_min)
							{
							GminSV = sv;
							obj_diff_min = obj_diff;
							}
						}
					}
				}
			else
				{
				if (!sv.isUpperBound())
					{
					double grad_diff = Gmax - sv.G;
					if (-sv.G >= Gmax2)
						{
						Gmax2 = -sv.G;
						}
					if (grad_diff > 0)
						{
						double obj_diff;
						double quad_coef = Q.evaluateDiagonal(GmaxSV) + Q.evaluateDiagonal(sv)
								+ 2.0f * (GmaxSV.targetValue ? 1f : -1f) * Q_svA[sv.rank]; //Q_GmaxSV[sv.rank];


						if (quad_coef > 0)
							{
							obj_diff = -(grad_diff * grad_diff) / quad_coef;
							}
						else
							{
							obj_diff = -(grad_diff * grad_diff) / 1e-12f;
							}

						if (obj_diff <= obj_diff_min)
							{
							GminSV = sv;
							obj_diff_min = obj_diff;
							}
						}
					}
				}
			}

		return new SolutionVectorPair(GmaxSV, GminSV, Gmax + Gmax2 < eps);
		}

// -------------------------- INNER CLASSES --------------------------

	protected class SolutionVectorPair
		{
// ------------------------------ FIELDS ------------------------------

		boolean isOptimal;
		SolutionVector svA;
		SolutionVector svB;


// --------------------------- CONSTRUCTORS ---------------------------

		protected SolutionVectorPair(SolutionVector svA, SolutionVector svB, boolean isOptimal)
			{
			this.svA = svA;
			this.svB = svB;
			this.isOptimal = isOptimal;
			}
		}
	}
