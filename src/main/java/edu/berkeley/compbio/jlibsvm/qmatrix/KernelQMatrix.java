package edu.berkeley.compbio.jlibsvm.qmatrix;

import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;
import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class KernelQMatrix<P> implements QMatrix<P>
	{
//	private SymmetricSoftHashMap2d<SolutionVector<P>, Float> cache =
//			new SymmetricSoftHashMap2d<SolutionVector<P>, Float>();

	private RecentActivitySquareCache cache;

	protected KernelFunction<P> kernel;

	KernelQMatrix(KernelFunction<P> kernel, int numExamples, int cacheRowsSquared)
		{
		this.kernel = kernel;
		this.cache = new RecentActivitySquareCache(numExamples, cacheRowsSquared);
		}

	public abstract float computeQ(SolutionVector<P> a, SolutionVector<P> b);

	public void maintainCache(Collection<SolutionVector<P>> activeSet)
		{
		List<Integer> activeSvIds = new ArrayList<Integer>(activeSet.size());
		for (SolutionVector<P> solutionVector : activeSet)
			{
			activeSvIds.add(solutionVector.id);
			}
		cache.maintainCache(activeSvIds);
		}

	public final float evaluate(SolutionVector<P> a, SolutionVector<P> b)
		{
		return cache.get(a, b);
/*		Float result = cache.get(a, b);
		if (result == null)
			{
			result = computeQ(a, b);
			cache.put(a, b, result);
			}
		return result;*/
		}

	public String perfString()
		{
		return cache.toString();
		}
	/*	protected Map<P, L> examples;
   protected float[] QD;



   public List<P> getVectors()
	   {
	   return x;
	   }


   public void swapIndex(int i, int j)
	   {
	   P swapnode = x[i];
	   x[i] = x[j];
	   x[j] = swapnode;

	   float swap = QD[i];
	   QD[i] = QD[j];
	   QD[j] = swap;
	   }

   public float[] getQD()
	   {
	   return QD;
	   }*/


	private class SymmetricSoftHashMap2d<K, V>
		{
		//ReferenceMap<K, Map<K, V>> l1Map = new ReferenceMap<K, Map<K, V>>(ReferenceType.SOFT, ReferenceType.SOFT);
		HashMap<K, Map<K, V>> l1Map = new HashMap<K, Map<K, V>>();//ReferenceType.SOFT, ReferenceType.SOFT);
		int hit = 0;
		int miss = 0;
		int size = 0;

		V get(K k1, K k2)
			{
			if (k1.hashCode() > k2.hashCode())
				{
				K k3 = k1;
				k1 = k2;
				k2 = k3;
				}

			Map<K, V> l2Map = l1Map.get(k1);
			if (l2Map == null)
				{
				l2Map = new HashMap<K, V>(); //ReferenceMap<K, V>(ReferenceType.SOFT, ReferenceType.SOFT);
				l1Map.put(k1, l2Map);
				}

			V result = l2Map.get(k2);
			if (result != null)
				{
				hit++;
				}
			else
				{
				miss++;
				}
			return result;
			}

		public void put(K k1, K k2, V value)
			{
			if (k1.hashCode() > k2.hashCode()) //k1.compareTo(k2) > 0)
				{
				K k3 = k1;
				k1 = k2;
				k2 = k3;
				}

			Map<K, V> l2Map = l1Map.get(k1);
			if (l2Map == null)
				{
				l2Map = new ReferenceMap<K, V>(ReferenceType.SOFT, ReferenceType.SOFT);
				l1Map.put(k1, l2Map);
				}

			l2Map.put(k2, value);
			size++;
			}

		public String toString()
			{
			return "QMatrix hits = " + hit + ", misses = " + miss + ", rate = " + (float) hit / (float) (hit + miss)
					+ ", size = " + size;
			}
		}

	private class RecentActivitySquareCache
		{
		private final Logger logger = Logger.getLogger(RecentActivitySquareCache.class);

		// map from the SV index to the order of most recent activity (most active SVs first).
		int[] svIdToRank;
		int[] rankToSvIdA;
		int[] rankToSvIdB;

		int[] rankToCacheIndexA;
		int[] rankToCacheIndexB;

		float[][] data;

		float[] diagonal;

		int numExamples;
		int maxCachedRank;

		int hits = 0;
		int misses = 0;
		int widemisses = 0;
		int diagonalhits = 0;
		int diagonalmisses = 0;

		public RecentActivitySquareCache(int numExamples, int cacheRowsSquared)
			{
			this.numExamples = numExamples;

			// how big should the cache really be
			maxCachedRank = Math.min(numExamples, cacheRowsSquared);

			// allocate rank maps
			svIdToRank = new int[numExamples];
			rankToSvIdA = new int[numExamples];
			rankToSvIdB = new int[numExamples];
			rankToCacheIndexA = new int[maxCachedRank];
			rankToCacheIndexB = new int[maxCachedRank];

			// initialize the ranks to the SV order
			for (int i = 0; i < numExamples; i++)
				{
				svIdToRank[i] = i;
				rankToSvIdA[i] = i;
				}

			// initialize the cache to the rank order
			for (int i = 0; i < maxCachedRank; i++)
				{
				rankToCacheIndexA[i] = i;
				}

			// allocate and compute diagonal

			diagonal = new float[numExamples];
			Arrays.fill(diagonal, NOTCACHED);

			// allocate triangular cache
			data = new float[maxCachedRank][];
			for (int i = 0; i < maxCachedRank; i++)
				{
				data[i] = new float[i];
				Arrays.fill(data[i], NOTCACHED);
				}
			}

		public void maintainCache(Collection<Integer> activeSvIds)
			{
			//	System.err.println(this);

			// sort the rank array so that all the active SVs come before the inactive SVs,
			// but leave the order otherwise unaffected (so it maintains memory of the previous activity)

			if (activeSvIds.size() > maxCachedRank)
				{
				logger.warn("Active set of " + activeSvIds.size() + " SVs doesn't fit in cache of size " + maxCachedRank
						+ ", try increasing it");
				}

			// stable partitioning of the ranks.

			int newActiveRank = 0;
			int newInactiveRank = activeSvIds.size();

			// just go through in the old rank order.

			// first deal with those that have cache entries

			for (int rank = 0; rank < maxCachedRank; rank++)
				{
				int svid = rankToSvIdA[rank];

				// figure out the new rank
				int newRank;
				if (activeSvIds.contains(svid))
					{
					newRank = newActiveRank;
					newActiveRank++;
					}
				else
					{
					newRank = newInactiveRank;
					newInactiveRank++;
					}

				svIdToRank[svid] = newRank;
				rankToSvIdB[newRank] = svid;
				// note we need to leave rankToSvIdA intact as the old version for now

				//  reorder the cache mapping
				if (newRank < maxCachedRank)
					{
					// previously cached; still cached; maybe the cache order has changed though
					rankToCacheIndexB[newRank] = rankToCacheIndexA[rank];
					}
				else
					{
					// previously cached, but no longer, so the cache entries are now invalid
					invalidate(rankToCacheIndexA[rank]);
					}
				}

			// then the rest that weren't previously in the cache; identical to above without the rankToCacheIndex part
			for (int rank = maxCachedRank; rank < numExamples; rank++)
				{
				int svid = rankToSvIdA[rank];

				// figure out the new rank
				int newRank;
				if (activeSvIds.contains(svid))
					{
					newRank = newActiveRank;
					newActiveRank++;
					}
				else
					{
					newRank = newInactiveRank;
					newInactiveRank++;
					}

				svIdToRank[svid] = newRank;
				rankToSvIdB[newRank] = svid;
				// note we need to leave rankToSvIdA intact as the old version for now

				//  reorder the cache mapping
				if (newRank < maxCachedRank)
					{
					// previously not cached; need to allocate a new row
					rankToCacheIndexB[newRank] = allocateRow();
					}
				else
					{
					// previously not cached; still not cached; do nothing
					}
				}

			// swap the rankToSvId arrays
			int[] rankToSvIdSwap = rankToSvIdA;
			rankToSvIdA = rankToSvIdB;  // A is now the valid one
			rankToSvIdB =
					rankToSvIdSwap;  // B is now the old one, useless except it's already allocated, will be overwritten on the next round


			// swap the rankToCache arrays
			int[] rankToCacheIndexSwap = rankToCacheIndexA;
			rankToCacheIndexA = rankToCacheIndexB;  // A is now the valid one
			rankToCacheIndexB =
					rankToCacheIndexSwap;  // B is now the old one, useless except it's already allocated, will be overwritten on the next round
			}

		private Queue<Integer> availableRows = new LinkedList<Integer>();

		private void invalidate(int cacheIndex)
			{
			Arrays.fill(data[cacheIndex], NOTCACHED);
			for (int i = cacheIndex; i < maxCachedRank; i++)
				{
				data[i][cacheIndex] = NOTCACHED;
				}
			availableRows.add(cacheIndex);
			}

		private int allocateRow()
			{
			return availableRows.remove();
			}

		public final static float NOTCACHED = Float.NEGATIVE_INFINITY;

		public float get(SolutionVector<P> a, SolutionVector<P> b)
			{
			if (a == b)
				{
				float result = diagonal[a.id];
				if (result == NOTCACHED)
					{
					result = computeQ(a, b);
					diagonal[a.id] = result;
					diagonalmisses++;
					}
				else
					{
					diagonalhits++;
					}
				return result;
				}

			int rank1 = svIdToRank[a.id];
			int rank2 = svIdToRank[b.id];

			assert rank1 != rank2;

			if (rank1 >= maxCachedRank || rank2 >= maxCachedRank)
				{
				//return NOTCACHED;
				widemisses++;
				return computeQ(a, b);
				}

			int cacheIndex1 = rankToCacheIndexA[rank1];
			int cacheIndex2 = rankToCacheIndexA[rank2];

			if (cacheIndex2 > cacheIndex1)
				{
				int cacheIndexTmp = cacheIndex1;
				cacheIndex1 = cacheIndex2;
				cacheIndex2 = cacheIndexTmp;
				}

			float result = data[cacheIndex1][cacheIndex2];
			if (result == NOTCACHED)
				{
				result = computeQ(a, b);
				data[cacheIndex1][cacheIndex2] = result;
				misses++;
				}
			else
				{
				//assert result == computeQ(a, b);
				hits++;
				}
			return result;
			}

		public String toString()
			{
			return "QMatrix hits = " + hits + ", misses = " + misses + ", widemisses = " + widemisses
					+ ", diagonalhits = " + diagonalhits + ", diagonalmisses = " + diagonalmisses + ", rate = "
					+ (float) (hits + diagonalhits) / (float) (hits + diagonalhits + misses + widemisses
					+ diagonalmisses) + ", size = " + data.length;
			}
		}
	}
