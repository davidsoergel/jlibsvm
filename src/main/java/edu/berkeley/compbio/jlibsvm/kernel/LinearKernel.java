package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.MathSupport;
import edu.berkeley.compbio.jlibsvm.SvmPoint;

import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class LinearKernel implements KernelFunction
	{
	public LinearKernel(Properties props)
		{
		this();
		}

	public LinearKernel()
		{

		}

	public float evaluate(SvmPoint x, SvmPoint y)
		{
		//float dot = MathSupport.dotOrig(x,y);
		float dot = MathSupport.dot(x, y);
		//assert dot1 == dot2;
		return dot;
		}

	public String toString()
		{
		StringBuilder sb = new StringBuilder();
		sb.append("kernel_type linear\n");
		return sb.toString();
		}

	public String perfString()
		{
		return "";
		}
	}
