package edu.berkeley.compbio.jlibsvm.labelinverter;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface LabelInverter<L>
	{
// -------------------------- OTHER METHODS --------------------------

	L invert(L label);
	}
