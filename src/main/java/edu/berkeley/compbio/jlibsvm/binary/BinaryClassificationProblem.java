package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.SvmProblem;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface BinaryClassificationProblem<L extends Comparable, P> extends SvmProblem<L, P>
	{
	Map<P, Boolean> getBooleanExamples();

	L getTrueLabel();

	L getFalseLabel();

	void setupLabels();
	}
