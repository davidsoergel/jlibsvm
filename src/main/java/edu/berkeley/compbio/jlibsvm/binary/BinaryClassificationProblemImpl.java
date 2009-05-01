package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.AbstractFold;
import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblemImpl;
import edu.berkeley.compbio.jlibsvm.Fold;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import org.jetbrains.annotations.NotNull;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BinaryClassificationProblemImpl<L extends Comparable, P>
		extends ExplicitSvmProblemImpl<L, P, BinaryClassificationProblem<L, P>>
		implements BinaryClassificationProblem<L, P>
	{
// ------------------------------ FIELDS ------------------------------

	Class labelClass;

	// these are redundant with getLabels() but clearer which is which
	L trueLabel;
	L falseLabel;

	// cache the scaled copy, taking care that the scalingModelLearner is the same one.
	// only bother keeping one (i.e. don't make a map from learners to scaled copies)
	private ScalingModelLearner<P> lastScalingModelLearner = null;
	private BinaryClassificationProblem<L, P> scaledCopy = null;


// --------------------------- CONSTRUCTORS ---------------------------

	public BinaryClassificationProblemImpl(Class labelClass, Map<P, L> examples, @NotNull Map<P, Integer> exampleIds)
		{
		super(examples, exampleIds);
		setupLabels();
		this.labelClass = labelClass;
		}

	public void setupLabels()
		{
		// note the labels are sorted
		List<L> result = super.getLabels();
		if (result != null)
			{
			// ** Just switched these, does it matter??
			falseLabel = result.get(1);
			trueLabel = result.get(0);
			}
		}

	public BinaryClassificationProblemImpl(Class labelClass, Map<P, L> examples, Map<P, Integer> exampleIds,
	                                       ScalingModel<P> scalingModel, L trueLabel, L falseLabel)
		{
		super(examples, exampleIds, scalingModel);
		this.trueLabel = trueLabel;
		this.falseLabel = falseLabel;
		this.labelClass = labelClass;
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	public L getFalseLabel()
		{
		return falseLabel;
		}

	public L getTrueLabel()
		{
		return trueLabel;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface BinaryClassificationProblem ---------------------


	public Map<P, Boolean> getBooleanExamples()
		{
		setupLabels();

		Map<P, Boolean> result = new HashMap<P, Boolean>(examples.size());
		for (Map.Entry<P, L> entry : examples.entrySet())
			{
			result.put(entry.getKey(), entry.getValue().equals(trueLabel) ? Boolean.TRUE : Boolean.FALSE);
			}
		return result;
		}

	public BinaryClassificationProblem<L, P> getScaledCopy(@NotNull ScalingModelLearner<P> scalingModelLearner)
		{
		if (!scalingModelLearner.equals(lastScalingModelLearner))
			{
			ScalingModel<P> scalingModel = scalingModelLearner.learnScaling(examples.keySet());

			Map<P, L> unscaledExamples = getExamples();
			Map<P, L> scaledExamples = new HashMap<P, L>(examples.size());
			Map<P, Integer> scaledExampleIds = new HashMap<P, Integer>(exampleIds.size());

			for (Map.Entry<P, L> entry : unscaledExamples.entrySet())
				{
				P scaledPoint = scalingModel.scaledCopy(entry.getKey());
				scaledExamples.put(scaledPoint, entry.getValue());
				scaledExampleIds.put(scaledPoint, exampleIds.get(entry.getKey()));
				}

			lastScalingModelLearner = scalingModelLearner;
			scaledCopy = new BinaryClassificationProblemImpl<L, P>(labelClass, scaledExamples, scaledExampleIds,
			                                                       scalingModel, trueLabel, falseLabel);
			}
		return scaledCopy;
		}

// --------------------- Interface SvmProblem ---------------------


	public L getTargetValue(P point)
		{
		return examples.get(point);
		}

// -------------------------- OTHER METHODS --------------------------

	protected Fold<L, P, BinaryClassificationProblem<L, P>> makeFold(Set<P> heldOutPoints)
		{
		return new BinaryClassificationFold(heldOutPoints);
		}

// -------------------------- INNER CLASSES --------------------------

	public class BinaryClassificationFold extends AbstractFold<L, P, BinaryClassificationProblem<L, P>>
			implements BinaryClassificationProblem<L, P>
		{
// ------------------------------ FIELDS ------------------------------

		//** Note we scale each fold independently, since that best simulates the real situation (where we can't use the test samples during scaling)


		// cache the scaled copy, taking care that the scalingModelLearner is the same one.
		// only bother keeping one (i.e. don't make a map from learners to scaled copies)
		private ScalingModelLearner<P> lastScalingModelLearner = null;
		private BinaryClassificationProblem<L, P> scaledCopy = null;


// --------------------------- CONSTRUCTORS ---------------------------

		public BinaryClassificationFold(Set<P> heldOutPoints)
			{
			super(BinaryClassificationProblemImpl.this.getExamples(), heldOutPoints,
			      BinaryClassificationProblemImpl.this.getScalingModel());
			}

// --------------------- GETTER / SETTER METHODS ---------------------

		public L getFalseLabel()
			{
			return falseLabel;
			}

		public L getTrueLabel()
			{
			return trueLabel;
			}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface BinaryClassificationProblem ---------------------


		public Map<P, Boolean> getBooleanExamples()
			{
			setupLabels();
			Map<P, Boolean> result = new HashMap<P, Boolean>(getExamples().size());
			for (Map.Entry<P, L> entry : getExamples().entrySet())
				{
				result.put(entry.getKey(), entry.getValue().equals(trueLabel) ? Boolean.TRUE : Boolean.FALSE);
				}
			return result;
			}

		public BinaryClassificationProblem<L, P> getScaledCopy(@NotNull ScalingModelLearner<P> scalingModelLearner)
			{
			if (!scalingModelLearner.equals(lastScalingModelLearner))
				{
				ScalingModel<P> scalingModel = scalingModelLearner.learnScaling(examples.keySet());

				Map<P, L> unscaledExamples = getExamples();
				Map<P, L> scaledExamples = new HashMap<P, L>(examples.size());
				Map<P, Integer> scaledExampleIds = new HashMap<P, Integer>(exampleIds.size());

				for (Map.Entry<P, L> entry : unscaledExamples.entrySet())
					{
					P scaledPoint = scalingModel.scaledCopy(entry.getKey());
					scaledExamples.put(scaledPoint, entry.getValue());
					scaledExampleIds.put(scaledPoint, exampleIds.get(entry.getKey()));
					}

				lastScalingModelLearner = scalingModelLearner;
				scaledCopy = new BinaryClassificationProblemImpl<L, P>(labelClass, scaledExamples, scaledExampleIds,
				                                                       scalingModel, trueLabel, falseLabel);
				}
			return scaledCopy;
			}

		public void setupLabels()
			{
			BinaryClassificationProblemImpl.this.setupLabels();
			}

// --------------------- Interface SvmProblem ---------------------


		public Map<P, Integer> getExampleIds()
			{
			return BinaryClassificationProblemImpl.this.getExampleIds();
			}

		public int getId(P key)
			{
			return BinaryClassificationProblemImpl.this.getId(key);
			}

		public List<L> getLabels()
			{
			return BinaryClassificationProblemImpl.this.getLabels();
			}

		public L getTargetValue(P point)
			{
			assert !heldOutPoints.contains(point);  // we should never ask to cheat
			return BinaryClassificationProblemImpl.this.getTargetValue(point);
			}

// -------------------------- OTHER METHODS --------------------------

		public Set<Fold<Boolean, P, BinaryClassificationProblem<L, P>>> makeFolds(int numberOfFolds)
			{
			throw new NotImplementedException();
			}
		}
	}
