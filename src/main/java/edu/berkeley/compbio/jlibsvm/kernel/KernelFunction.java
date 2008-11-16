package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.SvmPoint;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public interface KernelFunction
	{
	float evaluate(SvmPoint x, SvmPoint y);

//	float evaluate(svm_node[] x, svm_node[] y, float xDotX, float yDotY);

	String perfString();
	}
