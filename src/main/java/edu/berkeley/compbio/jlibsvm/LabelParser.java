package edu.berkeley.compbio.jlibsvm;

/**
 * An object that can map strings to classification labels of the generic type L.
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface LabelParser<L>
	{
	L parse(String s);
	}
