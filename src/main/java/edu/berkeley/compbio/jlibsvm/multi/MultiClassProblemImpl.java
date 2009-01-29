package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.AbstractFold;
import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblemImpl;
import edu.berkeley.compbio.jlibsvm.Fold;
import edu.berkeley.compbio.jlibsvm.labelinverter.LabelInverter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
	Class labelClass;

	public Class getLabelClass()
		{
		return labelClass;
		}

	private LabelInverter<L> labelInverter;


	/**
	 * For now, pending further cleanup, we need to create arrays of the label type.  That's impossible to do with generics
	 * alone, so we need to provide the class object (e.g., String.class or whatever) for the label type used.  Of course
	 * this should match the generics used on SvmProblem, etc.
	 *
	 * @param labelClass
	 * @param examples
	 */
	public MultiClassProblemImpl(Class labelClass, LabelInverter<L> labelInverter, Map<P, L> examples,
	                             Map<P, Integer> exampleIds)
		{
		super(examples, exampleIds);
		this.labelClass = labelClass;
		this.labelInverter = labelInverter;
		//targetValues = (T[]) java.lang.reflect.Array.newInstance(type, length);
		}

	public LabelInverter<L> getLabelInverter()
		{
		return labelInverter;
		}
	/*
	 public MultiClassProblem<L, P> newSubProblem(int length)
		 {
		 return new MultiClassProblemImpl<L, P>(type, length);
		 }
 */


	private Map<L, Set<P>> theInverseMap = null;

	public Map<L, Set<P>> getExamplesByLabel()
		{
		if (theInverseMap == null)
			{
			theInverseMap = new HashMap<L, Set<P>>();
			for (L label : getLabels())
				{//** Maintain order for debugging, temporary
				theInverseMap.put(label, new LinkedHashSet<P>());
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


	protected Fold<L, P, MultiClassProblem<L, P>> makeFold(Set<P> heldOutPoints)
		{
		return new MultiClassFold(heldOutPoints);
		}

	public class MultiClassFold extends AbstractFold<L, P, MultiClassProblem<L, P>> implements MultiClassProblem<L, P>
		{
		public MultiClassFold(Set<P> heldOutPoints)
			{
			super(MultiClassProblemImpl.this.getExamples(), heldOutPoints);
			}

		public List<L> getLabels()
			{
			return MultiClassProblemImpl.this.getLabels();
			}

		public Class getLabelClass()
			{
			return MultiClassProblemImpl.this.getLabelClass();
			}

		public L getTargetValue(P point)
			{
			assert !heldOutPoints.contains(point);  // we should never ask to cheat
			return MultiClassProblemImpl.this.getTargetValue(point);
			}

		public Set<edu.berkeley.compbio.jlibsvm.Fold<L, P, MultiClassProblem<L, P>>> makeFolds(int numberOfFolds)
			{
			throw new NotImplementedException();
			}

		// ** copied from above, yuck
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

		public LabelInverter<L> getLabelInverter()
			{
			return MultiClassProblemImpl.this.getLabelInverter();
			}

		public int getId(P key)
			{
			return MultiClassProblemImpl.this.getId(key);
			}

		public Map<P, Integer> getExampleIds()
			{
			return MultiClassProblemImpl.this.getExampleIds();
			}
		}
	}
