package edu.berkeley.compbio.jlibsvm;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class AbstractSvmProblem<L extends Comparable, P, R extends SvmProblem<L, P>>
		implements SvmProblem<L, P>
	{/*	public class GroupedClasses
			{
			public int numberOfClasses;
			public List<L> label;
			public List<Integer> count;
			public int[] start;

			public GroupedClasses(int numberOfClasses, List<L> label, int[] start, List<Integer> count)
				{
				this.numberOfClasses = numberOfClasses;
				this.label = label;
				this.start = start;
				this.count = count;
				}
			}
	*/
	protected Map<L, Integer> exampleCounts = null;

	public Map<L, Integer> getExampleCounts()
		{
		if (exampleCounts == null)
			{
			// would be nice to use a google or apache Bag here, but trying to avoid dependencies
			exampleCounts = new HashMap<L, Integer>();
			for (Map.Entry<P, L> entry : getExamples().entrySet())
				{
				Integer c = exampleCounts.get(entry.getValue());
				if (c == null)
					{
					c = 0;
					}
				c++;
				exampleCounts.put(entry.getValue(), c);
				}
			}
		return exampleCounts;
		}
	}
