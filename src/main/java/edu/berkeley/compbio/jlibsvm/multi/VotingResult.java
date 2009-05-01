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
	private float bestProbability = 0;
	private float secondBestProbability = 0;
	private float bestOneVsAllProbability = 0;
	private float secondBestOneVsAllProbability = 0;


// --------------------------- CONSTRUCTORS ---------------------------

	public VotingResult()
		{
		}

	public VotingResult(L bestLabel, float bestVoteProportion, float secondBestVoteProportion, float bestProbability,
	                    float secondBestProbability, float bestOneVsAllProbability, float secondBestOneVsAllProbability)
		{
		this.bestLabel = bestLabel;
		this.bestVoteProportion = bestVoteProportion;
		this.secondBestVoteProportion = secondBestVoteProportion;
		this.bestProbability = bestProbability;
		this.secondBestProbability = secondBestProbability;
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

	public float getBestProbability()
		{
		return bestProbability;
		}

	public float getBestVoteProportion()
		{
		return bestVoteProportion;
		}

	public float getSecondBestOneVsAllProbability()
		{
		return secondBestOneVsAllProbability;
		}

	public float getSecondBestProbability()
		{
		return secondBestProbability;
		}

	public float getSecondBestVoteProportion()
		{
		return secondBestVoteProportion;
		}
	}
