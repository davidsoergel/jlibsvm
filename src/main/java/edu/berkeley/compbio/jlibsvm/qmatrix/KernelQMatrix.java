package edu.berkeley.compbio.jlibsvm.qmatrix;

import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;
import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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

	KernelQMatrix(KernelFunction<P> kernel, int numExamples, int cacheRows)
		{
		this.kernel = kernel;
		this.cache = new RecentActivitySquareCache(numExamples, cacheRows);
/*		idToRankMap = new int[numExamples];

		for (int i = 0; i < numExamples; i++)
			{
			idToRankMap[i] = i;
			}*/
		}

	public abstract float computeQ(SolutionVector<P> a, SolutionVector<P> b);

	public void maintainCache(SolutionVector<P>[] active, SolutionVector<P>[] newlyInactive)
		{
		//List<Integer> activeSvIds = new ArrayList<Integer>(active.length);
		/*int[] activeSvIds = new int[active.length];
		int c = 0;
		for (SolutionVector<P> solutionVector : active)
			{
			activeSvIds[c++] = solutionVector.id;
			}*/
		cache.maintainCache(active, newlyInactive);
		}

	public final float evaluateDiagonal(SolutionVector<P> a)
		{
		return cache.getDiagonal(a);
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

	public void getQ(SolutionVector<P> svA, SolutionVector<P>[] active, float[] buf)
		{
		//float[] result = new float[length];
		cache.get(svA, active, buf);
		}

	public void getQ(SolutionVector<P> svA, SolutionVector<P>[] active, SolutionVector<P>[] inactive, float[] buf)
		{
		//float[] result = new float[length];
		cache.get(svA, active, inactive, buf);
		}


// This cache may be reused across optimizations with (some of?) the same samples, but with newly created SolutionVectors,
	// e.g. with cross-validation.  So, we need to remember and restore the mapping from sample ids to ranks across runs.
//	private int[] idToRankMap;

/*	public void storeRanks(Collection<SolutionVector<P>> allExamples)
		{
		for (SolutionVector<P> a : allExamples)
			{
			idToRankMap[a.id] = a.rank;
			}
		}*/

//	public void loadRanks(Collection<SolutionVector<P>> allExamples)
//		{
	// it may be that only some of the previously cached examples are part of this run.

	// PERF test whether this helps or not
	// first partition the cache so that the examples that are part of this run come first
/*
		List<Integer> active = new ArrayList<Integer>();
		List<Integer> inactive = new LinkedList<Integer>();
		int numExamples = idToRankMap.length;
		for (int i = 0; i < numExamples; i++)
			{
			inactive.add(i);
			}

		for (SolutionVector<P> a : allExamples)
			{
			active.add(a.id);
			inactive.remove(new Integer(a.id));
			}

		cache.reorderCacheNoSolutionVectors(active.toArray(new Integer[0]), inactive.toArray(new Integer[0]));
*/

	// now tell the SVs what their ranks are
//		for (SolutionVector<P> a : allExamples)
//			{
//			a.rank = idToRankMap[a.id];
//			}

	//		}

	public void initRanks(Collection<SolutionVector<P>> allExamples)
		{
		int c = 0;
		for (SolutionVector<P> a : allExamples)
			{
			a.rank = c++;
			}
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

	/**
	 * This cache was supposed to be clever by leaving entries in place to avoid moving things around in memory; but that
	 * turns out to be the opposite of what we want due to cache locality issues. The original LIBSVM strategy of
	 * rearranging the actual entries, and of referring to them directly by rank instead of by id, is really good in this
	 * regard-- the cost of the memory rearrangement is apparently much less than the cache locality gain since the cache
	 * is read far more than it is written or rearranged.
	 */
	private class SlowRecentActivitySquareCache
		{
		private final Logger logger = Logger.getLogger(SlowRecentActivitySquareCache.class);

		// map from the SV index to the order of most recent activity (most active SVs first).
		int[] svIdToRank;

		// one of these is active at any given time, we swap them on alternating rounds
		int[] rankToSvIdA;
		int[] rankToSvIdB;

		// one of these is active at any given time, we swap them on alternating rounds
		int[] rankToCacheIndexA;
		int[] rankToCacheIndexB;

		float[][] data;

		float[] diagonal;

		int numExamples;
		int maxCachedRank;

		long hits = 0;
		long misses = 0;
		long widemisses = 0;
		long diagonalhits = 0;
		long diagonalmisses = 0;

		public SlowRecentActivitySquareCache(int numExamples, int cacheRows)
			{
			this.numExamples = numExamples;

			// how big should the cache really be
			maxCachedRank = Math.min(numExamples, cacheRows);

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

			// allocate triangular cache.  Does not include the diagonal!
			data = new float[maxCachedRank][];
			for (int i = 0; i < maxCachedRank; i++)
				{
				data[i] = new float[i];
				Arrays.fill(data[i], NOTCACHED);
				}
			}

		public void maintainCache(int[] activeSvIds)
			{
			Arrays.sort(activeSvIds); // for binary searches later

			//	System.err.println(this);

			// sort the rank array so that all the active SVs come before the inactive SVs,
			// but leave the order otherwise unaffected (so it maintains memory of the previous activity)

			if (activeSvIds.length > maxCachedRank)
				{
				logger.warn("Active set of " + activeSvIds.length + " SVs doesn't fit in cache of size " + maxCachedRank
						+ ", try increasing it");
				}

			// stable partitioning of the ranks.

			int newActiveRank = 0;
			int newInactiveRank = activeSvIds.length;

			// just go through in the old rank order.

			// first deal with those that have cache entries

			for (int rank = 0; rank < maxCachedRank; rank++)
				{
				int svid = rankToSvIdA[rank];

				// figure out the new rank
				int newRank;
				if (Arrays.binarySearch(activeSvIds, svid) >= 0) //activeSvIds.contains(svid))
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
				if (Arrays.binarySearch(activeSvIds, svid) >= 0) //activeSvIds.contains(svid))
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
			diagonal[cacheIndex] = NOTCACHED;  // the diagonal cell
			Arrays.fill(data[cacheIndex], NOTCACHED);  // the matching row
			for (int i = cacheIndex + 1; i < maxCachedRank; i++)  // the matching column
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

	/**
	 * This one is more like the original LIBSVM cache, rearranging the entries in memory according to which SVs are
	 * currently active
	 */
	private class RecentActivitySquareCache
		{
		float[][] data;

		float[] diagonal;

		int maxCachedRank;

		public final static float NOTCACHED = Float.NEGATIVE_INFINITY;

		long hits = 0;
		long misses = 0;
		long widemisses = 0;
		long diagonalhits = 0;
		long diagonalmisses = 0;


		public RecentActivitySquareCache(int numExamples, int cacheRows)
			{
			//	this.numExamples = numExamples;

			// how big should the cache really be
			maxCachedRank = Math.min(numExamples, cacheRows);


			// PERF maybe we don't need to preallocate the whole thing?

			// allocate square cache.
			data = new float[maxCachedRank][];
			for (int i = 0; i < maxCachedRank; i++)
				{
				data[i] = new float[maxCachedRank];
				Arrays.fill(data[i], NOTCACHED);
				}

			// allocate diagonal.  Redundant with the square cache, but this way it can sit in the processor cache sequentially.
			diagonal = new float[numExamples];
			Arrays.fill(diagonal, NOTCACHED);
			}

		public float getDiagonal(SolutionVector<P> a)
			{
			float result = diagonal[a.rank];
			if (result == NOTCACHED)
				{
				result = computeQ(a, a);
				diagonal[a.rank] = result;
				diagonalmisses++;
				}
			else
				{
				diagonalhits++;
				}
			return result;
			}

		/**
		 * Get the kernel value from a given SV to all those provided in the active array, computing any that are not already
		 * cached.  Requires that the active array is in rank order, including all ranks from 0 to n!
		 *
		 * @param a
		 * @param active
		 * @param buf
		 */
		public void get(SolutionVector<P> a, SolutionVector<P>[] active, float[] buf)
			{
			// active array is in rank order
			/*	int count = 0;
		   for (SolutionVector<P> b : active)
			   {
			   assert b.rank == count;
			   count++;
			   }*/

			if (a.rank >= maxCachedRank)
				{
				//float[] result = new float[active.length];
				for (int i = 0; i < active.length; i++)
					{
					buf[i] = computeQ(a, active[i]);
					widemisses++;
					}
				// result;
				}

			float[] row = data[a.rank];
			//	assert row[a.rank] == NOTCACHED || (row[a.rank] == getDiagonal(a));

			int cachedAndActive = Math.min(row.length, active.length);

			for (int i = 0; i < cachedAndActive; i++)
				{
				if (row[i] == NOTCACHED)
					{
					final SolutionVector<P> b = active[i];
					//		assert b.rank == i;
					row[i] = computeQ(a, b);
					//data[a.rank][b.rank] = row[i];
					data[b.rank][a.rank] = row[i];
					/*		if (a == b)
					   {
					   assert (row[a.rank] == getDiagonal(a));
					   }*/
					misses++;
					}
				else
					{
					SolutionVector<P> b = active[i];
					//		assert b.rank == i;
					//	assert row[i] == computeQ(a, b);
					hits++;
					}
				}
			//	assert (row[a.rank] == getDiagonal(a));
			//float[] result = new float[active.length];
			System.arraycopy(row, 0, buf, 0, cachedAndActive);  // PERF test whether this really helps (cache locality?)

			for (int i = cachedAndActive; i < active.length; i++)
				{
				final SolutionVector<P> b = active[i];
				buf[i] = computeQ(a, b);
				widemisses++;
				}

/*
			for (SolutionVector<P> b : active)
				{
				if (buf[b.rank] != computeQ(a, b))
					{
					float wtf = computeQ(a, b);
					assert false;
					}
				}
*/
			//return result;
			//return row;
			}

		/**
		 * pass active and inactive instead of allExamples to guarantee rank order. Requires that the active array is in rank
		 * order, including all ranks from 0 to n. Does not require that the inactive array has any particular order, but does
		 * return the results in buf to match the requested order.
		 *
		 * @param a
		 * @param active
		 * @param inactive
		 * @param buf
		 */
		public void get(SolutionVector<P> a, SolutionVector<P>[] active, SolutionVector<P>[] inactive, float[] buf)
			{
			// first fill the active portion.  Here the requested order must match the rank order anyway
			get(a, active, buf);

			float[] row = data[a.rank];

			// then fill the inactive portion one element at a time in the requested order, not the rank order
			int i = active.length;
			for (SolutionVector<P> b : inactive)
				{
				if (b.rank >= maxCachedRank)
					{
					buf[i] = computeQ(a, b);
					widemisses++;
					}
				else
					{
					if (row[b.rank] == NOTCACHED)
						{
						row[b.rank] = computeQ(a, b);
						//data[a.rank][b.rank] = row[i];
						data[b.rank][a.rank] = row[b.rank];
						misses++;
						}
					else
						{
						//assert result == computeQ(a, b);
						hits++;
						}
					buf[i] = row[b.rank];
					}
				i++;
				}
			}

		public float get(SolutionVector<P> a, SolutionVector<P> b)
			{
			//assert a != b;  // the diagonal entries should always stay empty; use getDiagonal instead
			if (a == b)
				{
				return getDiagonal(a);
				}

			// note the use of the redundant sv.rank field here instead of idToRankMap[sv.id].  This is just for cache locality.

			if (a.rank >= maxCachedRank || b.rank >= maxCachedRank)
				{
				//return NOTCACHED;
				widemisses++;
				return computeQ(a, b);
				}

			float result = data[a.rank][b.rank];
			if (result == NOTCACHED)
				{
				result = computeQ(a, b);
				data[a.rank][b.rank] = result;
				data[b.rank][a.rank] = result;
				misses++;
				}
			else
				{
				//assert result == computeQ(a, b);
				hits++;
				}
			return result;
			}


		/**
		 * Rearrange the ranks so that all active SVs come before all inactive SVs. Sort the data[][] and diagonal[] arrays
		 * according to the new ranking. The provided arrays are in the correct rank order already.
		 */
		public void maintainCache(SolutionVector<P>[] active,
		                          SolutionVector<P>[] newlyInactive) //, SolutionVector<P>[] previouslyInactive)
			{
			//int rankTrav = 0;

			// the desired partitioning is provided by the arguments; the current partitioning is buried inside each element as SV.rank.

			// note the ranks of the previously inactive SVs don't change, so we don't have to touch them or their cache entries at all

			// the partitioning mechanism is similar to that used in quicksort:
			//    find all elements of newlyInactive with prior rank less than the partition rank
			//    find all elements of active that with prior rank greater than the partition rank
			//    exchange these pairwise until done

			// it doesn't matter which pairs we choose to achieve partitioning, but it may improve things some to maintain order as well as possible.
			// thus, we exchange them in order.

			// Once we're done with this we want the SVs to know their new ranks, so we tkae this opportunity to reassign those.

			int partitionRank = active.length;

			int i = 0;
			int j = 0;

			while (true)
				{
				// find the first active element that was previously ranked too poorly
				while (i < active.length && active[i].rank < partitionRank)
					{
					// this one is OK, leave it in place
					//	active[i].rank = i;
					i++;
					}

				// find the first newly inactive element that was previously ranked too well
				while (j < newlyInactive.length && newlyInactive[j].rank >= partitionRank)
					{
					// this one is OK, leave it in place
					//	newlyInactive[j].rank = partitionRank + j;
					j++;
					}

				if (i < active.length && j < newlyInactive.length)
					{
					// now we're pointing at the first available pair that should be swapped

					swapBySolutionVector(active[i], newlyInactive[j]);
					//	active[i].rank = i;
					//	newlyInactive[j].rank = j + partitionRank;

					// now the pair is swapped, advance the counters past it

					i++;
					j++;
					}
				else
					{
					break;
					}
				}
/*
			// Now the SV.rank entries should match the real ranks for the active list
			int count = 0;
			for (SolutionVector<P> b : active)
				{
				assert b.rank == count;
				count++;
				}
			for (SolutionVector<P> b : newlyInactive)
				{
				assert b.rank == count;
				count++;
				}
				*/
			}


		/**
		 * Rearrange the ranks so that all active SVs come before all inactive SVs. Sort the data[][] and diagonal[] arrays
		 * according to the new ranking. The provided arrays are in the correct rank order already.
		 */
		/*	public void reorderCacheNoSolutionVectors(Integer[] activeIDs,
													Integer[] inactiveIDs) //, SolutionVector<P>[] previouslyInactive)
			  {
			  //int rankTrav = 0;

			  // the desired partitioning is provided by the arguments; the current partitioning is buried inside each element as SV.rank.

			  // note the ranks of the previously inactive SVs don't change, so we don't have to touch them or their cache entries at all

			  // the partitioning mechanism is similar to that used in quicksort:
			  //    find all elements of newlyInactive with prior rank less than the partition rank
			  //    find all elements of active that with prior rank greater than the partition rank
			  //    exchange these pairwise until done

			  // it doesn't matter which pairs we choose to achieve partitioning, but it may improve things some to maintain order as well as possible.
			  // thus, we exchange them in order.


			  int partitionRank = activeIDs.length;

			  int i = 0;
			  int j = 0;

			  while (true)
				  {
				  // find the first active element that was previously ranked too poorly
				  while (i < activeIDs.length && idToRankMap[activeIDs[i]] < partitionRank)
					  {
					  // this one is OK, leave it in place
					  i++;
					  }

				  // find the first newly inactive element that was previously ranked too well
				  while (j < inactiveIDs.length && idToRankMap[inactiveIDs[j]] >= partitionRank)
					  {
					  // this one is OK, leave it in place
					  j++;
					  }

				  if (i < activeIDs.length && j < inactiveIDs.length)
					  {
					  // now we're pointing at the first available pair that should be swapped

					  swapById(activeIDs[i], inactiveIDs[j]);

					  // now the pair is swapped, advance the counters past it

					  i++;
					  j++;
					  }
				  else
					  {
					  break;
					  }
				  }
			  }
  */
		private void swapBySolutionVector(SolutionVector<P> svA, SolutionVector<P> svB)
			{
			swapByRank(svA.rank, svB.rank);
			int tmp = svA.rank;
			svA.rank = svB.rank;
			svB.rank = tmp;

			/*	swapById(svA.id, svB.id);
						svA.rank = idToRankMap[svA.id];
						svB.rank = idToRankMap[svB.id];*/
			}


		/*		private void swapById(int idA, int idB)
			  {
			  swapByRank(idToRankMap[idA], idToRankMap[idB]);
			  int tmp = idToRankMap[idA];
			  idToRankMap[idA] = idToRankMap[idB];
			  idToRankMap[idB] = tmp;
			  }
  */
		private void swapByRank(int rankA, int rankB)
			{
			float tmp = diagonal[rankA];
			diagonal[rankA] = diagonal[rankB];
			diagonal[rankB] = tmp;

			if (rankA < maxCachedRank && rankB < maxCachedRank)
				{
				float[] dtmp = data[rankA];
				data[rankA] = data[rankB];
				data[rankB] = dtmp;

				for (float[] drow : data)
					{
					float d = drow[rankA];
					drow[rankA] = drow[rankB];
					drow[rankB] = d;
					}
				}
			else if (rankA < maxCachedRank)  // && rankB > maxCachedRank
				{
				Arrays.fill(data[rankA], NOTCACHED);
				}
			else //if (rankB < maxCachedRank && rankA > maxCachedRank
				{
				Arrays.fill(data[rankB], NOTCACHED);
				}
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
