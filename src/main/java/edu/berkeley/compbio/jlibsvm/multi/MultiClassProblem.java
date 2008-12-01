package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.SvmProblem;
import edu.berkeley.compbio.jlibsvm.labelinverter.LabelInverter;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface MultiClassProblem<L extends Comparable, P>//, R extends SvmProblem<L,P>>
		extends SvmProblem<L, P>
	{
//	void addExampleFloat(P point, Float x);

	Map<L, Set<P>> getExamplesByLabel();

	LabelInverter<L> getLabelInverter();

	Class getLabelClass();
	}
