package edu.berkeley.compbio.jlibsvm;

import com.google.common.collect.Multiset;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface SvmProblem<L extends Comparable, P, R>
	{
// -------------------------- OTHER METHODS --------------------------

	Multiset<L> getExampleCounts();

	Map<P, Integer> getExampleIds();

	Map<P, L> getExamples();

	int getId(P key);

	List<L> getLabels();

	int getNumExamples();

	ScalingModel<P> getScalingModel();

	L getTargetValue(P point);

	Set<R> makeFolds(int numberOfFolds);


//	R asR();

	Set<P> getHeldOutPoints();
	}
