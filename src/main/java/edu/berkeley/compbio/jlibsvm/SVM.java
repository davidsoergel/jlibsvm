package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class SVM<L extends Comparable, P, R extends SvmProblem<L, P>> extends SvmContext<L, P>
	{
// ------------------------------ FIELDS ------------------------------

	public static final int LIBSVM_VERSION = 288;
	public ScalingModelLearner<P> scalingModelLearner;


// --------------------------- CONSTRUCTORS ---------------------------

	protected SVM(KernelFunction<P> kernel, ScalingModelLearner<P> scalingModelLearner, SvmParameter<L> param)
		{
		super(kernel, param);
		this.scalingModelLearner = scalingModelLearner;
		if (param.eps < 0)
			{
			throw new SvmException("eps < 0");
			}
		}

// -------------------------- OTHER METHODS --------------------------

	public Map<P, Float> continuousCrossValidation(ExplicitSvmProblem<L, P, R> problem, int numberOfFolds)
		{
		Map<P, Float> predictions = new HashMap<P, Float>();

		if (numberOfFolds >= problem.getNumExamples())
			{
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}

		Set<Fold<L, P, R>> folds = problem.makeFolds(numberOfFolds);


		for (Fold<L, P, R> f : folds)
			{
			// this will throw ClassCastException if you try cross-validation on a discrete-only model (e.g. MultiClassModel)
			ContinuousModel<P> model = (ContinuousModel<P>) train(f.asR());
			for (P p : f.getHeldOutPoints())
				{
				predictions.put(p, model.predictValue(p));
				}
			}
		return predictions;
		}

	public abstract SolutionModel<P> train(R problem);

	public Map<P, L> discreteCrossValidation(ExplicitSvmProblem<L, P, R> problem, int numberOfFolds)
		{
		Map<P, L> predictions = new HashMap<P, L>();

		if (numberOfFolds >= problem.getNumExamples())
			{
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}

		Set<Fold<L, P, R>> folds = problem.makeFolds(numberOfFolds);

		for (Fold<L, P, R> f : folds)
			{
			// this will throw ClassCastException if you try cross-validation on a continuous-only model (e.g. RegressionModel)
			DiscreteModel<L, P> model = (DiscreteModel<L, P>) train(f.asR()); //, qMatrix);
			for (P p : f.getHeldOutPoints())
				{
				predictions.put(p, model.predictLabel(p));
				}
			}
		// now predictions contains the prediction for each point based on training with e.g. 80% of the other points (for 5-fold).
		return predictions;
		}

	public abstract String getSvmType();
	}
