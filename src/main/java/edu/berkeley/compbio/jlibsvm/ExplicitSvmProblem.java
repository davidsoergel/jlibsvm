package edu.berkeley.compbio.jlibsvm;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface ExplicitSvmProblem<L extends Comparable, P, R extends SvmProblem<L, P>> extends SvmProblem<L, P>
	{
	Map<P, L> getExamples();

	List<L> getLabels();

	L getTargetValue(P point);

	Set<Fold<L, P, R>> makeFolds(int numberOfFolds);
	}
