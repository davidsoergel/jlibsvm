package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.binary.AlphaModel;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Solver for nu-svm classification and regression
 * <p/>
 * additional constraint: e^T \alpha = constant
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class Solver_NU<L extends Comparable, P> extends Solver<L, P>
	{
// --------------------------- CONSTRUCTORS ---------------------------

/*	protected Solver_NU(QMatrix<P> Q, float Cp, float Cn, float eps, boolean shrinking)
		{
		super(Q, Cp, Cn, eps, shrinking);
		}
*/
	public Solver_NU(@NotNull List<SolutionVector<P>> solutionVectors, @NotNull QMatrix<P> Q, float Cp, float Cn, float eps,
	                 boolean shrinking)
		{
		super(solutionVectors, Q, Cp, Cn, eps, shrinking);
		}

// -------------------------- OTHER METHODS --------------------------

	@Override
	protected void calculate_rho(AlphaModel<L, P> model)
		{
		int nr_free1 = 0, nr_free2 = 0;
		double ub1 = Double.POSITIVE_INFINITY, ub2 = Double.POSITIVE_INFINITY;
		double lb1 = Double.NEGATIVE_INFINITY, lb2 = Double.NEGATIVE_INFINITY;
		double sum_free1 = 0, sum_free2 = 0;


		for (SolutionVector<P> sv : allExamples)
			{
			if (sv.targetValue)
				{
				if (sv.isLowerBound())
					{
					ub1 = Math.min(ub1, sv.G);
					}
				else if (sv.isUpperBound())
					{
					lb1 = Math.max(lb1, sv.G);
					}
				else
					{
					++nr_free1;
					sum_free1 += sv.G;
					}
				}
			else
				{
				if (sv.isLowerBound())
					{
					ub2 = Math.min(ub2, sv.G);
					}
				else if (sv.isUpperBound())
					{
					lb2 = Math.max(lb2, sv.G);
					}
				else
					{
					++nr_free2;
					sum_free2 += sv.G;
					}
				}
			}

		double r1, r2;
		if (nr_free1 > 0)
			{
			r1 = sum_free1 / nr_free1;
			}
		else
			{
			r1 = (ub1 + lb1) / 2;
			}

		if (nr_free2 > 0)
			{
			r2 = sum_free2 / nr_free2;
			}
		else
			{
			r2 = (ub2 + lb2) / 2;
			}

		((BinaryModel) model).r = (float) ((r1 + r2) / 2);
		model.rho = (float) ((r1 - r2) / 2);
		}

	void do_shrinking()
		{
		double Gmax1 = Double.NEGATIVE_INFINITY;// max { -y_i * grad(f)_i | y_i = +1, i in I_up(\alpha) }
		double Gmax2 = Double.NEGATIVE_INFINITY;// max { y_i * grad(f)_i | y_i = +1, i in I_low(\alpha) }
		double Gmax3 = Double.NEGATIVE_INFINITY;// max { -y_i * grad(f)_i | y_i = -1, i in I_up(\alpha) }
		double Gmax4 = Double.NEGATIVE_INFINITY;// max { y_i * grad(f)_i | y_i = -1, i in I_low(\alpha) }

		// find maximal violating pair first
		for (SolutionVector<P> sv : active)
			{
			if (!sv.isUpperBound())
				{
				if (sv.targetValue)
					{
					if (-sv.G > Gmax1)
						{
						Gmax1 = -sv.G;
						}
					}
				else if (-sv.G > Gmax4)
					{
					Gmax4 = -sv.G;
					}
				}
			if (!sv.isLowerBound())
				{
				if (sv.targetValue)
					{
					if (sv.G > Gmax2)
						{
						Gmax2 = sv.G;
						}
					}
				else if (sv.G > Gmax3)
					{
					Gmax3 = sv.G;
					}
				}
			}

		if (!unshrink && Math.max(Gmax1 + Gmax2, Gmax3 + Gmax4) <= eps * 10)
			{
			unshrink = true;
			reconstruct_gradient();
			resetActiveSet();
			}


		// There was an extremely messy iteration here before, but I think it served only to separate the shrinkable vectors from the unshrinkable ones.

		Collection<SolutionVector<P>> activeList =
				new ArrayList<SolutionVector<P>>(Arrays.asList(active)); //Arrays.asList(active);
		Collection<SolutionVector<P>> inactiveList = new ArrayList<SolutionVector<P>>();

		// note the ordering: newly inactive SVs go at the beginning of the inactive list, maintaining order

		for (Iterator<SolutionVector<P>> iter = activeList.iterator(); iter.hasNext();)
			{
			SolutionVector sv = iter.next();

			if (sv.isShrinkable(Gmax1, Gmax2, Gmax3, Gmax4))
				{
				iter.remove();
				inactiveList.add(sv);
				}
			}

		active = activeList.toArray(EMPTY_SV_ARRAY);
		SolutionVector<P>[] newlyInactive = inactiveList.toArray(EMPTY_SV_ARRAY);
		Q.maintainCache(active, newlyInactive);

		// previously inactive SVs come after that
		inactiveList.addAll(Arrays.asList(inactive));
		inactive = inactiveList.toArray(EMPTY_SV_ARRAY);
		}

	// return null if optimal

	@Override
	protected SolutionVectorPair selectWorkingPair()
		{
		// return i,j such that y_i = y_j and
		// i: maximizes -y_i * grad(f)_i, i in I_up(\alpha)
		// j: minimizes the decrease of obj value
		//    (if quadratic coefficeint <= 0, replace it with tau)
		//    -y_j*grad(f)_j < -y_i*grad(f)_i, j in I_low(\alpha)

		double Gmaxp = Float.NEGATIVE_INFINITY;
		double Gmaxp2 = Float.NEGATIVE_INFINITY;

		double Gmaxn = Float.NEGATIVE_INFINITY;
		double Gmaxn2 = Float.NEGATIVE_INFINITY;

		SolutionVector GmaxnSV = null;
		SolutionVector GmaxpSV = null;

		SolutionVector GminSV = null;

		double obj_diff_min = Float.POSITIVE_INFINITY;

		for (SolutionVector sv : active)
			{
			if (sv.targetValue)
				{
				if (!sv.isUpperBound())
					{
					if (-sv.G >= Gmaxp)
						{
						Gmaxp = -sv.G;
						GmaxpSV = sv;
						}
					}
				}
			else
				{
				if (!sv.isLowerBound())
					{
					if (sv.G >= Gmaxn)
						{
						Gmaxn = sv.G;
						GmaxnSV = sv;
						}
					}
				}
			}

		Q.getQ(GmaxpSV, active, Q_svA);
		Q.getQ(GmaxnSV, active, Q_svB);

		for (SolutionVector sv : active)
			{
			if (sv.targetValue)
				{
				if (!sv.isLowerBound())
					{
					double grad_diff = Gmaxp + sv.G;
					if (sv.G >= Gmaxp2)
						{
						Gmaxp2 = sv.G;
						}
					if (grad_diff > 0)
						{
						double obj_diff;
						double quad_coef = Q.evaluateDiagonal(GmaxpSV) + Q.evaluateDiagonal(sv)
								- 2.0f * Q_svA[sv.rank]; //Q_GmaxpSV[sv.rank];
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
					double grad_diff = Gmaxn - sv.G;
					if (-sv.G >= Gmaxn2)
						{
						Gmaxn2 = -sv.G;
						}
					if (grad_diff > 0)
						{
						double obj_diff;


						double quad_coef = Q.evaluateDiagonal(GmaxnSV) + Q.evaluateDiagonal(sv)
								- 2.0f * Q_svB[sv.rank]; //Q_GmaxnSV[sv.rank];

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


		return new SolutionVectorPair(GminSV.targetValue ? GmaxpSV : GmaxnSV, GminSV,
		                              Math.max(Gmaxp + Gmaxp2, Gmaxn + Gmaxn2) < eps);
		}
	}
