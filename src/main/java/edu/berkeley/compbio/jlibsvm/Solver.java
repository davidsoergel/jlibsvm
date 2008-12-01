package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.binary.AlphaModel;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


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
	private static final Logger logger = Logger.getLogger(Solver.class);

	private static final int MAXITER = 10000;

	protected Collection<SolutionVector<P>> allExamples;
	protected Collection<SolutionVector<P>> activeSet;
	protected Collection<SolutionVector<P>> inactiveSet;

//	int activeSize;
//	boolean[] y;
//	float[] G;// gradient of objective function

//	static final byte LOWER_BOUND = 0;
//	static final byte UPPER_BOUND = 1;
	//	static final byte FREE = 2;
//	Status[] alphaStatus;// LOWER_BOUND, UPPER_BOUND, FREE

	/**
	 * In the course of shrinking it's convenient to reorder the alpha array.
	 */
//	float[] shuffledAlpha;

	/**
	 * This array maps the rearranged indices back to the original indices.
	 */
//	int[] shuffledExampleIndexToOriginalIndex;


	QMatrix<P> Q;
	//	float[] QD;
	float eps;
	protected float Cp, Cn;
//	float[] p;

	//	float[] G_bar;// gradient, if we treat free variables as 0
	protected int numExamples;
	boolean unshrink = false;// XXX
	boolean shrinking;

/*
	void swap_index(int i, int j)
		{
		Q.swapIndex(i, j);

		boolean b = y[i];
		y[i] = y[j];
		y[j] = b;

		float f = G[i];
		G[i] = G[j];
		G[j] = f;

		Status y = alphaStatus[i];
		alphaStatus[i] = alphaStatus[j];
		alphaStatus[j] = y;

		float f1 = shuffledAlpha[i];
		shuffledAlpha[i] = shuffledAlpha[j];
		shuffledAlpha[j] = f1;

		float f2 = p[i];
		p[i] = p[j];
		p[j] = f2;

		int i2 = shuffledExampleIndexToOriginalIndex[i];
		shuffledExampleIndexToOriginalIndex[i] = shuffledExampleIndexToOriginalIndex[j];
		shuffledExampleIndexToOriginalIndex[j] = i2;

		float g = G_bar[i];
		G_bar[i] = G_bar[j];
		G_bar[j] = g;
		}
		*/

	void reconstruct_gradient()
		{		// reconstruct inactive elements of G from G_bar and free variables

		if (activeSet.size() == numExamples)
			{
			return;
			}

		//int i, j;
		int nr_free = 0;


		for (SolutionVector sv : inactiveSet)
			{
			sv.G = sv.G_bar + sv.linearTerm;
			}

		for (SolutionVector sv : activeSet)
			{
			if (sv.isFree())
				{
				nr_free++;
				}
			}

		int activeSize = activeSet.size();

		if (2 * nr_free < activeSize)
			{
			System.out.print("\nWarning: using -h 0 may be faster\n");
			}

		if (nr_free * numExamples > 2 * activeSize * (numExamples - activeSize))
			{
			for (SolutionVector svA : inactiveSet)
				{
				//float[] Q_i = Q.getQ(i, activeSize);
				for (SolutionVector svB : activeSet)
					{
					if (svB.isFree()) //is_free(j))
						{
						svA.G += svB.alpha * Q.evaluate(svA, svB);//[j];
						}
					}
				}
			}
		else
			{
			for (SolutionVector svA : activeSet)
				{
				if (svA.isFree()) //is_free(i))
					{
					//	float[] Q_i = Q.getQ(i, numExamples);
					//	float alpha_i = shuffledAlpha[i];

					for (SolutionVector svB : inactiveSet)
						{
						svB.G += svA.alpha * Q.evaluate(svA, svB);
						}
					}
				}
			}
		}

	protected Solver(QMatrix<P> Q, float Cp, float Cn, float eps, boolean shrinking)
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
		}


	public Solver(Collection<SolutionVector<P>> solutionVectors, QMatrix<P> Q, float Cp, float Cn, float eps,
	              boolean shrinking)
		{
		this(Q, Cp, Cn, eps, shrinking);

		this.allExamples = solutionVectors;

		this.numExamples = allExamples.size();
		}

/*
	public Solver(Map<P, Boolean> examples, QMatrix<P> Q, float linearTerm, float Cp, float Cn, float eps, boolean shrinking)
		{
		this(Q,Cp,Cn,eps,shrinking);

		this.allExamples = new HashSet<SolutionVector>();
		for (Map.Entry<P, Boolean> example : examples.entrySet())
			{
			SolutionVector sv = new SolutionVector( example.getKey(), example.getValue(), linearTerm);
			allExamples.add(sv);
			}

		this.numExamples = allExamples.size();

		}


	public Solver(Map<P, Boolean> examples, QMatrix<P> Q, float linearTerm, Map<P, Float> initAlpha, float Cp, float Cn, float eps, boolean shrinking)
		{
		this(Q, Cp, Cn, eps, shrinking);


		this.allExamples = new HashSet<SolutionVector>();
		for (Map.Entry<P, Boolean> example : examples.entrySet())
			{
			SolutionVector sv = new SolutionVector(example.getKey(), example.getValue(), linearTerm, initAlpha.get(example.getKey()));
			allExamples.add(sv);
			}

		this.numExamples = allExamples.size();
		}
*/


	protected int optimize()
		{

//		if (shuffledAlpha == null)
//			{

		// initialize shuffledAlpha if needed (the constructor may or may not have already set it)
//			shuffledAlpha = new float[numExamples];
//			}

		// initialize alpha_status

//		alphaStatus = new Status[numExamples];
		for (SolutionVector svA : allExamples)
			//	for (int i = 0; i < numExamples; i++)
			{
			svA.updateAlphaStatus(Cp, Cn);
			//update_alpha_status(i);
			}


		// initialize active set (for shrinking)

		resetActiveSet();

		/*	shuffledExampleIndexToOriginalIndex = new int[numExamples];
		  for (int i = 0; i < numExamples; i++)
			  {
			  shuffledExampleIndexToOriginalIndex[i] = i;
			  }
		  activeSize = numExamples;
  */

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
				//	float alpha_i = shuffledAlpha[i];
				for (SolutionVector svB : allExamples)
					{
					svB.G += svA.alpha * Q.evaluate(svA, svB);
					}
				if (svA.isUpperBound()) //is_upper_bound(i))
					{
					for (SolutionVector svB : allExamples)
						{
						svB.G_bar += svA.getC(Cp, Cn) * Q.evaluate(svA, svB); //getC(i) * Q_i[j];
						}
					}
				}
			}


		// optimization step

		int iter = 0;
		int counter = Math.min(numExamples, 1000) + 1;
		//int[] working_set = new int[2];

		SolutionVector<P> svA;
		SolutionVector<P> svB;

		//SolutionVectorPair pair, oldPair;

		while (true)
			{			// show progress and do shrinking

			if (--counter == 0)
				{
				counter = Math.min(numExamples, 1000);
				if (shrinking)
					{
					do_shrinking();
					}
				System.err.print(".");
				}
//oldPair = pair;
			SolutionVectorPair pair = selectWorkingPair();

			if (pair.isOptimal) // pair already optimal
				{				// reconstruct the whole gradient
				reconstruct_gradient();				// reset active set size and check
				resetActiveSet();
				//activeSize = numExamples;
				System.err.print("*");
				//svA = pair.svA;
				//svB = pair.svB;

				pair = selectWorkingPair();
				if (pair == null) // pair already optimal
					{
					//svA = oldPair.svA;
					//svB = oldPair.svB;
					break;
					}
				else
					{
					counter = 1;// do shrinking next iteration
					// leave the working pair the same as before
					//pair = oldPair;
					}
				}
			svA = pair.svA;
			svB = pair.svB;
//			int i = working_set[0];
//			int j = working_set[1];

			++iter;

			if (iter > MAXITER)
				{
				logger.error("Solver reached maximum iterations, aborting");
				break;
				}

			// update alpha[i] and alpha[j], handle bounds carefully

			//	float[] Q_i = Q.getQ(i, activeSize);
			//	float[] Q_j = Q.getQ(j, activeSize);

			float C_i = svA.getC(Cp, Cn); //getC(i);
			float C_j = svB.getC(Cp, Cn); //getC(j);

			double old_alpha_i = svA.alpha; //shuffledAlpha[i];
			double old_alpha_j = svB.alpha; //shuffledAlpha[j];

			if (svA.targetValue != svB.targetValue)
				{
				float quad_coef = Q.evaluate(svA, svA) + Q.evaluate(svB, svB) + 2 * Q.evaluate(svA, svB);
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
				float quad_coef = Q.evaluate(svA, svA) + Q.evaluate(svB, svB) - 2 * Q.evaluate(svA, svB);
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


			for (SolutionVector<P> svC : activeSet)
				{
				svC.G += Q.evaluate(svA, svC) * delta_alpha_i + Q.evaluate(svB, svC) * delta_alpha_j;
				}

			// update alpha_status and G_bar


			boolean ui = svA.isUpperBound(); //is_upper_bound(i);
			boolean uj = svB.isUpperBound(); //is_upper_bound(j);
			svA.updateAlphaStatus(Cp, Cn); //update_alpha_status(i);
			svB.updateAlphaStatus(Cp, Cn); //update_alpha_status(j);
			//int k;
			if (ui != svA.isUpperBound()) //is_upper_bound(i))
				{
				//Q_i = Q.getQ(i, numExamples);
				if (ui)
					{
					for (SolutionVector svC : allExamples)
						{
						svC.G_bar -= C_i * Q.evaluate(svA, svC);
						}
					}
				else
					{
					for (SolutionVector svC : allExamples)
						{
						svC.G_bar += C_i * Q.evaluate(svA, svC);
						}
					}
				}

			if (uj != svB.isUpperBound()) //is_upper_bound(j))
				{
				//Q_j = Q.getQ(j, numExamples);
				if (uj)
					{
					for (SolutionVector svC : allExamples)
						{
						svC.G_bar -= C_j * Q.evaluate(svB, svC);
						}
					}
				else
					{
					for (SolutionVector svC : allExamples)
						{
						svC.G_bar += C_j * Q.evaluate(svB, svC);
						}
					}
				}
			}

		//System.err.println(Q.perfString());
		return iter;
		// activeSet;
		}


	protected void resetActiveSet()
		{
		activeSet = new ArrayList<SolutionVector<P>>(allExamples);
		inactiveSet = new ArrayList<SolutionVector<P>>(allExamples.size()); // it should get that big eventually
		}

	// return 1 if already optimal, return 0 otherwise
	//boolean select_working_set(int[] working_set)
	//	{

	//	}

	protected class SolutionVectorPair
		{
		boolean isOptimal;
		SolutionVector svA;
		SolutionVector svB;

		protected SolutionVectorPair(SolutionVector svA, SolutionVector svB, boolean isOptimal)
			{
			this.svA = svA;
			this.svB = svB;
			this.isOptimal = isOptimal;
			}
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

		for (SolutionVector sv : activeSet)
			{
			//	for (int t = 0; t < activeSize; t++)
			//		{
			if (sv.targetValue) //y[t])
				{
				if (!sv.isUpperBound()) //is_upper_bound(t))
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
				if (!sv.isLowerBound()) //is_lower_bound(t))
					{
					if (sv.G >= Gmax)
						{
						Gmax = sv.G;
						GmaxSV = sv;
						}
					}
				}
			}

		//int i = Gmax_idx;
		//float[] Q_i = null;
		//if (GmaxSV != null) //i != -1)// null Q_i not accessed: Gmax=Float.NEGATIVE_INFINITY if i=-1
		//	{
		//	Q_i = Q.getQ(GmaxSV, activeSize);
		//	}

		for (SolutionVector sv : activeSet)
			{
			//for (int j = 0; j < activeSize; j++)
			//	{
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
						double quad_coef = Q.evaluate(GmaxSV, GmaxSV) + Q.evaluate(sv, sv)
								- 2.0f * (GmaxSV.targetValue ? 1f : -1f) * Q.evaluate(GmaxSV, sv);
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
						double quad_coef = Q.evaluate(GmaxSV, GmaxSV) + Q.evaluate(sv, sv)
								+ 2.0f * (GmaxSV.targetValue ? 1f : -1f) * Q.evaluate(GmaxSV, sv);
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

		/*	if (Gmax + Gmax2 < eps)
		   {

		   return new SolutionVectorPair(GmaxSV, GminSV, true);
		   }*/
		/*
		if(Gmax + Gmax2 < Math.ulp(Gmax) )
			{
			logger.warn("Pair is optimal within available numeric precision of " + Math.ulp(Gmax) + ", but this is still larger than requested eps = " + eps + ".");
			return null;
			}
			*/

		//working_set[0] = Gmax_idx;
		//working_set[1] = Gmin_idx;
		//	return new SolutionVectorPair(GmaxSV, GminSV, false);
		}

	void do_shrinking()
		{
		int i;
		double Gmax1 = Float.NEGATIVE_INFINITY;// max { -y_i * grad(f)_i | i in I_up(\alpha) }
		double Gmax2 = Float.NEGATIVE_INFINITY;// max { y_i * grad(f)_i | i in I_low(\alpha) }

		// find maximal violating pair first

		for (SolutionVector<P> sv : activeSet)
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
			resetActiveSet();
			//activeSize = numExamples;
			}


		// There was an extremely messy iteration here before, but I think it served only to separate the shrinkable vectors from the unshrinkable ones.

		for (Iterator<SolutionVector<P>> iter = activeSet.iterator(); iter.hasNext();)
			{
			SolutionVector sv = iter.next();

			if (sv.isShrinkable(Gmax1, Gmax2))
				{
				iter.remove();
				inactiveSet.add(sv);
				}
			}

		Q.maintainCache(activeSet);
		}

	protected void calculate_rho(AlphaModel<L, P> si)
		{
		double r;
		int nr_free = 0;
		double ub = Double.POSITIVE_INFINITY, lb = Double.NEGATIVE_INFINITY, sum_free = 0;

		for (SolutionVector<P> sv : activeSet)
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

		si.rho = (float) r;		//return r;
		}
	}
