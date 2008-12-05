package edu.berkeley.compbio.jlibsvm.kernel;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class GammaKernel<T> implements KernelFunction<T>
	{
	public double gamma;

	public GammaKernel(double gamma)
		{
		this.gamma = gamma;
		}

	public void setGamma(double gamma)
		{
		this.gamma = gamma;
		}

	public double getGamma()
		{
		return gamma;
		}
	}
