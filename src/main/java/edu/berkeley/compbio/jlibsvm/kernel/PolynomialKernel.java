package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.util.MathSupport;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class PolynomialKernel extends GammaKernel<SparseVector>
	{
// ------------------------------ FIELDS ------------------------------

	public int degree;
	public float coef0;


// --------------------------- CONSTRUCTORS ---------------------------

/*	public PolynomialKernel(Properties props)
		{
		this(Integer.parseInt(props.getProperty("degree")), Float.parseFloat(props.getProperty("gamma")),
		     Float.parseFloat(props.getProperty("coef0")));
		}
*/

	public PolynomialKernel(int degree, float gamma, float coef0)
		{
		super(gamma);
		if (degree < 0)
			{
			throw new SvmException("degree of polynomial kernel < 0");
			}

		this.degree = degree;
		this.coef0 = coef0;
		}

// ------------------------ CANONICAL METHODS ------------------------

	public String toString()
		{
		StringBuilder sb = new StringBuilder();
		sb.append("kernel_type polynomial\n");
		sb.append("degree " + degree + "\n");
		sb.append("gamma " + gamma + "\n");
		sb.append("coef0 " + coef0 + "\n");
		return sb.toString();
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface KernelFunction ---------------------

	public double evaluate(SparseVector x, SparseVector y)
		{
		return MathSupport.powi(gamma * MathSupport.dot(x, y) + coef0, degree);
		}
	}
