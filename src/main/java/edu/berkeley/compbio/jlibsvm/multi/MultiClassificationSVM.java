package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationProblem;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationSVM;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;

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
	private Class labelClass;

	public MultiClassificationSVM(BinaryClassificationSVM binarySvm, Class labelClass)
		{

		super(binarySvm.kernel, (SvmParameter<T>) (binarySvm.param));
		this.binarySvm = binarySvm;
		this.labelClass = labelClass;
		}

	public String getSvmType()
		{
		return "multiclass " + binarySvm.getSvmType();
		}

	@Override
	public Class getLabelClass()
		{
		return labelClass;
		}

	public MultiClassModel<T> train(MultiClassProblem<T> problem)
		{
		MultiClassModel<T> model = new MultiClassModel<T>(kernel, param);
		model.setSvmType(getSvmType());

		// classification
		int l = problem.examples.length;
		int[] perm = new int[l];

		// group training data of the same class
		MultiClassProblem<T>.GroupedClasses groupedExamples = problem.groupClasses(perm);
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

				BinaryClassificationProblem subProblem = new BinaryClassificationProblem(subprobLength);

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

		return model;
		}


	protected T[] foldPredict(MultiClassProblem<T> subprob, Iterator<SvmPoint> foldIterator, int length)
		{
		MultiClassModel<T> model = train(subprob);

		T[] result = (T[]) new Comparable[length];

		int i = 0;
		while (foldIterator.hasNext())
			{
			result[i] = model.predictLabel(foldIterator.next());
			i++;
			}
		return result;
		}
	}
