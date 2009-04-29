package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;
import edu.berkeley.compbio.jlibsvm.util.SubtractionMap;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class AbstractFold<L extends Comparable, P, R extends SvmProblem<L, P>>
		extends AbstractSvmProblem<L, P, R> implements Fold<L, P, R>
	{

//	SvmProblem<L, P> fullProblem;

	protected Set<P> heldOutPoints = new HashSet<P>();
	private Map<P, L> subtractionMap;
	private int numExamples;

	public AbstractFold(Map<P, L> allExamples, Set<P> heldOutPoints, @NotNull ScalingModel<P> scalingModel)
		{
		//	this.fullProblem = fullProblem;
		this.heldOutPoints = heldOutPoints;
		subtractionMap = new SubtractionMap(allExamples, heldOutPoints);
		numExamples = subtractionMap.size();
		this.scalingModel = scalingModel;
		}


	public R asR()
		{
		return (R) this;
		}

	public Map<P, L> getExamples()
		{
		return subtractionMap;
		}

	public int getNumExamples()
		{
		return numExamples;
		}

	public Set<P> getHeldOutPoints()
		{
		return heldOutPoints;
		}


	public ScalingModel<P> scalingModel = new NoopScalingModel<P>();

	@NotNull
	public ScalingModel<P> getScalingModel()
		{
		return scalingModel;
		}
	}
