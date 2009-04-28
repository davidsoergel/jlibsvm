package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.binary.AlphaModel;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import org.apache.log4j.Logger;

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
	private static final Logger logger = Logger.getLogger(Solver.class);

	private static final int MAXITER = 50000;

	protected List<SolutionVector<P>> allExamples;
	//protected Collection<SolutionVector<P>> activeSet;
	protected SolutionVector<P>[] active;
	//protected Collection<SolutionVector<P>> inactiveSet;
	protected SolutionVector<P>[] inactive;

	//	int activeSize;//	boolean[] y;//	float[] G;// gradient of objective function

	//	static final byte LOWER_BOUND = 0;//	static final byte UPPER_BOUND = 1;	//	static final byte FREE = 2;//	Status[] alphaStatus;// LOWER_BOUND, UPPER_BOUND, FREE

	/**
	 * In the course of shrinking it's convenient to reorder the alpha array.
	 *///	float[] shuffledAlpha;

	/**
	 * This array maps the rearranged indices back to the original indices.
	 *///	int[] shuffledExampleIndexToOriginalIndex;


	QMatrix<P> Q;
	//	float[] QD;
	float[] Q_svA;
	float[] Q_svB;
	float[] Q_all;

	float eps;
	protected float Cp, Cn;//	float[] p;

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

		if (active.length == numExamples)
			{
			return;
			}

		//int i, j;
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
				{				//float[] Q_i = Q.getQ(i, activeSize);
				Q.getQ(svA, active, Q_svA);
				for (SolutionVector svB : active)
					{
					if (svB.isFree()) //is_free(j))
						{
						//assert Q_svA[svB.rank] == Q.evaluate(svA, svB);
						svA.G += svB.alpha * Q_svA[svB.rank];//[j]; //Q.evaluate(svA, svB); // ** TEST
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
					{					//	float[] Q_i = Q.getQ(i, numExamples);					//	float alpha_i = shuffledAlpha[i];
					Q.getQ(svA, active, inactive, Q_all);
					for (SolutionVector svB : inactive)
						{
						//assert Q_all[svB.rank] == Q.evaluate(svA, svB);
						svB.G += svA.alpha * Q_all[svB.rank]; //Q.evaluate(svA, svB); //** TEST  //
						//		svA.wasEvaluated = true;
						//		svB.wasEvaluated = true;
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


	public Solver(List<SolutionVector<P>> solutionVectors, QMatrix<P> Q, float Cp, float Cn, float eps,
	              boolean shrinking)
		{
		this(Q, Cp, Cn, eps, shrinking);

		this.allExamples = solutionVectors; //.toArray[EMPTY_SV_ARRAY];

		this.numExamples = allExamples.size();
		Q_all = new float[numExamples];
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

		Q.initRanks(allExamples);

		//		if (shuffledAlpha == null)//			{

		// initialize shuffledAlpha if needed (the constructor may or may not have already set it)//			shuffledAlpha = new float[numExamples];//			}

		// initialize alpha_status

		//		alphaStatus = new Status[numExamples];
		for (SolutionVector svA : allExamples)			//	for (int i = 0; i < numExamples; i++)
			{
			svA.updateAlphaStatus(Cp, Cn);			//update_alpha_status(i);
			}


		// initialize active set (for shrinking)

		initActiveSet();

		/*	shuffledExampleIndexToOriginalIndex = new int[numExamples];
		  for (int i = 0; i < numExamples; i++)
			  {
			  shuffledExampleIndexToOriginalIndex[i] = i;
			  }
		  activeSize = numExamples;
  */

		// initialize gradient

		//	G = new float[numExamples];		//	G_bar = new float[numExamples];
		for (SolutionVector svA : allExamples)
			{
			svA.G = svA.linearTerm;
			svA.G_bar = 0;
			}
		for (SolutionVector svA : allExamples)
			{

			if (!svA.isLowerBound()) //is_lower_bound(i))
				{				//	float[] Q_i = Q.getQ(i, numExamples);				//	float alpha_i = shuffledAlpha[i];

				//float[] Q_svA =
				Q.getQ(svA, active, Q_svA);
				for (SolutionVector svB : allExamples)
					{
					//	assert Q_svA[svB.rank] == Q.evaluate(svA, svB);
					svB.G += svA.alpha * Q_svA[svB.rank]; //Q.evaluate(svA, svB); // ** TEST //Q_svA[svB.rank];
					//	svA.wasEvaluated = true;
					//	svB.wasEvaluated = true;
					}
				if (svA.isUpperBound()) //is_upper_bound(i))
					{
					for (SolutionVector svB : allExamples)
						{
						//		assert Q_svA[svB.rank] == Q.evaluate(svA, svB);
						svB.G_bar += svA.getC(Cp, Cn)
								* Q_svA[svB.rank]; //Q.evaluate(svA, svB); // ** TEST Q_svA[svB.rank]; //getC(i) * Q_i[j];
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
			{			// show progress and do shrinking

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
				{				// reconstruct the whole gradient
				reconstruct_gradient();				// reset active set size and check
				resetActiveSet();				//activeSize = numExamples;


				// ** logging output disabled for now
				//logger.debug("*");
				// 			//svA = pair.svA;
				// 		//svB = pair.svB;

				pair = selectWorkingPair();
				if (pair.isOptimal) // pair already optimal
					{
					//svA = oldPair.svA;
					// 			//svB = oldPair.svB;
					break;
					}
				else
					{
					counter = 1;
					// do shrinking next iteration
					// 				// leave the working pair the same as before
					// 				//pair = oldPair;
					}
				}
			svA = pair.svA;
			svB = pair.svB;
			//			int i = working_set[0];
			// //			int j = working_set[1];

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

			//** TEST
			/*		for (SolutionVector<P> svC : active)
							   {
							   //svC.G += Q.evaluate(svC, svA) * delta_alpha_i + Q.evaluate(svC, svB) * delta_alpha_j;
							   assert Q_svA[svC.rank] == Q.evaluate(svA, svC);
							   assert Q_svB[svC.rank] == Q.evaluate(svB, svC);

							   }*/


			float C_i = svA.getC(Cp, Cn); //getC(i);
			float C_j = svB.getC(Cp, Cn); //getC(j);

			double old_alpha_i = svA.alpha; //shuffledAlpha[i];
			double old_alpha_j = svB.alpha; //shuffledAlpha[j];

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
				{				// pair was already optimal, but selectWorkingPair() didn't realize it because the numeric precision of float is insufficient with respect to eps
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
						svC.G_bar -= C_i * Q_all[svC.rank]; //Q.evaluate(svA, svC); //** TEST  //Q_all[svC.rank];
						//		svA.wasEvaluated = true;
						//		svC.wasEvaluated = true;
						}
					}
				else
					{
					for (SolutionVector<P> svC : allExamples)
						{
						//		assert Q_all[svC.rank] == Q.evaluate(svA, svC);
						svC.G_bar += C_i * Q_all[svC.rank]; //Q.evaluate(svA, svC); //** TEST  //Q_all[svC.rank];
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
						svC.G_bar -= C_j * Q_all[svC.rank]; //Q.evaluate(svB, svC); //** TEST  //Q_all[svC.rank];
						//		svB.wasEvaluated = true;
						//		svC.wasEvaluated = true;
						}
					}
				else
					{
					for (SolutionVector<P> svC : allExamples)
						{
						//		assert Q_all[svC.rank] == Q.evaluate(svB, svC);
						svC.G_bar += C_j * Q_all[svC.rank]; //Q.evaluate(svB, svC); //** TEST  //Q_all[svC.rank];
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

	protected void resetActiveSet()
		{
		active = allExamples.toArray(EMPTY_SV_ARRAY);
		Arrays.sort(active);
		inactive = EMPTY_SV_ARRAY;
		Q_svA = new float[active.length];
		Q_svB = new float[active.length];
		}

	/*
	protected void resetActiveSet()

		{

	//	Collection<SolutionVector<P>> activeSet = new ArrayList<SolutionVector<P>>(allExamples);
	//	Collection<SolutionVector<P>> inactiveSet = new ArrayList<SolutionVector<P>>(allExamples.size()); // it should get that big eventually
	//	active = activeSet.toArray(EMPTY_SV_ARRAY);
	//	inactive = inactiveSet.toArray(EMPTY_SV_ARRAY);

		//active = new SolutionVector[numExamples];
		//System.arraycopy(allExamples,0,active,0,numExamples);

		// Can't do this since we need to maintain the sort order
		//active = allExamples.toArray(EMPTY_SV_ARRAY);


		// MAINTAINING RANK ORDER
		// When we run Q.getQ, the buffer is filled in rank order; so we need to make sure that the active and inactive arrays are always in rank order too

		List<SolutionVector<P>> activeList = new ArrayList<SolutionVector<P>>(numExamples);

		//active = new SolutionVector[numExamples];

		// the active ones must be in rank order, right?
		activeList.addAll(Arrays.asList(active));
		activeList.addAll(Arrays.asList(inactive));

		active = activeList.toArray(EMPTY_SV_ARRAY);

		//Q_svA = new float[active.length];
		//Q_svB = new float[active.length];
		inactive = EMPTY_SV_ARRAY;
		}
	// return 1 if already optimal, return 0 otherwise	//boolean select_working_set(int[] working_set)	//	{

	//	}
*/

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

	protected final static SolutionVector[] EMPTY_SV_ARRAY = new SolutionVector[0];

	protected SolutionVectorPair selectWorkingPair()
		{		/*
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

		//SolutionVector[] active = activeSet.toArray(EMPTY_SV_ARRAY);

		//for (SolutionVector sv : active)
		int l = active.length;
		for (int i = 0; i < l; i++)
			{			//	for (int t = 0; t < activeSize; t++)			//		{
			SolutionVector<P> sv = active[i];
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
		// //float[] Q_i = null;
		// //if (GmaxSV != null)
		// //i != -1)
		// // null Q_i not accessed: Gmax=Float.NEGATIVE_INFINITY if i=-1
		// 	//	{
		// 	//	Q_i = Q.getQ(GmaxSV, activeSize);
		// 	//	}

		// PERF this is where cache locality issues kick in big time.

		// Q.prefetch(GmaxSV);  // this can be built in to the cache itself
		//float[] Q_GmaxSV =
		if (GmaxSV != null)  // => Gmax == Float.NEGATIVE_INFINITY
			{
			Q.getQ(GmaxSV, active, Q_svA);
			}

		//for (SolutionVector sv : active)
		//int l = active.length;
		for (int i = 0; i < l; i++)
			{			//for (int j = 0; j < activeSize; j++)			//	{
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
						//	GmaxSV.wasEvaluated = true;
						//	sv.wasEvaluated = true;

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
						//	GmaxSV.wasEvaluated = true;
						//	sv.wasEvaluated = true;
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
		   }*/		/*
		if(Gmax + Gmax2 < Math.ulp(Gmax) )
			{
			logger.warn("Pair is optimal within available numeric precision of " + Math.ulp(Gmax) + ", but this is still larger than requested eps = " + eps + ".");
			return null;
			}
			*/

		//working_set[0] = Gmax_idx;		//working_set[1] = Gmin_idx;		//	return new SolutionVectorPair(GmaxSV, GminSV, false);
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

		si.rho = (float) r;		//return r;
		}
	}
