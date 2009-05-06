package edu.berkeley.compbio.jlibsvm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
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
