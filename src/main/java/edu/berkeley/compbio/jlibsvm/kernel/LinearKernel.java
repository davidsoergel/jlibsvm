package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.SparseVector;
import edu.berkeley.compbio.jlibsvm.util.MathSupport;

import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class LinearKernel implements KernelFunction<SparseVector>
	{
	public LinearKernel(Properties props)
		{
		this();
		}

	public LinearKernel()
		{

		}


	public double evaluate(SparseVector x, SparseVector y)
		{
		return MathSupport.dot(x, y);
		}


	public String toString()
		{
		StringBuilder sb = new StringBuilder();
		sb.append("kernel_type linear\n");
		return sb.toString();
		}
	}
