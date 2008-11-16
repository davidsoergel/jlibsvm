package edu.berkeley.compbio.jlibsvm;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SvmException extends RuntimeException
	{
	public SvmException()
		{
		}

	public SvmException(String s)
		{
		super(s);
		}

	public SvmException(String s, Throwable throwable)
		{
		super(s, throwable);
		}

	public SvmException(Throwable throwable)
		{
		super(throwable);
		}
	}
