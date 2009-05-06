package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.regression.RegressionProblemImpl;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class OneClassProblemImpl<L, P> extends RegressionProblemImpl<P, OneClassProblem<L, P>>
		implements OneClassProblem<L, P>
	{
// ------------------------------ FIELDS ------------------------------

	L label;

// --------------------------- CONSTRUCTORS ---------------------------

	public OneClassProblemImpl(Map<P, Float> examples, Map<P, Integer> exampleIds, L label)  // set<P> examples
		{
		super(examples, exampleIds);
		this.label = label;
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	public L getLabel()
		{
		return label;
		}

	private ScalingModelLearner<P> lastScalingModelLearner = null;
	private OneClassProblemImpl<L, P> scaledCopy = null;

// --------------------------- CONSTRUCTORS ---------------------------


	public OneClassProblemImpl(Map<P, Float> examples, Map<P, Integer> exampleIds, L label,
	                           ScalingModel<P> learnedScalingModel)  // set<P> examples
		{
		super(examples, exampleIds, learnedScalingModel);
		this.label = label;
		}


	public OneClassProblemImpl<L, P> getScaledCopy(@NotNull ScalingModelLearner<P> scalingModelLearner)
		{
		if (!scalingModelLearner.equals(lastScalingModelLearner))
			{
			scaledCopy = (OneClassProblemImpl<L, P>) learnScaling(scalingModelLearner);
			lastScalingModelLearner = scalingModelLearner;
			}
		return scaledCopy;
		}

	public OneClassProblemImpl<L, P> createScaledCopy(Map<P, Float> scaledExamples, Map<P, Integer> scaledExampleIds,
	                                                  ScalingModel<P> learnedScalingModel)
		{
		return new OneClassProblemImpl<L, P>(scaledExamples, scaledExampleIds, label, learnedScalingModel);
		}
	}
