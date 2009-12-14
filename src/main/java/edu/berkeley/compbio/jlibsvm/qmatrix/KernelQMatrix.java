package edu.berkeley.compbio.jlibsvm.qmatrix;

import edu.berkeley.compbio.jlibsvm.SolutionVector;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class KernelQMatrix<P> implements QMatrix<P>
	{
// ------------------------------ FIELDS ------------------------------

	protected KernelFunction<P> kernel;

	private RecentActivitySquareCache cache;


// --------------------------- CONSTRUCTORS ---------------------------

	KernelQMatrix(@NotNull KernelFunction<P> kernel, int numExamples, int cacheRows)
		{
		this.kernel = kernel;
		this.cache = new RecentActivitySquareCache(numExamples, cacheRows);
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface QMatrix ---------------------

	public final float evaluateDiagonal(SolutionVector<P> a)
		{
		return cache.getDiagonal(a);
		}

	public void getQ(SolutionVector<P> svA, SolutionVector<P>[] active, float[] buf)
		{
		cache.get(svA, active, buf);
		}

	public void getQ(SolutionVector<P> svA, SolutionVector<P>[] active, SolutionVector<P>[] inactive, float[] buf)
		{
		cache.get(svA, active, inactive, buf);
		}

	public void initRanks(Collection<SolutionVector<P>> allExamples)
		{
		int c = 0;
		for (SolutionVector<P> a : allExamples)
			{
			a.rank = c++;
			}
		}

	public void maintainCache(SolutionVector<P>[] active, SolutionVector<P>[] newlyInactive)
		{
		cache.maintainCache(active, newlyInactive);
		}

	public String perfString()
		{
		return cache.toString();
		}

// -------------------------- OTHER METHODS --------------------------

	public abstract float computeQ(SolutionVector<P> a, SolutionVector<P> b);

	public final float evaluate(SolutionVector<P> a, SolutionVector<P> b)
		{
		return cache.get(a, b);
		}

// -------------------------- INNER CLASSES --------------------------

	/**
	 * I had written a SlowRecentActivitySquareCache that was supposed to be clever by leaving entries in place to avoid
	 * moving things around in memory; but that turns out to be the opposite of what we want due to cache locality issues.
	 * The original LIBSVM strategy of rearranging the actual entries, and of referring to them directly by rank instead of
	 * by id, is really good in this regard-- the cost of the memory rearrangement is apparently much less than the cache
	 * locality gain since the cache is read far more than it is written or rearranged.
	 * <p/>
	 * This one is more like the original LIBSVM cache, rearranging the entries in memory according to which SVs are
	 * currently active
	 */
	private class RecentActivitySquareCache
		{
// ------------------------------ FIELDS ------------------------------

		public final static float NOTCACHED = Float.NEGATIVE_INFINITY;
		float[][] data;

		float[] diagonal;

		int maxCachedRank;

		long hits = 0;
		long misses = 0;
		long widemisses = 0;
		long diagonalhits = 0;
		long diagonalmisses = 0;


// --------------------------- CONSTRUCTORS ---------------------------

		public RecentActivitySquareCache(int numExamples, int cacheRows)
			{
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

// ------------------------ CANONICAL METHODS ------------------------

		public String toString()
			{
			return "QMatrix hits = " + hits + ", misses = " + misses + ", widemisses = " + widemisses
			       + ", diagonalhits = " + diagonalhits + ", diagonalmisses = " + diagonalmisses + ", rate = "
			       + (float) (hits + diagonalhits) / (float) (hits + diagonalhits + misses + widemisses
			                                                  + diagonalmisses) + ", size = " + data.length;
			}

// -------------------------- OTHER METHODS --------------------------

		public float get(SolutionVector<P> a, SolutionVector<P> b)
			{
			//assert a != b;
			// the diagonal entries should always stay empty; use getDiagonal instead
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

			if (a.rank >= maxCachedRank)
				{
				for (int i = 0; i < active.length; i++)
					{
					buf[i] = computeQ(a, active[i]);
					widemisses++;
					}
				return;
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

			System.arraycopy(row, 0, buf, 0, cachedAndActive);  // PERF test whether this really helps (cache locality?)

			for (int i = cachedAndActive; i < active.length; i++)
				{
				final SolutionVector<P> b = active[i];
				buf[i] = computeQ(a, b);
				widemisses++;
				}
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

			// then fill the inactive portion one element at a time in the requested order, not the rank order

			if (a.rank >= maxCachedRank)
				{
				int i = active.length;
				for (SolutionVector<P> b : inactive)
					{
					buf[i] = computeQ(a, b);
					widemisses++;
					i++;
					}
				}
			else
				{
				float[] row = data[a.rank];

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

			if (rankA >= maxCachedRank && rankB >= maxCachedRank)
				{
				// do nothing
				}
			else if (rankA < maxCachedRank && rankB < maxCachedRank)
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
		}
	}
