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
	private static final Logger logger = Logger.getLogger(MultiClassificationSVM.class);

	BinaryClassificationSVM<L, P> binarySvm;	//private Class labelClass;

	boolean redistributeUnbalancedC;

	public MultiClassificationSVM(BinaryClassificationSVM<L, P> binarySvm, boolean redistributeUnbalancedC)
		{
		super(binarySvm.kernel, binarySvm.param);
		this.binarySvm = binarySvm;
		this.redistributeUnbalancedC = redistributeUnbalancedC;
		}

	public String getSvmType()
		{
		return "multiclass " + binarySvm.getSvmType();
		}

	/*	@Override
   public Class getLabelClass()
	   {
	   return labelClass;
	   }*/


/*	public void setupQMatrix(SvmProblem<L, P> problem)
		{
		binarySvm.setupQMatrix(problem);
		qMatrix = binarySvm.qMatrix; // just for the sake of reporting later
		}
*/


	public MultiClassModel<L, P> train(MultiClassProblem<L, P> problem)
		{
		int numLabels = problem.getLabels().size();

		MultiClassModel<L, P> model = new MultiClassModel<L, P>(kernel, param, numLabels);


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
				//Map<P, L> subExamples = new HashMap<P, L>(problem.getNumExamples());
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


/*
				Collection<Map.Entry<P, L>> entries = problem.getExamples().entrySet();
				if (param.falseClassSVlimit != 0)
					{
					// guarantee entries in random order if limiting the number of false examples
					List<Map.Entry<P, L>> entryList = new ArrayList<Map.Entry<P, L>>(entries);
					Collections.shuffle(entryList);
					entries = entryList;
					}

					int falseExamples = 0;
								for (Map.Entry<P, L> entry : entries)
									{
									if (entry.getValue().equals(label))
										{
										subExamples.put(entry.getKey(), label);
										}
									else if (param.falseClassSVlimit == 0 || falseExamples < param.falseClassSVlimit)
										{
										subExamples.put(entry.getKey(), notLabel);
										falseExamples++;
										}
									}

								final BinaryClassificationProblem<L, P> subProblem =
										new BinaryClassificationProblemImpl<L, P>(problem.getLabelClass(), subExamples,
																				  problem.getExampleIds());*/

				final BinaryClassificationProblem<L, P> subProblem =
						new BooleanClassificationProblemImpl<L, P>(problem.getLabelClass(), label, labelExamples,
						                                           notLabel, notlabelExamples, problem.getExampleIds());
				//** Unbalanced data: see prepareWeights
				// since these will be extremely unbalanced, this should nearly guarantee that no positive examples are misclassified.

				//float costOfNegativeMisclassification = weights.get(label);
				//float costOfPositiveMisclassification = weights.get(notLabel);

				Future<BinaryModel<L, P>> fut = execService
						.submit(binarySvm.trainCallable(subProblem, weights.get(label), weights.get(notLabel)));

				//final BinaryModel<L, P> binaryModel =
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
		/*	try
			  {
			  execService.awaitTermination(10, TimeUnit.SECONDS);
			  }
		  catch (InterruptedException e)
			  {
			  // no problem, just cycle
			  }
		  assert execService.isTerminated();
  */
		// ** apparently it takes a while for the pool to terminate...


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

						// PERF constructing each example set explicitly sucks, especially since they'll later be rebuilt with Boolean values anyway;
						// can we make a UnionMap or something?
						/*				Map<P, L> subExamples = new HashMap<P, L>(label1Examples.size() + label2Examples.size());

							 for (P label1Example : label1Examples)
								 {
								 subExamples.put(label1Example, label1);
								 }
							 for (P label2Example : label2Examples)
								 {
								 subExamples.put(label2Example, label2);
								 }
	  */
						//					Map<P,L> subExamples = new BinaryMap<P,L>(label1Examples, label1, label2Examples, label2);

						//BinaryClassificationProblem<P> subProblem = new BinaryClassificationProblem<P>(label1Examples, label2Examples);

						final BinaryClassificationProblem<L, P> subProblem =
								new BooleanClassificationProblemImpl<L, P>(problem.getLabelClass(), label1,
								                                           label1Examples, label2, label2Examples,
								                                           problem.getExampleIds());

						/*final BinaryClassificationProblem<L, P> subProblem =
								new BinaryClassificationProblemImpl<L, P>(problem.getLabelClass(), subExamples,
								                                          problem.getExampleIds());*/

						Future<BinaryModel<L, P>> fut = execService
								.submit(binarySvm.trainCallable(subProblem, weights.get(label1), weights.get(label2)));

						//final BinaryModel<L, P> binaryModel =
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

		if (!redistributeUnbalancedC)
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
			//** Unbalanced data: redistribute the misclassification cost C according to
			// the numbers of examples in each class, so that each class has the same total
			// misclassification weight assigned to it and the average is param.C

			int numExamples = problem.getNumExamples();
			;


			int numClasses = examplesByLabel.size();

			// first figu
			// re out the average total C for each class if the samples were uniformly distributed
			float totalCPerClass = param.C * numExamples / numClasses;
			//float totalCPerRemainder = totalCPerClass * (numClasses - 1);


			// then assign the proper C per _sample_ within each class by distributing the per-class C
			for (Map.Entry<L, Set<P>> entry : examplesByLabel.entrySet())
				{
				L label = entry.getKey();
				Set<P> examples = entry.getValue();
				float weight = totalCPerClass / examples.size();

				weights.put(label, weight);


				//** For one-vs-all, we want the inverse class to have the same total weight as the positive class, i.e. totalCPerClass.
				//** Note scaling problem: we can't scale up the positive class, so we have to scale down the negative class
				//** i.e. we pretend that all of the negative examples are in one class, and so have totalCPerClass.

				L inverse = labelInverter.invert(label);
				int numFalseExamples = param.falseClassSVlimit;
				if (numFalseExamples == Integer.MAX_VALUE)
					{
					numFalseExamples = numExamples - examples.size();
					}
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


	/*	public MultiClassModel<L, P> train(MultiClassProblem<L, P> problem)
		 {
		 MultiClassModel<L, P> model = new MultiClassModel<L, P>(kernel, param);
		 model.setSvmType(getSvmType());

		 // classification
		 int l = problem.examples.size();
		 int[] perm = new int[l];

		 // group training data of the same class
		 MultiClassProblem<L, P>.GroupedClasses groupedExamples = problem.groupClasses(perm);
		 int numberOfClasses = groupedExamples.numberOfClasses;
		 List<L> groupLabels = groupedExamples.label;
		 int[] groupStarts = groupedExamples.start;
		 List<Integer> groupCounts = groupedExamples.count;

		 // sort the examples so they occur in class blocks
		 P[] x = new P[l];
		 int i;
		 for (i = 0; i < l; i++)
			 {
			 x[i] = problem.examples[perm[i]];
			 }

		 // calculate weighted C

 //		List<Float> weightedC = new ArrayList<Float>(numberOfClasses);

 //		for (i = 0; i < numberOfClasses; i++)
 //			{
 //			weightedC.add(param.C);
 //			}

		 // if any weights are provided, apply them; else just use param.C
		 Map<L, Float> weights = param.getWeights();
		 for (Map.Entry<L, Float> weightEntry : weights.entrySet())
			 {
			 L key = weightEntry.getKey();
			 Float value = weightEntry.getValue();
			 int j = groupLabels.indexOf(key);
			 if (j == -1)
				 {
				 System.err.print("warning: class label " + key + " specified in weight is not found\n");
				 }
			 else
				 {
				 Float w = weightEntry.getValue();
				 weightEntry.setValue((w == null ? 1f : w) * param.C);
				 }
			 }

		 model.oneVsOneModels = new BinaryModel[numberOfClasses * (numberOfClasses - 1) / 2];

		 float[] probA = null, probB = null;
		 if (param.probability)
			 {
			 probA = new float[numberOfClasses * (numberOfClasses - 1) / 2];
			 probB = new float[numberOfClasses * (numberOfClasses - 1) / 2];
			 }

		 int oneVsOneIndex = 0;
		 for (i = 0; i < numberOfClasses; i++)
			 {
			 for (int j = i + 1; j < numberOfClasses; j++)
				 {
				 int iStart = groupStarts[i], jStart = groupStarts[j];
				 int iCount = groupCounts.get(i), jCount = groupCounts.get(j);
				 int subprobLength = iCount + jCount;

				 BinaryClassificationProblem<P> subProblem = new BinaryClassificationProblem<P>(subprobLength);

				 for (int k = 0; k < iCount; k++)
					 {
					 subProblem.examples[k] = x[iStart + k];
					 subProblem.putTargetValue(k, true);
					 }
				 for (int k = 0; k < jCount; k++)
					 {
					 subProblem.examples[iCount + k] = x[jStart + k];
					 subProblem.putTargetValue(iCount + k, false);
					 }

				 if (param.probability)
					 {
					 float[] probAB = binarySvm.svcProbability(subProblem, weightedC[i], weightedC[j]);
					 probA[oneVsOneIndex] = probAB[0];
					 probB[oneVsOneIndex] = probAB[1];
					 }

				 model.oneVsOneModels[oneVsOneIndex] = binarySvm.trainOne(subProblem, weightedC[i], weightedC[j]);

				 ++oneVsOneIndex;
				 }
			 }

		 // build output

		 model.numberOfClasses = numberOfClasses;


		 model.labels = new Object[numberOfClasses];
		 for (i = 0; i < numberOfClasses; i++)
			 {
			 model.labels[i] = groupLabels.get(i);
			 }

		 if (param.probability)
			 {
			 model.probA = new float[numberOfClasses * (numberOfClasses - 1) / 2];
			 model.probB = new float[numberOfClasses * (numberOfClasses - 1) / 2];
			 for (i = 0; i < numberOfClasses * (numberOfClasses - 1) / 2; i++)
				 {
				 model.probA[i] = probA[i];
				 model.probB[i] = probB[i];
				 }
			 }
		 else
			 {
			 model.probA = null;
			 model.probB = null;
			 }

		 return model;
		 }
 *//*
	protected L[] foldPredict(MultiClassProblem<L,P> subprob, Iterator<P> foldIterator, int length)
		{
		MultiClassModel<L,P> model = train(subprob);

		L[] result = (L[]) new Comparable[length];

		int i = 0;
		while (foldIterator.hasNext())
			{
			result[i] = model.predictLabel(foldIterator.next());
			i++;
			}
		return result;
		}*/

	private class LabelPair<T>
		{
		T one;
		T two;

		private LabelPair(T one, T two)
			{
			this.one = one;
			this.two = two;
			}

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
