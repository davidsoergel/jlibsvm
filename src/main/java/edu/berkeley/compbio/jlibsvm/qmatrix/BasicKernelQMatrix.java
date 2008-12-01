package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BasicKernelQMatrix<P> extends KernelQMatrix<P>
	{
	/*
	private final Cache cache;


	public ONE_CLASS_Q(Map<P,L> examples, KernelFunction<P> kernel, float cache_size)
		{
		super(examples, kernel);
		cache = new Cache(examples.size(), (long) (cache_size * (1 << 20)));
		QD = new float[examples.size()];
		for (int i = 0; i < examples.size(); i++)
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
	*/

	public BasicKernelQMatrix(KernelFunction<P> kernel, int numExamples, int maxCachedRank)
		{
		super(kernel, numExamples, maxCachedRank);
		}

	public float computeQ(SolutionVector<P> a, SolutionVector<P> b)
		{
		return (float) kernel.evaluate(a.point, b.point);
		}
	}