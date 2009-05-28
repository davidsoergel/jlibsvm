package edu.berkeley.compbio.jlibsvm.multi;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class VotingResult<L>
	{
// ------------------------------ FIELDS ------------------------------

	private L bestLabel = null;
	private float bestVoteProportion = 0;
	private float secondBestVoteProportion = 0;
	private float bestOneClassProbability = 0;
	private float secondBestOneClassProbability = 0;
	private float bestOneVsAllProbability = 0;
	private float secondBestOneVsAllProbability = 0;


// --------------------------- CONSTRUCTORS ---------------------------

	public VotingResult()
		{
		}

	public VotingResult(L bestLabel, float bestVoteProportion, float secondBestVoteProportion,
	                    float bestOneClassProbability, float secondBestOneClassProbability,
	                    float bestOneVsAllProbability, float secondBestOneVsAllProbability)
		{
		this.bestLabel = bestLabel;
		this.bestVoteProportion = bestVoteProportion;
		this.secondBestVoteProportion = secondBestVoteProportion;
		this.bestOneClassProbability = bestOneClassProbability;
		this.secondBestOneClassProbability = secondBestOneClassProbability;
		this.bestOneVsAllProbability = bestOneVsAllProbability;
		this.secondBestOneVsAllProbability = secondBestOneVsAllProbability;
		}

// --------------------- GETTER / SETTER METHODS ---------------------


	public L getBestLabel()
		{
		return bestLabel;
		}

	public float getBestOneVsAllProbability()
		{
		return bestOneVsAllProbability;
		}

	public float getBestOneClassProbability()
		{
		return bestOneClassProbability;
		}

	public float getBestVoteProportion()
		{
		return bestVoteProportion;
		}

	public float getSecondBestOneVsAllProbability()
		{
		return secondBestOneVsAllProbability;
		}

	public float getSecondBestOneClassProbability()
		{
		return secondBestOneClassProbability;
		}

	public float getSecondBestVoteProportion()
		{
		return secondBestVoteProportion;
		}
	}
