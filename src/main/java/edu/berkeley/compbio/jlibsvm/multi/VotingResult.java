package edu.berkeley.compbio.jlibsvm.multi;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class VotingResult<L>
	{
	L bestLabel;
	float bestVoteProportion;
	float secondBestVoteProportion;
	float bestProbability;
	float secondBestProbability;
	float bestOneVsAllProbability;
	float secondBestOneVsAllProbability;

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

	public L getBestLabel()
		{
		return bestLabel;
		}

	public float getBestProbability()
		{
		return bestProbability;
		}

	public float getSecondBestProbability()
		{
		return secondBestProbability;
		}

	public float getBestOneVsAllProbability()
		{
		return bestOneVsAllProbability;
		}

	public float getSecondBestOneVsAllProbability()
		{
		return secondBestOneVsAllProbability;
		}

	public float getBestVoteProportion()
		{
		return bestVoteProportion;
		}

	public float getSecondBestVoteProportion()
		{
		return secondBestVoteProportion;
		}
	}
