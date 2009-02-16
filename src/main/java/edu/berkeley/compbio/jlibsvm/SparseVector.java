package edu.berkeley.compbio.jlibsvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SparseVector //implements java.io.Serializable
	{
	public int[] indexes;
	public float[] values;

	//private static int hashcodeIncrementor = 0;
	//private final int hashcode;

	public SparseVector(int dimensions)
		{
		//	hashcode = new Integer(hashcodeIncrementor).hashCode();  // just maek the vectors uniquely identifiable
		//	hashcodeIncrementor++;
		indexes = new int[dimensions];
		values = new float[dimensions];
		}

	// don't want to put this here since it assumes the usual dot product, but we might want a different one
	/*
	private static final float NOT_COMPUTED_YET = -1;
	private float normSquared = NOT_COMPUTED_YET;

	public float getNormSquared()
		{
		if (normSquared == -1)
			{
			normSquared = 0;
			int xlen = values.length;
			int i = 0;
			while (i < xlen)
				{
				normSquared += values[i] * values[i];
				++i;
				}
			}
		return normSquared;
		}*/

	/**
	 * Create randomized vectors for testing
	 *
	 * @param maxDimensions
	 * @param nonzeroProbability
	 * @param maxValue
	 */
	public SparseVector(int maxDimensions, float nonzeroProbability, float maxValue)
		{
		//	hashcode = new Integer(hashcodeIncrementor).hashCode();  // just maek the vectors uniquely identifiable
		//	hashcodeIncrementor++;
		List<Integer> indexList = new ArrayList<Integer>();

		for (int i = 0; i < maxDimensions; i++)
			{
			if (Math.random() < nonzeroProbability)
				{
				indexList.add(i);
				}
			}

		indexes = new int[indexList.size()];
		values = new float[indexes.length];


		for (int i = 0; i < indexes.length; i++)
			{
			indexes[i] = indexList.get(i);
			values[i] = (float) (Math.random() * maxValue);
			}
		}

	public SparseVector(int maxDimensions, SparseVector sv1, float p1, SparseVector sv2, float p2)
		{
		//	hashcode = new Integer(hashcodeIncrementor).hashCode();  // just maek the vectors uniquely identifiable
		//	hashcodeIncrementor++;
		List<Integer> indexList = new ArrayList<Integer>();
		List<Float> valueList = new ArrayList<Float>();

		for (int i = 0; i < maxDimensions; i++)
			{
			float v = sv1.get(i) * p1 + sv2.get(i) * p2;
			if (v != 0)
				{
				indexList.add(i);
				valueList.add(v);
				}
			}

		indexes = new int[indexList.size()];
		values = new float[indexes.length];

		for (int i = 0; i < indexList.size(); i++)
			{
			indexes[i] = indexList.get(i);
			values[i] = valueList.get(i);
			}
		}

	private float get(int i)
		{
		int j = Arrays.binarySearch(indexes, i);
		if (j < 0)
			{
			return 0;
			}
		else
			{
			return values[j];
			}
		}

	public String toString()
		{
		StringBuffer sb = new StringBuffer();
		for (int j = 0; j < indexes.length; j++)
			{
			sb.append(indexes[j] + ":" + values[j] + " ");
			}
		return sb.toString();
		}
/*
	@Override
	public boolean equals(Object o)
		{
		if (this == o)
			{
			return true;
			}
		if (o == null || getClass() != o.getClass())
			{
			return false;
			}

		return true;
		}

	@Override
	public int hashCode()
		{
		return hashcode;
		}*/
	}
