package edu.berkeley.compbio.jlibsvm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface SvmProblem<L extends Comparable, P> //, R extends SvmProblem<L,  P,  R>>
	{
	Map<P, L> getExamples();

	int getId(P key);
	//int getNumLabels();

	//int getNumExamples();

	List<L> getLabels();

	L getTargetValue(P point);

	Map<L, Integer> getExampleCounts();
	}
