package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.labelinverter.LabelInverter;
import edu.berkeley.compbio.ml.cluster.BasicBatchCluster;
import edu.berkeley.compbio.ml.cluster.Clusterable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BatchClusterLabelInverter<T extends Clusterable<T>> implements LabelInverter<BasicBatchCluster<T>>
	{
// ------------------------------ FIELDS ------------------------------

	// cache the inversions so as not to always create new ones
	private final Map<BasicBatchCluster<T>, BasicBatchCluster<T>> inversions =
			new HashMap<BasicBatchCluster<T>, BasicBatchCluster<T>>();


// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface LabelInverter ---------------------

	public BasicBatchCluster<T> invert(final BasicBatchCluster<T> label)
		{
		BasicBatchCluster<T> result = inversions.get(label);
		if (result == null)
			{
			result = new BasicBatchCluster<T>(-label.getId());
			inversions.put(label, result);
			inversions.put(result, label);
			}
		return result;
		}
	}
