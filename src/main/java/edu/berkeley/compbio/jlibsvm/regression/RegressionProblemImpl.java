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
	public List<Float> getLabels()
		{
		throw new SvmException("Shouldn't try to get unique target values for a regression problem");
		}

	public RegressionProblemImpl(Map<P, Float> examples, HashMap<P, Integer> exampleIds)
		{
		super(examples, exampleIds);
		//	targetValues = new Float[numExamples];
		}

	protected Fold<Float, P, RegressionProblem<P>> makeFold(Set<P> heldOutPoints)
		{
		return new RegressionFold(//this,
		                          heldOutPoints);
		}


	public class RegressionFold extends AbstractFold<Float, P, RegressionProblem<P>> implements RegressionProblem<P>
		{
		//	private BinaryClassificationProblemImpl<P> fullProblem;

		public RegressionFold(//BinaryClassificationProblemImpl<P> fullProblem,
		                      Set<P> heldOutPoints)
			{
			super(RegressionProblemImpl.this.getExamples(), heldOutPoints,
			      RegressionProblemImpl.this.getScalingModel());
			//this.fullProblem = fullProblem;
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

		public Set<Fold<Boolean, P, RegressionProblem<P>>> makeFolds(int numberOfFolds)
			{
			throw new NotImplementedException();
			}

		public int getId(P key)
			{
			return RegressionProblemImpl.this.getId(key);
			}

		public Map<P, Integer> getExampleIds()
			{
			return RegressionProblemImpl.this.getExampleIds();
			}
		}
	}
