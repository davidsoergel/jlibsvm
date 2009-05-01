package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class SolutionModel<P> extends SvmContext
	{
// ------------------------------ FIELDS ------------------------------

	/**
	 * names the SVM that was used to produce this model; used only for writeToStream
	 */
	public String svmType;


// -------------------------- STATIC METHODS --------------------------

	public static SolutionModel identifyTypeAndLoad(String model_file_name)
		{
		try
			{
			BufferedReader fp = new BufferedReader(new FileReader(model_file_name));
			Properties props = new Properties();
			//props.load(new StringReader(readUpToSVs(fp))));  // java 1.6 only
			props.load(new StringBufferInputStream(readUpToSVs(fp)));

			// first figure out which model type it is
			Class c = Class.forName(props.getProperty("svm_type"));

			SolutionModel model = (SolutionModel) (c.getConstructor(Properties.class).newInstance(props));

			model.readSupportVectors(fp);
			fp.close();
			return model;
			}
		catch (Throwable e)
			{
			throw new SvmException("Unable to load file " + model_file_name, e);
			}
		}

	private static String readUpToSVs(BufferedReader reader) throws IOException
		{
		StringBuffer sb = new StringBuffer();
		while (true)
			{
			String l = reader.readLine();
			if (l.startsWith("SV"))
				{
				break;
				}
			sb.append(l);
			}
		return sb.toString();
		}

	protected abstract void readSupportVectors(BufferedReader fp) throws IOException;


// --------------------------- CONSTRUCTORS ---------------------------

	public SolutionModel()
		{
		super();
		}

	public SolutionModel(Properties props)
		{
		super(null, null);

		try
			{
			kernel = (KernelFunction) Class.forName(props.getProperty("kernel_type")).getConstructor(Properties.class)
					.newInstance(props);
			}
		catch (Throwable e)
			{
			throw new SvmException(e);
			}

		// param is only useful for training; when loading a trained model for testing, we can leave it null
		//param = new SvmParameter(props);

		// ... oops, except that we need the labels.

		param = new SvmParameter();

		StringTokenizer st = new StringTokenizer(props.getProperty("label"));
		while (st.hasMoreTokens())
			{
			param.putWeight(new Integer(st.nextToken()), null);
			}
		}

	public SolutionModel(KernelFunction kernel, SvmParameter param)
		{
		super(kernel, param);
		}

// --------------------- GETTER / SETTER METHODS ---------------------

	public void setSvmType(String svmType)
		{
		this.svmType = svmType;
		}

// -------------------------- OTHER METHODS --------------------------

	public void save(String model_file_name) throws IOException
		{
		DataOutputStream fp = new DataOutputStream(new FileOutputStream(model_file_name));
		writeToStream(fp);
		}

	/**
	 * This really should be addToProps, for symmetry
	 *
	 * @param fp
	 * @throws IOException
	 */
	protected void writeToStream(DataOutputStream fp) throws IOException
		{
		fp.writeBytes("svm_type " + svmType + "\n");
		fp.writeBytes(kernel.toString());

		fp.writeBytes("label");
		for (Object i : param.getWeights().keySet())  // note these are in insertion order
			{
			fp.writeBytes(" " + i);
			}
		fp.writeBytes("\n");
		}
	}
