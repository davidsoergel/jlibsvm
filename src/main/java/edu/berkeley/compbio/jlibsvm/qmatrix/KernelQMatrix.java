package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import edu.berkeley.compbio.jlibsvm.SvmPoint;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class KernelQMatrix implements QMatrix
	{
	protected SvmPoint[] x;
//	private final float[] x_square;


	protected float[] QD;

	KernelFunction kernel;

	// svm_parameter//	private final int kernel_type;//	private final int degree;//	private final float gamma;//	private final float coef0;


	public SvmPoint[] getVectors()
		{
		return x;
		}

	KernelQMatrix(SvmPoint[] x, KernelFunction kernel)
		{	/*	this.kernel_type = param.kernel_type;
		this.degree = param.degree;
		this.gamma = param.gamma;
		this.coef0 = param.coef0;*/

		this.x = x.clone();
		this.kernel = kernel;

		// ** ugh
	/*	if (kernel instanceof RBFKernel) //_type == svm_parameter.RBF)
			{
			x_square = new float[x.length];
			for (int i = 0; i < x.length; i++)
				{
				x_square[i] = MathSupport.dot(x[i], x[i]);
				}
			}
		else
			{
			x_square = null;
			}*/
		}

	public void swapIndex(int i, int j)
		{
		SvmPoint swapnode = x[i];
		x[i] = x[j];
		x[j] = swapnode;

		float swap = QD[i];
		QD[i] = QD[j];
		QD[j] = swap;
/*		if (x_square != null)
			{
			float d = x_square[i];
			x_square[i] = x_square[j];
			x_square[j] = d;
			}*/
		}

	public float[] getQD()
		{
		return QD;
		}/*
	float kernel_function(int i, int j)
		{
		return kernel.evaluate(x[i],x[j], x_square[i], x_square[j]);

		switch (kernel_type)
			{
			case svm_parameter.LINEAR:
				return k_function(x[i], x[j],null);
			case svm_parameter.POLY:
				return MathSupport.powi(gamma * MathSupport.dot(x[i], x[j]) + coef0, degree);
			case svm_parameter.RBF:
				return Math.exp(-gamma * (x_square[i] + x_square[j] - 2 * MathSupport.dot(x[i], x[j])));
			case svm_parameter.SIGMOID:
				return Math.tanh(gamma * MathSupport.dot(x[i], x[j]) + coef0);
			case svm_parameter.PRECOMPUTED:
				return x[i][(int) (x[j][0].value)].value;
			default:
				return 0;// java
			}
		}
		*//*
	static float k_function(svm_node[] x, svm_node[] y, svm_parameter param)
		{
		switch (param.kernel_type)
			{
			case svm_parameter.LINEAR:
				return MathSupport.dot(x, y);
			case svm_parameter.POLY:
				return MathSupport.powi(param.gamma * MathSupport.dot(x, y) + param.coef0, param.degree);
			case svm_parameter.RBF:
			{
			float sum = 0;
			int xlen = x.length;
			int ylen = y.length;
			int i = 0;
			int j = 0;
			while (i < xlen && j < ylen)
				{
				if (x[i].index == y[j].index)
					{
					float d = x[i++].value - y[j++].value;
					sum += d * d;
					}
				else if (x[i].index > y[j].index)
					{
					sum += y[j].value * y[j].value;
					++j;
					}
				else
					{
					sum += x[i].value * x[i].value;
					++i;
					}
				}

			while (i < xlen)
				{
				sum += x[i].value * x[i].value;
				++i;
				}

			while (j < ylen)
				{
				sum += y[j].value * y[j].value;
				++j;
				}

			return Math.exp(-param.gamma * sum);
			}
			case svm_parameter.SIGMOID:
				return Math.tanh(param.gamma * MathSupport.dot(x, y) + param.coef0);
			case svm_parameter.PRECOMPUTED:
				return x[(int) (y[0].value)].value;
			default:
				return 0;// java
			}
		}
*/
	}
