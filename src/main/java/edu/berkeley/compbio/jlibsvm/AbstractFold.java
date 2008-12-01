package edu.berkeley.compbio.jlibsvm;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
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

	public AbstractFold(Map<P, L> allExamples, Set<P> heldOutPoints)
		{
		//	this.fullProblem = fullProblem;
		this.heldOutPoints = heldOutPoints;
		subtractionMap = new SubtractionMap(allExamples, heldOutPoints);
		}

	public R asR()
		{
		return (R) this;
		}

	public Map<P, L> getExamples()
		{
		return subtractionMap;
		}


	public Set<P> getHeldOutPoints()
		{
		return heldOutPoints;
		}


	private class SubtractionMap extends AbstractMap<P, L>
		{
		//private Map<P,L> orig;
		//private Set<P> except;
		private Set<Entry<P, L>> entries;

		public SubtractionMap(Map<P, L> orig, Set<P> except)
			{
			//this.orig = orig;
			//this.except = except;
			entries = new SubtractionEntrySet<P, L>(orig.entrySet(), except);
			}

		public Set<Entry<P, L>> entrySet()
			{
			return entries;
			}

		private class SubtractionEntrySet<K, V> extends AbstractSet<Entry<K, V>>
			{
			private Set<Entry<K, V>> orig;
			private Set<K> except;
			int size;

			public SubtractionEntrySet(Set<Entry<K, V>> orig, Set<K> except)
				{
				this.orig = orig;
				this.except = except;
				size = orig.size() - except
						.size();  // assume that all of the exceptions were actually in the original set to begin with
				}

			public Iterator<Entry<K, V>> iterator()
				{
				Iterator<Entry<K, V>> oi = orig.iterator();

				return new ExceptKeyIterator<Entry<K, V>>(oi);
				}

			public int size()
				{
				return size;
				}

			private class ExceptKeyIterator<T> implements Iterator<Entry<K, V>>
				{
				Iterator<Entry<K, V>> oi;

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
					Entry<K, V> result = next;
					prepNext();
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
	}
