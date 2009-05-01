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
// ------------------------------ FIELDS ------------------------------

	private Set<Entry<P, L>> entries;


// --------------------------- CONSTRUCTORS ---------------------------

	public SubtractionMap(Map<P, L> orig, Set<P> except)
		{
		this(orig.entrySet(), except);
		}

	public SubtractionMap(Collection<Entry<P, L>> origEntries, Set<P> except)
		{
		entries = new SubtractionEntrySet<P, L>(origEntries, except);
		}

	public SubtractionMap(Map<P, L> orig, Set<P> except, int maxSize)
		{
		this(orig.entrySet(), except, maxSize);
		}

	public SubtractionMap(Collection<Entry<P, L>> origEntries, Set<P> except, int maxSize)
		{
		entries = new SubtractionEntrySet<P, L>(origEntries, except, maxSize);
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Map ---------------------

	@Override
	public int size()
		{
		return entries.size();
		}

	public Set<Entry<P, L>> entrySet()
		{
		return entries;
		}

// -------------------------- INNER CLASSES --------------------------

	private class SubtractionEntrySet<K, V> extends AbstractSet<Entry<K, V>>
		{
// ------------------------------ FIELDS ------------------------------

		private Collection<Entry<K, V>> orig;
		private Set<K> except;
		private int size;


// --------------------------- CONSTRUCTORS ---------------------------

		public SubtractionEntrySet(Collection<Entry<K, V>> orig, Set<K> except)
			{
			this(orig, except, Integer.MAX_VALUE);
			}

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
			size = c;
			// now hasNext() should work right
			}

		public Iterator<Entry<K, V>> iterator()
			{
			Iterator<Entry<K, V>> oi = orig.iterator();

			return new SubtractionEntrySet.ExceptKeyIterator(oi);
			}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Collection ---------------------

		public int size()
			{
			return size;
			}

// -------------------------- INNER CLASSES --------------------------

		private class ExceptKeyIterator implements Iterator<Entry<K, V>>
			{
// ------------------------------ FIELDS ------------------------------

			Iterator<Entry<K, V>> oi;

			int c = 0;

			Entry<K, V> next;


// --------------------------- CONSTRUCTORS ---------------------------

			private ExceptKeyIterator(Iterator<Entry<K, V>> oi)
				{
				this.oi = oi;
				prepNext();
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

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Iterator ---------------------

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


				c++;
				// now c is the number of items successfully returned so far, including the one we're about to return

				if (c >= size)
					{
					next = null;  // make sure hasNext behaves.  prepNext doesn't know about the count
					}
				return result;
				}

			public void remove()
				{
				throw new UnsupportedOperationException();
				}
			}
		}
	}
