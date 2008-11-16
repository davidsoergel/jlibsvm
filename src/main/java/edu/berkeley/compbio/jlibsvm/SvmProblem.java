package edu.berkeley.compbio.jlibsvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class SvmProblem<T extends Comparable, P extends SvmProblem<? extends T, ? extends P>>
		implements java.io.Serializable
	{
	//public int l;

	//** would be nice to make examples = Map<SvmPoint[], T>, but extensive consequences...

	protected T[] targetValues;

	public T[] getTargetValues()
		{
		return targetValues;
		}

	//public abstract float[] targetValueAsFloat();

	//public float[] targetValues;
	public SvmPoint[] examples;

	// the unique set of targetvalues, in a defined order
	private List<T> labels = null;  // avoid populating for regression!  OK, regression should never call getLabels(), then.

	public SvmProblem(int numExamples)
		{
		examples = new SvmPoint[numExamples];
		}

	public int getNumLabels()
		{
		return getLabels().size();
		}

	public abstract P newSubProblem(int numExamples);

	public List<T> getLabels()
		{
		if (labels == null)
			{
			Set<T> uniq = new HashSet<T>(Arrays.asList(targetValues));
			labels = new ArrayList<T>(uniq);
			Collections.sort(labels);
			}
		return labels;
		}

	public GroupedClasses groupClasses(int[] perm)
		{
		int l = examples.length;
		//int max_nr_class = 16;
		int numberOfClasses = 0;
		//int[] label = new int[max_nr_class];
		//int[] count = new int[max_nr_class];

		List<T> label = new ArrayList<T>();
		List<Integer> count = new ArrayList<Integer>();

		int[] dataLabel = new int[l];
		int i;

		for (i = 0; i < l; i++)
			{
			T thisLabel = targetValues[i];
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
/*		nr_class_ret[0] = nr_class;
		label_ret[0] = label;
		start_ret[0] = start;
		count_ret[0] = count;*/
		}

	public void putTargetValue(int i, T x)
		{
		targetValues[i] = x;
		}

	public abstract void putTargetFloat(int i, Float x);

	public T getTargetValue(int i)
		{
		return targetValues[i];
		}


	public class GroupedClasses
		{
		public int numberOfClasses;
		public List<T> label;
		public List<Integer> count;
		public int[] start;

		public GroupedClasses(int numberOfClasses, List<T> label, int[] start, List<Integer> count)
			{
			this.numberOfClasses = numberOfClasses;
			this.label = label;
			this.start = start;
			this.count = count;
			}
		}

	private Map<T, Integer> exampleCounts = null;

	public Map<T, Integer> getExampleCounts()
		{
		if (exampleCounts == null)
			{
			// would be nice to use a google or apache Bag here, but trying to avoid dependencies
			exampleCounts = new HashMap<T, Integer>();
			for (int i = 0; i < examples.length; i++)
				{
				Integer c = exampleCounts.get(targetValues[i]);
				if (c == null)
					{
					c = 0;
					}
				c++;
				exampleCounts.put(targetValues[i], c);
				}
			}
		return exampleCounts;
		}

/*	public int countExamples(T label)
		{
		return Arrays.count(targetValues, label); //return targetValues.count(label);
		}*/
	}
