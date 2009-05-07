package edu.berkeley.compbio.jlibsvm;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class SVM<L extends Comparable, P, R extends SvmProblem<L, P, R>> extends SvmContext
	{
// ------------------------------ FIELDS ------------------------------

	public static final int LIBSVM_VERSION = 288;


// -------------------------- OTHER METHODS --------------------------

	public Map<P, Float> continuousCrossValidation(SvmProblem<L, P, R> problem, ImmutableSvmParameter<L, P> param)
		{

		Map<P, Float> predictions = new HashMap<P, Float>();

		if (param.crossValidationFolds >= problem.getNumExamples())
			{
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}

		Set<R> folds = problem.makeFolds(param.crossValidationFolds);

		//subparam.probability = false;

		for (R f : folds)
			{
			// this will throw ClassCastException if you try cross-validation on a discrete-only model (e.g. MultiClassModel)
			ContinuousModel<P> model = (ContinuousModel<P>) train(f, param);

			// PERF multithread
			for (P p : f.getHeldOutPoints())
				{
				predictions.put(p, model.predictValue(p));
				}
			}
		return predictions;
		}

	public abstract SolutionModel<L, P> train(R problem, ImmutableSvmParameter<L, P> param);

	public Map<P, L> discreteCrossValidation(SvmProblem<L, P, R> problem, ImmutableSvmParameter<L, P> param)
		{
		Map<P, L> predictions = new HashMap<P, L>();

		if (param.crossValidationFolds >= problem.getNumExamples())
			{
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}

		Set<R> folds = problem.makeFolds(param.crossValidationFolds);

		for (R f : folds)
			{
			// this will throw ClassCastException if you try cross-validation on a continuous-only model (e.g. RegressionModel)
			DiscreteModel<L, P> model = (DiscreteModel<L, P>) train(f, param); //, qMatrix);


			// PERF multithread
			for (P p : f.getHeldOutPoints())
				{
				predictions.put(p, model.predictLabel(p));
				}
			}
		// now predictions contains the prediction for each point based on training with e.g. 80% of the other points (for 5-fold).
		return predictions;
		}

	public abstract String getSvmType();
	//public ScalingModelLearner<P> scalingModelLearner;
	/*
	protected SVM(ImmutableSvmParameter param)
		{
		//super(param);
		//this.scalingModelLearner = scalingModelLearner;
		if (param.eps < 0)
			{
			throw new SvmException("eps < 0");
			}
		}
		*/

	public void validateParam(@NotNull ImmutableSvmParameter<L, P> param)
		{
		if (param.eps < 0)
			{
			throw new SvmException("eps < 0");
			}
		}

	public abstract CrossValidationResults performCrossValidation(R problem, ImmutableSvmParameter<L, P> param);
	}
