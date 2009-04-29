package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BooleanClassificationProblemImpl<L extends Comparable, P> extends BinaryClassificationProblemImpl<L, P>
	{

	private Map<P, Boolean> booleanExamples;
	private Set<P> trueExamples;
	private Set<P> falseExamples;

	public BooleanClassificationProblemImpl(Class labelClass, L trueLabel, Set<P> trueExamples, L falseLabel,
	                                        Set<P> falseExamples, Map<P, Integer> exampleIds)
		{
		// this is a hack: we leave examples==null and just deal with booleanExamples directly

		super(labelClass, null, exampleIds);
		this.falseLabel = falseLabel;
		this.trueLabel = trueLabel;
		this.trueExamples = trueExamples;
		this.falseExamples = falseExamples;

		labels = new ArrayList<L>(2);
		labels.add(trueLabel);
		labels.add(falseLabel);

		numExamples = trueExamples.size() + falseExamples.size();

		exampleCounts = new HashMap<L, Integer>(2);
		exampleCounts.put(trueLabel, trueExamples.size());
		exampleCounts.put(falseLabel, falseExamples.size());
		}

	public void setupLabels()
		{
		// the constructor already dealt with this
		}

	public L getTargetValue(P point)
		{

		if (booleanExamples.get(point))
			{
			return trueLabel;
			}
		else
			{
			return falseLabel;
			}
		}


	public Map<P, Boolean> getBooleanExamples()
		{
		if (booleanExamples == null)
			{
			booleanExamples = new HashMap<P, Boolean>(numExamples);
			for (P trueExample : trueExamples)
				{
				booleanExamples.put(trueExample, Boolean.TRUE);
				}
			for (P falseExample : falseExamples)
				{
				booleanExamples.put(falseExample, Boolean.FALSE);
				}
			assert booleanExamples.size() == numExamples;
			}
		return booleanExamples;
		}


	/**
	 * There's no sense in scaling Boolean values, so this is a noop.  note we don't make a copy for efficiency.
	 *
	 * @param scalingModelLearner
	 * @return
	 */
	public BinaryClassificationProblem<L, P> getScaledCopy(@NotNull ScalingModelLearner<P> scalingModelLearner)
		{
		return this;
		}
	}
