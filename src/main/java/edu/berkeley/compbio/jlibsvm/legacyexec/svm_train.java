package edu.berkeley.compbio.jlibsvm.legacyexec;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterGrid;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterPoint;
import edu.berkeley.compbio.jlibsvm.MutableSvmProblem;
import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SolutionModel;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationProblem;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationSVM;
import edu.berkeley.compbio.jlibsvm.binary.C_SVC;
import edu.berkeley.compbio.jlibsvm.binary.MutableBinaryClassificationProblemImpl;
import edu.berkeley.compbio.jlibsvm.binary.Nu_SVC;
import edu.berkeley.compbio.jlibsvm.kernel.GammaKernel;
import edu.berkeley.compbio.jlibsvm.kernel.GaussianRBFKernel;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.kernel.LinearKernel;
import edu.berkeley.compbio.jlibsvm.kernel.PolynomialKernel;
import edu.berkeley.compbio.jlibsvm.kernel.PrecomputedKernel;
import edu.berkeley.compbio.jlibsvm.kernel.SigmoidKernel;
import edu.berkeley.compbio.jlibsvm.labelinverter.StringLabelInverter;
import edu.berkeley.compbio.jlibsvm.multi.MultiClassModel;
import edu.berkeley.compbio.jlibsvm.multi.MultiClassificationSVM;
import edu.berkeley.compbio.jlibsvm.multi.MutableMultiClassProblemImpl;
import edu.berkeley.compbio.jlibsvm.oneclass.OneClassSVC;
import edu.berkeley.compbio.jlibsvm.regression.EpsilonSVR;
import edu.berkeley.compbio.jlibsvm.regression.MutableRegressionProblemImpl;
import edu.berkeley.compbio.jlibsvm.regression.Nu_SVR;
import edu.berkeley.compbio.jlibsvm.regression.RegressionSVM;
import edu.berkeley.compbio.jlibsvm.scaler.LinearScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModel;
import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.scaler.ScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.scaler.ZscoreScalingModelLearner;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;
import edu.berkeley.compbio.ml.CrossValidationResults;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

public class svm_train
	{
// ------------------------------ FIELDS ------------------------------

	/* svm_type */
	static final int C_SVC = 0;
	static final int NU_SVC = 1;
	static final int ONE_CLASS = 2;
	static final int EPSILON_SVR = 3;
	static final int NU_SVR = 4;

	/* kernel_type */
	static final int LINEAR = 0;
	static final int POLY = 1;
	static final int RBF = 2;
	static final int SIGMOID = 3;
	static final int PRECOMPUTED = 4;
	//KernelFunction kernel;
	SVM svm;

	ImmutableSvmParameter param;

	private MutableSvmProblem problem;		// set by read_problem
	private SolutionModel model;
	private String input_file_name;		// set by parse_command_line
	private String model_file_name;		// set by parse_command_line
	//private String error_msg;
	//private boolean cross_validation;
	private boolean crossValidation;
	//	private int nr_fold;
	private static final Float UNSPECIFIED_GAMMA = -1F;

// --------------------------- main() method ---------------------------

	public static void main(String argv[]) throws IOException
		{
		//	try
		//		{
		svm_train t = new svm_train();
		t.run(argv);
		//		}
		//	finally
		//		{
		//		Parallel.shutdown();
		//		}
		}

	private void run(String argv[]) throws IOException
		{
		parse_command_line(argv);
		read_problem();
		//error_msg = svm.svm_check_parameter(problem, param);

		long startTime = System.currentTimeMillis();

		if (svm instanceof BinaryClassificationSVM && problem.getLabels().size() > 2)
			{
			svm = new MultiClassificationSVM((BinaryClassificationSVM) svm);
			}

/*		if (error_msg != null)
			{
			System.err.print("Error: " + error_msg + "\n");
			System.exit(1);
			}*/


		//TreeExecutorService execService = new DepthFirstThreadPoolExecutor();

		model = svm.train(problem, param); //, execService);

		model.save(model_file_name);

		// CV might have been done already for grid search or whatever
		CrossValidationResults cv = model.getCrossValidationResults();
		if (cv == null && crossValidation)
			{
			// but if not, force it
			cv = svm.performCrossValidation(problem, param); //, execService);
			}
		if (cv != null)
			{
			System.out.println(cv.toString());
			}


		long endTime = System.currentTimeMillis();

		float time = (endTime - startTime) / 1000f;

		System.out.println("Finished in " + time + " secs");

		//	execService.shutdown();
		}

	/*
	private static float atof(String s)
		{
		float d = Float.valueOf(s).floatValue();
		if (Float.isNaN(d) || Float.isInfinite(d))
			{
			System.err.print("NaN or Infinity in input\n");
			System.exit(1);
			}
		return (d);
		}
*/

	private void parse_command_line(String argv[])
		{
		int i;

		//SvmParameter
		ImmutableSvmParameterGrid.Builder builder = ImmutableSvmParameterGrid.builder();
		//SvmParameterVariableBuilder vparam = new SvmParameterVariableBuilder();

		// default values
		/*	param.svm_type = svm_parameter.C_SVC;
							param.kernel_type = svm_parameter.RBF;
							param.degree = 3;
							param.gamma = 0;
							param.coef0 = 0;*/
		builder.nu = 0.5f;
		builder.cache_size = 100;
		builder.eps = 1e-3f;
		builder.p = 0.1f;
		builder.shrinking = true;
		builder.probability = false;
		builder.redistributeUnbalancedC = true;
		//param.nr_weight = 0;
		//param.weightLabel = new int[0];
		//param.weight = new float[0];


		ScalingModelLearner<SparseVector> scalingModelLearner = new NoopScalingModelLearner<SparseVector>();

		String scalingType = null;
		int scalingExamples = 1000;
		boolean normalizeL2 = false;
		int svm_type = 0;
		int kernel_type = 2;
		int degree = 3;
		Set<Float> gammaSet = new HashSet<Float>();
//		float gamma = 0;
		float coef0 = 0;

		// parse options
		for (i = 0; i < argv.length; i++)
			{
			if (argv[i].charAt(0) != '-')
				{
				break;
				}
			if (++i >= argv.length)
				{
				exit_with_help();
				}
			switch (argv[i - 1].charAt(1))
				{
				case 's':
					svm_type = Integer.parseInt(argv[i]);
					break;
				case 't':
					kernel_type = Integer.parseInt(argv[i]);
					break;
				case 'd':
					degree = Integer.parseInt(argv[i]);
					break;
				case 'g':
					gammaSet = new HashSet<Float>();
					for (String s : argv[i].split(","))
						{
						gammaSet.add(Float.parseFloat(s));
						}
					//gamma = Float.parseFloat(argv[i]);
					break;
				case 'r':
					coef0 = Float.parseFloat(argv[i]);
					break;
				case 'n':
					builder.nu = Float.parseFloat(argv[i]);
					break;
				case 'm':
					builder.cache_size = Float.parseFloat(argv[i]);
					break;
				case 'c':
					// override the default
					builder.Cset = new HashSet<Float>();

					for (String s : argv[i].split(","))
						{
						builder.Cset.add(Float.parseFloat(s));
						}
					break;
				case 'e':
					builder.eps = Float.parseFloat(argv[i]);
					break;
				case 'p':
					builder.p = Float.parseFloat(argv[i]);
					break;
				case 'h':
					builder.shrinking = argv[i].equals("1") || Boolean.parseBoolean(argv[i]);
					break;
				case 'b':
					builder.probability = argv[i].equals("1") || Boolean.parseBoolean(argv[i]);
					break;
				case 'u':
					builder.redistributeUnbalancedC = argv[i].equals("1") || Boolean.parseBoolean(argv[i]);
					break;
				case 'v':
					builder.crossValidationFolds = Integer.parseInt(argv[i]);
					if (builder.crossValidationFolds < 2)
						{
						System.err.print("n-fold cross validation: n must >= 2\n");
						exit_with_help();
						}
					break;
				case 'q':
					// don't put this in the param; that causes a hassle with the nested learning jobs
					crossValidation = argv[i].equals("1") || Boolean.parseBoolean(argv[i]);
					break;
				case 'w':
					builder.putWeight(Integer.parseInt(argv[i - 1].substring(2)), Float.parseFloat(argv[i]));
					break;
				case 'a':
					builder.allVsAllMode = MultiClassModel.AllVsAllMode.valueOf(argv[i]);
					break;
				case 'o':
					builder.oneVsAllMode = MultiClassModel.OneVsAllMode.valueOf(argv[i]);
					break;
				case 'k':
					builder.oneVsAllThreshold = Double.parseDouble(argv[i]);
					break;
				case 'j':
					builder.minVoteProportion = Double.parseDouble(argv[i]);
					break;
				case 'f':
					scalingType = argv[i];

					break;
				case 'x':
					scalingExamples = Integer.parseInt(argv[i]);
					break;
				case 'l':
					int normalizeDim = Integer.parseInt(argv[i]);
					if (normalizeDim == 2)
						{
						normalizeL2 = true;
						}
					else
						{
						System.err.print("-l must == 2\n");
						exit_with_help();
						}
					break;
				case 'y':
					builder.gridsearchBinaryMachinesIndependently =
							argv[i].equals("1") || Boolean.parseBoolean(argv[i]);
					break;
				case 'z':
					builder.scaleBinaryMachinesIndependently = argv[i].equals("1") || Boolean.parseBoolean(argv[i]);
					break;
				default:
					System.err.print("Unknown option: " + argv[i - 1] + "\n");
					exit_with_help();
				}
			}

		if (scalingType == null)
			{
			// do nothing
			}
		else if (scalingType.equals("linear"))
			{
			scalingModelLearner = new LinearScalingModelLearner(scalingExamples, normalizeL2);
			}
		else if (scalingType.equals("zscore"))
				{
				scalingModelLearner = new ZscoreScalingModelLearner(scalingExamples, normalizeL2);
				}

		// determine filenames

		if (i >= argv.length)
			{
			exit_with_help();
			}

		input_file_name = argv[i];

		if (i < argv.length - 1)
			{
			model_file_name = argv[i + 1];
			}
		else
			{
			int p = argv[i].lastIndexOf('/');
			++p;	// whew...
			model_file_name = argv[i].substring(p) + ".model";
			}

		if (gammaSet.isEmpty())
			{
			gammaSet.add(UNSPECIFIED_GAMMA);
			}

		builder.kernelSet = new HashSet<KernelFunction>();

		switch (kernel_type)
			{
			case svm_train.LINEAR:
				builder.kernelSet.add(new LinearKernel());
				break;
			case svm_train.POLY:
				for (Float gamma : gammaSet)
					{
					builder.kernelSet.add(new PolynomialKernel(degree, gamma, coef0));
					}
				break;
			case svm_train.RBF:
				for (Float gamma : gammaSet)
					{
					builder.kernelSet.add(new GaussianRBFKernel(gamma));
					}
				break;
			case svm_train.SIGMOID:
				for (Float gamma : gammaSet)
					{
					builder.kernelSet.add(new SigmoidKernel(gamma, coef0));
					}
				break;
			case svm_train.PRECOMPUTED:
				builder.kernelSet.add(new PrecomputedKernel());
				break;
			default:
				throw new SvmException("Unknown kernel type: " + kernel_type);
			}

		builder.scalingModelLearner = scalingModelLearner;
		//param.kernel = kernel;

		this.param = builder.build();

		//ivparam = new SvmParameterVariable(vparam);
		switch (svm_type)
			{
			case svm_train.C_SVC:
				svm = new C_SVC();
				break;
			case svm_train.NU_SVC:
				svm = new Nu_SVC();
				break;
			case svm_train.ONE_CLASS:
				svm = new OneClassSVC();
				break;
			case svm_train.EPSILON_SVR:
				svm = new EpsilonSVR();
				break;
			case svm_train.NU_SVR:
				svm = new Nu_SVR();
				break;
			default:
				throw new SvmException("Unknown svm type: " + kernel_type);
			}
		}

	private static void exit_with_help()
		{
		System.out.print("Usage: svm_train [options] training_set_file [model_file]\n" + "options:\n"
		                 + "-s svm_type : set type of SVM (default 0)\n" + "	0 -- C-SVC\n" + "	1 -- nu-SVC\n"
		                 + "	2 -- one-class SVM\n" + "	3 -- epsilon-SVR\n" + "	4 -- nu-SVR\n"
		                 + "-t kernel_type : set type of kernel function (default 2)\n" + "	0 -- linear: u'*v\n"
		                 + "	1 -- polynomial: (gamma*u'*v + coef0)^degree\n"
		                 + "	2 -- radial basis function: exp(-gamma*|u-v|^2)\n"
		                 + "	3 -- sigmoid: tanh(gamma*u'*v + coef0)\n"
		                 + "	4 -- precomputed kernel (kernel values in training_set_file)\n"
		                 + "-d degree : set degree in kernel function (default 3)\n"
		                 + "-g gamma : set gamma in kernel function (default 1/k)\n"
		                 + "-r coef0 : set coef0 in kernel function (default 0)\n"
		                 + "-c cost : set the parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1)\n"
		                 + "-n nu : set the parameter nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)\n"
		                 + "-p epsilon : set the epsilon in loss function of epsilon-SVR (default 0.1)\n"
		                 + "-m cachesize : set cache memory size in MB (default 100)\n"
		                 + "-e epsilon : set tolerance of termination criterion (default 0.001)\n"
		                 + "-h shrinking: whether to use the shrinking heuristics, 0 or 1 (default 1)\n"
		                 + "-b probability_estimates: whether to train a SVC or SVR model for probability estimates, 0 or 1 (default 0)\n"
		                 + "-wi weight: set the parameter C of class i to weight*C, for C-SVC (default 1)\n"
		                 + "-a allVsAllMode: None, AllVsAll, FilteredVsAll, FilteredVsFiltered\n"
		                 + "-j minVoteProportion: the chosen class must have at least this proportion of the total votes\n"
		                 + "-o oneVsAllMode: None, Best, Veto, BreakTies, VetoAndBreakTies \n"
		                 + "-k oneVsAllProb: the chosen class must have at least this one-vs-all probability; if -b is not set, probabilities are 0 or 1\n"
		                 + "-v n: n-fold cross validation mode\n" + "-f scalingmode : none (default), linear, zscore\n"
		                 + "-x scalinglimit : maximum examples to use for scaling (default 1000)\n"
		                 + "-l 2: project to unit sphere (normalize L2 distance)\n");
		System.exit(1);
		}

	// read in a problem (in svmlight format)

	private void read_problem() throws IOException
		{
		BufferedReader fp = new BufferedReader(new FileReader(input_file_name));
		Vector<Float> vy = new Vector<Float>();
		Vector<SparseVector> vx = new Vector<SparseVector>();
		int max_index = 0;

		while (true)
			{
			String line = fp.readLine();
			if (line == null)
				{
				break;
				}

			StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

			vy.addElement(Float.parseFloat(st.nextToken()));
			int m = st.countTokens() / 2;
			SparseVector x = new SparseVector(m);
			for (int j = 0; j < m; j++)
				{
				//x[j] = new svm_node();
				x.indexes[j] = Integer.parseInt(st.nextToken());
				x.values[j] = Float.parseFloat(st.nextToken());
				}
			if (m > 0)
				{
				max_index = Math.max(max_index, x.indexes[m - 1]);
				}
			vx.addElement(x);
			}


		// build problem
		if (svm instanceof RegressionSVM)
			{
			problem = new MutableRegressionProblemImpl(vy.size());
			}
		else
			{
			Set<Float> uniqueClasses = new HashSet<Float>(vy);
			int numClasses = uniqueClasses.size();
			if (numClasses == 1)
				{
				problem = new MutableRegressionProblemImpl(vy.size());
				}
			else if (numClasses == 2)
				{
				problem = new MutableBinaryClassificationProblemImpl(String.class, vy.size());
				}
			else
				{
				problem =
						new MutableMultiClassProblemImpl<String, SparseVector>(String.class, new StringLabelInverter(),
						                                                       vy.size(),
						                                                       new NoopScalingModel<SparseVector>());
				}

			/*for (int i = 0; i < vy.size(); i++)
				{
				problem.addExample(vx.elementAt(i))
				problem.examples[i] = vx.elementAt(i);
				}*/
			}


		//boolean isClassification = svm instanceof BinaryClassificationSVM;
		for (int i = 0; i < vy.size(); i++)
			{
			problem.addExampleFloat(vx.elementAt(i), vy.elementAt(i));
			/*		if (isClassification)
			   {
			   problem.uniqueValues.add(new Float(vy.elementAt(i)));
			   }*/
			}
		if (problem instanceof BinaryClassificationProblem)
			{
			((BinaryClassificationProblem) problem).setupLabels();
			}


		if (param instanceof ImmutableSvmParameterGrid)
			{
			Collection<ImmutableSvmParameterPoint> gridParams = ((ImmutableSvmParameterGrid) param).getGridParams();
			for (ImmutableSvmParameterPoint subparam : gridParams)
				{
				updateKernelWithNumExamples(subparam, max_index);
				}
			}
		else
			{
			updateKernelWithNumExamples((ImmutableSvmParameterPoint) param, max_index);
			}

		fp.close();
		}

	private void updateKernelWithNumExamples(ImmutableSvmParameterPoint pointParam, int max_index)
		{
		KernelFunction kernel = pointParam.kernel;

		if (kernel instanceof GammaKernel && ((GammaKernel) kernel).getGamma() == UNSPECIFIED_GAMMA)
			{
			((GammaKernel) kernel).setGamma(1.0f / max_index);
			}
		if (kernel instanceof PrecomputedKernel)
			{
			throw new NotImplementedException();
			/*
		for (int i = 0; i < vy.size(); i++)
			{
			if (problem.examples[i].indexes[0] != 0)
				{
				System.err.print("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
				System.exit(1);
				}
			if ((int) problem.examples[i].values[0] <= 0 || (int) problem.examples[i].values[0] > max_index)
				{
				System.err.print("Wrong input format: sample_serial_number out of range\n");
				System.exit(1);
				}
			}*/
			}
		}

/*	private void do_cross_validation()
		{
		//int i;
		int total_correct = 0;
		int total_unknown = 0;
		double total_error = 0;
		double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
		//double[] target = new double[problem.l];

		int numExamples = problem.getNumExamples();

		if (svm instanceof RegressionSVM) //param.svm_type == svm_parameter.EPSILON_SVR || param.svm_type == svm_parameter.NU_SVR)
			{
			Map cvResult = svm.continuousCrossValidation(problem, param);
			//for (i = 0; i < numExamples; i++)
			for (Object p : problem.getExamples().keySet())
				{
				Float y = (Float) problem.getTargetValue(p);
				Float v = (Float) cvResult.get(p);
				total_error += (v - y) * (v - y);
				sumv += v;
				sumy += y;
				sumvv += v * v;
				sumyy += y * y;
				sumvy += v * y;
				}
			System.out.print("Cross Validation Mean squared error = " + total_error / numExamples + "\n");
			System.out.print("Cross Validation Squared correlation coefficient = "
					+ ((numExamples * sumvy - sumv * sumy) * (numExamples * sumvy - sumv * sumy)) / (
					(numExamples * sumvv - sumv * sumv) * (numExamples * sumyy - sumy * sumy)) + "\n");
			}
		else
			{
			Map cvResult = svm.discreteCrossValidation(problem, param);
			for (Object p : problem.getExamples().keySet())
				//	for (i = 0; i < numExamples; i++)
				{
				Object prediction = cvResult.get(p);
				if (prediction == null)
					{
					++total_unknown;
					}
				else if (prediction.equals(problem.getTargetValue(p)))
					{
					++total_correct;
					}
				}

			int classifiedExamples = numExamples - total_unknown;
			System.out.print("Cross Validation Classified = " + 100.0 * classifiedExamples / numExamples + "%\n");
			System.out.print("Cross Validation Accuracy (of those classified) = "
					+ 100.0 * total_correct / classifiedExamples + "%\n");
			System.out.print("Cross Validation Accuracy (of total) = " + 100.0 * total_correct / numExamples + "%\n");
			}
		}*/
	}
