package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.util.SparseVector;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class GaussianRBFKernel extends GammaKernel<SparseVector>
	{
// -------------------------- STATIC METHODS --------------------------

	/*	public String perfString()
		 {
		 return "" + evaluateCount + " evaluations, " + interpolatingExp.perfString();
		 }
 */
/*	private float float2xDotProduct(SvmPoint x, SvmPoint y)
		{
		// this ends up horribly wrong near the boundaries... ???
		// or not, and I was previously worried about the exp method?
		float sum = -2f * MathSupport.dot(x, y);

		sum += x.getSquared();
		sum += y.getSquared();

		return sum;
		}
*/

/*		private float float2xDotProduct(SvmPoint x, SvmPoint y)
		 {
		 // this ends up horribly wrong near the boundaries... ???
		 // or not, and I was previously worried about the exp method?
		 float sum = -2f * MathSupport.dot(x, y);

		 sum += x.getSquared();
		 sum += y.getSquared();

		 return sum;
		 }
*/


	/**
	 * Subtract one vector from the other and take the dot product of the difference with itself, to get the square of the
	 * norm.
	 *
	 * @param x
	 * @param y
	 * @return
	 */
/*	private float explicitFloatSum(SvmPoint x, SvmPoint y)
		{
		float sum = 0;
		int xlen = x.indexes.length;
		int ylen = y.indexes.length;
		int i = 0;
		int j = 0;
		while (i < xlen && j < ylen)
			{
			if (x.indexes[i] == y.indexes[j])
				{
				float d = x.values[i++] - y.values[j++];
				sum += d * d;
				}
			else if (x.indexes[i] > y.indexes[j])
				{
				// there is an entry for y but not for x at this index => x.value == 0
				sum += y.values[j] * y.values[j];
				++j;
				}
			else
				{
				// there is an entry for x but not for y at this index => y.value == 0
				sum += x.values[i] * x.values[i];
				++i;
				}
			}

		// finish off any trailing entries in one vector but not the other
		while (i < xlen)
			{
			sum += x.values[i] * x.values[i];
			++i;
			}

		while (j < ylen)
			{
			sum += y.values[j] * y.values[j];
			++j;
			}
		return sum;
		}*/

	/**
	 * Subtract one vector from the other and take the dot product of the difference with itself, to get the square of the
	 * norm.
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	private static final double explicitSumOptimized(@NotNull final SparseVector x, @NotNull final SparseVector y)
		{
		double sum = 0;

		// making final local copies may help performance??  Or not, the JIT should figure this out
		final int[] xIndexes = x.indexes;
		final int xlen = xIndexes.length;
		final int[] yIndexes = y.indexes;
		final int ylen = yIndexes.length;
		final float[] xValues = x.values;
		final float[] yValues = y.values;

		int i = 0;
		int j = 0;
		int xIndex = xIndexes[0];
		int yIndex = yIndexes[0];

		// use Integer.MAX_VALUE as a marker that we've used up the whole array

		while (xIndex != Integer.MAX_VALUE || yIndex != Integer.MAX_VALUE)
			{
			if (xIndex == yIndex)
				{
				double d = (double) xValues[i] - (double) yValues[j];
				sum += d * d;

				i++;
				if (i >= xlen)
					{
					xIndex = Integer.MAX_VALUE;
					}
				else
					{
					xIndex = xIndexes[i];
					}

				j++;
				if (j >= ylen)
					{
					yIndex = Integer.MAX_VALUE;
					}
				else
					{
					yIndex = yIndexes[j];
					}
				}
			else
				{
				try
					{
					while (xIndex > yIndex)
						{
						// there is an entry for y but not for x at this index => x.value == 0

						sum += (double) yValues[j] * (double) yValues[j];
						j++;
						yIndex = yIndexes[j];
						}
					}
				catch (ArrayIndexOutOfBoundsException e)
					{
					yIndex = Integer.MAX_VALUE;
					}

				try
					{
					while (yIndex > xIndex)
						{
						// there is an entry for x but not for y at this index => y.value == 0

						sum += (double) xValues[i] * (double) xValues[i];
						i++;
						xIndex = xIndexes[i];
						}
					}
				catch (ArrayIndexOutOfBoundsException e)
					{
					xIndex = Integer.MAX_VALUE;
					}
				/*
				while (xIndex > yIndex)
					{
					// there is an entry for y but not for x at this index => x.value == 0

					sum += (double) yValues[j] * (double) yValues[j];
					j++;
					if (j >= ylen)
						{
						yIndex = Integer.MAX_VALUE;
						}
					else
						{
						yIndex = yIndexes[j];
						}
					}

				while (yIndex > xIndex)
					{
					// there is an entry for x but not for y at this index => y.value == 0

					sum += (double) xValues[i] * (double) xValues[i];
					i++;
					if (i >= xlen)
						{
						xIndex = Integer.MAX_VALUE;
						}
					else
						{
						xIndex = xIndexes[i];
						}
					}*/
				}
			}

		return sum;
		}

// --------------------------- CONSTRUCTORS ---------------------------

	public GaussianRBFKernel(Properties props)
		{
		this(Float.parseFloat(props.getProperty("gamma")));
		}

	public GaussianRBFKernel(float gamma)
		{
		super(gamma);
		}

	// ------------------------ CANONICAL METHODS ------------------------

	@Override
	public String toString()
		{
		return "RBF gamma=" + gamma;
		}

	// BAD file output infrastructure

	public String toFileOutputString()
		{
		StringBuilder sb = new StringBuilder();
		sb.append("kernel_type rbf\n");
		sb.append("gamma " + gamma + "\n");
		return sb.toString();
		}


// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface KernelFunction ---------------------

	public double evaluate(@NotNull SparseVector x, @NotNull SparseVector y)
		{
		// try doing the internal stuff at double precision

		// FIRST CHOOSE THE SUM METHOD
		// we're looking for the square of the distance between x and y in the original space
		// which equals x_square + y_square - 2 * dot(x, y);

		//float sum = float2xDotProduct(x, y);  // DOESN'T WORK
		//double sum = explicitDoubleSum(x, y);  // Most precise.
		//float sum = explicitFloatSum(x, y);  // Works just as well; no evident speed improvement though
		double sum = explicitSumOptimized(x, y);  // Works just as well; faster

		//	assert sum == sum2;


		// THEN CHOOSE THE EXP METHOD

		double result = Math.exp(-gamma * sum);
		//	float result = interpolatingExp.evaluate(-gamma * sum);  // approximation is as good as the interpolator specifies, but no faster than Math.exp()
		//float result =  (float) MathSupport.expApprox(-gamma * sum);  // APPROXIMATION IS NOT GOOD ENOUGH

		// STRANGE PERFORMANCE ISSUES

		// for some reason, using Math.exp() seems to halve the total time spent in explicitFloatSum (which comes first!), compared to the interpolating exp.
		// The total number of iterations is the same, and the total number of comparisons and multiplications performed is the same,
		// so it's not a matter of some upstream function calling evaluate() more often.
		// this makes no sense at all... must be due to a compiler optimization or the processor cache size or some such thing!?

		// Profiling results don't make any more sense with java -Xint

		// Okay, yes, it could be that the interpolation table bumps data out of the l1 cache,
		// which then needs to be reloaded in explicitFloatSumOptimized the next time around
		// whereas Math.exp() leaves data in the l1 cache

		// this makes sense because evaluate() is typically called a bunch of times in a row holding point x constant but changing point y.

		// My Merom cores have 32kb l1 cache; it might be worth testing the difference on a machine with more to confirm the reason.
		// anyhow I'll just suck up the Math.exp() for now.


//		evaluateCount++;

		return result;
		}
	}
