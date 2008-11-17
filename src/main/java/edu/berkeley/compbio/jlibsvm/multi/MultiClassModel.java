package edu.berkeley.compbio.jlibsvm.multi;

import edu.berkeley.compbio.jlibsvm.DiscreteModel;
import edu.berkeley.compbio.jlibsvm.MathSupport;
import edu.berkeley.compbio.jlibsvm.SolutionModel;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassModel<T> extends SolutionModel
		implements DiscreteModel<T>// implements ProbabilitySupportingModel//implements java.io.Serializable
	{
	int numberOfClasses;// number of classes, = 2 in regression/one class svm

	//	float[][] sv_coef;// coefficients for SVs in decision functions (sv_coef[k-1][l])
	//	float[] rho;// constants in decision functions (rho[k*(k-1)/2])
	float[] probA;// pairwise probability information
	float[] probB;

	BinaryModel[] oneVsOneModels;   // don't like array vs collection, but it's consistent with the rest for now

	// for classification only

	// generics are a hassle here  (T[] label; makes a mess)
	Object[] label;
			// label of each class, just to maintain a known order for the sake of keeping the decision_values etc. straight
//	int[] numSupportVectors;// number of SVs for each class (nSV[k])
// nSV[0] + nSV[1] + ... + nSV[k-1] = l

	public MultiClassModel(KernelFunction kernel, SvmParameter param)
		{
		super(kernel, param);
		}


	/*public float predictValue(SvmPoint x)
		{
		return predict(x);
		}*/


	public T predictLabel(SvmPoint x)
		{
		int i;
		float[] decisionValues = oneVsOneValues(x);

		int[] vote = new int[numberOfClasses];
		/*for (i = 0; i < nr_class; i++)
			{
			vote[i] = 0;
			}*/
		int pos = 0;
		for (i = 0; i < numberOfClasses; i++)
			{
			for (int j = i + 1; j < numberOfClasses; j++)
				{
				if (decisionValues[pos++] > 0)
					{
					++vote[i];
					}
				else
					{
					++vote[j];
					}
				}
			}

		int bestVoteIndex = 0;
		for (i = 1; i < numberOfClasses; i++)
			{
			if (vote[i] > vote[bestVoteIndex])
				{
				bestVoteIndex = i;
				}
			}
		return (T) label[bestVoteIndex];
		}


	public float[] oneVsOneValues(SvmPoint x)
		{
		float[] decisionValues = new float[oneVsOneModels.length];

		int i = 0;
		for (BinaryModel m : oneVsOneModels)
			{
			decisionValues[i] = m.predictValue(x);
			i++;
			}
		return decisionValues;
		}


	public boolean supportsProbability()
		{
		return probA != null && probB != null;
		}

	public float[] predictProbability(SvmPoint x)
		{
		if (!supportsProbability())
			{
			throw new SvmException("Can't make probability predictions");
//			return predict(x);
			}

		int i;
		//float[] dec_values = new float[nr_class * (nr_class - 1) / 2];
		float[] decisionValues = oneVsOneValues(x);//, dec_values);

		float minimumProbability = 1e-7f;
		float[][] pairwiseProbabilities = new float[numberOfClasses][numberOfClasses];

		int k = 0;
		for (i = 0; i < numberOfClasses; i++)
			{
			for (int j = i + 1; j < numberOfClasses; j++)
				{
				pairwiseProbabilities[i][j] = Math.min(
						Math.max(MathSupport.sigmoidPredict(decisionValues[k], probA[k], probB[k]), minimumProbability),
						1 - minimumProbability);
				pairwiseProbabilities[j][i] = 1 - pairwiseProbabilities[i][j];
				k++;
				}
			}
		float[] probabilityEstimates = multiclassProbability(numberOfClasses, pairwiseProbabilities);

		return probabilityEstimates;// label[bestProbabilityIndex];
		}

	public T bestProbabilityLabel(float[] labelProbabilities)
		{
		int bestProbabilityIndex = 0;
		for (int i = 1; i < numberOfClasses; i++)
			{
			if (labelProbabilities[i] > labelProbabilities[bestProbabilityIndex])
				{
				bestProbabilityIndex = i;
				}
			}
		return (T) label[bestProbabilityIndex];
		}


	// Method 2 from the multiclass_prob paper by Wu, Lin, and Weng
	private float[] multiclassProbability(int k, float[][] r)
		{

		float[] p = new float[k];
		int t, j;
		int iter = 0, maximumIterations = Math.max(100, k);
		float[][] Q = new float[k][k];
		float[] Qp = new float[k];
		float pQp, eps = 0.005f / k;

		for (t = 0; t < k; t++)
			{
			p[t] = 1.0f / k;// Valid if k = 1
			Q[t][t] = 0;
			for (j = 0; j < t; j++)
				{
				Q[t][t] += r[j][t] * r[j][t];
				Q[t][j] = Q[j][t];
				}
			for (j = t + 1; j < k; j++)
				{
				Q[t][t] += r[j][t] * r[j][t];
				Q[t][j] = -r[j][t] * r[t][j];
				}
			}
		for (iter = 0; iter < maximumIterations; iter++)
			{			// stopping condition, recalculate QP,pQP for numerical accuracy
			pQp = 0;
			for (t = 0; t < k; t++)
				{
				Qp[t] = 0;
				for (j = 0; j < k; j++)
					{
					Qp[t] += Q[t][j] * p[j];
					}
				pQp += p[t] * Qp[t];
				}
			float maxError = 0;
			for (t = 0; t < k; t++)
				{
				float error = Math.abs(Qp[t] - pQp);
				if (error > maxError)
					{
					maxError = error;
					}
				}
			if (maxError < eps)
				{
				break;
				}

			for (t = 0; t < k; t++)
				{
				float diff = (-Qp[t] + pQp) / Q[t][t];
				p[t] += diff;
				pQp = (pQp + diff * (diff * Q[t][t] + 2 * Qp[t])) / (1 + diff) / (1 + diff);
				for (j = 0; j < k; j++)
					{
					Qp[j] = (Qp[j] + diff * Q[t][j]) / (1 + diff);
					p[j] /= (1 + diff);
					}
				}
			}
		if (iter >= maximumIterations)
			{
			System.err.print("Multiclass probability attempted too many iterations\n");
			}
		return p;
		}

	public MultiClassModel(Properties props)
		{
		super(props);
		numberOfClasses = Integer.parseInt(props.getProperty("nr_class"));

		StringTokenizer st = new StringTokenizer(props.getProperty("rho"));

		ArrayList<BinaryModel> models = new ArrayList<BinaryModel>();
		while (st.hasMoreTokens())
			{
			BinaryModel m = new BinaryModel(kernel, param);
			m.rho = Float.parseFloat(st.nextToken());
			// no SVs yet
			models.add(m);
			}
		oneVsOneModels = models.toArray(new BinaryModel[]{});

		probA = parseFloatArray(props.getProperty("probA"));
		probB = parseFloatArray(props.getProperty("probB"));
		//	numSupportVectors = parseIntArray(props.getProperty("nr_sv"));
		}


	public void writeToStream(DataOutputStream fp) throws IOException
		{
		super.writeToStream(fp);
		//svm_parameter param = model.param;

		//int nr_class = nr_class;
		//int l = model.l;
		fp.writeBytes("nr_class " + numberOfClasses + "\n");

		fp.writeBytes("rho");
		for (BinaryModel m : oneVsOneModels)
			{
			fp.writeBytes(" " + m.rho);
			}
		fp.writeBytes("\n");

		if (probA != null)// regression has probA only
			{
			fp.writeBytes("probA");
			for (int i = 0; i < numberOfClasses * (numberOfClasses - 1) / 2; i++)
				{
				fp.writeBytes(" " + probA[i]);
				}
			fp.writeBytes("\n");
			}
		if (probB != null)
			{
			fp.writeBytes("probB");
			for (int i = 0; i < numberOfClasses * (numberOfClasses - 1) / 2; i++)
				{
				fp.writeBytes(" " + probB[i]);
				}
			fp.writeBytes("\n");
			}

/*		if (numSupportVectors != null)
			{
			fp.writeBytes("nr_sv");
			for (int i = 0; i < numberOfClasses; i++)
				{
				fp.writeBytes(" " + numSupportVectors[i]);
				}
			fp.writeBytes("\n");
			}*/

		//these must come after everything else
		writeSupportVectors(fp);

		/*
		  float[][] sv_coef = model.sv_coef;
		  svm_node[][] SV = model.SV;

		  for (int i = 0; i < l; i++)
			  {
			  for (int j = 0; j < nr_class - 1; j++)
				  {
				  fp.writeBytes(sv_coef[j][i] + " ");
				  }

			  svm_node[] p = SV[i];
			  if (param.kernel_type == svm_parameter.PRECOMPUTED)
				  {
				  fp.writeBytes("0:" + (int) (p[0].value));
				  }
			  else
				  {
				  for (int j = 0; j < p.length; j++)
					  {
					  fp.writeBytes(p[j].index + ":" + p[j].value + " ");
					  }
				  }
			  fp.writeBytes("\n");
			  }
  */
		fp.close();
		}

	protected void readSupportVectors(BufferedReader fp)
		{
		throw new UnsupportedOperationException();
		}

	protected void writeSupportVectors(DataOutputStream fp) throws IOException
		{
		fp.writeBytes("SV\n");
		fp.writeBytes("Saving multi-class support vectors is not implemented yet");

		// the original format is a spaghetti

		}
	}
