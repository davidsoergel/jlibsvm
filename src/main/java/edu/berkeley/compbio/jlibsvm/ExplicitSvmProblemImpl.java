package edu.berkeley.compbio.jlibsvm;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class ExplicitSvmProblemImpl<L extends Comparable, P, R extends SvmProblem<L, P>>
		extends AbstractSvmProblem<L, P, R> implements ExplicitSvmProblem<L, P, R>
		//	implements java.io.Serializable
	{
	protected int numExamples = 0;

	/*	protected L targetValues;

   public L[] getTargetValues()
	   {
	   return targetValues;
	   }*/

	protected ExplicitSvmProblemImpl(@NotNull Map<P, L> examples, @NotNull Map<P, Integer> exampleIds)
		{
		this.examples = examples;
		this.exampleIds = exampleIds;
		}

	public Map<P, L> examples;
	public Map<P, Integer> exampleIds; // maintain a known order

	@NotNull
	public Map<P, L> getExamples()
		{
		return examples;
		}

	public int getId(P key)
		{
		return exampleIds.get(key);
		}

	public int getNumExamples()
		{
		if (examples == null)
			{
			return numExamples;
			}

		return examples.size();
		}

	@NotNull
	public Map<P, Integer> getExampleIds()
		{
		return exampleIds;
		}

	// the unique set of targetvalues, in a defined order
	protected List<L> labels = null;
	// avoid populating for regression!  OK, regression should never call getLabels(), then.

	/*public int getNumLabels()
		{
		return getLabels().size();
		}

	public int getNumExamples()
		{
		return examples.size();
		}*/

	//	public abstract R newSubProblem(int numExamples);

	public List<L> getLabels()
		{
		if (labels == null)
			{
			if (examples.isEmpty())
				{
				return null;
				}
			Set<L> uniq = new HashSet<L>(examples.values()); //Arrays.asList(targetValues));
			labels = new ArrayList<L>(uniq);
			Collections.sort(labels);
			}
		return labels;
		}
	/*
	 public GroupedClasses groupClasses(int[] perm)
		 {
		 int l = examples.size();
		 int numberOfClasses = 0;
		 List<L> label = new ArrayList<L>();
		 List<Integer> count = new ArrayList<Integer>();

		 int[] dataLabel = new int[l];
		 int i;

		 for (Map.Entry<P, L> entry : examples.entrySet())
	 //	for (i = 0; i < l; i++)
			 {
			 L thisLabel = entry.getValue(); //targetValues[i];
			 int j;
			 for (j = 0; j < numberOfClasses; j++)
				 {
				 if (thisLabel.equals(label.get(j)))
					 {
					 count.set(j, count.get(j) + 1);
					 break;
					 }
				 }
			 dataLabel[i] = j;
			 if (j == numberOfClasses)
				 {
				 label.add(thisLabel);
				 count.add(1);
				 ++numberOfClasses;
				 }
			 }

		 int[] start = new int[numberOfClasses];
		 start[0] = 0;
		 for (i = 1; i < numberOfClasses; i++)
			 {
			 start[i] = start[i - 1] + count.get(i - 1);
			 }
		 for (i = 0; i < l; i++)
			 {
			 perm[start[dataLabel[i]]] = i;
			 ++start[dataLabel[i]];
			 }
		 start[0] = 0;
		 for (i = 1; i < numberOfClasses; i++)
			 {
			 start[i] = start[i - 1] + count.get(i - 1);
			 }

		 return new GroupedClasses(numberOfClasses, label, start, count);
		 }
 */

	public L getTargetValue(P point)
		{
		return examples.get(point); //targetValues[i];
		}

	public Set<Fold<L, P, R>> makeFolds(int numberOfFolds)
		{
		Set<Fold<L, P, R>> result = new HashSet<Fold<L, P, R>>();

		List<P> points = new ArrayList<P>(getExamples().keySet());

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

		for (Set<P> heldOutPoints : heldOutPointSets)
			{
			result.add(makeFold(heldOutPoints));
			}

		return result;
		}

	protected abstract Fold<L, P, R> makeFold(Set<P> heldOutPoints);
	}
