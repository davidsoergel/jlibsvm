package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmProblem;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassProblem<T extends Comparable> extends SvmProblem<T, MultiClassProblem<T>>
	{
	Class type;

	/**
	 * For now, pending further cleanup, we need to create arrays of the label type.  That's impossible to do with generics
	 * alone, so we need to provide the class object (e.g., String.class or whatever) for the label type used.  Of course
	 * this should match the generics used on SvmProblem, etc.
	 *
	 * @param type
	 * @param length
	 */
	public MultiClassProblem(Class type, int length)
		{
		super(length);
		this.type = type;
		targetValues = (T[]) java.lang.reflect.Array.newInstance(type, length);
		}


	public MultiClassProblem<T> newSubProblem(int length)
		{
		return new MultiClassProblem<T>(type, length);
		}


	public void putTargetFloat(int i, Float x)
		{
		try
			{
			putTargetValue(i, (T) type.getConstructor(String.class).newInstance(x.toString()));
			}
		catch (Exception e)
			{
			throw new SvmException(e);
			}
		}
	}
