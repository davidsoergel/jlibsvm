package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.AbstractFold;
import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblemImpl;
import edu.berkeley.compbio.jlibsvm.Fold;
import edu.berkeley.compbio.jlibsvm.labelinverter.LabelInverter;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import org.jetbrains.annotations.NotNull;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
				//** Maintain order for debugging, temporary:  new LinkedHashSet<P>()

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
			scaledCopy = new MultiClassProblemImpl<L, P>(labelClass, labelInverter, scaledExamples, scaledExampleIds,
			                                             scalingModel);
			}
		return scaledCopy;
		}

// -------------------------- OTHER METHODS --------------------------

	protected Fold<L, P, MultiClassProblem<L, P>> makeFold(Set<P> heldOutPoints)
		{
		return new MultiClassFold(heldOutPoints);
		}

// -------------------------- INNER CLASSES --------------------------

	public class MultiClassFold extends AbstractFold<L, P, MultiClassProblem<L, P>> implements MultiClassProblem<L, P>
		{
// ------------------------------ FIELDS ------------------------------

		// ** copied from above, yuck
		// PERF this could be more efficient...
		private Map<L, Set<P>> theInverseMap = null;

		//** Note we scale each fold independently, since that best simulates the real situation (where we can't use the test samples during scaling)


		// cache the scaled copy, taking care that the scalingModelLearner is the same one.
		// only bother keeping one (i.e. don't make a map from learners to scaled copies)
		private ScalingModelLearner<P> lastScalingModelLearner = null;
		private MultiClassProblem<L, P> scaledCopy = null;


// --------------------------- CONSTRUCTORS ---------------------------

		public MultiClassFold(Set<P> heldOutPoints)
			{
			super(MultiClassProblemImpl.this.getExamples(), heldOutPoints,
			      MultiClassProblemImpl.this.getScalingModel());
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

				for (Map.Entry<P, L> entry : getExamples().entrySet())
					{
					theInverseMap.get(entry.getValue()).add(entry.getKey());
					//examples.put(label, sample);
					}
				}
			return theInverseMap;
			}

		public Class getLabelClass()
			{
			return MultiClassProblemImpl.this.getLabelClass();
			}

		public LabelInverter<L> getLabelInverter()
			{
			return MultiClassProblemImpl.this.getLabelInverter();
			}

		public MultiClassProblem<L, P> getScaledCopy(@NotNull ScalingModelLearner<P> scalingModelLearner)
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
				scaledCopy =
						new MultiClassProblemImpl<L, P>(labelClass, labelInverter, scaledExamples, scaledExampleIds,
						                                scalingModel);
				}
			return scaledCopy;
			}

// --------------------- Interface SvmProblem ---------------------


		public Map<P, Integer> getExampleIds()
			{
			return MultiClassProblemImpl.this.getExampleIds();
			}

		public int getId(P key)
			{
			return MultiClassProblemImpl.this.getId(key);
			}

		public List<L> getLabels()
			{
			return MultiClassProblemImpl.this.getLabels();
			}

		public L getTargetValue(P point)
			{
			assert !heldOutPoints.contains(point);  // we should never ask to cheat
			return MultiClassProblemImpl.this.getTargetValue(point);
			}

// -------------------------- OTHER METHODS --------------------------

		public Set<Fold<L, P, MultiClassProblem<L, P>>> makeFolds(int numberOfFolds)
			{
			throw new NotImplementedException();
			}
		}
	}
