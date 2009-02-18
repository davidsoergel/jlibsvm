package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.AbstractFold;
import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblemImpl;
import edu.berkeley.compbio.jlibsvm.Fold;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
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
	Class labelClass;
	/*
	public float[] targetValueAsFloat()
		{
		float[] result =  new float[targetValues.length];
		int i = 0;
		for (Boolean targetValue : targetValues)
			{
			result[i] = targetValue? 1f : -1f;
			i++;
			}
		return result;
		}
		*/
	/*
	 public BinaryClassificationProblemImpl(int numExamples)
		 {
		 super(numExamples);
		 //targetValues = new Boolean[numExamples];
		 }
 */

	// these are redundant with getLabels() but clearer which is which
	L trueLabel;
	L falseLabel;

	public L getTrueLabel()
		{
		return trueLabel;
		}

	public L getFalseLabel()
		{
		return falseLabel;
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

	public BinaryClassificationProblemImpl(Class labelClass, Map<P, L> examples, Map<P, Integer> exampleIds)
		{
		super(examples, exampleIds);
		setupLabels();
		this.labelClass = labelClass;
		}

	Map<P, Boolean> booleanExamples;

	public BinaryClassificationProblemImpl(Class labelClass, L trueLabel, Set<P> trueExamples, L falseLabel,
	                                       Set<P> falseExamples, Map<P, Integer> exampleIds)
		{
		// this is a hack: we leave examples==null and just deal with booleanExamples directly

		super(null, exampleIds);
		this.falseLabel = falseLabel;
		this.trueLabel = trueLabel;
		this.labelClass = labelClass;

		numExamples = trueExamples.size() + falseExamples.size();
		booleanExamples = new HashMap<P, Boolean>(numExamples);
		for (P trueExample : trueExamples)
			{
			booleanExamples.put(trueExample, Boolean.TRUE);
			}
		for (P falseExample : falseExamples)
			{
			booleanExamples.put(falseExample, Boolean.FALSE);
			}

		exampleCounts = new HashMap<L, Integer>(2);
		exampleCounts.put(trueLabel, trueExamples.size());
		exampleCounts.put(falseLabel, falseExamples.size());

		labels = new ArrayList<L>(2);
		labels.add(trueLabel);
		labels.add(falseLabel);
		}

	/*	public BinaryClassificationProblem newSubProblem(int numExamples)
		 {
		 return new BinaryClassificationProblem(numExamples);
		 }
 */

	// not bothering with generic "L"
	/*	Object trueLabel;
   Object falseLabel;

   public BinaryClassificationProblemImpl(Object trueLabel, Object falseLabel, Map<P, Boolean> examples)
	   {
	   this(examples);
	   this.trueLabel = trueLabel;
	   this.falseLabel = falseLabel;
	   }*/

	//Float trueClass = null;

	/*	public void addExampleFloat(P point, Float x)
	   {
	   if (getLabels().size() == 0) // == null)
		   {
trueLabel =
		   }
	   addExample(point, x.equals(trueLabel));
	   }*/

	public L getTargetValue(P point)
		{
		if (booleanExamples != null)
			{
			if (booleanExamples.get(point))
				{
				return trueLabel;
				}
			else
				{
				return falseLabel;
				}
			}
		return examples.get(point); //targetValues[i];
		}

	protected Fold<L, P, BinaryClassificationProblem<L, P>> makeFold(Set<P> heldOutPoints)
		{
		return new BinaryClassificationFold(//this,
		                                    heldOutPoints);
		}

	public Map<P, Boolean> getBooleanExamples()
		{
		// hack to accommodate special boolean constructor
		if (booleanExamples != null)
			{
			return booleanExamples;
			}

		setupLabels();

		Map<P, Boolean> result = new HashMap<P, Boolean>(examples.size());
		for (Map.Entry<P, L> entry : examples.entrySet())
			{
			result.put(entry.getKey(), entry.getValue().equals(trueLabel) ? Boolean.TRUE : Boolean.FALSE);
			}
		return result;
		}

	public class BinaryClassificationFold extends AbstractFold<L, P, BinaryClassificationProblem<L, P>>
			implements BinaryClassificationProblem<L, P>
		{
		//	private BinaryClassificationProblemImpl<P> fullProblem;

		public BinaryClassificationFold(//BinaryClassificationProblemImpl<P> fullProblem,
		                                Set<P> heldOutPoints)
			{
			super(BinaryClassificationProblemImpl.this.getExamples(), heldOutPoints);
			//this.fullProblem = fullProblem;
			}

		public void setupLabels()
			{
			BinaryClassificationProblemImpl.this.setupLabels();
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

		public Set<Fold<Boolean, P, BinaryClassificationProblem<L, P>>> makeFolds(int numberOfFolds)
			{
			throw new NotImplementedException();
			}

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

		public L getTrueLabel()
			{
			return trueLabel; //BinaryClassificationProblemImpl.this.getTrueLabel();
			}

		public L getFalseLabel()
			{
			return falseLabel; //BinaryClassificationProblemImpl.this.getFalseLabel();
			}

		public int getId(P key)
			{
			return BinaryClassificationProblemImpl.this.getId(key);
			}

		public Map<P, Integer> getExampleIds()
			{
			return BinaryClassificationProblemImpl.this.getExampleIds();
			}
		}
	}
