package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;

/**
 * Solver for nu-svm classification and regression
 * <p/>
 * additional constraint: e^T \alpha = constant
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class Solver_NU extends Solver
	{

	//private SolutionInfoNu si;

	public Solver_NU(QMatrix Q, float[] p, boolean[] y, float[] initAlpha, float Cp, float Cn, float eps,
	                 boolean shrinking)
		{
		super(Q, p, y, Cp, Cn, eps, shrinking);
		shuffledAlpha = initAlpha;
		//	this.si = si;
		}

	/*	public SolutionInfo Solve(int l, QMatrix Q, float[] p, byte[] y, float[] alpha, float Cp, float Cn, float eps,
				SolutionInfoNu si, int shrinking)
		 {
	 //	this.si = si;
	 //	super.Solve(l, Q, p, y, alpha, Cp, Cn, eps, si, shrinking);
		 return super.Solve();
		 }
 */
	// return 1 if already optimal, return 0 otherwise
	boolean select_working_set(int[] working_set)
		{
		// return i,j such that y_i = y_j and
		// i: maximizes -y_i * grad(f)_i, i in I_up(\alpha)
		// j: minimizes the decrease of obj value
		//    (if quadratic coefficeint <= 0, replace it with tau)
		//    -y_j*grad(f)_j < -y_i*grad(f)_i, j in I_low(\alpha)

		float Gmaxp = Float.NEGATIVE_INFINITY;
		float Gmaxp2 = Float.NEGATIVE_INFINITY;
		int Gmaxp_idx = -1;

		float Gmaxn = Float.NEGATIVE_INFINITY;
		float Gmaxn2 = Float.NEGATIVE_INFINITY;
		int Gmaxn_idx = -1;

		int Gmin_idx = -1;
		float obj_diff_min = Float.POSITIVE_INFINITY;

		for (int t = 0; t < active_size; t++)
			{
			if (y[t])
				{
				if (!is_upper_bound(t))
					{
					if (-G[t] >= Gmaxp)
						{
						Gmaxp = -G[t];
						Gmaxp_idx = t;
						}
					}
				}
			else
				{
				if (!is_lower_bound(t))
					{
					if (G[t] >= Gmaxn)
						{
						Gmaxn = G[t];
						Gmaxn_idx = t;
						}
					}
				}
			}

		int ip = Gmaxp_idx;
		int in = Gmaxn_idx;
		float[] Q_ip = null;
		float[] Q_in = null;
		if (ip != -1)// null Q_ip not accessed: Gmaxp=Float.NEGATIVE_INFINITY if ip=-1
			{
			Q_ip = Q.getQ(ip, active_size);
			}
		if (in != -1)
			{
			Q_in = Q.getQ(in, active_size);
			}

		for (int j = 0; j < active_size; j++)
			{
			if (y[j])
				{
				if (!is_lower_bound(j))
					{
					float grad_diff = Gmaxp + G[j];
					if (G[j] >= Gmaxp2)
						{
						Gmaxp2 = G[j];
						}
					if (grad_diff > 0)
						{
						float obj_diff;
						float quad_coef = Q_ip[ip] + QD[j] - 2 * Q_ip[j];
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
							Gmin_idx = j;
							obj_diff_min = obj_diff;
							}
						}
					}
				}
			else
				{
				if (!is_upper_bound(j))
					{
					float grad_diff = Gmaxn - G[j];
					if (-G[j] >= Gmaxn2)
						{
						Gmaxn2 = -G[j];
						}
					if (grad_diff > 0)
						{
						float obj_diff;
						float quad_coef = Q_in[in] + QD[j] - 2 * Q_in[j];
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
							Gmin_idx = j;
							obj_diff_min = obj_diff;
							}
						}
					}
				}
			}

		if (Math.max(Gmaxp + Gmaxp2, Gmaxn + Gmaxn2) < eps)
			{
			return true;
			}

		if (y[Gmin_idx])
			{
			working_set[0] = Gmaxp_idx;
			}
		else
			{
			working_set[0] = Gmaxn_idx;
			}
		working_set[1] = Gmin_idx;

		return false;
		}

	private boolean be_shrunk(int i, float Gmax1, float Gmax2, float Gmax3, float Gmax4)
		{
		if (is_upper_bound(i))
			{
			if (y[i])
				{
				return (-G[i] > Gmax1);
				}
			else
				{
				return (-G[i] > Gmax4);
				}
			}
		else if (is_lower_bound(i))
			{
			if (y[i])
				{
				return (G[i] > Gmax2);
				}
			else
				{
				return (G[i] > Gmax3);
				}
			}
		else
			{
			return (false);
			}
		}

	void do_shrinking()
		{
		float Gmax1 = Float.NEGATIVE_INFINITY;// max { -y_i * grad(f)_i | y_i = +1, i in I_up(\alpha) }
		float Gmax2 = Float.NEGATIVE_INFINITY;// max { y_i * grad(f)_i | y_i = +1, i in I_low(\alpha) }
		float Gmax3 = Float.NEGATIVE_INFINITY;// max { -y_i * grad(f)_i | y_i = -1, i in I_up(\alpha) }
		float Gmax4 = Float.NEGATIVE_INFINITY;// max { y_i * grad(f)_i | y_i = -1, i in I_low(\alpha) }

		// find maximal violating pair first
		int i;
		for (i = 0; i < active_size; i++)
			{
			if (!is_upper_bound(i))
				{
				if (y[i])
					{
					if (-G[i] > Gmax1)
						{
						Gmax1 = -G[i];
						}
					}
				else if (-G[i] > Gmax4)
					{
					Gmax4 = -G[i];
					}
				}
			if (!is_lower_bound(i))
				{
				if (y[i])
					{
					if (G[i] > Gmax2)
						{
						Gmax2 = G[i];
						}
					}
				else if (G[i] > Gmax3)
					{
					Gmax3 = G[i];
					}
				}
			}

		if (unshrink == false && Math.max(Gmax1 + Gmax2, Gmax3 + Gmax4) <= eps * 10)
			{
			unshrink = true;
			reconstruct_gradient();
			active_size = l;
			}

		for (i = 0; i < active_size; i++)
			{
			if (be_shrunk(i, Gmax1, Gmax2, Gmax3, Gmax4))
				{
				active_size--;
				while (active_size > i)
					{
					if (!be_shrunk(active_size, Gmax1, Gmax2, Gmax3, Gmax4))
						{
						swap_index(i, active_size);
						break;
						}
					active_size--;
					}
				}
			}
		}

	@Override
	void calculate_rho(BinaryModel si)
		{
		int nr_free1 = 0, nr_free2 = 0;
		float ub1 = Float.POSITIVE_INFINITY, ub2 = Float.POSITIVE_INFINITY;
		float lb1 = Float.NEGATIVE_INFINITY, lb2 = Float.NEGATIVE_INFINITY;
		float sum_free1 = 0, sum_free2 = 0;

		for (int i = 0; i < active_size; i++)
			{
			if (y[i])
				{
				if (is_lower_bound(i))
					{
					ub1 = Math.min(ub1, G[i]);
					}
				else if (is_upper_bound(i))
					{
					lb1 = Math.max(lb1, G[i]);
					}
				else
					{
					++nr_free1;
					sum_free1 += G[i];
					}
				}
			else
				{
				if (is_lower_bound(i))
					{
					ub2 = Math.min(ub2, G[i]);
					}
				else if (is_upper_bound(i))
					{
					lb2 = Math.max(lb2, G[i]);
					}
				else
					{
					++nr_free2;
					sum_free2 += G[i];
					}
				}
			}

		float r1, r2;
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

		si.r = (r1 + r2) / 2;
		si.rho = (r1 - r2) / 2;
		}
	}
