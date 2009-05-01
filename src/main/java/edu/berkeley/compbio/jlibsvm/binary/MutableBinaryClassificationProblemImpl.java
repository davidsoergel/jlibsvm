package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.MutableSvmProblem;
import edu.berkeley.compbio.jlibsvm.SvmException;

import java.util.HashMap;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MutableBinaryClassificationProblemImpl<L extends Comparable, P>
		extends BinaryClassificationProblemImpl<L, P>
		implements MutableSvmProblem<L, P, BinaryClassificationProblem<L, P>>
	{
// --------------------------- CONSTRUCTORS ---------------------------

	public MutableBinaryClassificationProblemImpl(Class labelClass, int numExamples)
		{
		super(labelClass, new HashMap<P, L>(numExamples), new HashMap<P, Integer>(numExamples));
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface MutableSvmProblem ---------------------

	public void addExample(P point, L label)
		{
		examples.put(point, label);
		exampleIds.put(point, exampleIds.size());
		}

	public void addExampleFloat(P point, Float x)
		{
		try
			{
			addExample(point, (L) labelClass.getConstructor(String.class).newInstance(x.toString()));
			}
		catch (Exception e)
			{
			throw new SvmException(e);
			}
		}
	}
