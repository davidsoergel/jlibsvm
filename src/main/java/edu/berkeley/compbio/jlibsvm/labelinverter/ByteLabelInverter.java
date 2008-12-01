package edu.berkeley.compbio.jlibsvm.labelinverter;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class ByteLabelInverter implements LabelInverter<Byte>
	{
	public Byte invert(Byte label)
		{
		return new Byte((byte) -label);
		}
	}
