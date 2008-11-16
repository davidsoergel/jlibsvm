package edu.berkeley.compbio.jlibsvm;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SvmPoint implements java.io.Serializable
	{
	public int[] indexes;
	public float[] values;
	private static final float NOT_COMPUTED_YET = -1;
	private float squared = NOT_COMPUTED_YET;

	public SvmPoint(int dimensions)
		{
		indexes = new int[dimensions];
		values = new float[dimensions];
		}

	public float getSquared()
		{
		if (squared == -1)
			{
			squared = 0;
			int xlen = values.length;
			int i = 0;
			while (i < xlen)
				{
				squared += values[i] * values[i];
				++i;
				}
			}
		return squared;
		}
	}
