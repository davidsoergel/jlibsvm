package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;


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

public class Solver
	{
	int active_size;
	boolean[] y;
	float[] G;// gradient of objective function
	static final byte LOWER_BOUND = 0;
	static final byte UPPER_BOUND = 1;
	static final byte FREE = 2;
	byte[] alpha_status;// LOWER_BOUND, UPPER_BOUND, FREE

	/**
	 * In the course of shrinking it's convenient to reorder the alpha array.
	 */
	float[] shuffledAlpha;

	/**
	 * This array maps the rearranged indices back to the original indices.
	 */
	int[] shuffledExampleIndexToOriginalIndex;


	QMatrix Q;
	float[] QD;
	float eps;
	float Cp, Cn;
	float[] p;

	float[] G_bar;// gradient, if we treat free variables as 0
	int l;
	boolean unshrink;// XXX
	boolean shrinking;


	float get_C(int i)
		{
		return (y[i]) ? Cp : Cn;
		}

	void update_alpha_status(int i)
		{
		if (shuffledAlpha[i] >= get_C(i))
			{
			alpha_status[i] = UPPER_BOUND;
			}
		else if (shuffledAlpha[i] <= 0)
			{
			alpha_status[i] = LOWER_BOUND;
			}
		else
			{
			alpha_status[i] = FREE;
			}
		}

	boolean is_upper_bound(int i)
		{
		return alpha_status[i] == UPPER_BOUND;
		}

	boolean is_lower_bound(int i)
		{
		return alpha_status[i] == LOWER_BOUND;
		}

	boolean is_free(int i)
		{
		return alpha_status[i] == FREE;
		}

	void swap_index(int i, int j)
		{
		Q.swapIndex(i, j);
		do
			{
			boolean _ = y[i];
			y[i] = y[j];
			y[j] = _;
			}
		while (false);
		do
			{
			float _ = G[i];
			G[i] = G[j];
			G[j] = _;
			}
		while (false);
		do
			{
			byte _ = alpha_status[i];
			alpha_status[i] = alpha_status[j];
			alpha_status[j] = _;
			}
		while (false);
		do
			{
			float _ = shuffledAlpha[i];
			shuffledAlpha[i] = shuffledAlpha[j];
			shuffledAlpha[j] = _;
			}
		while (false);
		do
			{
			float _ = p[i];
			p[i] = p[j];
			p[j] = _;
			}
		while (false);
		do
			{
			int _ = shuffledExampleIndexToOriginalIndex[i];
			shuffledExampleIndexToOriginalIndex[i] = shuffledExampleIndexToOriginalIndex[j];
			shuffledExampleIndexToOriginalIndex[j] = _;
			}
		while (false);
		do
			{
			float _ = G_bar[i];
			G_bar[i] = G_bar[j];
			G_bar[j] = _;
			}
		while (false);
		}

	void reconstruct_gradient()
		{		// reconstruct inactive elements of G from G_bar and free variables

		if (active_size == l)
			{
			return;
			}

		int i, j;
		int nr_free = 0;

		for (j = active_size; j < l; j++)
			{
			G[j] = G_bar[j] + p[j];
			}

		for (j = 0; j < active_size; j++)
			{
			if (is_free(j))
				{
				nr_free++;
				}
			}

		if (2 * nr_free < active_size)
			{
			System.out.print("\nWarning: using -h 0 may be faster\n");
			}

		if (nr_free * l > 2 * active_size * (l - active_size))
			{
			for (i = active_size; i < l; i++)
				{
				float[] Q_i = Q.getQ(i, active_size);
				for (j = 0; j < active_size; j++)
					{
					if (is_free(j))
						{
						G[i] += shuffledAlpha[j] * Q_i[j];
						}
					}
				}
			}
		else
			{
			for (i = 0; i < active_size; i++)
				{
				if (is_free(i))
					{
					float[] Q_i = Q.getQ(i, l);
					float alpha_i = shuffledAlpha[i];
					for (j = active_size; j < l; j++)
						{
						G[j] += alpha_i * Q_i[j];
						}
					}
				}
			}
		}


	public Solver(QMatrix Q, float[] p, boolean[] y, float Cp, float Cn, float eps, boolean shrinking)
		{
		if (eps <= 0)
			{
			throw new SvmException("eps <= 0");
			}

		this.Q = Q;
		this.QD = Q.getQD();
		this.l = y.length;
		this.p = p.clone();
		this.y = y.clone();
		this.Cp = Cp;
		this.Cn = Cn;
		this.eps = eps;
		this.unshrink = false;

		this.shrinking = shrinking;
		}

	public BinaryModel Solve()
		{

		if (shuffledAlpha == null)
			{

			// initialize shuffledAlpha if needed (the constructor may or may not have already set it)
			shuffledAlpha = new float[l];
			}

		// initialize alpha_status
		{
		alpha_status = new byte[l];
		for (int i = 0; i < l; i++)
			{
			update_alpha_status(i);
			}
		}

		// initialize active set (for shrinking)
		{
		shuffledExampleIndexToOriginalIndex = new int[l];
		for (int i = 0; i < l; i++)
			{
			shuffledExampleIndexToOriginalIndex[i] = i;
			}
		active_size = l;
		}

		// initialize gradient
		{
		G = new float[l];
		G_bar = new float[l];
		int i;
		for (i = 0; i < l; i++)
			{
			G[i] = p[i];
			G_bar[i] = 0;
			}
		for (i = 0; i < l; i++)
			{
			if (!is_lower_bound(i))
				{
				float[] Q_i = Q.getQ(i, l);
				float alpha_i = shuffledAlpha[i];
				int j;
				for (j = 0; j < l; j++)
					{
					G[j] += alpha_i * Q_i[j];
					}
				if (is_upper_bound(i))
					{
					for (j = 0; j < l; j++)
						{
						G_bar[j] += get_C(i) * Q_i[j];
						}
					}
				}
			}
		}

		// optimization step

		int iter = 0;
		int counter = Math.min(l, 1000) + 1;
		int[] working_set = new int[2];

		while (true)
			{			// show progress and do shrinking

			if (--counter == 0)
				{
				counter = Math.min(l, 1000);
				if (shrinking)
					{
					do_shrinking();
					}
				System.err.print(".");
				}

			if (select_working_set(working_set))
				{				// reconstruct the whole gradient
				reconstruct_gradient();				// reset active set size and check
				active_size = l;
				System.err.print("*");
				if (select_working_set(working_set))
					{
					break;
					}
				else
					{
					counter = 1;// do shrinking next iteration
					}
				}

			int i = working_set[0];
			int j = working_set[1];

			++iter;

			// update alpha[i] and alpha[j], handle bounds carefully

			float[] Q_i = Q.getQ(i, active_size);
			float[] Q_j = Q.getQ(j, active_size);

			float C_i = get_C(i);
			float C_j = get_C(j);

			float old_alpha_i = shuffledAlpha[i];
			float old_alpha_j = shuffledAlpha[j];

			if (y[i] != y[j])
				{
				float quad_coef = Q_i[i] + Q_j[j] + 2 * Q_i[j];
				if (quad_coef <= 0)
					{
					quad_coef = 1e-12f;
					}
				float delta = (-G[i] - G[j]) / quad_coef;
				float diff = shuffledAlpha[i] - shuffledAlpha[j];
				shuffledAlpha[i] += delta;
				shuffledAlpha[j] += delta;

				if (diff > 0)
					{
					if (shuffledAlpha[j] < 0)
						{
						shuffledAlpha[j] = 0;
						shuffledAlpha[i] = diff;
						}
					}
				else
					{
					if (shuffledAlpha[i] < 0)
						{
						shuffledAlpha[i] = 0;
						shuffledAlpha[j] = -diff;
						}
					}
				if (diff > C_i - C_j)
					{
					if (shuffledAlpha[i] > C_i)
						{
						shuffledAlpha[i] = C_i;
						shuffledAlpha[j] = C_i - diff;
						}
					}
				else
					{
					if (shuffledAlpha[j] > C_j)
						{
						shuffledAlpha[j] = C_j;
						shuffledAlpha[i] = C_j + diff;
						}
					}
				}
			else
				{
				float quad_coef = Q_i[i] + Q_j[j] - 2 * Q_i[j];
				if (quad_coef <= 0)
					{
					quad_coef = 1e-12f;
					}
				float delta = (G[i] - G[j]) / quad_coef;
				float sum = shuffledAlpha[i] + shuffledAlpha[j];
				shuffledAlpha[i] -= delta;
				shuffledAlpha[j] += delta;

				if (sum > C_i)
					{
					if (shuffledAlpha[i] > C_i)
						{
						shuffledAlpha[i] = C_i;
						shuffledAlpha[j] = sum - C_i;
						}
					}
				else
					{
					if (shuffledAlpha[j] < 0)
						{
						shuffledAlpha[j] = 0;
						shuffledAlpha[i] = sum;
						}
					}
				if (sum > C_j)
					{
					if (shuffledAlpha[j] > C_j)
						{
						shuffledAlpha[j] = C_j;
						shuffledAlpha[i] = sum - C_j;
						}
					}
				else
					{
					if (shuffledAlpha[i] < 0)
						{
						shuffledAlpha[i] = 0;
						shuffledAlpha[j] = sum;
						}
					}
				}

			// update G

			float delta_alpha_i = shuffledAlpha[i] - old_alpha_i;
			float delta_alpha_j = shuffledAlpha[j] - old_alpha_j;

			for (int k = 0; k < active_size; k++)
				{
				G[k] += Q_i[k] * delta_alpha_i + Q_j[k] * delta_alpha_j;
				}

			// update alpha_status and G_bar

			{
			boolean ui = is_upper_bound(i);
			boolean uj = is_upper_bound(j);
			update_alpha_status(i);
			update_alpha_status(j);
			int k;
			if (ui != is_upper_bound(i))
				{
				Q_i = Q.getQ(i, l);
				if (ui)
					{
					for (k = 0; k < l; k++)
						{
						G_bar[k] -= C_i * Q_i[k];
						}
					}
				else
					{
					for (k = 0; k < l; k++)
						{
						G_bar[k] += C_i * Q_i[k];
						}
					}
				}

			if (uj != is_upper_bound(j))
				{
				Q_j = Q.getQ(j, l);
				if (uj)
					{
					for (k = 0; k < l; k++)
						{
						G_bar[k] -= C_j * Q_j[k];
						}
					}
				else
					{
					for (k = 0; k < l; k++)
						{
						G_bar[k] += C_j * Q_j[k];
						}
					}
				}
			}
			}


		BinaryModel model = new BinaryModel(null, null);

		// calculate rho

		//		si.rho =
		calculate_rho(model);

		// calculate objective value
		{
		float v = 0;
		int i;
		for (i = 0; i < l; i++)
			{
			v += shuffledAlpha[i] * (G[i] + p[i]);
			}

		model.obj = v / 2;
		}

		// put the solution, mapping the alphas back to their original order

		// note the swapping process applied to the Q matrix as well, so we have to map that back too

		model.alpha = new float[l];
		model.supportVectors = new SvmPoint[l];
		for (int i = 0; i < l; i++)
			{
			model.alpha[shuffledExampleIndexToOriginalIndex[i]] = shuffledAlpha[i];
			model.supportVectors[shuffledExampleIndexToOriginalIndex[i]] = Q.getVectors()[i];
			}

		// note at this point the solution includes _all_ vectors, even if their alphas are zero

		// we can't do this yet because in the regression case there are twice as many alphas as vectors
		// model.compact();

		model.upperBoundPositive = Cp;
		model.upperBoundNegative = Cn;

		System.out.print("\noptimization finished, #iter = " + iter + "\n");

		return model;
		}

	// return 1 if already optimal, return 0 otherwise
	boolean select_working_set(int[] working_set)
		{

		/*
		return i,j such that
		i: maximizes -y_i * grad(f)_i, i in I_up(\alpha)
		j: mimimizes the decrease of obj value
		(if quadratic coefficeint <= 0, replace it with tau)
		 -y_j*grad(f)_j < -y_i*grad(f)_i, j in I_low(\alpha)
		*/

		float Gmax = Float.NEGATIVE_INFINITY;
		float Gmax2 = Float.NEGATIVE_INFINITY;
		int Gmax_idx = -1;
		int Gmin_idx = -1;
		float obj_diff_min = Float.POSITIVE_INFINITY;

		for (int t = 0; t < active_size; t++)
			{
			if (y[t])
				{
				if (!is_upper_bound(t))
					{
					if (-G[t] >= Gmax)
						{
						Gmax = -G[t];
						Gmax_idx = t;
						}
					}
				}
			else
				{
				if (!is_lower_bound(t))
					{
					if (G[t] >= Gmax)
						{
						Gmax = G[t];
						Gmax_idx = t;
						}
					}
				}
			}

		int i = Gmax_idx;
		float[] Q_i = null;
		if (i != -1)// null Q_i not accessed: Gmax=Float.NEGATIVE_INFINITY if i=-1
			{
			Q_i = Q.getQ(i, active_size);
			}

		for (int j = 0; j < active_size; j++)
			{
			if (y[j])
				{
				if (!is_lower_bound(j))
					{
					float grad_diff = Gmax + G[j];
					if (G[j] >= Gmax2)
						{
						Gmax2 = G[j];
						}
					if (grad_diff > 0)
						{
						float obj_diff;
						float quad_coef = Q_i[i] + QD[j] - 2.0f * (y[i] ? 1f : -1f) * Q_i[j];
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
					float grad_diff = Gmax - G[j];
					if (-G[j] >= Gmax2)
						{
						Gmax2 = -G[j];
						}
					if (grad_diff > 0)
						{
						float obj_diff;
						float quad_coef = Q_i[i] + QD[j] + 2.0f * (y[i] ? 1f : -1f) * Q_i[j];
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

		if (Gmax + Gmax2 < eps)
			{
			return true;
			}

		working_set[0] = Gmax_idx;
		working_set[1] = Gmin_idx;
		return false;
		}

	private boolean be_shrunk(int i, float Gmax1, float Gmax2)
		{
		if (is_upper_bound(i))
			{
			if (y[i])
				{
				return (-G[i] > Gmax1);
				}
			else
				{
				return (-G[i] > Gmax2);
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
				return (G[i] > Gmax1);
				}
			}
		else
			{
			return (false);
			}
		}

	void do_shrinking()
		{
		int i;
		float Gmax1 = Float.NEGATIVE_INFINITY;// max { -y_i * grad(f)_i | i in I_up(\alpha) }
		float Gmax2 = Float.NEGATIVE_INFINITY;// max { y_i * grad(f)_i | i in I_low(\alpha) }

		// find maximal violating pair first
		for (i = 0; i < active_size; i++)
			{
			if (y[i])
				{
				if (!is_upper_bound(i))
					{
					if (-G[i] >= Gmax1)
						{
						Gmax1 = -G[i];
						}
					}
				if (!is_lower_bound(i))
					{
					if (G[i] >= Gmax2)
						{
						Gmax2 = G[i];
						}
					}
				}
			else
				{
				if (!is_upper_bound(i))
					{
					if (-G[i] >= Gmax2)
						{
						Gmax2 = -G[i];
						}
					}
				if (!is_lower_bound(i))
					{
					if (G[i] >= Gmax1)
						{
						Gmax1 = G[i];
						}
					}
				}
			}

		if (unshrink == false && Gmax1 + Gmax2 <= eps * 10)
			{
			unshrink = true;
			reconstruct_gradient();
			active_size = l;
			}

		for (i = 0; i < active_size; i++)
			{
			if (be_shrunk(i, Gmax1, Gmax2))
				{
				active_size--;
				while (active_size > i)
					{
					if (!be_shrunk(active_size, Gmax1, Gmax2))
						{
						swap_index(i, active_size);
						break;
						}
					active_size--;
					}
				}
			}
		}

	void calculate_rho(BinaryModel si)
		{
		float r;
		int nr_free = 0;
		float ub = Float.POSITIVE_INFINITY, lb = Float.NEGATIVE_INFINITY, sum_free = 0;
		for (int i = 0; i < active_size; i++)
			{
			float yG = (y[i] ? 1f : -1f) * G[i];

			if (is_lower_bound(i))
				{
				if (y[i])
					{
					ub = Math.min(ub, yG);
					}
				else
					{
					lb = Math.max(lb, yG);
					}
				}
			else if (is_upper_bound(i))
				{
				if (!y[i])
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

		si.rho = r;		//return r;
		}
	}
