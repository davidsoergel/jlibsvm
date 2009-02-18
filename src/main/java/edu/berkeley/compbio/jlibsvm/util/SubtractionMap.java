package edu.berkeley.compbio.jlibsvm.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SubtractionMap<P, L> extends AbstractMap<P, L>
	{
	//private Map<P,L> orig;
	//private Set<P> except;
	private Set<Entry<P, L>> entries;

	public SubtractionMap(Map<P, L> orig, Set<P> except, int maxSize)
		{
		this(orig.entrySet(), except, maxSize);
		//this.orig = orig;
		//this.except = except;
		/*
		for(P p : except)
			{
			assert orig.containsKey(p);
			}*/
		//entries = new SubtractionEntrySet<P, L>(orig.entrySet(), except);
		}

	public SubtractionMap(Map<P, L> orig, Set<P> except)
		{
		this(orig.entrySet(), except);
		}


	public SubtractionMap(Collection<Entry<P, L>> origEntries, Set<P> except, int maxSize)
		{
		//this.orig = orig;
		//this.except = except;
		/*
		for(P p : except)
			{
			assert orig.containsKey(p);
			}*/
		entries = new SubtractionEntrySet<P, L>(origEntries, except, maxSize);
		}

	public SubtractionMap(Collection<Entry<P, L>> origEntries, Set<P> except)
		{
		//this.orig = orig;
		//this.except = except;
		/*
		for(P p : except)
			{
			assert orig.containsKey(p);
			}*/
		entries = new SubtractionEntrySet<P, L>(origEntries, except);
		}


	public Set<Entry<P, L>> entrySet()
		{
		return entries;
		}

	@Override
	public int size()
		{
		return entries.size();
		}

	private class SubtractionEntrySet<K, V> extends AbstractSet<Entry<K, V>>
		{
		private Collection<Entry<K, V>> orig;
		private Set<K> except;
		private int size;


		public SubtractionEntrySet(Collection<Entry<K, V>> orig, Set<K> except, int maxSize)
			{
			this.size = maxSize;
			this.orig = orig;
			this.except = except;
			// PERF sucks that we have to iterate the whole thing just to learn the size, but there's no other way...
			int c = 0;
			for (Iterator i = iterator(); i.hasNext();)
				{
				try
					{
					i.next();
					}
				catch (NoSuchElementException e)  // this happens if there are fewer than the requested elements available
					{
					break;
					}
				c++;
				}
			size = c;  // now hasNext() should work right
			//orig.size() - except.size();  // assume that all of the exceptions were actually in the original set to begin with
			}

		public SubtractionEntrySet(Collection<Entry<K, V>> orig, Set<K> except)
			{
			this(orig, except, Integer.MAX_VALUE);
			}

		public int size()
			{
			return size;
			}

		public Iterator<Entry<K, V>> iterator()
			{
			Iterator<Entry<K, V>> oi = orig.iterator();

			return new SubtractionEntrySet.ExceptKeyIterator(oi);
			}


		private class ExceptKeyIterator implements Iterator<Entry<K, V>>
			{
			Iterator<Entry<K, V>> oi;
			int c = 0;

			private ExceptKeyIterator(Iterator<Entry<K, V>> oi)
				{
				this.oi = oi;
				prepNext();
				}

			Entry<K, V> next;

			public boolean hasNext()
				{
				return next != null;
				}

			public Entry<K, V> next()
				{
				if (next == null)
					{
					throw new NoSuchElementException();
					}
				Entry<K, V> result = next;
				prepNext();


				//			assert c <= size;

				c++;
				// now c is the number of items successfully returned so far, including the one we're about to return

				if (c >= size)
					{
					next = null;  // make sure hasNext behaves.  prepNext doesn't know about the count
					}
				return result;
				}

			private void prepNext()
				{
				try
					{
					do
						{
						next = oi.next();
						}
					while (except.contains(next.getKey()));
					}
				catch (NoSuchElementException e)
					{
					next = null;
					}
				}

			public void remove()
				{
				throw new UnsupportedOperationException();
				}
			}
		}
	}
