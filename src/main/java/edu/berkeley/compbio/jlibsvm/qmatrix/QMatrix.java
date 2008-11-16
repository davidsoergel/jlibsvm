package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.SvmPoint;


/**
 * Kernel evaluation
 * <p/>
 * the static method k_function is for doing single kernel evaluation
 *
 * the constructor of Kernel prepares to calculate the l*l kernel matrix
 *
 * the member function get_Q is for getting one column from the Q Matrix
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */

public interface QMatrix
	{
	float[] getQ(int column, int len);

	float[] getQD();

	void swapIndex(int i, int j);

	SvmPoint[] getVectors();
	}
