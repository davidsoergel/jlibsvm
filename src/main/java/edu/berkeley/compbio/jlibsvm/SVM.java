package edu.berkeley.compbio.jlibsvm;

import com.davidsoergel.conja.Function;
import com.davidsoergel.conja.Parallel;
import edu.berkeley.compbio.ml.CrossValidationResults;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

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

	public Map<P, Float> continuousCrossValidation(SvmProblem<L, P, R> problem, final ImmutableSvmParameter<L, P> param)
		//	,final TreeExecutorService execService)
		{

		final Map<P, Float> predictions = new ConcurrentHashMap<P, Float>();

		if (param.crossValidationFolds >= problem.getNumExamples())
			{
			// this can happen when the points chosen from a multiclass CV don't include enough points from a given pair of classes
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}


		Iterator<R> foldIterator = problem.makeFolds(param.crossValidationFolds);

		Parallel.forEach(foldIterator, new Function<R, Void>()
		{
		public Void apply(final R f)
			{// this will throw ClassCastException if you try cross-validation on a discrete-only model (e.g. MultiClassModel)
			final ContinuousModel<P> model = (ContinuousModel<P>) train(f, param); //, execService);

			for (final P p : f.getHeldOutPoints())
				{
				predictions.put(p, model.predictValue(p));
				}

			// multithreaded version: avoids problem that cpus % folds != 0, but at the cost of lots of fine-grained tasks
			// usually there's a higher level of multithreading anyway

			/*
			   Set<Runnable> pointTasks = new HashSet<Runnable>();

			   for (final P p : f.getHeldOutPoints())
				   {
				   pointTasks.add(new Runnable()
				   {
				   public void run()
					   {
					   //return model.predictLabel(p);
					   predictions.put(p, model.predictValue(p));
					   }
				   });
				   }

			   execService.submitAndWaitForAll(pointTasks);
			   */
			return null;
			}
		});

		//subparam.probability = false;

		/*	Iterator<Runnable> foldTaskIterator = new MappingIterator<R, Runnable>(foldIterator)
		{
		public Runnable function(final R f)
			{
			return new Runnable()
			{
			public void run()
				{

				}
			};
			}
		};

		execService.submitAndWaitForAll(foldTaskIterator);*/
		/*	execService.submitTaskGroup(new TaskGroup(foldTaskIterator)
		  {
		  public void done()
			  {
			  gah
			  }
		  });
  */
		// now predictions contains the prediction for each point based on training with e.g. 80% of the other points (for 5-fold).
		return predictions;
		}

	public abstract SolutionModel<L, P> train(R problem, ImmutableSvmParameter<L, P> param);
	//,  final TreeExecutorService execService);

	public Map<P, L> discreteCrossValidation(SvmProblem<L, P, R> problem, final ImmutableSvmParameter<L, P> param)
		// , final TreeExecutorService execService)
		{
		final Map<P, L> predictions = new ConcurrentHashMap<P, L>();
		final Set<P> nullPredictionPoints =
				new ConcurrentSkipListSet<P>(); // necessary because ConcurrentHashMap doesn't support null values

		if (param.crossValidationFolds >= problem.getNumExamples())
			{
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}

		Iterator<R> foldIterator = problem.makeFolds(param.crossValidationFolds);

		//Set<Runnable> foldTasks = new HashSet<Runnable>();

		Parallel.forEach(foldIterator, new Function<R, Void>()
		{
		public Void apply(final R f)
			{// this will throw ClassCastException if you try cross-validation on a continuous-only model (e.g. RegressionModel)
			final DiscreteModel<L, P> model = (DiscreteModel<L, P>) train(f, param); //, execService); //, qMatrix);

			// note the param has not changed here, so if the method includes oneVsAll models with a
			// probability threshold, those will be independently computed for each fold and so play
			// into the predictLabel

			for (final P p : f.getHeldOutPoints())
				{
				L prediction = model.predictLabel(p);
				if (prediction == null)
					{
					nullPredictionPoints.add(p);
					}
				else
					{
					predictions.put(p, prediction);
					}
				}

			// multithreaded version: avoids problem that cpus % folds != 0, but at the cost of lots of fine-grained tasks
			// usually there's a higher level of multithreading anyway

			/*
			   Set<Runnable> pointTasks = new HashSet<Runnable>();

			   for (final P p : f.getHeldOutPoints())
				   {
				   pointTasks.add(new Runnable()
				   {
				   public void run()
					   {
					   //return model.predictLabel(p);
					   predictions.put(p, model.predictLabel(p));
					   }
				   });
				   }

			   execService.submitAndWaitForAll(pointTasks);
			   */

			return null;
			}
		});

/*
		Iterator<Runnable> foldTaskIterator = new MappingIterator<R, Runnable>(foldIterator)
		{
		public Runnable function(final R f)
			{
			return new Runnable()
			{
			public void run()
				{
				}
			};
			}
		};
*/
		//execService.submitAndWaitForAll(foldTaskIterator);

		// collapse into non-concurrent map that supports null values
		Map<P, L> result = new HashMap<P, L>(predictions);
		for (P nullPredictionPoint : nullPredictionPoints)
			{
			result.put(nullPredictionPoint, null);
			}
		// now predictions contains the prediction for each point based on training with e.g. 80% of the other points (for 5-fold).
		return result;
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
	//, final TreeExecutorService execService);
	}
