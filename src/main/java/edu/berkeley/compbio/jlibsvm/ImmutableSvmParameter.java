package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.multi.MultiClassModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * I do not like it
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class ImmutableSvmParameter<L extends Comparable, P>
	{
// ------------------------------ FIELDS ------------------------------

	// these are for training only
	public final float cache_size;// in MB
	public final float eps;// stopping criteria

	public final float nu;// for NU_SVC, ONE_CLASS, and NU_SVR
	public final float p;// for EPSILON_SVR
	public final boolean shrinking;// use the shrinking heuristics


	public final double oneVsAllThreshold;
	public final MultiClassModel.OneVsAllMode oneVsAllMode;
	public final MultiClassModel.AllVsAllMode allVsAllMode;
	public final double minVoteProportion;
	public final int falseClassSVlimit;

	/**
	 * For unbalanced data, redistribute the misclassification cost C according to the numbers of examples in each class,
	 * so that each class has the same total misclassification weight assigned to it and the average is param.C
	 */
	public final boolean redistributeUnbalancedC;


	public final boolean scaleBinaryMachinesIndependently;
	public final boolean gridsearchBinaryMachinesIndependently;
	public final boolean normalizeL2;

	public final int crossValidationFolds;
//	public final boolean crossValidation;
	// do a performance report at the multiclass level even if grid search is at the binary level

	/**
	 * When learning scaling, only bother with this many examples, assuming they're in random order.
	 */
	//public final int scalingExamples;


	public final ScalingModelLearner<P> scalingModelLearner;

	// these params are most likely to change in a copy


	public final boolean probability;// do probability estimates
	// We need to maintain the labels (the key on this map) in insertion order
	private final LinkedHashMap<L, Float> weights;// = new LinkedHashMap<L, Float>();


// --------------------------- CONSTRUCTORS ---------------------------

	/*	private ImmutableSvmParameter(ImmutableSvmParameter<L, P> copyFrom)
		 {
		 cache_size = copyFrom.cache_size;
		 eps = copyFrom.eps;
		 C = copyFrom.C;
		 weights = new LinkedHashMap<L, Float>(copyFrom.weights); //.clone();
		 nu = copyFrom.nu;
		 p = copyFrom.p;
		 shrinking = copyFrom.shrinking;
		 probability = copyFrom.probability;
		 oneVsAllThreshold = copyFrom.oneVsAllThreshold;
		 oneVsAllMode = copyFrom.oneVsAllMode;
		 allVsAllMode = copyFrom.allVsAllMode;
		 minVoteProportion = copyFrom.minVoteProportion;
		 falseClassSVlimit = copyFrom.falseClassSVlimit;
		 scaleBinaryMachinesIndependently = copyFrom.scaleBinaryMachinesIndependently;
		 normalizeL2 = copyFrom.normalizeL2;
		 scalingExamples = copyFrom.scalingExamples;
		 redistributeUnbalancedC = copyFrom.redistributeUnbalancedC;
		 gridsearchBinaryMachinesIndependently = copyFrom.gridsearchBinaryMachinesIndependently;
		 kernel = copyFrom.kernel;
		 scalingModelLearner = copyFrom.scalingModelLearner;
		 }
 */

	protected ImmutableSvmParameter(Builder<L, P> copyFrom)
		{
		cache_size = copyFrom.cache_size;
		eps = copyFrom.eps;
		weights = new LinkedHashMap<L, Float>(copyFrom.weights); //.clone();
		nu = copyFrom.nu;
		p = copyFrom.p;
		shrinking = copyFrom.shrinking;
		probability = copyFrom.probability;
		oneVsAllThreshold = copyFrom.oneVsAllThreshold;
		oneVsAllMode = copyFrom.oneVsAllMode;
		allVsAllMode = copyFrom.allVsAllMode;
		minVoteProportion = copyFrom.minVoteProportion;
		falseClassSVlimit = copyFrom.falseClassSVlimit;
		scaleBinaryMachinesIndependently = copyFrom.scaleBinaryMachinesIndependently;
		normalizeL2 = copyFrom.normalizeL2;
		//scalingExamples = copyFrom.scalingExamples;
		redistributeUnbalancedC = copyFrom.redistributeUnbalancedC;
		gridsearchBinaryMachinesIndependently = copyFrom.gridsearchBinaryMachinesIndependently;

		scalingModelLearner = copyFrom.scalingModelLearner;
		crossValidationFolds = copyFrom.crossValidationFolds;
		//	crossValidation = copyFrom.crossValidation;
		}


// -------------------------- OTHER METHODS --------------------------

	public int getCacheRows()
		{
		// assume the O(n) term is in the noise
		double mb = cache_size;
		double kb = mb * 1024;
		double bytes = kb * 1024;
		double floats = bytes / 4; // float = 4 bytes
		double floatrows = Math.sqrt(floats);
		//Math.sqrt(floats * 2);
		// the sqrt 2 term is because the cache will be symmetric
		// no it won't
		return (int) (floatrows);
		}

	public Collection<L> getLabels()
		{
		return weights.keySet();
		}

	public Float getWeight(L key)
		{
		return weights.get(key);
		}

	public boolean isWeightsEmpty()
		{
		return weights.isEmpty();
		}


/*	public Collection<ImmutableSvmParameter<L, P>> getGridParams()
		{
		Set<ImmutableSvmParameter<L, P>> result= new HashSet<ImmutableSvmParameter<L, P>>(1);
		result.add(this);
		return result;
		}
*/

	public abstract static class Builder<L extends Comparable, P>
		{
// ------------------------------ FIELDS ------------------------------

		// these are for training only
		public float cache_size;// in MB
		public float eps;// stopping criteria

		public float nu;// for NU_SVC, ONE_CLASS, and NU_SVR
		public float p;// for EPSILON_SVR
		public boolean shrinking;// use the shrinking heuristics


		public double oneVsAllThreshold = 0.5;
		public MultiClassModel.OneVsAllMode oneVsAllMode = MultiClassModel.OneVsAllMode.None;
		public MultiClassModel.AllVsAllMode allVsAllMode = MultiClassModel.AllVsAllMode.AllVsAll;
		public double minVoteProportion;
		public int falseClassSVlimit = Integer.MAX_VALUE;

		/**
		 * For unbalanced data, redistribute the misclassification cost C according to the numbers of examples in each class,
		 * so that each class has the same total misclassification weight assigned to it and the average is param.C
		 */
		public boolean redistributeUnbalancedC = true;


		public boolean scaleBinaryMachinesIndependently = false;
		public boolean gridsearchBinaryMachinesIndependently = false;
		public boolean normalizeL2 = false;

		/**
		 * When learning scaling, only bother with this many examples, assuming they're in random order.
		 */
		//	public int scalingExamples = Integer.MAX_VALUE;


		public int crossValidationFolds = 5;

		// these params are most likely to change in a copy

		public boolean probability;// do probability estimates
		// We need to maintain the labels (the key on this map) in insertion order
		public LinkedHashMap<L, Float> weights = new LinkedHashMap<L, Float>();
		public ScalingModelLearner<P> scalingModelLearner;
		//	public boolean crossValidation;


// --------------------------- CONSTRUCTORS ---------------------------

		public Builder()
			{
			}

		protected Builder(ImmutableSvmParameter<L, P> copyFrom)
			{
			cache_size = copyFrom.cache_size;
			eps = copyFrom.eps;
			weights = new LinkedHashMap<L, Float>(copyFrom.weights); //.clone();
			nu = copyFrom.nu;
			p = copyFrom.p;
			shrinking = copyFrom.shrinking;
			probability = copyFrom.probability;
			oneVsAllThreshold = copyFrom.oneVsAllThreshold;
			oneVsAllMode = copyFrom.oneVsAllMode;
			allVsAllMode = copyFrom.allVsAllMode;
			minVoteProportion = copyFrom.minVoteProportion;
			falseClassSVlimit = copyFrom.falseClassSVlimit;
			scaleBinaryMachinesIndependently = copyFrom.scaleBinaryMachinesIndependently;
			normalizeL2 = copyFrom.normalizeL2;
			//scalingExamples = copyFrom.scalingExamples;
			redistributeUnbalancedC = copyFrom.redistributeUnbalancedC;
			gridsearchBinaryMachinesIndependently = copyFrom.gridsearchBinaryMachinesIndependently;
			crossValidationFolds = copyFrom.crossValidationFolds;
			//	crossValidation = copyFrom.crossValidation;
			scalingModelLearner = copyFrom.scalingModelLearner;
			}

		protected Builder(Builder<L, P> copyFrom)
			{
			cache_size = copyFrom.cache_size;
			eps = copyFrom.eps;
			weights = new LinkedHashMap<L, Float>(copyFrom.weights); //.clone();
			nu = copyFrom.nu;
			p = copyFrom.p;
			shrinking = copyFrom.shrinking;
			probability = copyFrom.probability;
			oneVsAllThreshold = copyFrom.oneVsAllThreshold;
			oneVsAllMode = copyFrom.oneVsAllMode;
			allVsAllMode = copyFrom.allVsAllMode;
			minVoteProportion = copyFrom.minVoteProportion;
			falseClassSVlimit = copyFrom.falseClassSVlimit;
			scaleBinaryMachinesIndependently = copyFrom.scaleBinaryMachinesIndependently;
			normalizeL2 = copyFrom.normalizeL2;
			//scalingExamples = copyFrom.scalingExamples;
			redistributeUnbalancedC = copyFrom.redistributeUnbalancedC;
			gridsearchBinaryMachinesIndependently = copyFrom.gridsearchBinaryMachinesIndependently;
			crossValidationFolds = copyFrom.crossValidationFolds;
			//	crossValidation = copyFrom.crossValidation;
			scalingModelLearner = copyFrom.scalingModelLearner;
			}

// -------------------------- OTHER METHODS --------------------------

		/*
	  public int getCacheRows()
		  {
		  // assume the O(n) term is in the noise
		  double mb = cache_size;
		  double kb = mb * 1024;
		  double bytes = kb * 1024;
		  double floats = bytes / 4; // float = 4 bytes
		  double floatrows = Math.sqrt(floats);
		  //Math.sqrt(floats * 2);
		  // the sqrt 2 term is because the cache will be symmetric
		  // no it won't
		  return (int) (floatrows);
		  }
  */

		public void putWeight(L key, Float weight)
			{
			weights.put(key, weight);
			}

		public abstract ImmutableSvmParameter<L, P> build();
		}


	public ImmutableSvmParameter<L, P> noProbabilityCopy()
		{
		if (!probability)
			{
			return this;
			}
		else
			{
			ImmutableSvmParameter.Builder<L, P> builder = asBuilder(); //new Builder(this);
			builder.probability = false;
			return builder.build();
			}
		}

	public ImmutableSvmParameter<L, P> withProbabilityCopy()
		{
		if (probability)
			{
			return this;
			}
		else
			{
			ImmutableSvmParameter.Builder<L, P> builder = asBuilder(); //new Builder(this);
			builder.probability = true;
			return builder.build();
			}
		}

	public abstract Builder<L, P> asBuilder();
	}
