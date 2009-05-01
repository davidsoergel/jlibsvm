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
// ------------------------------ FIELDS ------------------------------

	public ScalingModel<P> scalingModel = new NoopScalingModel<P>();


	protected Set<P> heldOutPoints = new HashSet<P>();
	private Map<P, L> subtractionMap;
	private int numExamples;


// --------------------------- CONSTRUCTORS ---------------------------

	public AbstractFold(Map<P, L> allExamples, Set<P> heldOutPoints, @NotNull ScalingModel<P> scalingModel)
		{
		this.heldOutPoints = heldOutPoints;
		subtractionMap = new SubtractionMap(allExamples, heldOutPoints);
		numExamples = subtractionMap.size();
		this.scalingModel = scalingModel;
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	public Set<P> getHeldOutPoints()
		{
		return heldOutPoints;
		}

	public int getNumExamples()
		{
		return numExamples;
		}

	@NotNull
	public ScalingModel<P> getScalingModel()
		{
		return scalingModel;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Fold ---------------------

	public R asR()
		{
		return (R) this;
		}

// --------------------- Interface SvmProblem ---------------------


	public Map<P, L> getExamples()
		{
		return subtractionMap;
		}
	}
