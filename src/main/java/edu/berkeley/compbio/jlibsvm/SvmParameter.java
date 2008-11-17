package edu.berkeley.compbio.jlibsvm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * I do not like it
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SvmParameter<T> //implements Cloneable, java.io.Serializable
	{
	// these are for training only
	public float cache_size;// in MB
	public float eps;// stopping criteria
	public float C;// for C_SVC, EPSILON_SVR and NU_SVR

	// We need to maintain the labels (the key on this map) in insertion order
	private LinkedHashMap<T, Float> weights = new LinkedHashMap<T, Float>();

	public float nu;// for NU_SVC, ONE_CLASS, and NU_SVR
	public float p;// for EPSILON_SVR
	public boolean shrinking;// use the shrinking heuristics
	public boolean probability;// do probability estimates


	public SvmParameter()
		{
		}

	public SvmParameter(SvmParameter copyFrom)
		{
		cache_size = copyFrom.cache_size;
		eps = copyFrom.eps;
		C = copyFrom.C;
		weights = new LinkedHashMap<T, Float>(copyFrom.weights); //.clone();
		nu = copyFrom.nu;
		p = copyFrom.p;
		shrinking = copyFrom.shrinking;
		probability = copyFrom.probability;
		}

	public Float getWeight(T key)
		{
		return weights.get(key);
		}

	public void putWeight(T key, Float weight)
		{
		weights.put(key, weight);
		}

	public Map<T, Float> getWeights()
		{
		return weights;
		}
	}
