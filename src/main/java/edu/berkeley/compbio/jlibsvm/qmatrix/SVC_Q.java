package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.SvmProblem;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SVC_Q extends ONE_CLASS_Q
	{
	private final boolean[] y;

	public SVC_Q(SvmProblem problem, KernelFunction kernel, float cache_size, boolean[] y)
		{
		super(problem, kernel, cache_size);
		this.y =  y.clone();
		}


	float compute_Q(int i, int j)
		{
		return ((y[i] == y[j]) ? 1 : -1) * kernel.evaluate(x[i], x[j]);
		}


	public void swapIndex(int i, int j)
		{
		super.swapIndex(i, j);

		boolean _ = y[i];
		y[i] = y[j];
		y[j] = _;
		}
	}
