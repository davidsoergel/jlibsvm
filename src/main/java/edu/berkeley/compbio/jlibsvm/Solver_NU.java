package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.binary.AlphaModel;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;

import java.util.Collection;
import java.util.Iterator;

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

	protected Solver_NU(QMatrix<P> Q, float Cp, float Cn, float eps, boolean shrinking)
		{
		super(Q, Cp, Cn, eps, shrinking);
		}


	public Solver_NU(Collection<SolutionVector<P>> solutionVectors, QMatrix<P> Q, float Cp, float Cn, float eps,
	                 boolean shrinking)
		{
		super(solutionVectors, Q, Cp, Cn, eps, shrinking);
		}

	/*	public SolutionInfo Solve(int l, QMatrix Q, float[] p, byte[] y, float[] alpha, float Cp, float Cn, float eps,
				SolutionInfoNu si, int shrinking)
		 {
	 //	this.si = si;
	 //	super.Solve(l, Q, p, y, alpha, Cp, Cn, eps, si, shrinking);
		 return super.Solve();
		 }
 */

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
		//int Gmaxp_idx = -1;

		double Gmaxn = Float.NEGATIVE_INFINITY;
		double Gmaxn2 = Float.NEGATIVE_INFINITY;
		//int Gmaxn_idx = -1;


		SolutionVector GmaxnSV = null; //-1;
		SolutionVector GmaxpSV = null; //-1;

		SolutionVector GminSV = null; //-1;

		//int Gmin_idx = -1;
		double obj_diff_min = Float.POSITIVE_INFINITY;

		for (SolutionVector sv : activeSet)
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

		/*
		int ip = Gmaxp_idx;
		int in = Gmaxn_idx;
		float[] Q_ip = null;
		float[] Q_in = null;
		if (ip != -1)// null Q_ip not accessed: Gmaxp=Float.NEGATIVE_INFINITY if ip=-1
			{
			Q_ip = Q.getQ(ip, activeSize);
			}
		if (in != -1)
			{
			Q_in = Q.getQ(in, activeSize);
			}
			*/

		for (SolutionVector sv : activeSet)
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
						//float quad_coef = Q_ip[ip] + QD[j] - 2 * Q_ip[j];
						double quad_coef =
								Q.evaluate(GmaxpSV, GmaxpSV) + Q.evaluate(sv, sv) - 2.0f * Q.evaluate(GmaxpSV, sv);
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
						//double quad_coef = Q_in[in] + QD[j] - 2 * Q_in[j];


						double quad_coef =
								Q.evaluate(GmaxnSV, GmaxnSV) + Q.evaluate(sv, sv) - 2.0f * Q.evaluate(GmaxnSV, sv);

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

		/*	if (Math.max(Gmaxp + Gmaxp2, Gmaxn + Gmaxn2) < eps)
			  {
			  return null;
			  }
  */
		/*	if (y[Gmin_idx])
			  {
			  working_set[0] = Gmaxp_idx;
			  }
		  else
			  {
			  working_set[0] = Gmaxn_idx;
			  }
		  working_set[1] = Gmin_idx;
  */

		return new SolutionVectorPair(GminSV.targetValue ? GmaxpSV : GmaxnSV, GminSV,
		                              Math.max(Gmaxp + Gmaxp2, Gmaxn + Gmaxn2) < eps);
		}


	void do_shrinking()
		{
		double Gmax1 = Double.NEGATIVE_INFINITY;// max { -y_i * grad(f)_i | y_i = +1, i in I_up(\alpha) }
		double Gmax2 = Double.NEGATIVE_INFINITY;// max { y_i * grad(f)_i | y_i = +1, i in I_low(\alpha) }
		double Gmax3 = Double.NEGATIVE_INFINITY;// max { -y_i * grad(f)_i | y_i = -1, i in I_up(\alpha) }
		double Gmax4 = Double.NEGATIVE_INFINITY;// max { y_i * grad(f)_i | y_i = -1, i in I_low(\alpha) }

		// find maximal violating pair first
		for (SolutionVector<P> sv : activeSet)
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

		for (Iterator<SolutionVector<P>> iter = activeSet.iterator(); iter.hasNext();)
			{
			SolutionVector sv = iter.next();

			if (sv.isShrinkable(Gmax1, Gmax2, Gmax3, Gmax4))
				{
				iter.remove();
				inactiveSet.add(sv);
				}
			}
		}

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
	}
