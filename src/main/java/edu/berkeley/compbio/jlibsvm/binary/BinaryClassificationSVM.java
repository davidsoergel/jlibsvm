package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblem;
import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SigmoidProbabilityModel;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class BinaryClassificationSVM<L extends Comparable, P>
		extends SVM<L, P, BinaryClassificationProblem<L, P>>
	{
	protected BinaryClassificationSVM(KernelFunction<P> kernel, SvmParameter<L> param)
		{
		super(kernel, param);
		}

	/*
	 public Boolean[] makeTArray(int length)
		 {
		 return new Boolean[length];
		 }
 */ L trueLabel;
	L falseLabel;

	public BinaryModel<L, P> train(BinaryClassificationProblem<L, P> problem)
		{
		if (problem.getLabels().size() != 2)
			{
			throw new SvmException("Can't do binary classification; " + problem.getLabels().size() + " classes found");
			}

		falseLabel = problem.getLabels().get(0);
		trueLabel = problem.getLabels().get(1);

		// calculate weighted C

		float weightedCp = param.C;
		float weightedCn = param.C;

		Float weightP = param.getWeight(trueLabel);
		if (weightP != null)
			{
			weightedCp *= weightP;
			}

		Float weightN = param.getWeight(falseLabel);
		if (weightN != null)
			{
			weightedCn *= weightN;
			}
		setupQMatrix(problem);
		BinaryModel<L, P> result = train(problem, weightedCp, weightedCn);
		result.printSolutionInfo(problem);
		System.err.println(qMatrix.perfString());
		return result;
		}

	/**
	 * Train the classifier, and also prepare the probability sigmoid thing if requested.  Note that svcProbability will
	 * call this method in the course of cross-validation, but will first ensure that param.probability == false;
	 *
	 * @param problem
	 * @param Cp
	 * @param Cn
	 * @return
	 */
	public BinaryModel<L, P> train(BinaryClassificationProblem<L, P> problem, float Cp, float Cn)
		{
		BinaryModel<L, P> result = trainOne(problem, Cp, Cn);
		if (param.probability)
			{
			result.sigmoid = svcProbability(problem, Cp, Cn);
			}
		return result;
		}

	/**
	 * Normal training on the entire problem, with no cross-validation-based probability measure.
	 *
	 * @param problem
	 * @param Cp
	 * @param Cn
	 * @return
	 */
	protected abstract BinaryModel<L, P> trainOne(BinaryClassificationProblem<L, P> problem, float Cp, float Cn);

//	public abstract void setupQMatrix(SvmProblem<L, P> problem);
	// original separateFolds attempted to keep the class ratio uniform among the folds?

	/*
	public FoldSpec separateFolds(BinaryClassificationProblem<P> problem, int numberOfFolds)
		{
		FoldSpec fs = new FoldSpec(problem.getNumExamples(), numberOfFolds);


		BinaryClassificationProblem<P>.GroupedClasses groupedExamples = problem.groupClasses(fs.perm);
		int numberOfClasses = groupedExamples.numberOfClasses;
		int[] start = groupedExamples.start;
		List<Integer> count = groupedExamples.count;

		// random shuffle and then data grouped by fold using the array perm
		int[] foldCount = new int[numberOfFolds];

		int[] index = new int[fs.perm.length];
		for (int i = 0; i < fs.perm.length; i++)
			{
			index[i] = fs.perm[i];
			}
		for (int c = 0; c < numberOfClasses; c++)
			{
			for (int i = 0; i < count.get(c); i++)
				{
				int j = i + (int) (Math.random() * (count.get(c) - i));

				int swap = index[start[c] + j];
				index[start[c] + j] = index[start[c] + i];
				index[start[c] + i] = swap;
				}
			}
		for (int i = 0; i < numberOfFolds; i++)
			{
			foldCount[i] = 0;
			for (int c = 0; c < numberOfClasses; c++)
				{
				foldCount[i] += (i + 1) * count.get(c) / numberOfFolds - i * count.get(c) / numberOfFolds;
				}
			}
		fs.foldStart[0] = 0;
		for (int i = 1; i <= numberOfFolds; i++)
			{
			fs.foldStart[i] = fs.foldStart[i - 1] + foldCount[i - 1];
			}
		for (int c = 0; c < numberOfClasses; c++)
			{
			for (int i = 0; i < numberOfFolds; i++)
				{
				int begin = start[c] + i * count.get(c) / numberOfFolds;
				int end = start[c] + (i + 1) * count.get(c) / numberOfFolds;
				for (int j = begin; j < end; j++)
					{
					fs.perm[fs.foldStart[i]] = index[j];
					fs.foldStart[i]++;
					}
				}
			}
		fs.foldStart[0] = 0;
		for (int i = 1; i <= numberOfFolds; i++)
			{
			fs.foldStart[i] = fs.foldStart[i - 1] + foldCount[i - 1];
			}
		return fs;
		}
		*/

/*	@Override
	public Class getLabelClass()
		{
		return Boolean.class; // ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		}*/
/*
	protected Boolean[] foldPredict(BinaryClassificationProblem<P> subprob, Iterator<P> foldIterator,
	                                int length)
		{
		BinaryModel model = train(subprob);
		Boolean[] result = new Boolean[length];

		int i = 0;
		while (foldIterator.hasNext())
			{
			result[i] = model.predictLabel(foldIterator.next());
			i++;
			}
		return result;
		}
*/

	// Cross-validation decision values for probability estimates
	// ** unify with SVM.crossValidation?

	private SigmoidProbabilityModel svcProbability(BinaryClassificationProblem<L, P> problem, float Cp, float Cn)
		{
		// ** Original implementation makes a point of not explicitly training if all of the examples are in one class anyway.  Does that matter?

		SvmParameter<L> subparam = new SvmParameter<L>(param);
		subparam.probability = false;
		subparam.C = 1.0f;

		subparam.putWeight(trueLabel, Cp);
		subparam.putWeight(falseLabel, Cn);


		// ugly hack to temporarily replace the parameters.  This only works because train() is ultimately a method on this very object.
		SvmParameter origParam = param;
		param = subparam;

		Map<P, Float> decisionValues =
				continuousCrossValidation((ExplicitSvmProblem<L, P, BinaryClassificationProblem<L, P>>) problem, 5);

		param = origParam;

		/*
		  int i;
		  int numberOfFolds = 5;
		  int[] perm = new int[problem.getNumExamples()];
		  float[] decisionValues = new float[problem.getNumExamples()];

		  // random shuffle
		  for (i = 0; i < problem.getNumExamples(); i++)
			  {
			  perm[i] = i;
			  }
		  for (i = 0; i < problem.getNumExamples(); i++)
			  {
			  int j = i + (int) (Math.random() * (problem.getNumExamples() - i));

			  int swap = perm[i];
			  perm[i] = perm[j];
			  perm[j] = swap;
			  }
		  for (i = 0; i < numberOfFolds; i++)
			  {
			  int begin = i * problem.getNumExamples() / numberOfFolds;
			  int end = (i + 1) * problem.getNumExamples() / numberOfFolds;
			  int j, k;
			  int subprobLength = problem.getNumExamples() - (end - begin);
			  BinaryClassificationProblem<P> subprob = new BinaryClassificationProblem<P>(subprobLength);

			  k = 0;
			  for (j = 0; j < begin; j++)
				  {
				  subprob.examples[k] = problem.examples[perm[j]];
				  subprob.putTargetValue(k, problem.getTargetValue(perm[j]));
				  ++k;
				  }
			  for (j = end; j < problem.getNumExamples(); j++)
				  {
				  subprob.examples[k] = problem.examples[perm[j]];
				  subprob.putTargetValue(k, problem.getTargetValue(perm[j]));
				  ++k;
				  }
			  int positiveCount = 0, negativeCount = 0;
			  for (j = 0; j < k; j++)
				  {
				  if (subprob.getTargetValue(j))
					  {
					  positiveCount++;
					  }
				  else
					  {
					  negativeCount++;
					  }
				  }

			  if (positiveCount == 0 && negativeCount == 0)
				  {
				  for (j = begin; j < end; j++)
					  {
					  decisionValues[perm[j]] = 0;
					  }
				  }
			  else if (positiveCount > 0 && negativeCount == 0)
				  {
				  for (j = begin; j < end; j++)
					  {
					  decisionValues[perm[j]] = 1;
					  }
				  }
			  else if (positiveCount == 0 && negativeCount > 0)
					  {
					  for (j = begin; j < end; j++)
						  {
						  decisionValues[perm[j]] = -1;
						  }
					  }
				  else
					  {
					  SvmParameter<Boolean> subparam = new SvmParameter<Boolean>(param);
					  subparam.probability = false;
					  subparam.C = 1.0f;

					  subparam.putWeight(true, Cp);
					  subparam.putWeight(false, Cn);

					  SvmParameter origParam = param;
					  param = subparam;

					  BinaryModel submodel = train(subprob);

					  param = origParam;

					  for (j = begin; j < end; j++)
						  {
						  Float decisionValue = submodel.predictValue(problem.examples[perm[j]]);
						  decisionValues[perm[j]] = decisionValue;
						  // ensure +1 -1 order; reason not using CV subroutine
						  //	decisionValues[perm[j]] *= submodel.label[0];  // Huh?  this was always 1, right?
						  // Yep, decisionValue > 0 => true
						  }
					  }
			  }
  */

		//	return sigmoidTrain(problem.getExamples(), decisionValues);

		// convert to arrays

		int i = 0;
		float[] decisionValueArray = new float[decisionValues.size()];
		boolean[] labelArray = new boolean[decisionValues.size()];
		for (P point : decisionValues.keySet())
			{
			decisionValueArray[i] = decisionValues.get(point);
			labelArray[i] = problem.getTargetValue(point).equals(trueLabel);
			}

		return new SigmoidProbabilityModel(decisionValueArray, labelArray);
		}
	}
