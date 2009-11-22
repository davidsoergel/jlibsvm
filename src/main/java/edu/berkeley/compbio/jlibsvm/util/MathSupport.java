package edu.berkeley.compbio.jlibsvm.util;

public class MathSupport
	{
// -------------------------- STATIC METHODS --------------------------

	public static double powi(double base, int times)
		{
		assert times >= 0;
		double tmp = base, ret = 1.0f;

		for (int t = times; t > 0; t /= 2)
			{
			if (t % 2 != 0)
				{
				ret *= tmp;
				}
			tmp = tmp * tmp;
			}
		return ret;
		}

/*
	public static float dotOrig(SparseVector x, SparseVector y)
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
				sum += x.values[i++] * y.values[j++];
				}
			else
				{
				if (x.indexes[i] > y.indexes[j])
					{
					++j;
					}
				else
					{
					++i;
					}
				}
			}
		return sum;
		}
		*/

	public static double dot(SparseVector x, SparseVector y)
		{
		// try doing the internal stuff at double precision

		// making final local copies may help performance??
		final int[] xIndexes = x.indexes;
		final int xlen = xIndexes.length;
		final int[] yIndexes = y.indexes;
		final int ylen = yIndexes.length;
		final float[] xValues = x.values;
		final float[] yValues = y.values;

		double sum = 0;
		int i = 0;
		int j = 0;
		int xIndex = xIndexes[0];
		int yIndex = yIndexes[0];

		while (i < xlen && j < ylen)
			{
			if (xIndex == yIndex)
				{
				sum += (double) xValues[i] * (double) yValues[j];

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
						// so, do nothing

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
						// so, do nothing

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
					// so, do nothing

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
					// so, do nothing

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

	/**
	 * http://martin.ankerl.com/2007/02/11/optimized-exponential-functions-for-java/
	 * <p/>
	 * This approximation is much too coarse for our purposes
	 *
	 * @param val
	 * @return
	 */
//	public static double expApprox(double val)
//		{
//		long tmp = (long) (1512775. * val) + (1072693248L - 60801L);
//		return Double.longBitsToDouble(tmp << 32);
	/*
		 long tmp = (long) (1512775F * val) + (1072693248L - 0L);
		 double upperbound = Double.longBitsToDouble(tmp << 32);

		  tmp -= -90254L;
		 double lowerbound = Double.longBitsToDouble(tmp << 32);
 */

//		}

	/**
	 * This is provided by apache commons, but let's avoid the dependency
	 *
	 * @param x
	 * @return
	 */
	public static boolean[] toPrimitive(Boolean[] x)
		{
		boolean[] result = new boolean[x.length];
		int i = 0;
		for (Boolean b : x)
			{
			result[i] = b;
			i++;
			}
		return result;
		}
	}
