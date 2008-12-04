package edu.berkeley.compbio.jlibsvm.multi;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import edu.berkeley.compbio.jlibsvm.DiscreteModel;
import edu.berkeley.compbio.jlibsvm.SolutionModel;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassModel;
import org.apache.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassModel<L extends Comparable, P> extends SolutionModel<P> implements DiscreteModel<L, P>
	{
	private static final Logger logger = Logger.getLogger(MultiClassModel.class);

	private int numberOfClasses;

	//private float[] probA;
	// pairwise probability information
	//private float[] probB;

	//BinaryModel<P>[] oneVsOneModels;
	// don't like array vs collection, but it's consistent with the rest for now

	//	private List<BinaryModel<P>> oneVsOneModels = new ArrayList<BinaryModel<P>>();

	private SymmetricHashMap2d<L, BinaryModel<L, P>> oneVsOneModels;
	private HashMap<L, BinaryModel<L, P>> oneVsAllModels;
	private HashMap<L, OneClassModel<L, P>> oneClassModels;

	public enum TestMode
		{
			BestOneVsAll, OneVsOneVotes, OneVsOneVotesWithOneVsAllPostReject, OneVsOneVotesWithOneVsAllPreReject
		}

	// generics are a hassle here  (T[] label; makes a mess)
	// label of each class, just to maintain a known order for the sake of keeping the decision_values etc. straight
	//** proscribed 1-D order for 2-D decision_values is error-prone
	List<L> labels;


	public MultiClassModel(KernelFunction<P> kernel, SvmParameter<L> param, int numberOfClasses)
		{
		super(kernel, param);
		this.numberOfClasses = numberOfClasses;
		oneVsOneModels = new SymmetricHashMap2d<L, BinaryModel<L, P>>(numberOfClasses);
		oneVsAllModels = new HashMap<L, BinaryModel<L, P>>(numberOfClasses);

		//this.oneClassVeto = param.oneClassVeto;
		//this.oneClassOnly = param.oneClassOnly;
		this.oneClassThreshold = param.oneClassThreshold;

		//this.oneVsAllVeto = param.oneVsAllVeto;
		//this.oneVsAllOnly = param.oneVsAllOnly;
		this.oneVsAllThreshold = param.oneVsAllThreshold;

		this.multiclassMode = param.multiclassMode;
		this.minVoteProportion = param.minVoteProportion;
		}

	// allocate this only once; it'll get cleared on every predictLabel() anyway	//List<L> bestLabelList = new ArrayList<L>();


	public enum MulticlassMode
		{
			OneClassOnly, OneVsAllOnly, AllVsAll, FilteredVsAll, FilteredVsFiltered
		}


	// a bunch of parameters controlling prediction speed, accuracy, and likelihood of reporting "unknown"
	//	boolean oneClassVeto;
	//	boolean oneClassOnly;
	double oneClassThreshold;

	//	boolean oneVsAllVeto;
	//	boolean oneVsAllOnly;
	double oneVsAllThreshold;

	MulticlassMode multiclassMode;
	double minVoteProportion;


	public Map<L, Float> computeOneClassProbabilities(P x)
		{
		Map<L, Float> oneClassProbabilities = new HashMap<L, Float>();

		//	boolean oneClassProb = supportsOneClassProbability();

		for (OneClassModel<L, P> oneClassModel : oneClassModels.values())
			{
			final float probability = oneClassModel.getProbability(x);
			if (probability >= oneClassThreshold)
				{
				oneClassProbabilities.put(oneClassModel.getLabel(), probability);
				}
			}
		return oneClassProbabilities;
		}

	public Map<L, Float> computeOneVsAllProbabilities(P x)
		{
		Map<L, Float> oneVsAllProbabilities = new HashMap<L, Float>();

		boolean prob = supportsOneVsOneProbability();

		for (BinaryModel<L, P> binaryModel : oneVsAllModels.values())
			{			// if probability info isn't available, just substitute 1 and 0.
			final float probability =
					prob ? binaryModel.getTrueProbability(x) : (binaryModel.predictValue(x) > 0. ? 1f : 0f);
			if (probability >= oneVsAllThreshold)
				{
				oneVsAllProbabilities.put(binaryModel.getTrueLabel(), probability);
				}
			}
		return oneVsAllProbabilities;
		}

	/**
	 * @param x
	 * @return null if no good label is found, otherwise the best label.
	 */
	public L predictLabel(P x)
		{
		// stage 1: one-class
		// always compute these; we may need them to tie-break when voting anyway (though that only works when probabilities are turned on)

		Map<L, Float> oneClassProbabilities = computeOneClassProbabilities(x);

		if (oneClassThreshold > 0 && oneClassProbabilities.isEmpty())
			{
			return null;
			}

		if (multiclassMode == MulticlassMode.OneClassOnly)
			{
			L bestLabel = null;
			float bestProbability = 0;
			for (Map.Entry<L, Float> entry : oneClassProbabilities.entrySet())
				{
				if (entry.getValue() > bestProbability)
					{
					bestLabel = entry.getKey();
					bestProbability = entry.getValue();
					}
				}
			return bestLabel;
			}

		// now oneClassProbabilities is populated with all of the classes that pass the threshold (maybe all of them).


		// stage 2: one vs all
		// always compute these; we may need them to tie-break when voting anyway (though that only works when probabilities are turned on)

		Map<L, Float> oneVsAllProbabilities = computeOneVsAllProbabilities(x);

		if (oneVsAllThreshold > 0 && oneVsAllProbabilities.isEmpty())
			{
			return null;
			}

		if (multiclassMode == MulticlassMode.OneVsAllOnly)
			{
			L bestLabel = null;
			float bestProbability = 0;
			for (Map.Entry<L, Float> entry : oneVsAllProbabilities.entrySet())
				{
				if (entry.getValue() > bestProbability)
					{
					bestLabel = entry.getKey();
					bestProbability = entry.getValue();
					}
				}
			return bestLabel;
			}

		// now oneVsAllProbabilities is populated with all of the classes that pass the threshold (maybe all of them).


		// stage 3: voting

		Multiset<L> votes = new HashMultiset<L>();

		if ((oneClassThreshold <= 0 && oneVsAllThreshold <= 0) || multiclassMode == MulticlassMode.AllVsAll)
			{
			// vote using all models

			// if requiredActive == 0 but there is a oneVsAll threshold, we may compute votes between two
			// inactive classes; it may be that the winner of the voting fails the oneVsAll filter, in which
			// case we may want to report unknown instead of reporting the best class that does pass.  This
			// is what PhyloPythia does.

			for (BinaryModel<L, P> binaryModel : oneVsOneModels.values())
				{
				votes.add(binaryModel.predictLabel(x));
				}
			}
		else
			{
			//vote using only the active models one one side of the comparison, maybe on both.

			// compute remainder set after vetos
			Set<L> activeClasses = oneClassProbabilities.keySet();
			activeClasses.retainAll(oneVsAllProbabilities.keySet());

			int requiredActive = multiclassMode == MulticlassMode.FilteredVsAll ? 1 : 2;

			// assert requiredActive == 2 ? voteMode = VoteMode.FilteredVsFiltered

			for (BinaryModel<L, P> binaryModel : oneVsOneModels.values())
				{
				int activeCount = (activeClasses.contains(binaryModel.getTrueLabel()) ? 1 : 0) + (
						activeClasses.contains(binaryModel.getFalseLabel()) ? 1 : 0);

				if (activeCount >= requiredActive)
					{
					votes.add(binaryModel.predictLabel(x));
					}
				}
			}


		// stage 4: count votes

		L bestLabel = null;
		int bestCount = 0;
		float bestOneVsAllProbability = 0;
		float bestOneClassProbability = 0;
		int countSum = 0;
		for (L label : votes.elementSet())
			{
			int count = votes.count(label);
			countSum += count;

			// primary sort by number of votes
			// secondary sort by one-vs-all probability, if available
			// tertiary sort be one-class probability, if available

			// if this is null it means this label didn't pass the threshold earlier
			Float oneVsAll = oneVsAllProbabilities.get(label);
			oneVsAll = oneVsAll == null ? 0f : oneVsAll;

			// if this is null it means this label didn't pass the threshold earlier
			Float oneClass = oneClassProbabilities.get(label);
			oneClass = oneClass == null ? 0f : oneClass;

			if (count > bestCount || (count == bestCount && (oneVsAll > bestOneVsAllProbability
					|| oneClass > bestOneClassProbability)))
				{
				bestLabel = label;
				bestCount = count;
				bestOneVsAllProbability = oneVsAll;
				bestOneClassProbability = oneClass;
				}
			}


		// stage 5: check for inadequate evidence filters.
		// The oneVsAllThreshold thing is partly implicit in having filtered on it earlier, but there are loopholes so it's safer to just check

		if ((((double) bestCount / (double) countSum) < minVoteProportion)
				|| bestOneVsAllProbability < oneVsAllThreshold)
			{
			return null;
			}


		return bestLabel;

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


	public boolean supportsOneVsOneProbability()
		{		// just check the first model and assume the rest are the same
		return oneVsOneModels.valueIterator().next().sigmoid != null;//		return probA != null && probB != null;
		}

	/*
	public boolean supportsOneClassProbability()
		{		// just check the first model and assume the rest are the same
		return oneClassModels.valueIterator().next().sigmoid != null;//		return probA != null && probB != null;
		}
*/

	public Map<L, Float> predictProbability(P x)
		{
		if (!supportsOneVsOneProbability())
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

				float prob = binaryModel.sigmoid.predict(binaryModel.predictValue(x))
						;				//MathSupport.sigmoidPredict(decisionValues[k], probA[k], probB[k])

				pairwiseProbabilities[i][j] = Math.min(Math.max(prob, minimumProbability), 1 - minimumProbability);
				pairwiseProbabilities[j][i] = 1 - pairwiseProbabilities[i][j];				//k++;
				}
			}
		float[] probabilityEstimates = multiclassProbability(numberOfClasses, pairwiseProbabilities);

		// but then map back to the cleaner Map API.  Note the probabilityEstimates should come back in order corresponding to the labels list.

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
			{
			// stopping condition, recalculate QP,pQP for numerical accuracy
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
			logger.error("Multiclass probability attempted too many iterations");
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
		{		//** Implement support vector I/O
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
