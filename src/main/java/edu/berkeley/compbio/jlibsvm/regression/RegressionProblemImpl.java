package edu.berkeley.compbio.jlibsvm.regression;

import edu.berkeley.compbio.jlibsvm.AbstractFold;
import edu.berkeley.compbio.jlibsvm.ExplicitSvmProblemImpl;
import edu.berkeley.compbio.jlibsvm.Fold;
import edu.berkeley.compbio.jlibsvm.SvmException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class RegressionProblemImpl<P> extends ExplicitSvmProblemImpl<Float, P, RegressionProblem<P>>
		implements RegressionProblem<P>
	{
// --------------------------- CONSTRUCTORS ---------------------------

	public RegressionProblemImpl(Map<P, Float> examples, HashMap<P, Integer> exampleIds)
		{
		super(examples, exampleIds);
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface SvmProblem ---------------------

	public List<Float> getLabels()
		{
		throw new SvmException("Shouldn't try to get unique target values for a regression problem");
		}

// -------------------------- OTHER METHODS --------------------------

	protected Fold<Float, P, RegressionProblem<P>> makeFold(Set<P> heldOutPoints)
		{
		return new RegressionFold(heldOutPoints);
		}

// -------------------------- INNER CLASSES --------------------------

	public class RegressionFold extends AbstractFold<Float, P, RegressionProblem<P>> implements RegressionProblem<P>
		{
// --------------------------- CONSTRUCTORS ---------------------------

		public RegressionFold(Set<P> heldOutPoints)
			{
			super(RegressionProblemImpl.this.getExamples(), heldOutPoints,
			      RegressionProblemImpl.this.getScalingModel());
			}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface SvmProblem ---------------------


		public Map<P, Integer> getExampleIds()
			{
			return RegressionProblemImpl.this.getExampleIds();
			}

		public int getId(P key)
			{
			return RegressionProblemImpl.this.getId(key);
			}

		public List<Float> getLabels()
			{
			return RegressionProblemImpl.this.getLabels();
			}

		public Float getTargetValue(P point)
			{
			assert !heldOutPoints.contains(point);  // we should never ask to cheat
			return RegressionProblemImpl.this.getTargetValue(point);
			}

// -------------------------- OTHER METHODS --------------------------

		public Set<Fold<Boolean, P, RegressionProblem<P>>> makeFolds(int numberOfFolds)
			{
			throw new NotImplementedException();
			}
		}
	}
