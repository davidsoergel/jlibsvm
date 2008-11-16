package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.KernelQMatrix;
import edu.berkeley.compbio.jlibsvm.SvmProblem;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SVR_Q extends KernelQMatrix
	{
	private final int l;
	private final Cache cache;
	private final byte[] sign;
	private final int[] index;
	private int nextBuffer;
	private float[][] buffer;

	public SVR_Q(SvmProblem problem, KernelFunction kernel, float cache_size)
		{
		super(problem.examples, kernel);
		l = problem.examples.length;
		cache = new Cache(l, (long) (cache_size * (1 << 20)));
		QD = new float[2 * l];
		sign = new byte[2 * l];
		index = new int[2 * l];
		for (int k = 0; k < l; k++)
			{
			sign[k] = 1;
			sign[k + l] = -1;
			index[k] = k;
			index[k + l] = k;
			QD[k] =  kernel.evaluate(x[k], x[k]);
			QD[k + l] = QD[k];
			}
		buffer = new float[2][2 * l];
		nextBuffer = 0;
		}

	public void swapIndex(int i, int j)
		{
		byte b = sign[i];
		sign[i] = sign[j];
		sign[j] = b;

		int c = index[i];
		index[i] = index[j];
		index[j] = c;
		}

	public float[] getQ(int i, int len)
		{
		float[][] data = new float[1][];
		int j, real_i = index[i];
		if (cache.get_data(real_i, data, l) < l)
			{
			for (j = 0; j < l; j++)
				{
				data[0][j] =  kernel.evaluate(x[real_i], x[j]);
				}
			}

		// reorder and copy
		float buf[] = buffer[nextBuffer];
		nextBuffer = 1 - nextBuffer;
		byte si = sign[i];
		for (j = 0; j < len; j++)
			{
			buf[j] = (float) si * sign[j] * data[0][index[j]];
			}
		return buf;
		}
	}