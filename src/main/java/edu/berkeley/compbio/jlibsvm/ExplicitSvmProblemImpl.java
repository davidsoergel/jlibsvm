package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class ExplicitSvmProblemImpl<L extends Comparable, P, R extends SvmProblem<L, P>>
		extends AbstractSvmProblem<L, P, R> implements ExplicitSvmProblem<L, P, R>
	{
// ------------------------------ FIELDS ------------------------------

	public Map<P, L> examples;
	public Map<P, Integer> exampleIds; // maintain a known order


	public ScalingModel<P> scalingModel = new NoopScalingModel<P>();
	protected int numExamples = 0;

	/**
	 * the unique set of targetvalues, in a defined order avoid populating for regression!  OK, regression should never
	 * call getLabels(), then.
	 */
	protected List<L> labels = null;


// --------------------------- CONSTRUCTORS ---------------------------

	protected ExplicitSvmProblemImpl(Map<P, L> examples, @NotNull Map<P, Integer> exampleIds)
		{
		this.examples = examples;
		this.exampleIds = exampleIds;
		}

	protected ExplicitSvmProblemImpl(@NotNull Map<P, L> examples, @NotNull Map<P, Integer> exampleIds,
	                                 @NotNull ScalingModel<P> scalingModel)
		{
		this.examples = examples;
		this.exampleIds = exampleIds;
		this.scalingModel = scalingModel;
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	@NotNull
	public Map<P, Integer> getExampleIds()
		{
		return exampleIds;
		}

	@NotNull
	public Map<P, L> getExamples()
		{
		return examples;
		}

	public List<L> getLabels()
		{
		if (labels == null)
			{
			if (examples.isEmpty())
				{
				return null;
				}
			Set<L> uniq = new HashSet<L>(examples.values());
			labels = new ArrayList<L>(uniq);
			Collections.sort(labels);
			}
		return labels;
		}

	@NotNull
	public ScalingModel<P> getScalingModel()
		{
		return scalingModel;
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ExplicitSvmProblem ---------------------


	public Set<Fold<L, P, R>> makeFolds(int numberOfFolds)
		{
		Set<Fold<L, P, R>> result = new HashSet<Fold<L, P, R>>();

		List<P> points = new ArrayList<P>(getExamples().keySet());

		Collections.shuffle(points);

		// PERF this is maybe overwrought, but ensures the best possible balance among folds (unlike examples.size() / numberOfFolds)

		List<Set<P>> heldOutPointSets = new ArrayList<Set<P>>();
		for (int i = 0; i < numberOfFolds; i++)
			{
			heldOutPointSets.add(new HashSet<P>());
			}

		int f = 0;
		for (P point : points)
			{
			heldOutPointSets.get(f).add(point);
			f++;
			f %= numberOfFolds;
			}

		for (Set<P> heldOutPoints : heldOutPointSets)
			{
			result.add(makeFold(heldOutPoints));
			}

		return result;
		}

// --------------------- Interface SvmProblem ---------------------

	public int getId(P key)
		{
		return exampleIds.get(key);
		}

	public int getNumExamples()
		{
		if (examples == null)
			{
			return numExamples;
			}

		return examples.size();
		}

	public L getTargetValue(P point)
		{
		return examples.get(point);
		}

// -------------------------- OTHER METHODS --------------------------

	protected abstract Fold<L, P, R> makeFold(Set<P> heldOutPoints);
	}
