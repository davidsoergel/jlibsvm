package edu.berkeley.compbio.jlibsvm;

import com.google.common.collect.Multiset;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An SVM problem consisting of a mapping of training examples to labels.
 * <p/>
 * The generic parameters are L, the label type; P, the type of objects to be classified, and R, the concrete type of the problem itself.
 * <p/>
 * An SVM problem
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface SvmProblem<L extends Comparable, P, R> {
// -------------------------- OTHER METHODS --------------------------

    Multiset<L> getExampleCounts();

    Map<P, Integer> getExampleIds();

    Map<P, L> getExamples();

    int getId(P key);

    List<L> getLabels();

    int getNumExamples();

    ScalingModel<P> getScalingModel();

    L getTargetValue(P point);

    Iterator<R> makeFolds(int numberOfFolds);


//	R asR();

    Set<P> getHeldOutPoints();
}
