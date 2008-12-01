package edu.berkeley.compbio.jlibsvm.kernel;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface KernelFunction<P>
	{
	//float evaluate(SvmPoint x, SvmPoint y);

	double evaluate(P x, P y);

	//double evaluateD(P x, P y);

	/**
	 * Report performance statistics of the kernel, e.g. how often it was evaluated (which should respond to the cache
	 * size).
	 *
	 * @return
	 */
	//String perfString();
	}
