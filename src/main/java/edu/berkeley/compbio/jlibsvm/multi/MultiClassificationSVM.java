package edu.berkeley.compbio.jlibsvm.multi;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

	private MultiClassCrossValidationResults<L, P> performCrossValidation(MultiClassProblem<L, P> problem,
	                                                                      @NotNull ImmutableSvmParameter<L, P> param)
		{
		Map<P, L> predictions = discreteCrossValidation(problem, param);

		MultiClassCrossValidationResults<L, P> cv = new MultiClassCrossValidationResults<L, P>(problem, predictions);
		return cv;
		}

	public MultiClassModel<L, P> train(MultiClassProblem<L, P> problem, @NotNull ImmutableSvmParameter<L, P> param)
		{
		validateParam(param);

		MultiClassModel<L, P> result;
		if (param instanceof ImmutableSvmParameterGrid && !param.gridsearchBinaryMachinesIndependently)
			{
			result = trainGrid(problem, (ImmutableSvmParameterGrid<L, P>) param);
			}
		else
			{
			result = trainScaled(problem, param);
			}
		return result;
		}

	public MultiClassModel<L, P> trainGrid(MultiClassProblem<L, P> problem,
	                                       @NotNull ImmutableSvmParameterGrid<L, P> param)
		{
		// PERF should be concurrent.  Doesn't matter for multiclass since that's already multithreaded, but each binary grid-search will be single-threaded here.

		ImmutableSvmParameterPoint<L, P> bestParam = null;
		MultiClassCrossValidationResults<L, P> bestCrossValidationResults = null;
		float bestSensitivity = -1F;
		for (ImmutableSvmParameterPoint<L, P> gridParam : param.getGridParams())
			{
			// note we must use the CV variant in order to know which parameter set is best

			//float gridCp = Cp * gridParam.Cfactor;
			//float gridCn = Cn * gridParam.Cfactor;

			//MultiClassModel<L, P> result = trainScaledWithCV(problem, gridParam);
			MultiClassCrossValidationResults<L, P> crossValidationResults = performCrossValidation(problem, gridParam);
			float sensitivity = crossValidationResults.classNormalizedSensitivity();
			if (sensitivity > bestSensitivity)
				{
				bestParam = gridParam;
				bestSensitivity = sensitivity;
				bestCrossValidationResults = crossValidationResults;
				}
			}

		logger.info("Chose grid point: " + bestParam);

		// finally train once on all the data (including rescaling)
		MultiClassModel<L, P> result = trainScaled(problem, bestParam);
		result.crossValidationResults = bestCrossValidationResults;
		return result;
		}

	/**
	 * Train the classifier, and also prepare the probability sigmoid thing if requested.  Note that svcProbability will
	 * call this method in the course of cross-validation, but will first ensure that param.probability == false;
	 *
	 * @param problem
	 * @return
	 */
	private MultiClassModel<L, P> trainScaledWithCV(MultiClassProblem<L, P> problem,
	                                                @NotNull ImmutableSvmParameter<L, P> param)
		{
		// if scaling each binary machine is enabled, then each fold will be independently scaled also; so we don't need to scale the whole dataset prior to CV

		MultiClassCrossValidationResults<L, P> cv = performCrossValidation(problem, param);

		// finally train once on all the data (including rescaling)
		MultiClassModel<L, P> result = trainScaled(problem, param);
		result.crossValidationResults = cv;

		//	result.printSolutionInfo(problem);
		return result;
		}

	public MultiClassModel<L, P> trainScaled(MultiClassProblem<L, P> problem,
	                                         @NotNull ImmutableSvmParameter<L, P> param)
		{
		if (param.scalingModelLearner != null && !param.scaleBinaryMachinesIndependently)
			{
			// scale the entire problem before doing anything else
			problem = problem.getScaledCopy(param.scalingModelLearner);
			}


		int numLabels = problem.getLabels().size();

		MultiClassModel<L, P> model = new MultiClassModel<L, P>(param, numLabels);

		model.setScalingModel(problem.getScalingModel());

		/**
		 * The weights are not properly part of the param, because they may depend on the problem (i.e. the proportions of examples in different classes).
		 * They're also not properly part of the problem, since they certainly depend on the param.C and param.redistributeUnbalancedC.
		 *
		 * Now the approach is: just recompute them from scratch within each binary machine
		 */
		//final Map<L, Float> weights = prepareWeights(problem, param);

		// ** make number of threads adjustable?
		ExecutorService execService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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

		Map<L, Set<P>> examplesByLabel = problem.getExamplesByLabel();

		if (param.oneVsAllMode != MultiClassModel.OneVsAllMode.None)
			{
			Map<L, Future<BinaryModel<L, P>>> futureMap = new HashMap<L, Future<BinaryModel<L, P>>>();

			// create and train one vs all classifiers.

			// first queue up all the training tasks and submit them to the thread pool

			logger.info("Training one-vs-all classifiers for " + numLabels + " labels");
			int c = 0;
			LabelInverter<L> labelInverter = problem.getLabelInverter();
			for (final L label : problem.getLabels())
				{
				final L notLabel = labelInverter.invert(label);

				final Set<P> labelExamples = examplesByLabel.get(label);

				Collection<Map.Entry<P, L>> entries = problem.getExamples().entrySet();
				if (param.falseClassSVlimit != Integer.MAX_VALUE)
					{
					// guarantee entries in random order if limiting the number of false examples
					List<Map.Entry<P, L>> entryList = new ArrayList<Map.Entry<P, L>>(entries);
					Collections.shuffle(entryList);
					entries = entryList.subList(0, param.falseClassSVlimit + labelExamples.size());
					}

				final Set<P> notlabelExamples =
						new SubtractionMap<P, L>(entries, labelExamples, param.falseClassSVlimit).keySet();

				final BinaryClassificationProblem<L, P> subProblem =
						new BooleanClassificationProblemImpl<L, P>(problem.getLabelClass(), label, labelExamples,
						                                           notLabel, notlabelExamples, problem.getExampleIds());
				// Unbalanced data: see prepareWeights
				// since these will be extremely unbalanced, this should nearly guarantee that no positive examples are misclassified.

				Future<BinaryModel<L, P>> fut = execService.submit(binarySvm.trainCallable(subProblem, param));

				futureMap.put(label, fut);
				}

			execService.shutdown();
			// then collect the results every 30 seconds, reporting progress

			while (!futureMap.isEmpty())
				{
				try
					{
					execService.awaitTermination(30, TimeUnit.SECONDS);
					}
				catch (InterruptedException e)
					{
					// no problem, just cycle
					}

				for (Iterator<Map.Entry<L, Future<BinaryModel<L, P>>>> iter = futureMap.entrySet().iterator();
				     iter.hasNext();)
					{
					Map.Entry<L, Future<BinaryModel<L, P>>> entry = iter.next();
					final Future<BinaryModel<L, P>> fut = entry.getValue();
					if (fut.isDone())
						{
						try
							{
							c++;
							iter.remove();
							model.putOneVsAllModel(entry.getKey(), fut.get());
							}
						catch (InterruptedException e)
							{
							logger.error("Error", e);
							}
						catch (ExecutionException e)
							{
							logger.error("Error", e);
							//logger.error("Error",e.getCause());
							}
						}
					}

				logger.info("Trained " + c + " one-vs-all models");
				}
			}


		// ** apparently it takes a while for the pool to terminate... but we don't care since we already got our results
		//assert execService.isTerminated();


		execService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


		if (param.allVsAllMode != MultiClassModel.AllVsAllMode.None)
			{
			final int numClassifiers = (numLabels * (numLabels - 1)) / 2;

			Map<LabelPair<L>, Future<BinaryModel<L, P>>> futureMap2d =
					new HashMap<LabelPair<L>, Future<BinaryModel<L, P>>>(numClassifiers);
			// create and train all vs all classifiers

			// first queue up all the training tasks and submit them to the thread pool

			logger.info("Training " + numClassifiers + " one-vs-one classifiers for " + numLabels + " labels");
			int c = 0;
			for (final L label1 : problem.getLabels())
				{
				for (final L label2 : problem.getLabels())
					{
					if (label1.compareTo(label2) < 0)// avoid redundant pairs
						{
						final Set<P> label1Examples = examplesByLabel.get(label1);
						final Set<P> label2Examples = examplesByLabel.get(label2);

						final BinaryClassificationProblem<L, P> subProblem =
								new BooleanClassificationProblemImpl<L, P>(problem.getLabelClass(), label1,
								                                           label1Examples, label2, label2Examples,
								                                           problem.getExampleIds());


						Future<BinaryModel<L, P>> fut = execService.submit(binarySvm.trainCallable(subProblem, param));


						futureMap2d.put(new LabelPair(label1, label2), fut);

						c++;
						if (c % 1000 == 0)
							{
							logger.debug("Enqueued " + c + " one-vs-one training tasks");
							}
						}
					}
				}
			logger.info("Enqueued " + c + " one-vs-one training tasks");
			c = 0;
			// then collect the results every 30 seconds, reporting progress
			execService.shutdown();
			while (!futureMap2d.isEmpty())
				{
				try
					{
					execService.awaitTermination(30, TimeUnit.SECONDS);
					}
				catch (InterruptedException e)
					{
					// no problem, just cycle
					}

				for (Iterator<Map.Entry<LabelPair<L>, Future<BinaryModel<L, P>>>> iter =
						futureMap2d.entrySet().iterator(); iter.hasNext();)
					{
					Map.Entry<LabelPair<L>, Future<BinaryModel<L, P>>> entry = iter.next();
					final Future<BinaryModel<L, P>> fut = entry.getValue();

					if (fut.isDone())
						{
						try
							{
							c++;
							iter.remove();
							LabelPair<L> labels = entry.getKey();
							model.putOneVsOneModel(labels.getOne(), labels.getTwo(), fut.get());
							}
						catch (InterruptedException e)
							{
							logger.error("Error", e);
							}
						catch (ExecutionException e)
							{
							logger.error("Error", e);
							}
						}
					}

				logger.info("Trained " + c + " one-vs-one models");
				}
			}

		// ** apparently it takes a while for the pool to terminate... but we don't care since we already got our results
		//assert execService.isTerminated();

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

// -------------------------- INNER CLASSES --------------------------

	private class LabelPair<T>
		{
// ------------------------------ FIELDS ------------------------------

		T one;
		T two;


// --------------------------- CONSTRUCTORS ---------------------------

		private LabelPair(T one, T two)
			{
			this.one = one;
			this.two = two;
			}

// --------------------- GETTER / SETTER METHODS ---------------------

		public T getOne()
			{
			return one;
			}

		public T getTwo()
			{
			return two;
			}
		}
	}
