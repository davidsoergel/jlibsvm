package edu.berkeley.compbio.jlibsvm;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class CrossValidationResults
	{
	public abstract float accuracy();

	public abstract float accuracyGivenClassified();

	public abstract float unknown();

	public String toString()
		{
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("Cross Validation Classified = %.2f%%\n", 100.0 * unknown()));
		sb.append(String.format("Cross Validation Accuracy (of those classified) = %.2f%%\n",
		                        100.0 * accuracyGivenClassified()));
		sb.append(String.format("Cross Validation Accuracy (of total) = %.2f%%\n", 100.0 * accuracy()));
		return sb.toString();
		}
	}
