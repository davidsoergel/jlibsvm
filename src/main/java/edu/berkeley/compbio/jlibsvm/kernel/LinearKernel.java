package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.util.MathSupport;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class LinearKernel implements KernelFunction<SparseVector>
	{
// ------------------------ CANONICAL METHODS ------------------------

	public String toString()
		{
		StringBuilder sb = new StringBuilder();
		sb.append("kernel_type linear\n");
		return sb.toString();
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface KernelFunction ---------------------

	public double evaluate(SparseVector x, SparseVector y)
		{
		return MathSupport.dot(x, y);
		}
	}
