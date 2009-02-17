package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.util.SparseVector;

import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class PrecomputedKernel implements KernelFunction<SparseVector>
	{
	public PrecomputedKernel(Properties props)
		{
		throw new UnsupportedOperationException();// ** Hmm, not sure how to load precomputed kernels;
		}

	public PrecomputedKernel()
		{
		throw new UnsupportedOperationException();
		}

	public double evaluate(SparseVector x, SparseVector y)
		{
		return (double) evaluateF(x, y);
		}

	public float evaluateF(SparseVector x, SparseVector y)
		{
		return x.values[(int) (y.values[0])];
		}

	public String toString()
		{
		StringBuilder sb = new StringBuilder();
		sb.append("kernel_type precomputed\n");
		return sb.toString();
		}
	}