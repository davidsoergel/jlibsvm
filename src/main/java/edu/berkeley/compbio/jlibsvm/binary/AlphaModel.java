package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.SolutionModel;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

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
		throw new NotImplementedException();
		/*
		String line;
		int lineNo = 0;
		while ((line = reader.readLine()) != null)
			{
			StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

			alpha[lineNo] = Float.parseFloat(st.nextToken());

			// ** SparseVector not generified here, bah

			int n = st.countTokens() / 2;
			SparseVector p = new SparseVector(n);
			supportVectors[lineNo] = p;
			for (int j = 0; j < n; j++)
				{
				p.indexes[j] = Integer.parseInt(st.nextToken());
				p.values[j] = Float.parseFloat(st.nextToken());
				}
			}
			*/
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
