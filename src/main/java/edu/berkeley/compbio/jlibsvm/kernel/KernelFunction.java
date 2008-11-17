package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.SvmPoint;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface KernelFunction
	{
	float evaluate(SvmPoint x, SvmPoint y);

	/**
	 * Report performance statistics of the kernel, e.g. how often it was evaluated (which should respond to the cache
	 * size).
	 *
	 * @return
	 */
	String perfString();
	}
