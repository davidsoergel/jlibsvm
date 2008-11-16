package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.KernelQMatrix;
import edu.berkeley.compbio.jlibsvm.SvmProblem;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class ONE_CLASS_Q extends KernelQMatrix
	{
	private final Cache cache;

	public ONE_CLASS_Q(SvmProblem problem, KernelFunction kernel, float cache_size)
		{
		super(problem.examples, kernel);
		cache = new Cache(problem.examples.length, (long) (cache_size * (1 << 20)));
		QD = new float[problem.examples.length];
		for (int i = 0; i < problem.examples.length; i++)
			{
			QD[i] = kernel.evaluate(x[i], x[i]);
			}
		}

	public float[] getQ(int i, int len)
		{
		float[][] data = new float[1][];
		int start, j;
		if ((start = cache.get_data(i, data, len)) < len)
			{
			for (j = start; j < len; j++)
				{
				data[0][j] = compute_Q(i, j);
				}
			}
		return data[0];
		}


	float compute_Q(int i, int j)
		{
		return kernel.evaluate(x[i], x[j]);
		}

	public void swapIndex(int i, int j)
		{
		cache.swap_index(i, j);
		super.swapIndex(i, j);
		}
	}