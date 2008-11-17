package edu.berkeley.compbio.jlibsvm;

public class MathSupport
	{
	public static float powi(float base, int times)
		{
		float tmp = base, ret = 1.0f;

		for (int t = times; t > 0; t /= 2)
			{
			if (t % 2 == 1)
				{
				ret *= tmp;
				}
			tmp = tmp * tmp;
			}
		return ret;
		}

	public static float dotOrig(SvmPoint x, SvmPoint y)
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

	public static float dot(SvmPoint x, SvmPoint y)
		{
		// making final local copies may help performance??
		final int[] xIndexes = x.indexes;
		final int xlen = xIndexes.length;
		final int[] yIndexes = y.indexes;
		final int ylen = yIndexes.length;
		final float[] xValues = x.values;
		final float[] yValues = y.values;

		float sum = 0;
		int i = 0;
		int j = 0;
		int xIndex = xIndexes[0];
		int yIndex = yIndexes[0];

		while (i < xlen && j < ylen)
			{
			if (xIndex == yIndex)
				{
				sum += xValues[i] * yValues[j];

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
					}
				}
			}
		return sum;
		}


	public static float sigmoidPredict(float decisionValue, float A, float B)
		{
		float fApB = decisionValue * A + B;
		if (fApB >= 0)
			{
			return (float) (Math.exp(-fApB) / (1.0 + Math.exp(-fApB)));
			}
		else
			{
			return (float) (1.0 / (1 + Math.exp(fApB)));
			}
		}


	/**
	 * http://martin.ankerl.com/2007/02/11/optimized-exponential-functions-for-java/
	 *
	 * @param val
	 * @return
	 */
	public static double expApprox(double val)
		{
		long tmp = (long) (1512775. * val) + (1072693248L - 60801L);
		return Double.longBitsToDouble(tmp << 32);
		/*
		long tmp = (long) (1512775F * val) + (1072693248L - 0L);
		double upperbound = Double.longBitsToDouble(tmp << 32);

		 tmp -= -90254L;
		double lowerbound = Double.longBitsToDouble(tmp << 32);
*/

		}

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
