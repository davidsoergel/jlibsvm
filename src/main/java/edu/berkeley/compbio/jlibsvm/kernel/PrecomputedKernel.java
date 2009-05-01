package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.util.SparseVector;

import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class PrecomputedKernel implements KernelFunction<SparseVector>
	{
// --------------------------- CONSTRUCTORS ---------------------------

	public PrecomputedKernel()
		{
		throw new UnsupportedOperationException();
		}

	public PrecomputedKernel(Properties props)
		{
		throw new UnsupportedOperationException();// ** Hmm, not sure how to load precomputed kernels;
		}

// ------------------------ CANONICAL METHODS ------------------------

	public String toString()
		{
		StringBuilder sb = new StringBuilder();
		sb.append("kernel_type precomputed\n");
		return sb.toString();
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface KernelFunction ---------------------

	public double evaluate(SparseVector x, SparseVector y)
		{
		return (double) evaluateF(x, y);
		}

// -------------------------- OTHER METHODS --------------------------

	public float evaluateF(SparseVector x, SparseVector y)
		{
		return x.values[(int) (y.values[0])];
		}
	}
