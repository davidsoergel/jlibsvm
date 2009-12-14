package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BasicKernelQMatrix<P> extends KernelQMatrix<P>
	{
// --------------------------- CONSTRUCTORS ---------------------------

	public BasicKernelQMatrix(@NotNull KernelFunction<P> kernel, int numExamples, int maxCachedRank)
		{
		super(kernel, numExamples, maxCachedRank);
		}

// -------------------------- OTHER METHODS --------------------------

	public float computeQ(SolutionVector<P> a, SolutionVector<P> b)
		{
		return (float) kernel.evaluate(a.point, b.point);
		}
	}
