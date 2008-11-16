package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.SolutionModel;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.kernel.PrecomputedKernel;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class AlphaModel extends SolutionModel
	{
	public SvmPoint[] supportVectors;// SVs (SV[l])
	public float[] alpha;  // sv_coef
	public float rho;

	public AlphaModel(BinaryModel alphaModel)
		{
		super(alphaModel);
		alpha = alphaModel.alpha;
		rho = alphaModel.rho;
		supportVectors = alphaModel.supportVectors;
		}

	protected AlphaModel(KernelFunction kernel, SvmParameter param)
		{
		super(kernel, param);
		}

	public AlphaModel(Properties props)
		{
		super(props);
		rho = Float.parseFloat(props.getProperty("rho"));
		// ignore total_sv
		}

	/**
	 * Remove vectors whose alpha is zero, leaving only support vectors
	 */
	public void compact()
		{
		assert supportVectors.length == alpha.length;

		List<Integer> nonZeroAlphaIndexes = new ArrayList<Integer>();

		for (int i = 0; i < supportVectors.length; i++)
			{
			if (Math.abs(alpha[i]) > 0)
				{
				nonZeroAlphaIndexes.add(i);
				}
			}

		SvmPoint[] newSupportVectors = new SvmPoint[nonZeroAlphaIndexes.size()];
		float[] newAlphas = new float[nonZeroAlphaIndexes.size()];


		int j = 0;
		for (Integer i : nonZeroAlphaIndexes)
			{
			newSupportVectors[j] = supportVectors[i];
			newAlphas[j] = alpha[i];
			j++;
			}

		supportVectors = newSupportVectors;
		alpha = newAlphas;
		}

	public void writeToStream(DataOutputStream fp) throws IOException
		{
		super.writeToStream(fp);

		fp.writeBytes("rho " + rho + "\n");
		fp.writeBytes("total_sv " + supportVectors.length + "\n");
		}


	protected void writeSupportVectors(DataOutputStream fp) throws IOException
		{
		fp.writeBytes("SV\n");

		for (int i = 0; i < alpha.length; i++)
			{
			fp.writeBytes(alpha[i] + " ");

			SvmPoint p = supportVectors[i];
			if (kernel instanceof PrecomputedKernel) //param.kernel_type == svm_parameter.PRECOMPUTED)
				{
				fp.writeBytes("0:" + (int) (p.values[0]));
				}
			else
				{
				for (int j = 0; j < p.indexes.length; j++)
					{
					fp.writeBytes(p.indexes[j] + ":" + p.values[j] + " ");
					}
				}
			fp.writeBytes("\n");
			}
		}

	protected void readSupportVectors(BufferedReader reader) throws IOException
		{
		String line;
		int lineNo = 0;
		while ((line = reader.readLine()) != null)
			{
			StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

			alpha[lineNo] = Float.parseFloat(st.nextToken());
			int n = st.countTokens() / 2;
			SvmPoint p = new SvmPoint(n);
			supportVectors[lineNo] = p;
			for (int j = 0; j < n; j++)
				{
				p.indexes[j] = Integer.parseInt(st.nextToken());
				p.values[j] = Float.parseFloat(st.nextToken());
				}
			}
		}
	}
