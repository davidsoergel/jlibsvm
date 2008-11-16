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
	/* svm_type */
/*	public static final int C_SVC = 0;
	public static final int NU_SVC = 1;
	public static final int ONE_CLASS = 2;
	public static final int EPSILON_SVR = 3;
	public static final int NU_SVR = 4;
*/

	/* kernel_type */
//	public static final int LINEAR = 0;
//	public static final int POLY = 1;
//	public static final int RBF = 2;
//	public static final int SIGMOID = 3;
//	public static final int PRECOMPUTED = 4;

//	public int svm_type;
//	public int kernel_type;
//	public int degree;// for poly
//	public float gamma;// for poly/rbf/sigmoid
//	public float coef0;// for poly/sigmoid

	// these are for training only
	public float cache_size;// in MB
	public float eps;// stopping criteria
	public float C;// for C_SVC, EPSILON_SVR and NU_SVR
	//public int nr_weight;// for C_SVC

	// We need to maintain the labels (the key on this map) in insertion order
	private LinkedHashMap<T, Float> weights = new LinkedHashMap<T, Float>();


	//public int[] weightLabel;// for C_SVC
	//public float[] weight;// for C_SVC
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
		//nr_weight = copyFrom.nr_weight;
		//weightLabel = copyFrom.weightLabel.clone();
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
