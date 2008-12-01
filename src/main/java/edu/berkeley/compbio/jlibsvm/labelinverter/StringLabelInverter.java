package edu.berkeley.compbio.jlibsvm.labelinverter;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class StringLabelInverter implements LabelInverter<String>
	{
	public String invert(String label)
		{
		return "Not " + label;
		}
	}