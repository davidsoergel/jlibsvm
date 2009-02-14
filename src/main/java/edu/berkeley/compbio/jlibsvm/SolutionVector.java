package edu.berkeley.compbio.jlibsvm;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SolutionVector<P>
	{
	/**
	 * Used by the caching mechanism to keep track of which SVs are the most active.  Redundant with idToRankMap, but
	 * better cache performance this way (?)
	 */
	public int rank = -1;

	public enum Status
		{
			LOWER_BOUND, UPPER_BOUND, FREE
		}

	@Override
	public String toString()
		{
		return "SolutionVector{" + "point=" + point + ", targetValue=" + targetValue + ", alpha=" + alpha
				+ ", alphaStatus=" + alphaStatus + ", G=" + G + ", linearTerm=" + linearTerm + ", G_bar=" + G_bar + '}';
		}

	public int id; // keep track of the sample id for mapping to ranks
	public P point;
	public boolean targetValue;
	public double alpha;
	Status alphaStatus;
	public double G;
	public float linearTerm;
	float G_bar;

	// for debugging
	public boolean wasEvaluated = false;
	//	private Solver solver;

	/*	public SolutionVector(P key, Boolean value)
	   {
	   point = key;
	   targetValue = value;
	   }*/

	// PERF hack for speed

	public int hashCode()
		{
		return id;
		}

	public SolutionVector(P key, Boolean targetValue, float linearTerm)
		{
		//	this.solver = solver;
		//this(key,value);
		point = key;
		this.linearTerm = linearTerm;
		this.targetValue = targetValue;
		}


	public SolutionVector(P key, Boolean value, float linearTerm, float alpha)
		{
		this(key, value, linearTerm);
		this.alpha = alpha;
		}

	protected boolean isUpperBound()
		{
		return alphaStatus == Status.UPPER_BOUND;
		}

	boolean isLowerBound()
		{
		return alphaStatus == Status.LOWER_BOUND;
		}

	boolean isFree()
		{
		return alphaStatus == Status.FREE;
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
	}
