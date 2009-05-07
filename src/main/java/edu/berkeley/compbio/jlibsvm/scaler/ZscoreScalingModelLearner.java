package edu.berkeley.compbio.jlibsvm.scaler;

import edu.berkeley.compbio.jlibsvm.util.SparseVector;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class ZscoreScalingModelLearner implements ScalingModelLearner<SparseVector>
	{
// ------------------------------ FIELDS ------------------------------

	private final int maxExamples;
	private final boolean normalizeL2;


// -------------------------- STATIC METHODS --------------------------

	// running mean is obvious; running stddev from http://en.wikipedia.org/wiki/Standard_deviation

	public static float runningMean(int sampleCount, float priorMean, float value)
		{
		float d = sampleCount;  // cast only once
		return priorMean + (value - priorMean) / d;
		}

	public static float runningStddevQ(int sampleCount, float priorMean, float priorQ, float value)
		{
		float d = value - priorMean;
		float result = priorQ + ((sampleCount - 1) * d * d / sampleCount);
		//	assert result < 1000;  // temporary test
		//	assert !Float.isInfinite(result);
		//	assert !Float.isNaN(result);
		return result;
		}

	public static void runningStddevQtoStddevInPlace(Map<Integer, Float> stddevQ, int sampleCount)
		{
		//Map<Integer, Float> result = new HashMap<Integer,Float>();
		float d = sampleCount;  // cast only once

		for (Map.Entry<Integer, Float> entry : stddevQ.entrySet())
			{
			entry.setValue(new Float(Math.sqrt(entry.getValue() / d)));
			}
		}

// --------------------------- CONSTRUCTORS ---------------------------

	public ZscoreScalingModelLearner(int scalingExamples, boolean normalizeL2)
		{
		this.maxExamples = scalingExamples;
		this.normalizeL2 = normalizeL2;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ScalingModelLearner ---------------------

	public ScalingModel<SparseVector> learnScaling(Iterable<SparseVector> examples)
		{
		// PERF we don't know what the maximum index in the SparseVectors will be; just use HashMaps for now
		Map<Integer, Float> mean = new HashMap<Integer, Float>();
		Map<Integer, Float> stddevQ = new HashMap<Integer, Float>();

		int sampleCount = 0;
		for (SparseVector example : examples)
			{
			if (sampleCount >= maxExamples)
				{
				break;
				}
			sampleCount++;  // runningMean etc. assume 1-based indexes
			for (int index : example.indexes)
				{
				float v = example.get(index);

				Float currentMean = mean.get(index);

				if (currentMean == null)
					{
					mean.put(index, v);
					stddevQ.put(index, 0F);
					}
				else
					{
					mean.put(index, runningMean(sampleCount, currentMean, v));
					stddevQ.put(index, runningStddevQ(sampleCount, currentMean, stddevQ.get(index), v));
					}
				}

			// if an index is not seen, it's still counted as having a value of zero
			}

		runningStddevQtoStddevInPlace(stddevQ, sampleCount);
		return new ZscoreScalingModel(mean, stddevQ);
		}

// -------------------------- INNER CLASSES --------------------------

	public class ZscoreScalingModel implements ScalingModel<SparseVector>
		{
// ------------------------------ FIELDS ------------------------------

		private final Map<Integer, Float> mean;
		private final Map<Integer, Float> stddev;


// --------------------------- CONSTRUCTORS ---------------------------

		public ZscoreScalingModel(Map<Integer, Float> mean, Map<Integer, Float> stddev)
			{
			this.mean = mean;
			this.stddev = stddev;
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
				Float theMean = mean.get(index);

				// if this dimension was never seen in the training set, then we can't scale it
				if (theMean != null)
					{
					result.values[i] = (v - theMean) / stddev.get(index);
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
