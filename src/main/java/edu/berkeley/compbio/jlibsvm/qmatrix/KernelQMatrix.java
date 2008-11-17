package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class KernelQMatrix implements QMatrix
	{
	protected SvmPoint[] x;
	protected float[] QD;

	KernelFunction kernel;


	public SvmPoint[] getVectors()
		{
		return x;
		}

	KernelQMatrix(SvmPoint[] x, KernelFunction kernel)
		{
		this.x = x.clone();
		this.kernel = kernel;
		}

	public void swapIndex(int i, int j)
		{
		SvmPoint swapnode = x[i];
		x[i] = x[j];
		x[j] = swapnode;

		float swap = QD[i];
		QD[i] = QD[j];
		QD[j] = swap;
		}

	public float[] getQD()
		{
		return QD;
		}
	}
