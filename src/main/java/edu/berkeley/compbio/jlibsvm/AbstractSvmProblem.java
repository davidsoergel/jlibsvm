package edu.berkeley.compbio.jlibsvm;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class AbstractSvmProblem<L extends Comparable, P, R extends SvmProblem<L, P>>
		implements SvmProblem<L, P>
	{
// ------------------------------ FIELDS ------------------------------

	protected Multiset<L> exampleCounts = null;


// --------------------- GETTER / SETTER METHODS ---------------------

	public Multiset<L> getExampleCounts()
		{
		if (exampleCounts == null)
			{
			exampleCounts = new HashMultiset<L>();
			exampleCounts.addAll(getExamples().values());
			}
		return exampleCounts;
		}
	}
