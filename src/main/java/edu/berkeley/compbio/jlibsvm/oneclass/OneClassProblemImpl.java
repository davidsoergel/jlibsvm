package edu.berkeley.compbio.jlibsvm.oneclass;

import edu.berkeley.compbio.jlibsvm.regression.RegressionProblemImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class OneClassProblemImpl<L, P> extends RegressionProblemImpl<P> implements OneClassProblem<L, P>
	{
// ------------------------------ FIELDS ------------------------------

	L label;


// --------------------------- CONSTRUCTORS ---------------------------

	public OneClassProblemImpl(Map<P, Float> examples, HashMap<P, Integer> exampleIds, L label)  // set<P> examples
		{
		super(examples, exampleIds);
		this.label = label;
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	public L getLabel()
		{
		return label;
		}
	}
