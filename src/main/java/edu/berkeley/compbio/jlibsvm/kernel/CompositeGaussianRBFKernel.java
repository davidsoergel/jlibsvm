package edu.berkeley.compbio.jlibsvm.kernel;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class CompositeGaussianRBFKernel<T> extends GammaKernel<T>
	{
// ------------------------------ FIELDS ------------------------------

	KernelFunction<T> underlyingKernel;


// --------------------------- CONSTRUCTORS ---------------------------

	public CompositeGaussianRBFKernel(double gamma, KernelFunction<T> underlyingKernel)
		{
		super(gamma);
		this.underlyingKernel = underlyingKernel;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface KernelFunction ---------------------

	public double evaluate(T x, T y)
		{
		// we're looking for the square of the distance between x and y in the original space
		// which equals x_square + y_square - 2 * dot(x, y);

		// ** should cache the squares?
		double xSquare = underlyingKernel.evaluate(x, x);
		double ySquare = underlyingKernel.evaluate(y, y);

		double differenceNormSquared = xSquare + ySquare - (2f * underlyingKernel.evaluate(x, y));

		double result = Math.exp(-gamma * differenceNormSquared);

		return result;
		}
	}
