package edu.berkeley.compbio.jlibsvm.scaler;

import edu.berkeley.compbio.jlibsvm.util.SparseVector;

import java.util.HashMap;
import java.util.Map;

/**
 * Learn the minima and maxima of each dimension from the training data, so as to transform points into the [-1, 1]
 * interval.  Test points that lie outside the bounds given by the training data will have values lying outside this
 * interval.
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class LinearScalingModelLearner implements ScalingModelLearner<SparseVector>
	{
// ------------------------------ FIELDS ------------------------------

	//ImmutableSvmParameter param;
	private final int maxExamples;
	private final boolean normalizeL2;


// --------------------------- CONSTRUCTORS ---------------------------

	public LinearScalingModelLearner(int scalingExamples, boolean normalizeL2)
		{
		this.maxExamples = scalingExamples;
		this.normalizeL2 = normalizeL2;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ScalingModelLearner ---------------------

	public ScalingModel<SparseVector> learnScaling(Iterable<SparseVector> examples)
		{
		// PERF we don't know what the maximum index in the SparseVectors will be; just use HashMaps for now
		Map<Integer, Float> minima = new HashMap<Integer, Float>();
		//double[] maxima;
		Map<Integer, Float> sizes = new HashMap<Integer, Float>();

		int count = 0;
		for (SparseVector example : examples)
			{
			if (count >= maxExamples)
				{
				break;
				}
			for (int index : example.indexes)
				{
				float v = example.get(index);

				Float currentMin = minima.get(index);

				if (currentMin == null)
					{
					minima.put(index, v);
					sizes.put(index, 0F);
					}
				else
					{
					minima.put(index, Math.min(minima.get(index), v));
					//maxima[i] = Math.max(maxima[i], v);
					sizes.put(index, Math.max(sizes.get(index), v - minima.get(index)));
					}
				}
			count++;
			}

		return new LinearScalingModel(minima, sizes);
		}

// -------------------------- INNER CLASSES --------------------------

	public class LinearScalingModel implements ScalingModel<SparseVector>
		{
// ------------------------------ FIELDS ------------------------------

		Map<Integer, Float> minima;
		//double[] maxima;
		Map<Integer, Float> sizes;


// --------------------------- CONSTRUCTORS ---------------------------

		public LinearScalingModel(Map<Integer, Float> minima, Map<Integer, Float> sizes)
			{
			this.minima = minima;
			this.sizes = sizes;
			}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ScalingModel ---------------------

		public SparseVector scaledCopy(SparseVector example)
			{
			SparseVector result = new SparseVector(example.indexes.length);

			for (int i = 0; i < example.indexes.length; i++)
				{
				int index = example.indexes[i];
				float v = example.values[i];

				result.indexes[i] = index;
				Float min = minima.get(index);

				// if this dimension was never seen in the training set, then we can't scale it
				if (min != null)
					{
					result.values[i] = (2F * (v - min) / sizes.get(index)) - 1F;
					}
				}

			if (normalizeL2)
				{
				result.normalizeL2();
				}

			return result;
			}
		}
	}
