package edu.berkeley.compbio.jlibsvm.scaler;

import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;

import java.util.Collection;
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
	SvmParameter param;

	public LinearScalingModelLearner(SvmParameter param)
		{
		this.param = param;
		}

	public ScalingModel<SparseVector> learnScaling(Collection<SparseVector> examples)
		{
		// PERF we don't know what the maximum index in the SparseVectors will be; just use HashMaps for now
		Map<Integer, Float> minima = new HashMap<Integer, Float>();
		//double[] maxima;
		Map<Integer, Float> sizes = new HashMap<Integer, Float>();

		for (SparseVector example : examples)
			{
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
			}

		return new LinearScalingModel(minima, sizes);
		}


	public class LinearScalingModel implements ScalingModel<SparseVector>
		{

		Map<Integer, Float> minima;
		//double[] maxima;
		Map<Integer, Float> sizes;

		public LinearScalingModel(Map<Integer, Float> minima, Map<Integer, Float> sizes)
			{
			this.minima = minima;
			this.sizes = sizes;
			}

		public SparseVector scaledCopy(SparseVector example)
			{
			SparseVector result = new SparseVector(example.indexes.length);

			for (int i = 0; i < example.indexes.length; i++)
				{
				int index = example.indexes[i];
				float v = example.values[i];

				result.indexes[i] = index;
				result.values[i] = (2F * (v - minima.get(index)) / sizes.get(index)) - 1F;
				}

			if (param.normalizeL2)
				{
				result.normalizeL2();
				}

			return result;
			}
		}
	}
