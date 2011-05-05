package edu.berkeley.compbio.jlibsvm.multi;

import com.davidsoergel.conja.Function;
import com.davidsoergel.conja.Parallel;
import com.davidsoergel.stats.DissimilarityMeasure;
import com.davidsoergel.stats.DistributionException;
import com.davidsoergel.trees.htpn.HierarchicalTypedPropertyNode;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationSVM;
import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModel;
import edu.berkeley.compbio.ml.cluster.AbstractClusteringMethod;
import edu.berkeley.compbio.ml.cluster.BasicBatchCluster;
import edu.berkeley.compbio.ml.cluster.ClusterException;
import edu.berkeley.compbio.ml.cluster.ClusterMove;
import edu.berkeley.compbio.ml.cluster.Clusterable;
import edu.berkeley.compbio.ml.cluster.ClusterableIterator;
import edu.berkeley.compbio.ml.cluster.ClusteringTestResults;
import edu.berkeley.compbio.ml.cluster.NoGoodClusterException;
import edu.berkeley.compbio.ml.cluster.PointClusterFilter;
import edu.berkeley.compbio.ml.cluster.ProhibitionModel;
import edu.berkeley.compbio.ml.cluster.SampleInitializedBatchClusteringMethod;
import edu.berkeley.compbio.ml.cluster.SupervisedClusteringMethod;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassificationSVMAdapter<T extends Clusterable<T>>
		extends AbstractClusteringMethod<T, BasicBatchCluster<T>> implements SampleInitializedBatchClusteringMethod<T>,
//		extends AbstractBatchClusteringMethod<T, BasicBatchCluster<T>> implements
                                                                             SupervisedClusteringMethod<T>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(MultiClassificationSVMAdapter.class);


	final ImmutableSvmParameter<BasicBatchCluster<T>, T> param;

	final Map<T, BasicBatchCluster<T>> examples = new HashMap<T, BasicBatchCluster<T>>();
	final Map<T, Integer> exampleIds = new HashMap<T, Integer>();


	Map<String, BasicBatchCluster<T>> theClusterMap;

	//Map<String, MultiClassModel<BasicBatchCluster<T>, T>> leaveOneOutModels;

	private MultiClassModel<BasicBatchCluster<T>, T> model;


	private BinaryClassificationSVM<BasicBatchCluster<T>, T> binarySvm;


// --------------------------- CONSTRUCTORS ---------------------------

	//public MultiClassificationSVMAdapter(@NotNull ImmutableSvmParameter<BasicBatchCluster<T>, T> param)
	//	{
	//	super(null);
	//	this.param = param;
	//	}

	public MultiClassificationSVMAdapter(final Set<String> potentialTrainingBins,
	                                     final Map<String, Set<String>> predictLabelSets,
	                                     final ProhibitionModel<T> prohibitionModel, final Set<String> testLabels,
	                                     @NotNull final ImmutableSvmParameter<BasicBatchCluster<T>, T> param)
		{
		super(null, potentialTrainingBins, predictLabelSets, prohibitionModel, testLabels);
		this.param = param;
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	public void setBinarySvm(final BinaryClassificationSVM<BasicBatchCluster<T>, T> binarySvm)
		{
		this.binarySvm = binarySvm;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface BatchClusteringMethod ---------------------


	final AtomicInteger trainingCount = new AtomicInteger(0);

	public void initializeWithSamples(
			final ClusterableIterator<T> trainingIterator) //CollectionIteratorFactory<T> trainingCollectionIteratorFactory)
	{
	//	Iterator<T> trainingIterator = trainingCollectionIteratorFactory.next();


	// cache the training set into an example map
	//  (too bad the svm training requires all examples in memory)

	//Multimap<String, T> examples = new HashMultimap<String, T>();

	Parallel.forEach(trainingIterator, new Function<T, Void>()
	{
	public Void apply(@Nullable final T sample)
		{
		add(sample);
		return null;
		}
	});

	logger.info("Prepared " + trainingCount + " training samples");
	}

	private void add(final T sample)
		{
		final String label = sample.getImmutableWeightedLabels().getDominantKeyInSet(potentialTrainingBins);

		final BasicBatchCluster<T> cluster = theClusterMap.get(label);
		cluster.add(sample);

		//	synchronized (trainingCount)
		//		{
		examples.put(sample, cluster);
		exampleIds.put(sample, trainingCount.intValue());
		trainingCount.incrementAndGet();

/*			if (trainingCount.intValue() % 1000 == 0)
				{
				logger.debug("Prepared " + trainingCount + " training samples");
				}*/
		//		}
		}

//	public void initializeWithRealData(Iterator<T> trainingIterator, int initSamples,
//	                                   GenericFactory<T> prototypeFactory)
//			throws GenericFactoryException, ClusterException
//		{
	// do nothing with the iterator or any of that
	// assert initSamples == 0;
//		}

	public void createClusters()
		{

		theClusterMap = new HashMap<String, BasicBatchCluster<T>>(potentialTrainingBins.size());
		int i = 0;
		for (final String label : potentialTrainingBins)
			{
			final BasicBatchCluster<T> cluster = theClusterMap.get(label);

			if (cluster == null)
				{
				final BasicBatchCluster<T> cluster1 = new BasicBatchCluster<T>(i++);
				theClusterMap.put(label, cluster1);
				addCluster(cluster1);

				// ** consider how best to store the test labels

				// derive the label probabilities from the training weights later, as usual
				/*	HashWeightedSet<String> derivedLabelProbabilities = new HashWeightedSet<String>();
				derivedLabelProbabilities.add(label, 1.);
				derivedLabelProbabilities.incrementItems();
				cluster.setDerivedLabelProbabilities(derivedLabelProbabilities);
				*/
				}
			}
		//theClusters = theClusterMap.values();
		}

/*	public void train(CollectionIteratorFactory<T> trainingCollectionIteratorFactory,
	                  int trainingEpochs) throws IOException, ClusterException
		{
		addAll(trainingCollectionIteratorFactory.next());
		train();
		}*/

	//public ThreadPoolPerformanceStats trainingStats;

	public void train()
		{
		final MultiClassificationSVM<BasicBatchCluster<T>, T> svm =
				new MultiClassificationSVM<BasicBatchCluster<T>, T>(binarySvm);
		final MultiClassProblem<BasicBatchCluster<T>, T> problem =
				new MultiClassProblemImpl<BasicBatchCluster<T>, T>(BasicBatchCluster.class,
				                                                   new BatchClusterLabelInverter<T>(), examples,
				                                                   exampleIds, new NoopScalingModel<T>());
		//svm.setupQMatrix(problem);
		logger.debug("Performing multiclass training");

		//final DepthFirstThreadPoolExecutor execService = DepthFirstThreadPoolExecutor.getInstance();

		//	DepthFirstThreadPoolExecutor execService = new DepthFirstThreadPoolExecutor(nrThreads, nrThreads * 2);
		model = svm.train(problem, param); //, execService);

		//trainingStats = execService.shutdown();
		//execService.shutdown();


/*
		if (prohibitionModel != null)
			{
			leaveOneOutModels =
					new MapMaker().makeComputingMap(new Function<T, MultiClassModel<BasicBatchCluster<T>, T>>()
					{
					public MultiClassModel<BasicBatchCluster<T>, T> apply(@Nullable final T p) //String disallowedLabel)
						{
						final Set<BasicBatchCluster<T>> disallowedClusters = new HashSet<BasicBatchCluster<T>>();

						for (final BasicBatchCluster<T> cluster : model.getLabels())
							{
							if(prohibitionModel.isProhibited(p, cluster))
						//	if (cluster.getWeightedLabels().getDominantKeyInSet(leaveOneOutLabels)
						//			.equals(disallowedLabel))
								{
								disallowedClusters.add(cluster);
								}
							}
						return new MultiClassModel<BasicBatchCluster<T>, T>(model, disallowedClusters);
						}
					});
			}
*/
		removeEmptyClusters();
		normalizeClusterLabelProbabilities();
		doneLabellingClusters();
		}


	public void putResults(final HierarchicalTypedPropertyNode<String, Serializable, ?> innerResults)
		{
		/*	resultsNode.addChild("trainingCpuSeconds", trainingStats.getCpuSeconds());
		resultsNode.addChild("trainingUserSeconds", trainingStats.getUserSeconds());
		resultsNode.addChild("trainingSystemSeconds", trainingStats.getSystemSeconds());
		resultsNode.addChild("trainingBlockedSeconds", trainingStats.getBlockedSeconds());
		resultsNode.addChild("trainingWaitedSeconds", trainingStats.getWaitedSeconds());
		*/
		}

	private MultiClassModel<BasicBatchCluster<T>, T> makeMultiClassModelWithProhibition(
			@Nullable final T p) //String disallowedLabel)
	{
	final Set<BasicBatchCluster<T>> disallowedClusters = new HashSet<BasicBatchCluster<T>>();

	PointClusterFilter<T> clusterFilter = prohibitionModel == null ? null : prohibitionModel.getFilter(p);

	for (final BasicBatchCluster<T> cluster : model.getLabels())
		{
		if (clusterFilter != null && clusterFilter.isProhibited(cluster))
			//	if (cluster.getWeightedLabels().getDominantKeyInSet(leaveOneOutLabels)
			//			.equals(disallowedLabel))
			{
			disallowedClusters.add(cluster);
			}
		}
	return new MultiClassModel<BasicBatchCluster<T>, T>(model, disallowedClusters);
	}


	// -------------------------- OTHER METHODS --------------------------

	@Override
	public synchronized ClusteringTestResults test(final ClusterableIterator<T> theTestIterator,
	                                               final DissimilarityMeasure<String> intraLabelDistances)
			throws DistributionException, ClusterException
		{
		final ClusteringTestResults result = super.test(theTestIterator, intraLabelDistances);
		result.setInfo(model.getInfo());
		//result.setCrossValidationResults(model.getCrossValidationResults());
		return result;
		}

	public ClusterMove<T, BasicBatchCluster<T>> bestClusterMove(final T p) throws NoGoodClusterException
		{
		MultiClassModel<BasicBatchCluster<T>, T> leaveOneOutModel = model;
		if (prohibitionModel != null)
			{
			try
				{
				//BAD LOO doesn't work??
				//	final String disallowedLabel = p.getWeightedLabels().getDominantKeyInSet(leaveOneOutLabels);
				//	leaveOneOutModel = leaveOneOutModels.get(p); //disallowedLabel);
				leaveOneOutModel = makeMultiClassModelWithProhibition(p);
				}
			catch (NoSuchElementException e)
				{
				// OK, just use the full model then
				//leaveOneOutModel = model;
				}
			}


		final VotingResult<BasicBatchCluster<T>> r = leaveOneOutModel.predictLabelWithQuality(p);
		final ClusterMove<T, BasicBatchCluster<T>> result = new ClusterMove<T, BasicBatchCluster<T>>();
		result.bestCluster = r.getBestLabel();

		result.voteProportion = r.getBestVoteProportion();
		result.secondBestVoteProportion = r.getSecondBestVoteProportion();

		result.bestDistance = r.getBestOneVsAllProbability();
		result.secondBestDistance = r.getSecondBestOneVsAllProbability();


		//**  just drop these for now
		/*
		r.getBestOneClassProbability();
		r.getSecondBestOneClassProbability();
		*/


		if (result.bestCluster == null)
			{
			throw new NoGoodClusterException();
			}

		// no other fields of ClusterMove are populated :(
		return result;
		}
	}
