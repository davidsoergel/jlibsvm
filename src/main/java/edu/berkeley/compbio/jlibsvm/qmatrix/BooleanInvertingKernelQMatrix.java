package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BooleanInvertingKernelQMatrix<P> extends BasicKernelQMatrix<P>
	{
	/*	private final boolean[] targetValues;

   public SVC_Q(SvmProblem problem, KernelFunction kernel, float cache_size, boolean[] targetValues)
	   {
	   super(problem, kernel, cache_size);
	   this.targetValues =  targetValues.clone();
	   }


   float compute_Q(int i, int j)
	   {
	   return ((targetValues[i] == targetValues[j]) ? 1 : -1) * kernel.evaluate(x[i], x[j]);
	   }


   public void swapIndex(int i, int j)
	   {
	   super.swapIndex(i, j);

	   boolean _ = targetValues[i];
	   targetValues[i] = targetValues[j];
	   targetValues[j] = _;
	   }*/

	public BooleanInvertingKernelQMatrix(KernelFunction<P> kernel, int numExamples, int maxCachedRank)
		{
		super(kernel, numExamples, maxCachedRank);
		}


	Map<P, Boolean> examples;

	public float computeQ(SolutionVector<P> a, SolutionVector<P> b)
		{
		return (float) (((a.targetValue == b.targetValue) ? 1 : -1) * kernel.evaluate(a.point, b.point));
		}
	}
