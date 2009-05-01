package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.multi.MultiClassModel;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * I do not like it
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SvmParameter<L>
	{
// ------------------------------ FIELDS ------------------------------

	// these are for training only
	public float cache_size;// in MB
	public float eps;// stopping criteria
	public float C;// for C_SVC, EPSILON_SVR and NU_SVR

	public float nu;// for NU_SVC, ONE_CLASS, and NU_SVR
	public float p;// for EPSILON_SVR
	public boolean shrinking;// use the shrinking heuristics
	public boolean probability;// do probability estimates


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
	public boolean normalizeL2 = false;

	/**
	 * When learning scaling, only bother with this many examples, assuming they're in random order.
	 */
	public int scalingExamples = Integer.MAX_VALUE;

	// We need to maintain the labels (the key on this map) in insertion order
	private LinkedHashMap<L, Float> weights = new LinkedHashMap<L, Float>();


// --------------------------- CONSTRUCTORS ---------------------------

	public SvmParameter()
		{
		}

	public SvmParameter(SvmParameter copyFrom)
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
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	public Map<L, Float> getWeights()
		{
		return weights;
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

	public Float getWeight(L key)
		{
		return weights.get(key);
		}

	public void putWeight(L key, Float weight)
		{
		weights.put(key, weight);
		}
	}
