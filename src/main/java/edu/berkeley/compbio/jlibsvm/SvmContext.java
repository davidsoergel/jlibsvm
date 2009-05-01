package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.util.StringTokenizer;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SvmContext<L extends Comparable, P>
	{
// ------------------------------ FIELDS ------------------------------

	public KernelFunction<P> kernel;
	public SvmParameter<L> param;


// -------------------------- STATIC METHODS --------------------------

	public static float[] parseFloatArray(String s)
		{
		StringTokenizer st = new StringTokenizer(s);
		float[] result = new float[st.countTokens()];
		int i = 0;
		while (st.hasMoreTokens())
			{
			result[i] = Float.parseFloat(st.nextToken());
			i++;
			}
		return result;
		}

	public static int[] parseIntArray(String s)
		{
		StringTokenizer st = new StringTokenizer(s);
		int[] result = new int[st.countTokens()];
		int i = 0;
		while (st.hasMoreTokens())
			{
			result[i] = Integer.parseInt(st.nextToken());
			i++;
			}
		return result;
		}

// --------------------------- CONSTRUCTORS ---------------------------

	public SvmContext()
		{
		}

	public SvmContext(KernelFunction<P> kernel, SvmParameter<L> param)
		{
		this.kernel = kernel;
		this.param = param;
		}
	}
