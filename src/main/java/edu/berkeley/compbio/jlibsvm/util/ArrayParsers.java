package edu.berkeley.compbio.jlibsvm.util;

import java.util.StringTokenizer;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class ArrayParsers
	{
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
	}
