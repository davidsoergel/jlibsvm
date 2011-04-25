package edu.berkeley.compbio.jlibsvm.binary;

import com.davidsoergel.dsutils.collections.MappingIterator;
import com.google.common.collect.HashMultiset;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BooleanClassificationProblemImpl<L extends Comparable, P> extends BinaryClassificationProblemImpl<L, P>
	{
// ------------------------------ FIELDS ------------------------------

	private Map<P, Boolean> booleanExamples;
	private Set<P> trueExamples;
	private Set<P> falseExamples;
	int numExamples = 0;

// --------------------------- CONSTRUCTORS ---------------------------

	public BooleanClassificationProblemImpl(Class labelClass, L trueLabel, Set<P> trueExamples, L falseLabel,
	                                        Set<P> falseExamples, Map<P, Integer> exampleIds)
		{
		// this is a hack: we leave examples==null and just deal with booleanExamples directly

		super(labelClass, null, exampleIds);
		this.falseLabel = falseLabel;
		this.trueLabel = trueLabel;
		this.trueExamples = trueExamples;
		this.falseExamples = falseExamples;

		labels = new ArrayList<L>(2);
		labels.add(trueLabel);
		labels.add(falseLabel);

		numExamples = trueExamples.size() + falseExamples.size();

		exampleCounts = HashMultiset.create();
		exampleCounts.add(trueLabel, trueExamples.size());
		exampleCounts.add(falseLabel, falseExamples.size());
		}


	public BooleanClassificationProblemImpl(BooleanClassificationProblemImpl<L, P> backingProblem, Set<P> heldOutPoints)
		{
		super(backingProblem.labelClass, null, backingProblem.exampleIds, backingProblem.scalingModel,
		      backingProblem.trueLabel, backingProblem.falseLabel);
		this.heldOutPoints = heldOutPoints;

		// PERF use a SubtractionSet?
		this.trueExamples = new HashSet<P>(backingProblem.trueExamples);
		this.falseExamples = new HashSet<P>(backingProblem.falseExamples);
		trueExamples.removeAll(heldOutPoints);
		falseExamples.removeAll(heldOutPoints);

		labels = new ArrayList<L>(2);
		labels.add(trueLabel);
		labels.add(falseLabel);

		numExamples = trueExamples.size() + falseExamples.size();

		exampleCounts = HashMultiset.create();
		exampleCounts.add(trueLabel, trueExamples.size());
		exampleCounts.add(falseLabel, falseExamples.size());
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	public synchronized Map<P, Boolean> getBooleanExamples()
		{
		if (booleanExamples == null)
			{
			booleanExamples = new HashMap<P, Boolean>(numExamples);
			for (P trueExample : trueExamples)
				{
				booleanExamples.put(trueExample, Boolean.TRUE);
				}
			for (P falseExample : falseExamples)
				{
				booleanExamples.put(falseExample, Boolean.FALSE);
				}
			assert booleanExamples.size() == numExamples;
			}
		return booleanExamples;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface BinaryClassificationProblem ---------------------

	/**
	 * There's no sense in scaling Boolean values, so this is a noop.  note we don't make a copy for efficiency.
	 *
	 * @param scalingModelLearner
	 * @return
	 */
	public BinaryClassificationProblem<L, P> getScaledCopy(@NotNull ScalingModelLearner<P> scalingModelLearner)
		{
		return this;
		}

	public void setupLabels()
		{
		// the constructor already dealt with this
		}

// --------------------- Interface SvmProblem ---------------------

	public synchronized L getTargetValue(P point)
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

	public int getNumExamples()
		{
		return numExamples;
		}

	// need to override this because of the examples == null hack
	public Iterator<BinaryClassificationProblem<L, P>> makeFolds(int numberOfFolds)
		{
//		Set<BinaryClassificationProblem<L, P>> result = new HashSet<BinaryClassificationProblem<L, P>>();

		List<P> points = new ArrayList<P>(getBooleanExamples().keySet());

		Collections.shuffle(points);

		// PERF this is maybe overwrought, but ensures the best possible balance among folds (unlike examples.size() / numberOfFolds)

		List<Set<P>> heldOutPointSets = new ArrayList<Set<P>>();
		for (int i = 0; i < numberOfFolds; i++)
			{
			heldOutPointSets.add(new HashSet<P>());
			}

		int f = 0;
		for (P point : points)
			{
			heldOutPointSets.get(f).add(point);
			f++;
			f %= numberOfFolds;
			}

		Iterator<BinaryClassificationProblem<L, P>> foldIterator =
				new MappingIterator<Set<P>, BinaryClassificationProblem<L, P>>(heldOutPointSets.iterator())
				{
				@NotNull
				public BinaryClassificationProblem<L, P> function(Set<P> p)
					{
					return makeFold(p);
					}
				};
		return foldIterator;

		/*
				for (Set<P> heldOutPoints : heldOutPointSets)
					{
					result.add(makeFold(heldOutPoints));
					}

				return result;*/
		}


	protected BooleanClassificationProblemImpl<L, P> makeFold(Set<P> heldOutPoints)
		{
		return new BooleanClassificationProblemImpl(this, heldOutPoints);
		}
	}
