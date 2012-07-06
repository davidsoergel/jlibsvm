package edu.berkeley.compbio.jlibsvm.binary;

import com.davidsoergel.dsutils.DSArrayUtils;
import edu.berkeley.compbio.jlibsvm.SolutionModel;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class AlphaModel<L extends Comparable, P> extends SolutionModel<L, P>
	{
// ------------------------------ FIELDS ------------------------------

	// used only during training, then ditched
	public Map<P, Double> supportVectors;

	// more compact representation used after training
	public int numSVs;
	public P[] SVs;
	public double[] alphas;

	public float rho;


// --------------------------- CONSTRUCTORS ---------------------------

	protected AlphaModel()
		{
		super();
		}
/*
	public AlphaModel(Properties props, LabelParser<L> labelParser)
		{
		super(props, labelParser);
		rho = Float.parseFloat(props.getProperty("rho"));
		}

	protected AlphaModel(@NotNull ImmutableSvmParameterPoint<L, P> param)
		{
		super(param);
		}
		*/

// -------------------------- OTHER METHODS --------------------------

	/**
	 * Remove vectors whose alpha is zero, leaving only support vectors
	 */
	public void compact()
		{
		// do this first so as to make the arrays the right size below
		for (Iterator<Map.Entry<P, Double>> i = supportVectors.entrySet().iterator(); i.hasNext();)
			{
			Map.Entry<P, Double> entry = i.next();
			if (entry.getValue() == 0)
				{
				i.remove();
				}
			}


		// put the keys and values in parallel arrays, to free memory and maybe make things a bit faster (?)

		numSVs = supportVectors.size();
		SVs = (P[]) new Object[numSVs];
		alphas = new double[numSVs];

		int c = 0;
		for (Map.Entry<P, Double> entry : supportVectors.entrySet())
			{
			SVs[c] = entry.getKey();
			alphas[c] = entry.getValue();
			c++;
			}

		supportVectors = null;
		}

	protected void readSupportVectors(BufferedReader reader) throws IOException
		{
		//throw new NotImplementedException();
		List<Double> alphaList = new ArrayList<Double>();
		List<SparseVector> svList = new ArrayList<SparseVector>();

		String line;
		//int lineNo = 0;
		while ((line = reader.readLine()) != null)
			{
			StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

			//alphas[lineNo] = Float.parseFloat(st.nextToken());
			alphaList.add(Double.parseDouble(st.nextToken()));

			// ** Read directly into SparseVector instead of generic P... bah

			int n = st.countTokens() / 2;
			SparseVector p = new SparseVector(n);
			//supportVectors[lineNo] = p;
			for (int j = 0; j < n; j++)
				{
				p.indexes[j] = Integer.parseInt(st.nextToken());
				p.values[j] = Float.parseFloat(st.nextToken());
				}
			svList.add(p);
			}

		alphas = DSArrayUtils.toPrimitiveDoubleArray(alphaList);
		SVs = (P[]) svList.toArray(new SparseVector[0]);


		numSVs = SVs.length;

		supportVectors = null; // we read it directly to the compact representation
		}

	protected void writeSupportVectors(DataOutputStream fp) throws IOException
		{
		fp.writeBytes("SV\n");

		for (int i = 0; i < numSVs; i++)
			{
			fp.writeBytes(alphas[i] + " ");

			//	P p = supportVectors.get(i);

			fp.writeBytes(SVs[i].toString());

			/*			if (kernel instanceof PrecomputedKernel)
			   {
			   fp.writeBytes("0:" + (int) (p.values[0]));
			   }
		   else
			   {
			   for (int j = 0; j < p.indexes.length; j++)
				   {
				   fp.writeBytes(p.indexes[j] + ":" + p.values[j] + " ");
				   }
			   }*/
			fp.writeBytes("\n");
			}
		}

	public void writeToStream(DataOutputStream fp) throws IOException
		{
		super.writeToStream(fp);

		fp.writeBytes("rho " + rho + "\n");
		fp.writeBytes("total_sv " + numSVs + "\n");
		}
	}
