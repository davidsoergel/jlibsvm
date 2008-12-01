package edu.berkeley.compbio.jlibsvm.kernel;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class GammaKernel<T> implements KernelFunction<T>
	{
	public float gamma;

	public GammaKernel(float gamma)
		{
		this.gamma = gamma;
		}

	public void setGamma(float gamma)
		{
		this.gamma = gamma;
		}

	public float getGamma()
		{
		return gamma;
		}
	}
