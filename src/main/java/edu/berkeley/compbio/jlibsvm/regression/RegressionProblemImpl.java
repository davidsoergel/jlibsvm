package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblemImpl;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.util.SubtractionMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class RegressionProblemImpl<P, R extends RegressionProblem<P, R>> extends ExplicitSvmProblemImpl<Float, P, R>
		implements RegressionProblem<P, R>
	{
// --------------------------- CONSTRUCTORS ---------------------------

	public RegressionProblemImpl(Map<P, Float> examples, Map<P, Integer> exampleIds, ScalingModel<P> scalingModel)
		{
		super(examples, exampleIds, scalingModel);
		}

	public RegressionProblemImpl(Map<P, Float> examples, Map<P, Integer> exampleIds, ScalingModel<P> scalingModel,
	                             Set<P> heldOutPoints)
		{
		super(examples, exampleIds, scalingModel, heldOutPoints);
		}

	public RegressionProblemImpl(RegressionProblemImpl<P, R> backingProblem, Set<P> heldOutPoints)
		{
		super(new SubtractionMap<P, Float>(backingProblem.examples, heldOutPoints), backingProblem.exampleIds,
		      backingProblem.scalingModel, heldOutPoints);
		}

	public RegressionProblemImpl(Map<P, Float> examples, Map<P, Integer> exampleIds)
		{
		super(examples, exampleIds);
		}

// ------------------------ INTERFACE METHODS ------------------------

	// cache the scaled copy, taking care that the scalingModelLearner is the same one.
	// only bother keeping one (i.e. don't make a map from learners to scaled copies)
	private ScalingModelLearner<P> lastScalingModelLearner = null;
	private R scaledCopy = null;

// --------------------- Interface SvmProblem ---------------------

	public List<Float> getLabels()
		{
		throw new SvmException("Shouldn't try to get unique target values for a regression problem");
		}

// -------------------------- OTHER METHODS --------------------------

	protected R makeFold(Set<P> heldOutPoints)
		{
		return (R) new RegressionProblemImpl(this, heldOutPoints);
		}

	public R getScaledCopy(@NotNull ScalingModelLearner<P> scalingModelLearner)
		{
		if (!scalingModelLearner.equals(lastScalingModelLearner))
			{
			scaledCopy = learnScaling(scalingModelLearner);
			lastScalingModelLearner = scalingModelLearner;
			}
		return scaledCopy;
		}

	public R createScaledCopy(Map<P, Float> scaledExamples, Map<P, Integer> scaledExampleIds,
	                          ScalingModel<P> learnedScalingModel)
		{
		return (R) new RegressionProblemImpl<P, R>(scaledExamples, scaledExampleIds, learnedScalingModel);
		}
	}
