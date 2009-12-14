package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BooleanInvertingKernelQMatrix<P> extends BasicKernelQMatrix<P>
	{
// --------------------------- CONSTRUCTORS ---------------------------

	public BooleanInvertingKernelQMatrix(@NotNull KernelFunction<P> kernel, int numExamples, int maxCachedRank)
		{
		super(kernel, numExamples, maxCachedRank);
		}

// -------------------------- OTHER METHODS --------------------------

	public float computeQ(SolutionVector<P> a, SolutionVector<P> b)
		{
		return (float) (((a.targetValue == b.targetValue) ? 1 : -1) * kernel.evaluate(a.point, b.point));
		}
	}
