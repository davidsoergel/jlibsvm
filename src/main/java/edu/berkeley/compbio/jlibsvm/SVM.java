package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.qmatrix.BooleanInvertingKernelQMatrix;
import edu.berkeley.compbio.jlibsvm.qmatrix.QMatrix;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class SVM<L extends Comparable, P, R extends SvmProblem<L, P>> extends SvmContext<L, P>
	{
	private static final Logger logger = Logger.getLogger(SVM.class);
	public static final int LIBSVM_VERSION = 288;


	protected SVM(KernelFunction<P> kernel, SvmParameter<L> param)
		{
		super(kernel, param);
		if (param.eps < 0)
			{
			throw new SvmException("eps < 0");
			}
		}


	//	public abstract Class getLabelClass();

	public abstract String getSvmType();

	//public abstract void setupQMatrix(SvmProblem<L, P> problem);

	public QMatrix<P> qMatrix;

	public void setupQMatrix(SvmProblem<L, P> problem)
		{
		if (qMatrix == null)
			{
			qMatrix = new BooleanInvertingKernelQMatrix<P>(kernel, problem.getExamples().size(), param.getCacheRows());
			}
		}

	public abstract SolutionModel<P> train(R problem);

	/*	protected abstract Map<P,L> foldPredict(R subprob, Iterator<P> foldIterator, int length);


	 public FoldSpec separateFolds(R problem, int numberOfFolds)
		 {
		 FoldSpec fs = new FoldSpec(problem.examples.size(), numberOfFolds);
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
 */


	public Map<P, Float> continuousCrossValidation(ExplicitSvmProblem<L, P, R> problem, int numberOfFolds)
		{
		Map<P, Float> predictions = new HashMap<P, Float>();

		if (numberOfFolds >= problem.getExamples().size())
			{
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}

		Set<Fold<L, P, R>> folds = problem.makeFolds(numberOfFolds);
		setupQMatrix(problem);
		for (Fold<L, P, R> f : folds)
			{			// this will throw ClassCastException if you try cross-validation on a discrete-only model (e.g. MultiClassModel)
			ContinuousModel<P> model = (ContinuousModel<P>) train(f.asR());
			for (P p : f.getHeldOutPoints())
				{
				predictions.put(p, model.predictValue(p));
				}
			}
		logger.info(qMatrix.perfString());
		return predictions;
		}

	public Map<P, L> discreteCrossValidation(ExplicitSvmProblem<L, P, R> problem, int numberOfFolds)
		{
		Map<P, L> predictions = new HashMap<P, L>();

		if (numberOfFolds >= problem.getExamples().size())
			{
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}

		Set<Fold<L, P, R>> folds = problem.makeFolds(numberOfFolds);
		setupQMatrix(problem);
		for (Fold<L, P, R> f : folds)
			{			// this will throw ClassCastException if you try cross-validation on a continuous-only model (e.g. RegressionModel)
			DiscreteModel<L, P> model = (DiscreteModel<L, P>) train(f.asR());
			for (P p : f.getHeldOutPoints())
				{
				predictions.put(p, model.predictLabel(p));
				}
			}
		logger.info(qMatrix.perfString());		/*
		FoldSpec fs = separateFolds(problem, numberOfFolds);

		// stratified cv may not give leave-one-out rate
		// Each class to l folds -> some folds may have zero elements


		for (int i = 0; i < numberOfFolds; i++)
			{
			int begin = fs.foldStart[i];
			int end = fs.foldStart[i + 1];


			int subprobLength = problem.getNumExamples() - (end - begin);

			R subprob = problem.newSubProblem(subprobLength);

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

			//L[] foldPredictions =
					predictions.putAll(foldPredict(subprob, new FoldIterator(problem, fs.perm, begin, end), end - begin));		for (int j = begin; j < end; j++)
				{
				predictions[fs.perm[j]] = foldPredictions[j - begin];
				}
			}
	*/		// now predictions contains the prediction for each point based on training with e.g. 80% of the other points (for 5-fold).
		return predictions;
		}

	/*
   protected class FoldIterator implements Iterator<P>
	   {
	   int index;
	   private int end;
	   private List<P> examples;
	   private int[] perm;

	   public FoldIterator(SvmProblem<L,P,?> problem, int[] perm, int begin, int end)
		   {
		   index = begin;
		   this.end = end;
		   this.examples = new ArrayList(problem.examples.keySet());
		   this.perm = perm;
		   }

	   public boolean hasNext()
		   {
		   return index < end;
		   }

	   public P next()
		   {
		   P result = examples.get(perm[index]);
		   index++;
		   return result;
		   }

	   public void remove()
		   {
		   throw new UnsupportedOperationException();
		   }
	   }*/
	}