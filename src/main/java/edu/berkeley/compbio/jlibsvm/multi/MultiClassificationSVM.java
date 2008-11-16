package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationProblem;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationSVM;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassificationSVM<T extends Comparable> extends SVM<T, MultiClassProblem<T>>
	{
	BinaryClassificationSVM binarySvm;

	public MultiClassificationSVM(BinaryClassificationSVM binarySvm)
		{

		super(binarySvm.kernel, (SvmParameter<T>)(binarySvm.param));
		this.binarySvm = binarySvm;
		}

	public String getSvmType()
		{
		return "multiclass " + binarySvm.getSvmType();
		}

	@Override
	public Type getGenericType()
		{
		return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		}

	public MultiClassModel<T> train(MultiClassProblem<T> problem)
		{
		MultiClassModel<T> model = new MultiClassModel<T>(kernel, param);
		model.setSvmType(getSvmType());

		// classification
		int l = problem.examples.length;
		//int[] tmp_nr_class = new int[1];
		//int[][] tmp_label = new int[1][];
		//int[][] tmp_start = new int[1][];
		//int[][] tmp_count = new int[1][];
		int[] perm = new int[l];

		// group training data of the same class
		MultiClassProblem<T>.GroupedClasses groupedExamples = problem.groupClasses(perm);
		//	int nr_class = tmp_nr_class[0];
		//	int[] label = tmp_label[0];
		//	int[] start = tmp_start[0];
		//	int[] count = tmp_count[0];
		int numberOfClasses = groupedExamples.numberOfClasses;
		List<T> groupLabels = groupedExamples.label;
		int[] groupStarts = groupedExamples.start;
		List<Integer> groupCounts = groupedExamples.count;

		// sort the examples so they occur in class blocks
		SvmPoint[] x = new SvmPoint[l];
		int i;
		for (i = 0; i < l; i++)
			{
			x[i] = problem.examples[perm[i]];
			}

		// calculate weighted C

		float[] weightedC = new float[numberOfClasses];

//		T[] weightLabel = new ArrayList<T>(param.weights.keySet()).toArray(((T[])new Object[]{}));
		for (i = 0; i < numberOfClasses; i++)
			{
			weightedC[i] = param.C;
			}

		// if any weights are provided, apply them
		Map<T, Float> weights = param.getWeights();
		for (Map.Entry<T, Float> weightEntry : weights.entrySet())
			{
			T key = weightEntry.getKey();
			Float value = weightEntry.getValue();
			int j = groupLabels.indexOf(key);
			if (j == -1)
				{
				System.err.print("warning: class label " + key + " specified in weight is not found\n");
				}
			else
				{
				weightedC[j] *= value;
				}
			}

/*
		for (i = 0; i < weightLabel.length; i++)
			{
			int j;
			for (j = 0; j < numberOfClasses; j++)
				{
				if (weightLabel[i] == groupLabels.get(j))
					{
					break;
					}
				}
			if (j == numberOfClasses)
				{
				System.err.print("warning: class label " + weightLabel[i] + " specified in weight is not found\n");
				}
			else
				{
				weightedC[j] *= param.getWeight(weightLabel[i]);
				}
			}*/

		// train k*(k-1)/2 models

//		boolean[] nonzero = new boolean[l];
/*		for (i = 0; i < l; i++)
			{
			nonzero[i] = false;
			}
*/

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

				BinaryClassificationProblem subProblem = new BinaryClassificationProblem(subprobLength)
						; //problem.newSubProblem(subprobLength);
				//subProblem.examples = new SvmPoint[subprobLength];
				//subProblem.targetValues = new float[subprobLength];

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
					//				binarySvcProbability(
					probA[oneVsOneIndex] = probAB[0];
					probB[oneVsOneIndex] = probAB[1];
					}

				model.oneVsOneModels[oneVsOneIndex] = binarySvm.trainOne(subProblem, weightedC[i], weightedC[j]);

				// if an example has a nonzero alpha in _any_ of the (nr_class - 1) oneVsOne machines that it participates in, mark it "nonzero".
// no need
/*				for (int k = 0; k < iCount; k++)
					{
					if (!nonzero[iStart + k] && Math.abs(model.oneVsOneModels[oneVsOneIndex].alpha[k]) > 0)
						{
						nonzero[iStart + k] = true;
						}
					}
				for (int k = 0; k < jCount; k++)
					{
					if (!nonzero[jStart + k] && Math.abs(model.oneVsOneModels[oneVsOneIndex].alpha[iCount + k]) > 0)
						{
						nonzero[jStart + k] = true;
						}
					}
					*/
				++oneVsOneIndex;
				}
			}

		// build output

		model.numberOfClasses = numberOfClasses;


		model.label = new Object[numberOfClasses];
		for (i = 0; i < numberOfClasses; i++)
			{
			model.label[i] = groupLabels.get(i);
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


		// figure out how many support vectors there are for each class,
		// i.e. counting all those examples that are used as support vectors in _any_ of the oneVsOne machines
/*
		int numNonZero = 0;
		//int[] nz_count = new int[nr_class];
		model.numSupportVectors = new int[numberOfClasses];
		for (i = 0; i < numberOfClasses; i++)
			{
			int numSupportVectors = 0;
			for (int j = 0; j < groupCounts.get(i); j++)
				{
				if (nonzero[groupStarts[i] + j])
					{
					++numSupportVectors;
					++numNonZero;
					}
				}
			model.numSupportVectors[i] = numSupportVectors;
			// nz_count[i] = numSupportVectors;
			}

		System.out.print("Total nSV = " + numNonZero + "\n");
*/

		return model;
		}


	//public abstract float[] svm_predict_values(MultiClassModel model, svm_node[] x);


	//public abstract float svm_predict(MultiClassModel model, svm_node[] x);


	protected T[] foldPredict(MultiClassProblem<T> subprob, Iterator<SvmPoint> foldIterator, int length)
		{
		MultiClassModel<T> model = train(subprob);

		T[] result = (T[]) new Object[length];

		int i = 0;
		while (foldIterator.hasNext())
			{
			result[i] = model.predictLabel(foldIterator.next());
			i++;
			}
		return result;
		}
	}
