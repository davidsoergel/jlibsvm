package edu.berkeley.compbio.jlibsvm;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class SVM<L extends Comparable, P, R extends SvmProblem<L, P, R>> extends SvmContext
	{
	private static final Logger logger = Logger.getLogger(SVM.class);
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
		final Map<P, L> predictions = new HashMap<P, L>();

		if (param.crossValidationFolds >= problem.getNumExamples())
			{
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}

		Set<R> folds = problem.makeFolds(param.crossValidationFolds);

		for (final R f : folds)
			{
			// PERF multithread

			// ** make number of threads adjustable?
			ExecutorService execService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			Map<P, Future<L>> futureMap = new HashMap<P, Future<L>>();


			// this will throw ClassCastException if you try cross-validation on a continuous-only model (e.g. RegressionModel)
			final DiscreteModel<L, P> model = (DiscreteModel<L, P>) train(f, param); //, qMatrix);

			//PERF memory issues?

			for (final P p : f.getHeldOutPoints())
				{
				Future<L> fut = execService.submit(new Callable()
				{
				public L call()
					{
					return model.predictLabel(p);
					}
				});
				futureMap.put(p, fut);
				}

			try
				{
				// this will ask for the results in random order, blocking until each respective job is done, but we don't care
				for (Map.Entry<P, Future<L>> entry : futureMap.entrySet())
					{
					predictions.put(entry.getKey(), entry.getValue().get());
					}

				//execService.awaitTermination(365, TimeUnit.DAYS);
				}
			catch (InterruptedException e)
				{
				logger.error("Error", e);
				throw new SvmException(e);
				}
			catch (ExecutionException e)
				{
				logger.error("Error", e);
				throw new SvmException(e);
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
