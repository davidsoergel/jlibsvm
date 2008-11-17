package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.util.Iterator;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class SVM<T extends Comparable, P extends SvmProblem<T, P>> extends SvmContext
	{
	public static final int LIBSVM_VERSION = 288;


	protected SVM(KernelFunction kernel, SvmParameter<T> param)
		{
		super(kernel, param);
		if (param.eps < 0)
			{
			throw new SvmException("eps < 0");
			}
		}


	public abstract String getSvmType();

	public abstract SolutionModel train(P problem);

	protected abstract T[] foldPredict(P subprob, Iterator<SvmPoint> foldIterator, int length);


	public FoldSpec separateFolds(P problem, int numberOfFolds)
		{
		FoldSpec fs = new FoldSpec(problem.examples.length, numberOfFolds);
		for (int i = 0; i < fs.perm.length; i++)
			{
			fs.perm[i] = i;
			}
		//	Collections.shuffle(perm);
		for (int i = 0; i < fs.perm.length; i++)
			{
			int j = i + (int) (Math.random() * (fs.perm.length - i));

			int tmp = fs.perm[i];
			fs.perm[i] = fs.perm[j];
			fs.perm[j] = tmp;
			}
		for (int i = 0; i <= numberOfFolds; i++)
			{
			fs.foldStart[i] = i * fs.perm.length / numberOfFolds;
			}
		return fs;
		}

	public abstract Class getLabelClass();

	public T[] crossValidation(P problem, int numberOfFolds)
		{
		Class type = (Class) getLabelClass();
		T[] predictions = (T[]) java.lang.reflect.Array.newInstance(type, problem.examples.length);

		if (numberOfFolds >= problem.examples.length)
			{
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}

		FoldSpec fs = separateFolds(problem, numberOfFolds);

		// stratified cv may not give leave-one-out rate
		// Each class to l folds -> some folds may have zero elements


		for (int i = 0; i < numberOfFolds; i++)
			{
			int begin = fs.foldStart[i];
			int end = fs.foldStart[i + 1];


			int subprobLength = problem.examples.length - (end - begin);

			P subprob = problem.newSubProblem(subprobLength);

			int k = 0;
			for (int j = 0; j < begin; j++)
				{
				subprob.examples[k] = problem.examples[fs.perm[j]];
				subprob.targetValues[k] = problem.targetValues[fs.perm[j]];
				++k;
				}
			for (int j = end; j < fs.perm.length; j++)
				{
				subprob.examples[k] = problem.examples[fs.perm[j]];
				subprob.targetValues[k] = problem.targetValues[fs.perm[j]];
				++k;
				}

			T[] foldPredictions = foldPredict(subprob, new FoldIterator(problem, fs.perm, begin, end), end - begin);

			for (int j = begin; j < end; j++)
				{
				predictions[fs.perm[j]] = foldPredictions[j - begin];
				}
			}
		// now predictions contains the prediction for each point based on training with e.g. 80% of the other points (for 5-fold).
		return predictions;
		}


	protected class FoldIterator implements Iterator<SvmPoint>
		{
		int index;
		private int end;
		private SvmPoint[] examples;
		private int[] perm;

		public FoldIterator(SvmProblem problem, int[] perm, int begin, int end)
			{
			index = begin;
			this.end = end;
			this.examples = problem.examples;
			this.perm = perm;
			}

		public boolean hasNext()
			{
			return index < end;
			}

		public SvmPoint next()
			{
			SvmPoint result = examples[perm[index]];
			index++;
			return result;
			}

		public void remove()
			{
			throw new UnsupportedOperationException();
			}
		}
	}