package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblemImpl;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.util.SubtractionMap;
import org.jetbrains.annotations.NotNull;

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
			falseLabel = result.get(1);
			trueLabel = result.get(0);
			}
		}

	public BinaryClassificationProblemImpl(Class labelClass, Map<P, L> examples, Map<P, Integer> exampleIds,
	                                       @NotNull ScalingModel<P> scalingModel, L trueLabel, L falseLabel)
		{
		super(examples, exampleIds, scalingModel);
		this.trueLabel = trueLabel;
		this.falseLabel = falseLabel;
		this.labelClass = labelClass;
		}

	public BinaryClassificationProblemImpl(BinaryClassificationProblemImpl<L, P> backingProblem, Set<P> heldOutPoints)
		{
		super(new SubtractionMap<P, L>(backingProblem.examples, heldOutPoints), backingProblem.exampleIds,
		      backingProblem.scalingModel, heldOutPoints);
		this.trueLabel = backingProblem.trueLabel;
		this.falseLabel = backingProblem.falseLabel;
		this.labelClass = backingProblem.labelClass;
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
		if (labelClass.equals(Boolean.class))
			{
			return (Map<P, Boolean>) examples;
			}

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
			scaledCopy = learnScaling(scalingModelLearner);
			lastScalingModelLearner = scalingModelLearner;
			}
		return scaledCopy;
		}

	public BinaryClassificationProblem<L, P> createScaledCopy(Map<P, L> scaledExamples,
	                                                          Map<P, Integer> scaledExampleIds,
	                                                          ScalingModel<P> learnedScalingModel)
		{
		return new BinaryClassificationProblemImpl<L, P>(labelClass, scaledExamples, scaledExampleIds,
		                                                 learnedScalingModel, trueLabel, falseLabel);
		}

// --------------------- Interface SvmProblem ---------------------

	public L getTargetValue(P point)
		{
		return examples.get(point);
		}

// -------------------------- OTHER METHODS --------------------------

	protected BinaryClassificationProblem<L, P> makeFold(Set<P> heldOutPoints)
		{
		return new BinaryClassificationProblemImpl(this, heldOutPoints);
		}
// -------------------------- INNER CLASSES --------------------------
	}
