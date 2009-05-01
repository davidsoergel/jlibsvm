package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationProblem;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationSVM;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.binary.BooleanClassificationProblemImpl;
import edu.berkeley.compbio.jlibsvm.labelinverter.LabelInverter;
import edu.berkeley.compbio.jlibsvm.util.SubtractionMap;
import org.apache.log4j.Logger;

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

	private BinaryClassificationSVM<L, P> binarySvm;	//private Class labelClass;


// --------------------------- CONSTRUCTORS ---------------------------

	public MultiClassificationSVM(BinaryClassificationSVM<L, P> binarySvm)
		{
		super(binarySvm.kernel, binarySvm.scalingModelLearner, binarySvm.param);
		this.binarySvm = binarySvm;
		}

// -------------------------- OTHER METHODS --------------------------

	@Override
	public String getSvmType()
		{
		return "multiclass " + binarySvm.getSvmType();
		}

	public MultiClassModel<L, P> train(MultiClassProblem<L, P> problem)
		{
		if (scalingModelLearner != null && !param.scaleBinaryMachinesIndependently)
			{
			// scale the entire problem before doing anything else
			problem = problem.getScaledCopy(scalingModelLearner);
			}


		int numLabels = problem.getLabels().size();

		MultiClassModel<L, P> model = new MultiClassModel<L, P>(kernel, param, numLabels);

		model.setScalingModel(problem.getScalingModel());


		final Map<L, Float> weights = prepareWeights(problem);

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
				//** Unbalanced data: see prepareWeights
				// since these will be extremely unbalanced, this should nearly guarantee that no positive examples are misclassified.

				//float costOfNegativeMisclassification = weights.get(label);
				//float costOfPositiveMisclassification = weights.get(notLabel);

				Future<BinaryModel<L, P>> fut = execService
						.submit(binarySvm.trainCallable(subProblem, weights.get(label), weights.get(notLabel)));

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


						Future<BinaryModel<L, P>> fut = execService
								.submit(binarySvm.trainCallable(subProblem, weights.get(label1), weights.get(label2)));


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

	private Map<L, Float> prepareWeights(MultiClassProblem<L, P> problem)
		{
		LabelInverter<L> labelInverter = problem.getLabelInverter();

		Map<L, Float> weights = new HashMap<L, Float>();


		final Map<L, Set<P>> examplesByLabel = problem.getExamplesByLabel();

		if (!param.redistributeUnbalancedC)
			{
			for (Map.Entry<L, Set<P>> entry : examplesByLabel.entrySet())
				{
				L label = entry.getKey();

				weights.put(label, param.C);

				L inverse = labelInverter.invert(label);

				weights.put(inverse, param.C);
				}
			}
		else
			{
			int numExamples = problem.getNumExamples();

			int numClasses = examplesByLabel.size();

			// first figure out the average total C for each class if the samples were uniformly distributed
			float totalCPerClass = param.C * numExamples / numClasses;
			//float totalCPerRemainder = totalCPerClass * (numClasses - 1);


			// then assign the proper C per _sample_ within each class by distributing the per-class C
			for (Map.Entry<L, Set<P>> entry : examplesByLabel.entrySet())
				{
				// the weight per sample is just the total class weight divided by the number of samples

				L label = entry.getKey();
				Set<P> examples = entry.getValue();
				float weight = totalCPerClass / examples.size();

				weights.put(label, weight);

				// also prepare weights for the one-vs-all case.

				// For one-vs-all, we want the inverse class to have the same total weight as the positive class, i.e. totalCPerClass.
				// Note scaling problem: we can't scale up the positive class, so we have to scale down the negative class
				//*i.e. we pretend that all of the negative examples are in one class, and so have totalCPerClass.

				L inverse = labelInverter.invert(label);
				int numFalseExamples = numExamples - examples.size();
				numFalseExamples = Math.min(numFalseExamples, param.falseClassSVlimit);
				float inverseWeight = totalCPerClass / numFalseExamples;
				weights.put(inverse, inverseWeight);
				}

			if (!param.getWeights().isEmpty())
				{
				logger.warn("Ignoring provided class weights; we compute them from C and the number of examples");
				}
			}
		/*
	   // use param.C as the default weight...
	   for (L label : problem.getLabels())
		   {
		   weights.put(label, param.C);
		   }


	   // ... but if any weights are provided, apply them
	   for (Map.Entry<L, Float> weightEntry : param.getWeights().entrySet())
		   {
		   L key = weightEntry.getKey();
		   if (problem.getLabels().contains(key))
			   {
			   Float w = weightEntry.getValue();
			   weights.put(key, w * param.C);
			   }
		   else
			   {
			   logger.warn("class label " + key + " specified in weight is not found");
			   }
		   }*/
		return weights;
		}

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
