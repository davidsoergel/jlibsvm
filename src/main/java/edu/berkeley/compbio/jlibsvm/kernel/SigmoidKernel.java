package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.MathSupport;
import edu.berkeley.compbio.jlibsvm.SvmPoint;

import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SigmoidKernel extends GammaKernel
	{
	//public float gamma;// for poly/rbf/sigmoid
	public float coef0;// for poly/sigmoid

	public SigmoidKernel(Properties props)
		{
		this(Float.parseFloat(props.getProperty("gamma")), Float.parseFloat(props.getProperty("coef0")));
		}

	public SigmoidKernel(float gamma, float coef0)
		{
		super(gamma);
		this.coef0 = coef0;
		}

	public float evaluate(SvmPoint x, SvmPoint y)
		{
		return (float) Math.tanh(gamma * MathSupport.dot(x, y) + coef0);
		}


	public String toString()
		{
		StringBuilder sb = new StringBuilder();
		sb.append("kernel_type sigmoid\n");
		sb.append("gamma " + gamma + "\n");
		sb.append("coef0 " + coef0 + "\n");
		return sb.toString();
		}
	}