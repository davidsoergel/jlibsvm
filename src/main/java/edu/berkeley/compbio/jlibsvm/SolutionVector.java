package edu.berkeley.compbio.jlibsvm;

import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SolutionVector<P> implements Comparable<SolutionVector>
	{
// ------------------------------ FIELDS ------------------------------

	/**
	 * Used by the cacheing mechanism to keep track of which SVs are the most active.
	 */
	public int rank = -1;

	/**
	 * keep track of the sample id for mapping to ranks
	 */
	final public int id;
	final public P point;
	public boolean targetValue;
	public double alpha;
	public double G;
	public float linearTerm;
	Status alphaStatus;
	float G_bar;


// --------------------------- CONSTRUCTORS ---------------------------

	public SolutionVector(int id, @NotNull P key, Boolean targetValue, float linearTerm)
		{
		this.id = id;
		point = key;
		this.linearTerm = linearTerm;
		this.targetValue = targetValue;
		}

	public SolutionVector(int id, @NotNull P key, Boolean value, float linearTerm, float alpha)
		{
		this(id, key, value, linearTerm);
		this.alpha = alpha;
		}

// ------------------------ CANONICAL METHODS ------------------------

	@Override
	public boolean equals(Object o)
		{
		if (this == o)
			{
			return true;
			}
		if (o == null || getClass() != o.getClass())
			{
			return false;
			}

		SolutionVector that = (SolutionVector) o;

		if (rank != that.rank)
			{
			return false;
			}

		return true;
		}

	// PERF hack for speed

	public int hashCode()
		{
		return id;
		}

	@Override
	public String toString()
		{
		return "SolutionVector{" + "point=" + point + ", targetValue=" + targetValue + ", alpha=" + alpha
		       + ", alphaStatus=" + alphaStatus + ", G=" + G + ", linearTerm=" + linearTerm + ", G_bar=" + G_bar + '}';
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Comparable ---------------------

	public int compareTo(SolutionVector b)
		{
		return rank < b.rank ? -1 : (rank > b.rank ? 1 : 0);
		}

// -------------------------- OTHER METHODS --------------------------

	boolean isFree()
		{
		return alphaStatus == Status.FREE;
		}

	public boolean isShrinkable(double Gmax1, double Gmax2)
		{
		//return isShrinkable(Gmax1,Gmax2,Gmax1,Gmax2);

		if (isUpperBound())
			{
			if (targetValue)
				{
				return -G > Gmax1;
				}
			else
				{
				return -G > Gmax2;
				}
			}
		else if (isLowerBound())
			{
			if (targetValue)
				{
				return G > Gmax2;
				}
			else
				{
				return G > Gmax1;
				}
			}
		else
			{
			return false;
			}
		}

	protected boolean isUpperBound()
		{
		return alphaStatus == Status.UPPER_BOUND;
		}

	boolean isLowerBound()
		{
		return alphaStatus == Status.LOWER_BOUND;
		}

	public boolean isShrinkable(double Gmax1, double Gmax2, double Gmax3, double Gmax4)
		{
		if (isUpperBound())
			{
			if (targetValue)
				{
				return (-G > Gmax1);
				}
			else
				{
				return (-G > Gmax4);
				}
			}
		else if (isLowerBound())
			{
			if (targetValue)
				{
				return (G > Gmax2);
				}
			else
				{
				return (G > Gmax3);
				}
			}
		else
			{
			return false;
			}
		}

	public void updateAlphaStatus(float Cp, float Cn)
		{
		if (alpha >= getC(Cp, Cn))
			{
			alphaStatus = Status.UPPER_BOUND;
			}
		else if (alpha <= 0)
			{
			alphaStatus = Status.LOWER_BOUND;
			}
		else
			{
			alphaStatus = Status.FREE;
			}
		}

	float getC(float Cp, float Cn)
		{
		return targetValue ? Cp : Cn;
		}

// -------------------------- ENUMERATIONS --------------------------

	public enum Status
		{
			LOWER_BOUND, UPPER_BOUND, FREE
		}
	}
