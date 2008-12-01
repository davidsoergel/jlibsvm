package edu.berkeley.compbio.jlibsvm.multi;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import edu.berkeley.compbio.jlibsvm.DiscreteModel;
import edu.berkeley.compbio.jlibsvm.SolutionModel;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassModel<L extends Comparable, P> extends SolutionModel<P> implements DiscreteModel<L, P>
	{
	private int numberOfClasses;

	//private float[] probA;// pairwise probability information
	//private float[] probB;

	//BinaryModel<P>[] oneVsOneModels;   // don't like array vs collection, but it's consistent with the rest for now

//	private List<BinaryModel<P>> oneVsOneModels = new ArrayList<BinaryModel<P>>();

	private SymmetricHashMap2d<L, BinaryModel<L, P>> oneVsOneModels;
	private HashMap<L, BinaryModel<L, P>> oneVsAllModels;


	// generics are a hassle here  (T[] label; makes a mess)
	// label of each class, just to maintain a known order for the sake of keeping the decision_values etc. straight  //** proscribed 1-D order for 2-D decision_values is error-prone
	List<L> labels;


	public MultiClassModel(KernelFunction<P> kernel, SvmParameter<L> param, int numberOfClasses)
		{
		super(kernel, param);
		this.numberOfClasses = numberOfClasses;
		oneVsOneModels = new SymmetricHashMap2d<L, BinaryModel<L, P>>(numberOfClasses);
		oneVsAllModels = new HashMap<L, BinaryModel<L, P>>(numberOfClasses);
		}

	// allocate this only once; it'll get cleared on every predictLabel() anyway
	List<L> bestLabelList = new ArrayList<L>();

	public L predictLabel(P x)
		{
		int i;
		//float[] decisionValues = oneVsOneValues(x);

		Multiset<L> votes = new HashMultiset<L>();
		for (BinaryModel<L, P> binaryModel : oneVsOneModels.values())
			{
			votes.add(binaryModel.predictLabel(x));
			}

		// in case of a tie in the number of votes, pick one class randomly (not always the one that happens to be first)

		//	L bestLabel = null;
//		Multinomial<L> bestLabelSet
		int bestCount = 0;
		for (L label : votes.elementSet())
			{
			int count = votes.count(label);
			if (count == bestCount)
				{
				bestLabelList.add(label);
				}
			if (count > bestCount)
				{
				bestLabelList.clear();
				bestLabelList.add(label);
				//bestLabel = label;
				bestCount = count;
				}
			}
		if (bestLabelList.size() == 1)
			{
			return bestLabelList.get(0);
			}

		return bestLabelList.get((int) (Math.random() * bestLabelList.size()));

		//return bestLabel;

/*

		int[] vote = new int[numberOfClasses];

		int pos = 0;
		for (i = 0; i < numberOfClasses; i++)
			{
			for (int j = i + 1; j < numberOfClasses; j++)
				{
				if (decisionValues[pos++] > 0)
					{
					++vote[i];
					}
				else
					{
					++vote[j];
					}
				}
			}

		int bestVoteIndex = 0;
		for (i = 1; i < numberOfClasses; i++)
			{
			if (vote[i] > vote[bestVoteIndex])
				{
				bestVoteIndex = i;
				}
			}
		return labels.get(bestVoteIndex);*/
		}

/*
	private float[] oneVsOneValues(P x)
		{
		float[] decisionValues = new float[oneVsOneModels.size()];

		int i = 0;
		for (BinaryModel<P> m : oneVsOneModels)
			{
			decisionValues[i] = m.predictValue(x);
			i++;
			}
		return decisionValues;
		}*/


	public boolean supportsProbability()
		{
		// just check the first model and assume the rest are the same
		return oneVsOneModels.valueIterator().next().sigmoid != null;
//		return probA != null && probB != null;
		}


	public Map<L, Float> predictProbability(P x)
		{
		if (!supportsProbability())
			{
			throw new SvmException("Can't make probability predictions");
			}


		//** ugly Map2d vs. array issue etc.; oh well, adapt for now to the old multiclassProbability signature
		// the main thing is just to iterate through the Map2d in the order given by the labels list

		//	float[] decisionValues = oneVsOneValues(x);

		float minimumProbability = 1e-7f;
		float[][] pairwiseProbabilities = new float[numberOfClasses][numberOfClasses];

		//int k = 0;

		// this is kind of a lame way to do it, but whatever.

		for (int i = 0; i < numberOfClasses; i++)
			{
			L label1 = labels.get(i);
			for (int j = i + 1; j < numberOfClasses; j++)
				{
				L label2 = labels.get(j);

				BinaryModel<L, P> binaryModel = oneVsOneModels.get(label1, label2);

				float prob = binaryModel.sigmoid.predict(binaryModel.predictValue(x));
				//MathSupport.sigmoidPredict(decisionValues[k], probA[k], probB[k])

				pairwiseProbabilities[i][j] = Math.min(Math.max(prob, minimumProbability), 1 - minimumProbability);
				pairwiseProbabilities[j][i] = 1 - pairwiseProbabilities[i][j];
				//k++;
				}
			}
		float[] probabilityEstimates = multiclassProbability(numberOfClasses, pairwiseProbabilities);

		// but then map back to a cleaner Map API.  Note the probabilityEstimates should come back in order corresponding to the labels list.

		Map<L, Float> result = new HashMap<L, Float>();
		int i = 0;
		for (L label : labels)
			{
			result.put(label, probabilityEstimates[i]);
			i++;
			}

		return result;
		}

	public L bestProbabilityLabel(Map<L, Float> labelProbabilities)
		{
		Float bestProb = 0f;
		L bestLabel = null;
		for (Map.Entry<L, Float> entry : labelProbabilities.entrySet())
			{
			if (entry.getValue() > bestProb)
				{
				bestLabel = entry.getKey();
				bestProb = entry.getValue();
				}
			}
		return bestLabel;
		/*
				int bestProbabilityIndex = 0;
				for (int i = 1; i < numberOfClasses; i++)
					{
					if (labelProbabilities[i] > labelProbabilities[bestProbabilityIndex])
						{
						bestProbabilityIndex = i;
						}
					}
				return (L) label[bestProbabilityIndex];*/
		}


	// Method 2 from the multiclass_prob paper by Wu, Lin, and Weng
	private float[] multiclassProbability(int k, float[][] r)
		{

		float[] p = new float[k];
		int t, j;
		int iter = 0, maximumIterations = Math.max(100, k);
		float[][] Q = new float[k][k];
		float[] Qp = new float[k];
		float pQp, eps = 0.005f / k;

		for (t = 0; t < k; t++)
			{
			p[t] = 1.0f / k;// Valid if k = 1
			Q[t][t] = 0;
			for (j = 0; j < t; j++)
				{
				Q[t][t] += r[j][t] * r[j][t];
				Q[t][j] = Q[j][t];
				}
			for (j = t + 1; j < k; j++)
				{
				Q[t][t] += r[j][t] * r[j][t];
				Q[t][j] = -r[j][t] * r[t][j];
				}
			}
		for (iter = 0; iter < maximumIterations; iter++)
			{			// stopping condition, recalculate QP,pQP for numerical accuracy
			pQp = 0;
			for (t = 0; t < k; t++)
				{
				Qp[t] = 0;
				for (j = 0; j < k; j++)
					{
					Qp[t] += Q[t][j] * p[j];
					}
				pQp += p[t] * Qp[t];
				}
			float maxError = 0;
			for (t = 0; t < k; t++)
				{
				float error = Math.abs(Qp[t] - pQp);
				if (error > maxError)
					{
					maxError = error;
					}
				}
			if (maxError < eps)
				{
				break;
				}

			for (t = 0; t < k; t++)
				{
				float diff = (-Qp[t] + pQp) / Q[t][t];
				p[t] += diff;
				pQp = (pQp + diff * (diff * Q[t][t] + 2 * Qp[t])) / (1 + diff) / (1 + diff);
				for (j = 0; j < k; j++)
					{
					Qp[j] = (Qp[j] + diff * Q[t][j]) / (1 + diff);
					p[j] /= (1 + diff);
					}
				}
			}
		if (iter >= maximumIterations)
			{
			System.err.print("Multiclass probability attempted too many iterations\n");
			}
		return p;
		}

	public MultiClassModel(Properties props)
		{
		super(props);
		throw new NotImplementedException();
		/*
		numberOfClasses = Integer.parseInt(props.getProperty("nr_class"));

		StringTokenizer st = new StringTokenizer(props.getProperty("rho"));

		//ArrayList<BinaryModel<P>> models = new ArrayList<BinaryModel<P>>();
		while (st.hasMoreTokens())
			{
			BinaryModel<P> m = new BinaryModel<P>(kernel, param);
			m.rho = Float.parseFloat(st.nextToken());
			// no SVs yet
			oneVsOneModels.add(m);
			}
		//oneVsOneModels = models.toArray(new BinaryModel<P>[]{});

		probA = parseFloatArray(props.getProperty("probA"));
		probB = parseFloatArray(props.getProperty("probB"));*/
		}


	public void writeToStream(DataOutputStream fp) throws IOException
		{
		throw new NotImplementedException();
		/*
		super.writeToStream(fp);

		fp.writeBytes("nr_class " + numberOfClasses + "\n");

		fp.writeBytes("rho");
		for (BinaryModel<P> m : oneVsOneModels)
			{
			fp.writeBytes(" " + m.rho);
			}
		fp.writeBytes("\n");

		if (probA != null)// regression has probA only
			{
			fp.writeBytes("probA");
			for (int i = 0; i < numberOfClasses * (numberOfClasses - 1) / 2; i++)
				{
				fp.writeBytes(" " + probA[i]);
				}
			fp.writeBytes("\n");
			}
		if (probB != null)
			{
			fp.writeBytes("probB");
			for (int i = 0; i < numberOfClasses * (numberOfClasses - 1) / 2; i++)
				{
				fp.writeBytes(" " + probB[i]);
				}
			fp.writeBytes("\n");
			}

		//these must come after everything else
		writeSupportVectors(fp);

		fp.close();*/
		}

	protected void readSupportVectors(BufferedReader fp)
		{
		//** Implement support vector I/O
		throw new UnsupportedOperationException();
		}

	protected void writeSupportVectors(DataOutputStream fp) throws IOException
		{
		fp.writeBytes("SV\n");
		fp.writeBytes("Saving multi-class support vectors is not implemented yet");

		//** Implement support vector I/O
		// the original format is a spaghetti

		}

	public void putOneVsOneModel(L label1, L label2, BinaryModel<L, P> binaryModel)
		{
		oneVsOneModels.put(label1, label2, binaryModel);
		}

	public void putOneVsAllModel(L label1, BinaryModel<L, P> binaryModel)
		{
		oneVsAllModels.put(label1, binaryModel);
		}


	private class SymmetricHashMap2d<K extends Comparable, V>
		{
		HashMap<K, Map<K, V>> l1Map;
		private int sizePerDimension;

		public Iterable<V> values()
			{
			return new Iterable<V>()
			{
			public Iterator<V> iterator()
				{
				return valueIterator();
				}
			};
			}

		public Iterator<V> valueIterator()
			{
			return new Iterator<V>()
			{
			Iterator<K> k1iter = l1Map.keySet().iterator();
			Iterator<V> l2iter = null;

			public boolean hasNext()
				{
				return (l2iter != null && l2iter.hasNext()) || k1iter.hasNext();
				}

			public V next()
				{
				if (l2iter == null || !l2iter.hasNext())
					{
					if (k1iter.hasNext())
						{
						l2iter = l1Map.get(k1iter.next()).values().iterator();
						}
					else
						{
						return null;
						}
					}

				return l2iter.next();
				}

			public void remove()
				{
				throw new UnsupportedOperationException();
				}
			};
			}

		public SymmetricHashMap2d(int sizePerDimension)
			{
			this.sizePerDimension = sizePerDimension;
			l1Map = new HashMap<K, Map<K, V>>(sizePerDimension);
			}

		V get(K k1, K k2)
			{
			if (k1.compareTo(k2) > 0)
				{
				K k3 = k1;
				k1 = k2;
				k2 = k3;
				}

			Map<K, V> l2Map = l1Map.get(k1);
			if (l2Map == null)
				{
				l2Map = new HashMap<K, V>(sizePerDimension);
				l1Map.put(k1, l2Map);
				}

			return l2Map.get(k2);
			}

		public void put(K k1, K k2, V value)
			{
			if (k1.compareTo(k2) > 0)
				{
				K k3 = k1;
				k1 = k2;
				k2 = k3;
				}

			Map<K, V> l2Map = l1Map.get(k1);
			if (l2Map == null)
				{
				l2Map = new HashMap<K, V>();
				l1Map.put(k1, l2Map);
				}

			l2Map.put(k2, value);
			}
		}
	}
