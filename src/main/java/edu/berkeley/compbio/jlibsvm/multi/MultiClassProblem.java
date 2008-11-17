package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmProblem;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassProblem<T extends Comparable> extends SvmProblem<T, MultiClassProblem<T>>
	{
/*	public float[] targetValueAsFloat()
		{
		float[] result = new float[targetValues.length];
		int i = 0;

		List<T> uniq = getLabels();

		for (T targetValue : targetValues)
			{
			result[i] = (float)uniq.indexOf(targetValue);
			i++;
			}
		return result;
		}*/

	Class type;

	public MultiClassProblem(Class type, int length)
		{
		super(length);
		this.type = type;
		//Class<?> type = getClass().getComponentType();
		targetValues = (T[]) java.lang.reflect.Array.newInstance(type, length);
//(T[]) new Object[length];
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
