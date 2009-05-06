package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.SolutionVector;

import java.util.Collection;


/**
 * Kernel evaluation
 * <p/>
 * the static method k_function is for doing single kernel evaluation
 * <p/>
 * the constructor of Kernel prepares to calculate the l*l kernel matrix
 * <p/>
 * the member function get_Q is for getting one column from the Q Matrix
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */

public interface QMatrix<P>
	{
// -------------------------- OTHER METHODS --------------------------

//	float[] getQ(int column, int len);
//	float[] getQD();
//	void swapIndex(int i, int j);

//	List<P> getVectors();

	//float evaluate(SolutionVector<P> a, SolutionVector<P> b);

	float evaluateDiagonal(SolutionVector<P> a);

	void getQ(SolutionVector<P> svA, SolutionVector<P>[] active, float[] buf);

	void getQ(SolutionVector<P> svA, SolutionVector<P>[] active, SolutionVector<P>[] inactive, float[] buf);

//	void storeRanks(Collection<SolutionVector<P>> allExamples);

	void initRanks(Collection<SolutionVector<P>> allExamples);

	void maintainCache(SolutionVector<P>[] active, SolutionVector<P>[] newlyInactive);

	public String perfString();
	}
