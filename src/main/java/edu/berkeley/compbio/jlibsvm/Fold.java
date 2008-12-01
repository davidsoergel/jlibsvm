package edu.berkeley.compbio.jlibsvm;

import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface Fold<L extends Comparable, P, R> extends SvmProblem<L, P>
	{
	Set<P> getHeldOutPoints();

	R asR();
	}
