package edu.berkeley.compbio.jlibsvm.multi;

import com.davidsoergel.conja.Function;
import com.davidsoergel.conja.Parallel;
import com.davidsoergel.dsutils.collections.UnorderedPair;
import com.davidsoergel.dsutils.collections.UnorderedPairIterator;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterGrid;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationProblem;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationSVM;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.binary.BooleanClassificationProblemImpl;
import edu.berkeley.compbio.jlibsvm.labelinverter.LabelInverter;
import edu.berkeley.compbio.jlibsvm.util.SubtractionMap;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassificationSVM<L extends Comparable<L>, P> extends SVM<L, P, MultiClassProblem<L, P>>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(MultiClassificationSVM.class);

	private BinaryClassificationSVM<L, P> binarySvm;


// --------------------------- CONSTRUCTORS ---------------------------

	public MultiClassificationSVM(BinaryClassificationSVM<L, P> binarySvm)
		{
		//super(binarySvm.kernel, binarySvm.scalingModelLearner, binarySvm.param);
		this.binarySvm = binarySvm;
		}

// -------------------------- OTHER METHODS --------------------------

	@Override
	public String getSvmType()
		{
		return "multiclass " + binarySvm.getSvmType();
		}

	public SvmMultiClassCrossValidationResults<L, P> performCrossValidation(@NotNull MultiClassProblem<L, P> problem,
	                                                                        @NotNull ImmutableSvmParameter<L, P> param)
		//,@NotNull final TreeExecutorService execService)
		{
		Map<P, L> predictions = discreteCrossValidation(problem, param); //, execService);

		SvmMultiClassCrossValidationResults<L, P> cv =
				new SvmMultiClassCrossValidationResults<L, P>(problem, predictions);
		cv.param = param;
		return cv;
		}

	public MultiClassModel<L, P> train(@NotNull MultiClassProblem<L, P> problem,
	                                   @NotNull ImmutableSvmParameter<L, P> param)
		//,@NotNull final TreeExecutorService execService)
		{
		validateParam(param);

		MultiClassModel<L, P> result;
		if (param instanceof ImmutableSvmParameterGrid && !param.gridsearchBinaryMachinesIndependently)
			{
			//	if (param.gridsearchBinaryMachinesIndependently)
			//		{
			//		result = trainScaledWithCV(problem, param);
			//		}
			//	else
			//		{

			// performs cross-validation at each grid point regardless
			result = trainGrid(problem, (ImmutableSvmParameterGrid<L, P>) param); //, execService);


			//		}
			}
		else
			{
			// train once using all the data
			result = trainScaled(problem, param); //, execService);

			// also perform CV if requested
			/*	if (param.crossValidation)
			   {
			   MultiClassCrossValidationResults<L, P> cv = performCrossValidation(problem, param);
			   result.crossValidationResults = cv;
			   }*/
			}
		return result;
		}

	public MultiClassModel<L, P> trainGrid(@NotNull final MultiClassProblem<L, P> problem,
	                                       @NotNull final ImmutableSvmParameterGrid<L, P> param)
		//,@NotNull final TreeExecutorService execService)
		{
		final GridTrainingResult gtresult = new GridTrainingResult();

//		Set<Runnable> gridTasks = new HashSet<Runnable>();

		Collection<ImmutableSvmParameterPoint<L, P>> parameterPoints = param.getGridParams();

		//int numGridPoints = parameterPoints.size();

		Parallel.forEach(parameterPoints, new Function<ImmutableSvmParameterPoint<L, P>, Void>()
		{
		public Void apply(final ImmutableSvmParameterPoint<L, P> gridParam)
			{// note we must use the CV variant in order to know which parameter set is best
			SvmMultiClassCrossValidationResults<L, P> crossValidationResults =
					performCrossValidation(problem, gridParam); //, execService);
			gtresult.update(crossValidationResults); //
			// if we did a grid search, keep track of which parameter set was used for these results
			return null;
			}
		});

		// no need for the iterator version here; the set of params doesn't require too much memory


		/*
		for (final ImmutableSvmParameterPoint<L, P> gridParam : parameterPoints)
			{
			gridTasks.add(new Runnable()
			{
			public void run()
				{

				}
			});
			}

		execService.submitAndWaitForAll(gridTasks); //, "Evaluated %d of " + numGridPoints + " grid points",30);
*/
		logger.info("Chose grid point: " + gtresult.bestCrossValidationResults.param);

		// finally train once on all the data (including rescaling)
		MultiClassModel<L, P> result =
				trainScaled(problem, gtresult.bestCrossValidationResults.param); //, execService);
		result.crossValidationResults = gtresult.bestCrossValidationResults;
		return result;
		}


	private class GridTrainingResult
		{
		//ImmutableSvmParameterPoint<L, P> bestParam = null;
		SvmMultiClassCrossValidationResults<L, P> bestCrossValidationResults = null;
		float bestSensitivity = -1F;

		synchronized void update( //ImmutableSvmParameterPoint<L, P> gridParam,
		                          SvmMultiClassCrossValidationResults<L, P> crossValidationResults)
			{
			float sensitivity = crossValidationResults.classNormalizedSensitivity();
			if (sensitivity > bestSensitivity)
				{
				//bestParam = gridParam;
				bestSensitivity = sensitivity;
				bestCrossValidationResults = crossValidationResults;
				}
			}
		}


	/**
	 * Train the classifier, and also prepare the probability sigmoid thing if requested.
	 *
	 * @param problem
	 * @return
	 */
/*	private MultiClassModel<L, P> trainScaledWithCV(MultiClassProblem<L, P> problem,
	                                                @NotNull ImmutableSvmParameter<L, P> param)
		{
		// if scaling is enabled, then each fold will be independently scaled also; so we don't need to scale the whole dataset prior to CV
		// in fact  we don't want to scale based on the entire dataset, only based on each fold, since that would be cheating (if very subtly so)

		MultiClassCrossValidationResults<L, P> cv = performCrossValidation(problem, param);

		// finally train once on all the data (including rescaling)
		MultiClassModel<L, P> result = trainScaled(problem, param);
		result.crossValidationResults = cv;

		//	result.printSolutionInfo(problem);
		return result;
		}
*/
	public MultiClassModel<L, P> trainScaled(@NotNull final MultiClassProblem<L, P> problem,
	                                         @NotNull final ImmutableSvmParameter<L, P> param)
		//, @NotNull final TreeExecutorService execService)
		{
		if (param.scalingModelLearner != null && !param.scaleBinaryMachinesIndependently)
			{
			return trainWithoutScaling(problem.getScaledCopy(param.scalingModelLearner), param); //, execService);
			}
		else
			{
			return trainWithoutScaling(problem, param); //, execService);
			}
		}

	private MultiClassModel<L, P> trainWithoutScaling(@NotNull final MultiClassProblem<L, P> problem,
	                                                  @NotNull final ImmutableSvmParameter<L, P> param)
		//, @NotNull final TreeExecutorService execService)
		{
		int numLabels = problem.getLabels().size();

		final MultiClassModel<L, P> model = new MultiClassModel<L, P>(param, numLabels);

		model.setScalingModel(problem.getScalingModel());

		/**
		 * The weights are not properly part of the param, because they may depend on the problem (i.e. the proportions of examples in different classes).
		 * They're also not properly part of the problem, since they certainly depend on the param.C and param.redistributeUnbalancedC.
		 *
		 * Now the approach is: just recompute them from scratch within each binary machine
		 */
		//final Map<L, Float> weights = prepareWeights(problem, param);

		/*
		if (param.multiclassMode != MultiClassModel.MulticlassMode.OneVsAllOnly)
			{
			// create and train one-class classifiers

			logger.info("Training one-class classifiers for " + numLabels + " labels");

			for (Map.Entry<L,Set<P>> entry : problem.getExamplesByLabel().entrySet())
				{
				Map<P, Float> subExamples = new HashMap<P, Float>(problem.getNumExamples(););

				for (P point : entry.getValue())
					{
					subExamples.put(point, 1f);
					}

				OneClassProblem<L, P> subProblem =
						new OneClassProblemImpl<L, P>(subExamples, entry.getKey());

				model.putOneClassModel(entry.getKey(), oneClassSvm.train(subProblem));
				}
			}
*/

		final Map<L, Set<P>> examplesByLabel = problem.getExamplesByLabel();

		if (param.oneVsAllMode != MultiClassModel.OneVsAllMode.None)
			{
			// create and train one vs all classifiers.


			// oneVsAll models always need a probability sigmoid

			final ImmutableSvmParameter<L, P> probParam = param.withProbabilityCopy();

			// first queue up all the training tasks and submit them to the thread pool

			logger.info("Training one-vs-all classifiers for " + numLabels + " labels");

			final LabelInverter<L> labelInverter = problem.getLabelInverter();

			Parallel.forEach(problem.getLabels(), new Function<L, Void>()
			{
			public Void apply(final L label)
				{
				final L notLabel = labelInverter.invert(label);

				final Set<P> labelExamples = examplesByLabel.get(label);

				Collection<Map.Entry<P, L>> entries = problem.getExamples().entrySet();
				if (param.falseClassSVlimit != Integer.MAX_VALUE)
					{
					// guarantee entries in random order if limiting the number of false examples
					List<Map.Entry<P, L>> entryList = new ArrayList<Map.Entry<P, L>>(entries);
					Collections.shuffle(entryList);
					int toIndex = param.falseClassSVlimit + labelExamples.size();
					toIndex = Math.min(toIndex, entryList.size());
					entries = entryList.subList(0, toIndex);
					}

				final Set<P> notlabelExamples =
						new SubtractionMap<P, L>(entries, labelExamples, param.falseClassSVlimit).keySet();

				final BinaryClassificationProblem<L, P> subProblem =
						new BooleanClassificationProblemImpl<L, P>(problem.getLabelClass(), label, labelExamples,
						                                           notLabel, notlabelExamples, problem.getExampleIds());
				// Unbalanced data: see prepareWeights
				// since these will be extremely unbalanced, this should nearly guarantee that no positive examples are misclassified.

				//Future<BinaryModel<L, P>> fut = execService.submit(binarySvm.trainCallable(subProblem, param));

				BinaryModel<L, P> result = binarySvm.train(subProblem, probParam); //, execService);
				model.putOneVsAllModel(label, result);
				return null;
				}
			});

			/*
			Iterator<Runnable> oneVsAllTaskIterator = new MappingIterator<L, Runnable>(problem.getLabels())
			{
			public Runnable function(final L label)
				{
				return new Runnable()
				{
				public void run()
					{

					}
				};
				}
			};

			execService.submitAndWaitForAll(oneVsAllTaskIterator); //,"Trained %d one-vs-all models", 30);
			*/
			}


		if (param.allVsAllMode != MultiClassModel.AllVsAllMode.None)
			{
			final int numClassifiers = (numLabels * (numLabels - 1)) / 2;


			// Iterator version would be a hassle here, never mind
			//Iterator<Runnable> allVsAllTaskIterator = new MappingIterator<Pair<L>, Runnable>(problem.getLabelPairs()){};


//			Set<Runnable> allVsAllTasks = new HashSet<Runnable>(numClassifiers);

			// create and train all vs all classifiers

			// first queue up all the training tasks and submit them to the thread pool


			logger.info("Training " + numClassifiers + " one-vs-one classifiers for " + numLabels + " labels");
			int c = 0;

			UnorderedPairIterator<L> labelPairIterator =
					new UnorderedPairIterator<L>(problem.getLabels(), problem.getLabels());

			Parallel.forEach(labelPairIterator, new Function<UnorderedPair<L>, Void>()
			{
			public Void apply(final UnorderedPair<L> from)
				{
				final L label1 = from.getKey1();
				final L label2 = from.getKey2();

				final Set<P> label1Examples = examplesByLabel.get(label1);
				final Set<P> label2Examples = examplesByLabel.get(label2);

				final BinaryClassificationProblem<L, P> subProblem =
						new BooleanClassificationProblemImpl<L, P>(problem.getLabelClass(), label1, label1Examples,
						                                           label2, label2Examples, problem.getExampleIds());

				BinaryModel<L, P> result = binarySvm.train(subProblem, param); //, execService);
				model.putOneVsOneModel(label1, label2, result);

				return null;
				}
			});


			/*
			for (final L label1 : problem.getLabels())
				{
				for (final L label2 : problem.getLabels())
					{
					if (label1.compareTo(label2) < 0)// avoid redundant pairs
						{
						allVsAllTasks.add(new Runnable()
						{
						public void run()
							{
							final Set<P> label1Examples = examplesByLabel.get(label1);
							final Set<P> label2Examples = examplesByLabel.get(label2);

							final BinaryClassificationProblem<L, P> subProblem =
									new BooleanClassificationProblemImpl<L, P>(problem.getLabelClass(), label1,
									                                           label1Examples, label2, label2Examples,
									                                           problem.getExampleIds());

							BinaryModel<L, P> result = binarySvm.train(subProblem, param); //, execService);
							model.putOneVsOneModel(label1, label2, result);
							}
						});
						}
					}
				}

			execService.submitAndWaitForAll(allVsAllTasks); //,"Trained %d one-vs-one models", 30);
			*/
			}

		model.prepareModelSvMaps();
		return model;
		}


	// REVIEW whether weights should be made consistent across multiple binary machines
	/**
	 * Compute weights for each class, by which C should be multiplied later. Does not include the C term already, because
	 * there may later be a grid search based on Cfactor.
	 * <p/>
	 * This method places all classes on the same C scale: i.e., for classes A, B, and C, the pairwise ratios are all
	 * correct. The upshot is that the average cost of misclassifying a sample is made consistent across all the binary
	 * machines. Is there a point to that?  If we're doing grid searches for each binary machine anyway, then this
	 * consistency is obviated.  If not, then a pair of classes with few total examples will be evaluated using a much
	 * lower C than a pair of classes with a lot of examples.
	 * <p/>
	 * Alternatively, if each binary machine (even without a grid search) computes its own weighted C's, then each would
	 * use a consistent average C (whatever was specified in the parameters).
	 * <p/>
	 * Note also that if we don't let each binary machine recompute its own weights, then same weights would be used for
	 * each cross-validation run.  That should be fine assuming that the folds are uniformly sampled, but it is technically
	 * unfair in that data about the test samples in an input to training.
	 * <p/>
	 * I have no idea what the right solution is.  Perhaps we should do the binary grid searches and see how the optimal
	 * C's turn out.
	 * <p/>
	 * Disabling the whole procedure & delegating to the binary machines for now.
	 */
/*	private Map<L, Float> prepareWeights(MultiClassProblem<L, P> problem, @NotNull ImmutableSvmParameter<L, P> param)
		{
		LabelInverter<L> labelInverter = problem.getLabelInverter();

		Map<L, Float> weights = new HashMap<L, Float>();


		final Map<L, Set<P>> examplesByLabel = problem.getExamplesByLabel();

		if (!param.redistributeUnbalancedC)
			{
			for (Map.Entry<L, Set<P>> entry : examplesByLabel.entrySet())
				{
				L label = entry.getKey();

				weights.put(label, 1F);

				L inverse = labelInverter.invert(label);

				weights.put(inverse, 1F);
				}
			}
		else
			{
			int numExamples = problem.getNumExamples();

			int numClasses = examplesByLabel.size();

			// first figure out the average total weight for each class if the samples were uniformly distributed (i.e., the average number of examples with weight 1)
			float totalWeightPerClass = (float) numExamples / (float) numClasses;
			//float totalCPerRemainder = totalCPerClass * (numClasses - 1);


			// then assign the proper weight per _sample_ within each class by distributing the per-class weight
			for (Map.Entry<L, Set<P>> entry : examplesByLabel.entrySet())
				{
				// the weight per sample is just the total class weight divided by the number of samples

				L label = entry.getKey();
				Set<P> examples = entry.getValue();
				float weight = totalWeightPerClass / (float) examples.size();

				weights.put(label, weight);

				// also prepare weights for the one-vs-all case.

				// For one-vs-all, we want the inverse class to have the same total weight as the positive class, i.e. totalCPerClass.
				// Note scaling problem: we can't scale up the positive class, so we have to scale down the negative class
				//*i.e. we pretend that all of the negative examples are in one class, and so have totalCPerClass.

				L inverse = labelInverter.invert(label);
				int numFalseExamples = numExamples - examples.size();
				numFalseExamples = Math.min(numFalseExamples, param.falseClassSVlimit);
				float inverseWeight = totalWeightPerClass / numFalseExamples;
				weights.put(inverse, inverseWeight);
				}

			if (!param.isWeightsEmpty())
				{
				logger.warn("Ignoring provided class weights; we compute them from C and the number of examples");
				}
			}
//
//	   // use param.C as the default weight...
//	   for (L label : problem.getLabels())
//		   {
//		   weights.put(label, param.C);
//		   }
//
//
//	   // ... but if any weights are provided, apply them
//	   for (Map.Entry<L, Float> weightEntry : param.getWeights().entrySet())
//		   {
//		   L key = weightEntry.getKey();
//		   if (problem.getLabels().contains(key))
//			   {
//			   Float w = weightEntry.getValue();
//			   weights.put(key, w * param.C);
//			   }
//		   else
//			   {
//			   logger.warn("class label " + key + " specified in weight is not found");
//			   }
//		   }
		return weights;
		}

*/
	}
