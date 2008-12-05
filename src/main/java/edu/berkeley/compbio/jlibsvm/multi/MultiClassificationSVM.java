package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SvmProblem;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationProblem;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationProblemImpl;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationSVM;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.labelinverter.LabelInverter;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassificationSVM<L extends Comparable<L>, P> extends SVM<L, P, MultiClassProblem<L, P>>
	{
	private static final Logger logger = Logger.getLogger(MultiClassificationSVM.class);

	BinaryClassificationSVM<L, P> binarySvm;	//private Class labelClass;

	public MultiClassificationSVM(BinaryClassificationSVM<L, P> binarySvm, Class labelClass)
		{
		super(binarySvm.kernel, binarySvm.param);
		this.binarySvm = binarySvm;		//	this.labelClass = labelClass;
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


	public void setupQMatrix(SvmProblem<L, P> problem)
		{
		binarySvm.setupQMatrix(problem);
		qMatrix = binarySvm.qMatrix; // just for the sake of reporting later
		}


	public MultiClassModel<L, P> train(MultiClassProblem<L, P> problem)
		{
		int numLabels = problem.getLabels().size();

		MultiClassModel<L, P> model = new MultiClassModel<L, P>(kernel, param, numLabels);


		Map<L, Float> weights = prepareWeights(problem);

		/*
		if (param.multiclassMode != MultiClassModel.MulticlassMode.OneVsAllOnly)
			{
			// create and train one-class classifiers

			logger.info("Training one-class classifiers for " + numLabels + " labels");

			for (Map.Entry<L,Set<P>> entry : problem.getExamplesByLabel().entrySet())
				{
				Map<P, Float> subExamples = new HashMap<P, Float>(problem.getExamples().size());

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
		if (param.multiclassMode != MultiClassModel.MulticlassMode.OneClassOnly)
			{
			// create and train one vs all classifiers

			logger.info("Training one-vs-all classifiers for " + numLabels + " labels");

			LabelInverter<L> labelInverter = problem.getLabelInverter();
			for (L label : problem.getLabels())
				{
				Map<P, L> subExamples = new HashMap<P, L>(problem.getExamples().size());
				L notLabel = labelInverter.invert(label);

				for (Map.Entry<P, L> entry : problem.getExamples().entrySet())
					{
					subExamples.put(entry.getKey(), entry.getValue() == label ? label : notLabel);
					}

				BinaryClassificationProblem<L, P> subProblem =
						new BinaryClassificationProblemImpl<L, P>(problem.getLabelClass(), subExamples);

				//** Unbalanced data: see prepareWeights
				// since these will be extremely unbalanced, this should nearly guarantee that no positive examples are misclassified.

				//float costOfNegativeMisclassification = weights.get(label);
				//float costOfPositiveMisclassification = weights.get(notLabel);

				model.putOneVsAllModel(label, binarySvm.train(subProblem, weights.get(label), weights.get(notLabel)));
				}
			}

		if (param.multiclassMode != MultiClassModel.MulticlassMode.OneVsAllOnly
				&& param.multiclassMode != MultiClassModel.MulticlassMode.OneClassOnly)
			{

			// create and train all vs all classifiers

			logger.info(
					"Training " + numLabels * (numLabels - 1) + " one-vs-one classifiers for " + numLabels + " labels");

			Map<L, Set<P>> examplesByLabel = problem.getExamplesByLabel();
			for (L label1 : problem.getLabels())
				{
				for (L label2 : problem.getLabels())
					{
					if (label1.compareTo(label2) < 0)// avoid redundant pairs
						{
						final Set<P> label1Examples = examplesByLabel.get(label1);
						final Set<P> label2Examples = examplesByLabel.get(label2);

						// PERF constructing each example set explicitly sucks, especially since they'll later be rebuilt with Boolean values anyway;  can we make a UnionMap or something?
						Map<P, L> subExamples = new HashMap<P, L>(label1Examples.size() + label2Examples.size());

						for (P label1Example : label1Examples)
							{
							subExamples.put(label1Example, label1);
							}
						for (P label2Example : label2Examples)
							{
							subExamples.put(label2Example, label2);
							}

						// Map<P,L> subExamples = new BinaryMap<P,L>(label1Examples, label1, label2Examples, label2);

						//BinaryClassificationProblem<P> subProblem = new BinaryClassificationProblem<P>(label1Examples, label2Examples);

						BinaryClassificationProblem<L, P> subProblem =
								new BinaryClassificationProblemImpl<L, P>(problem.getLabelClass(), subExamples);

						//** Unbalanced data: see prepareWeights
						final BinaryModel<L, P> binaryModel =
								binarySvm.train(subProblem, weights.get(label1), weights.get(label2));

						model.putOneVsOneModel(label1, label2, binaryModel);
						}
					}
				}
			}

		return model;
		}

	private Map<L, Float> prepareWeights(MultiClassProblem<L, P> problem)
		{
		LabelInverter<L> labelInverter = problem.getLabelInverter();

		Map<L, Float> weights = new HashMap<L, Float>();

		//** Unbalanced data: redistribute the misclassification cost C according to
		// the numbers of examples in each class, so that each class has the same total
		// misclassification weight assigned to it and the average is param.C

		int numExamples = problem.getExamples().size();

		final Map<L, Set<P>> examplesByLabel = problem.getExamplesByLabel();

		int numClasses = examplesByLabel.size();

		float totalCPerClass = param.C * numExamples / numClasses;
		//float totalCPerRemainder = totalCPerClass * (numClasses - 1);

		for (Map.Entry<L, Set<P>> entry : examplesByLabel.entrySet())
			{
			L label = entry.getKey();
			Set<P> examples = entry.getValue();
			float weight = totalCPerClass / examples.size();

			weights.put(label, weight);


			//** For one-vs-all, we want the inverse class to have the same total weight as the positive class.
			//** Note scaling problem: we can't scale up the positive class, so we have to scale down the negative class
			//** i.e. we pretend that all of the negative examples are in one class, and so have totalCPerClass.

			L inverse = labelInverter.invert(label);
			float inverseWeight = totalCPerClass / (numExamples - examples.size());
			weights.put(inverse, inverseWeight);
			}

		if (!param.getWeights().isEmpty())
			{
			logger.warn("Ignoring provided class weights; we compute them from C and the number of examples");
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
	}
