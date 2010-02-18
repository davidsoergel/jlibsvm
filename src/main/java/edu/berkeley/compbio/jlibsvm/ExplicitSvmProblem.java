package edu.berkeley.compbio.jlibsvm;

import java.util.List;
import java.util.Map;

/**
 * This may seem pointless, but it helps with the generics spaghetti by constraining the type R.
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
@Deprecated
public interface ExplicitSvmProblem<L extends Comparable, P, R extends SvmProblem<L, P, R>> extends SvmProblem<L, P, R>
	{
// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface SvmProblem ---------------------

	Map<P, L> getExamples();

	List<L> getLabels();

	L getTargetValue(P point);


// -------------------------- OTHER METHODS --------------------------

	//Set<Fold<L, P,R>> makeFolds(int numberOfFolds);
	}
