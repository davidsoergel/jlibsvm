package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblemImpl;
import edu.berkeley.compbio.jlibsvm.labelinverter.LabelInverter;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.util.SubtractionMap;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassProblemImpl<L extends Comparable, P> //, R extends MultiClassProblem<? extends L, ? extends P, ? extends R>>
		extends ExplicitSvmProblemImpl<L, P, MultiClassProblem<L, P>>
		implements MultiClassProblem<L, P> //, MutableSvmProblem<L,P,R>
	{
// ------------------------------ FIELDS ------------------------------

	Class labelClass;

	private LabelInverter<L> labelInverter;

	// cache the scaled copy, taking care that the scalingModelLearner is the same one.
	// only bother keeping one (i.e. don't make a map from learners to scaled copies)
	private ScalingModelLearner<P> lastScalingModelLearner = null;
	private MultiClassProblem<L, P> scaledCopy = null;


	private Map<L, Set<P>> theInverseMap = null;


// --------------------------- CONSTRUCTORS ---------------------------

	/**
	 * For now, pending further cleanup, we need to create arrays of the label type.  That's impossible to do with generics
	 * alone, so we need to provide the class object (e.g., String.class or whatever) for the label type used.  Of course
	 * this should match the generics used on SvmProblem, etc.
	 *
	 * @param labelClass
	 * @param examples
	 */
	public MultiClassProblemImpl(Class labelClass, LabelInverter<L> labelInverter, Map<P, L> examples,
	                             Map<P, Integer> exampleIds, ScalingModel<P> scalingModel)
		{
		super(examples, exampleIds, scalingModel);
		this.labelClass = labelClass;
		this.labelInverter = labelInverter;
		}

	public MultiClassProblemImpl(MultiClassProblemImpl<L, P> backingProblem, Set<P> heldOutPoints)
		{
		super(new SubtractionMap(backingProblem.examples, heldOutPoints), backingProblem.exampleIds,
		      backingProblem.scalingModel, heldOutPoints);
		this.labelClass = backingProblem.labelClass;
		this.labelInverter = backingProblem.labelInverter;
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	public Class getLabelClass()
		{
		return labelClass;
		}

	public LabelInverter<L> getLabelInverter()
		{
		return labelInverter;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface MultiClassProblem ---------------------

	public Map<L, Set<P>> getExamplesByLabel()
		{
		if (theInverseMap == null)
			{
			theInverseMap = new HashMap<L, Set<P>>();
			for (L label : getLabels())
				{
				theInverseMap.put(label, new HashSet<P>());
				}

			// separate the training set into label-specific sets, caching all the while
			// (too bad the svm training requires all examples in memory)

			// The Apache or Google collections should have a reversible map...?  Whatever, do it by hand

			for (Map.Entry<P, L> entry : examples.entrySet())
				{
				theInverseMap.get(entry.getValue()).add(entry.getKey());
				//examples.put(label, sample);
				}
			}
		return theInverseMap;
		}


	public MultiClassProblem<L, P> getScaledCopy(@NotNull ScalingModelLearner<P> scalingModelLearner)
		{
		if (!scalingModelLearner.equals(lastScalingModelLearner))
			{
			scaledCopy = learnScaling(scalingModelLearner);
			lastScalingModelLearner = scalingModelLearner;
			}
		return scaledCopy;
		}

	public MultiClassProblem<L, P> createScaledCopy(Map<P, L> scaledExamples, Map<P, Integer> scaledExampleIds,
	                                                ScalingModel<P> learnedScalingModel)
		{
		return new MultiClassProblemImpl<L, P>(labelClass, labelInverter, scaledExamples, scaledExampleIds,
		                                       learnedScalingModel);
		}


// -------------------------- OTHER METHODS --------------------------

	protected MultiClassProblem<L, P> makeFold(Set<P> heldOutPoints)
		{
		return new MultiClassProblemImpl(this, heldOutPoints);
		}
	}
