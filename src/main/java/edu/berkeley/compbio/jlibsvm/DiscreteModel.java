package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.SvmPoint;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface DiscreteModel<T>
	{
	T predictLabel(SvmPoint x);
	}
