package edu.berkeley.compbio.jlibsvm;

import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.util.Iterator;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class SVM<T extends Comparable, P extends SvmProblem<T, P>> extends SvmContext
	{
	//
	// construct and solve various formulations
	//
	public static final int LIBSVM_VERSION = 288;


	protected SVM(KernelFunction kernel, SvmParameter<T> param)
		{
		super(kernel, param);
		if (param.eps < 0)
			{
			throw new SvmException("eps < 0");
			}
		}


	public abstract String getSvmType();
	// label: label name, start: begin of each class, count: #data of classes, perm: indices to the original data

	// perm, length l, must be allocated before calling this subroutine


	//
	// Interface functions

	//

	public abstract SolutionModel train(P problem);//, svm_parameter param);

//	public abstract FoldSpec separateFolds(svm_problem problem, int nr_fold);


	protected abstract T[] foldPredict(P subprob, Iterator<SvmPoint> foldIterator, int length);


	public FoldSpec separateFolds(P problem, int numberOfFolds)
		{
		FoldSpec fs = new FoldSpec(problem.examples.length, numberOfFolds);
		for (int i = 0; i < fs.perm.length; i++)
			{
			fs.perm[i] = i;
			}
		//	Collections.shuffle(perm);
		for (int i = 0; i < fs.perm.length; i++)
			{
			int j = i + (int) (Math.random() * (fs.perm.length - i));

			int tmp = fs.perm[i];
			fs.perm[i] = fs.perm[j];
			fs.perm[j] = tmp;
			}
		for (int i = 0; i <= numberOfFolds; i++)
			{
			fs.foldStart[i] = i * fs.perm.length / numberOfFolds;
			}
		return fs;
		}

	public abstract Class getLabelClass();

	public T[] crossValidation(P problem, int numberOfFolds)
		{
		Class type = (Class) getLabelClass();
		T[] predictions = (T[]) java.lang.reflect.Array.newInstance(type, problem.examples.length);
		//T[] predictions = (T[]) new Object[problem.examples.length];

		if (numberOfFolds >= problem.examples.length)
			{
			throw new SvmException("Can't have more cross-validation folds than there are examples");
			}

		FoldSpec fs = separateFolds(problem, numberOfFolds);


//		List<svm_node[]> exampleSets = permutedTiledSubsets(problem.examples, nr_fold);

		// stratified cv may not give leave-one-out rate		// Each class to l folds -> some folds may have zero elements


		for (int i = 0; i < numberOfFolds; i++)
			{
			int begin = fs.foldStart[i];
			int end = fs.foldStart[i + 1];


			int subprobLength = problem.examples.length - (end - begin);

			//	subprob.examples = new SvmPoint[subprobLength];
			//subprob.targetValues = new float[subprobLength];

			P subprob = problem.newSubProblem(subprobLength); //new SvmProblem();

			int k = 0;
			for (int j = 0; j < begin; j++)
				{
				subprob.examples[k] = problem.examples[fs.perm[j]];
				subprob.targetValues[k] = problem.targetValues[fs.perm[j]];
				++k;
				}
			for (int j = end; j < fs.perm.length; j++)
				{
				subprob.examples[k] = problem.examples[fs.perm[j]];
				subprob.targetValues[k] = problem.targetValues[fs.perm[j]];
				++k;
				}

			T[] foldPredictions = foldPredict(subprob, new FoldIterator(problem, fs.perm, begin, end), end - begin);

			for (int j = begin; j < end; j++)
				{
				predictions[fs.perm[j]] = foldPredictions[j - begin];
				}
			}
		// now target contains the prediction for each point based on training with e.g. 80% of the other points (for 5-fold).
		return predictions;
		}


/*	public static int svm_get_svm_type(svm_model model)
		{
		return model.param.svm_type;
		}

	public static int svm_get_nr_class(svm_model model)
		{
		return model.nr_class;
		}

	public static void svm_get_labels(svm_model model, int[] label)
		{
		if (model.label != null)
			{
			for (int i = 0; i < model.nr_class; i++)
				{
				label[i] = model.label[i];
				}
			}
		}
*/

	//public abstract float svm_predict_probability(svm_model model, svm_node[] x, float[] prob_estimates);


/*
   static final String svm_type_table[] = {
		   "c_svc",
		   "nu_svc",
		   "one_class",
		   "epsilon_svr",
		   "nu_svr",
   };

   static final String kernel_type_table[] = {
		   "linear",
		   "polynomial",
		   "rbf",
		   "sigmoid",
		   "precomputed"
   };*/


/*
	private static float atof(String s)
		{
		return Double.valueOf(s).doubleValue();
		}

	private static int atoi(String s)
		{
		return Integer.parseInt(s);
		}

	public static svm_model svm_load_model(String model_file_name) throws IOException
		{
		BufferedReader fp = new BufferedReader(new FileReader(model_file_name));

		// read parameters

		svm_model model = new svm_model();
		svm_parameter param = new svm_parameter();
		model.param = param;
		model.rho = null;
		model.probA = null;
		model.probB = null;
		model.label = null;
		model.nSV = null;

		while (true)
			{
			String cmd = fp.readLine();
			String arg = cmd.substring(cmd.indexOf(' ') + 1);

			if (cmd.startsWith("svm_type"))
				{
				int i;
				for (i = 0; i < svm_type_table.length; i++)
					{
					if (arg.indexOf(svm_type_table[i]) != -1)
						{
						param.svm_type = i;
						break;
						}
					}
				if (i == svm_type_table.length)
					{
					System.err.print("unknown svm type.\n");
					return null;
					}
				}
			else if (cmd.startsWith("kernel_type"))
				{
				int i;
				for (i = 0; i < kernel_type_table.length; i++)
					{
					if (arg.indexOf(kernel_type_table[i]) != -1)
						{
						param.kernel_type = i;
						break;
						}
					}
				if (i == kernel_type_table.length)
					{
					System.err.print("unknown kernel function.\n");
					return null;
					}
				}
			else if (cmd.startsWith("degree"))
				{
				param.degree = atoi(arg);
				}
			else if (cmd.startsWith("gamma"))
				{
				param.gamma = atof(arg);
				}
			else if (cmd.startsWith("coef0"))
				{
				param.coef0 = atof(arg);
				}
			else if (cmd.startsWith("nr_class"))
				{
				model.nr_class = atoi(arg);
				}
			else if (cmd.startsWith("total_sv"))
				{
				model.l = atoi(arg);
				}
			else if (cmd.startsWith("rho"))
				{
				int n = model.nr_class * (model.nr_class - 1) / 2;
				model.rho = new float[n];
				StringTokenizer st = new StringTokenizer(arg);
				for (int i = 0; i < n; i++)
					{
					model.rho[i] = atof(st.nextToken());
					}
				}
			else if (cmd.startsWith("label"))
				{
				int n = model.nr_class;
				model.label = new int[n];
				StringTokenizer st = new StringTokenizer(arg);
				for (int i = 0; i < n; i++)
					{
					model.label[i] = atoi(st.nextToken());
					}
				}
			else if (cmd.startsWith("probA"))
				{
				int n = model.nr_class * (model.nr_class - 1) / 2;
				model.probA = new float[n];
				StringTokenizer st = new StringTokenizer(arg);
				for (int i = 0; i < n; i++)
					{
					model.probA[i] = atof(st.nextToken());
					}
				}
			else if (cmd.startsWith("probB"))
				{
				int n = model.nr_class * (model.nr_class - 1) / 2;
				model.probB = new float[n];
				StringTokenizer st = new StringTokenizer(arg);
				for (int i = 0; i < n; i++)
					{
					model.probB[i] = atof(st.nextToken());
					}
				}
			else if (cmd.startsWith("nr_sv"))
				{
				int n = model.nr_class;
				model.nSV = new int[n];
				StringTokenizer st = new StringTokenizer(arg);
				for (int i = 0; i < n; i++)
					{
					model.nSV[i] = atoi(st.nextToken());
					}
				}
			else if (cmd.startsWith("SV"))
				{
				break;
				}
			else
				{
				System.err.print("unknown text in model file: [" + cmd + "]\n");
				return null;
				}
			}

		// read sv_coef and SV

		int m = model.nr_class - 1;
		int l = model.l;
		model.sv_coef = new float[m][l];
		model.SV = new svm_node[l][];

		for (int i = 0; i < l; i++)
			{
			String line = fp.readLine();
			StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

			for (int k = 0; k < m; k++)
				{
				model.sv_coef[k][i] = atof(st.nextToken());
				}
			int n = st.countTokens() / 2;
			model.SV[i] = new svm_node[n];
			for (int j = 0; j < n; j++)
				{
				model.SV[i][j] = new svm_node();
				model.SV[i][j].index = atoi(st.nextToken());
				model.SV[i][j].value = atof(st.nextToken());
				}
			}

		fp.close();
		return model;
		}
*/
/*	public static int svm_check_probability_model(svm_model model)
		{
		if (((model.param.svm_type == svm_parameter.C_SVC || model.param.svm_type == svm_parameter.NU_SVC)
				&& model.probA != null && model.probB != null) || (
				(model.param.svm_type == svm_parameter.EPSILON_SVR || model.param.svm_type == svm_parameter.NU_SVR)
						&& model.probA != null))
			{
			return 1;
			}
		else
			{
			return 0;
			}
		}
		*/

	protected class FoldIterator implements Iterator<SvmPoint>
		{
		int index;
		private int end;
		private SvmPoint[] examples;
		private int[] perm;

		public FoldIterator(SvmProblem problem, int[] perm, int begin, int end)
			{
			index = begin;
			this.end = end;
			this.examples = problem.examples;
			this.perm = perm;
			}

		public boolean hasNext()
			{
			return index < end;
			}

		public SvmPoint next()
			{
			SvmPoint result = examples[perm[index]];
			index++;
			return result;
			}

		public void remove()
			{
			throw new UnsupportedOperationException();
			}
		}
	}