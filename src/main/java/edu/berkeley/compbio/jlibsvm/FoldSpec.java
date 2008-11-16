package edu.berkeley.compbio.jlibsvm;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
* @version $Id$
*/
public class FoldSpec
	{
	public int[] foldStart;
	public int[] perm;

	public FoldSpec(int length, int numberOfFolds)
		{
		foldStart = new int[numberOfFolds + 1];
		perm = new int[length];
		}
	}
