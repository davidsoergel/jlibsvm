package edu.berkeley.compbio.jlibsvm.legacyexec;

import edu.berkeley.compbio.jlibsvm.ContinuousModel;
import edu.berkeley.compbio.jlibsvm.DiscreteModel;
import edu.berkeley.compbio.jlibsvm.SolutionModel;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.multi.MultiClassModel;
import edu.berkeley.compbio.jlibsvm.regression.RegressionModel;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class svm_predict
	{
// --------------------------- main() method ---------------------------

	public static void main(String argv[]) throws IOException
		{
		int i, predict_probability = 0;

		// parse options
		for (i = 0; i < argv.length; i++)
			{
			if (argv[i].charAt(0) != '-')
				{
				break;
				}
			++i;
			switch (argv[i - 1].charAt(1))
				{
				case 'b':
					predict_probability = Integer.parseInt(argv[i]);
					break;
				default:
					System.err.print("Unknown option: " + argv[i - 1] + "\n");
					exit_with_help();
				}
			}
		if (i >= argv.length)
			{
			exit_with_help();
			}
		try
			{
			BufferedReader input = new BufferedReader(new FileReader(argv[i]));
			DataOutputStream output = new DataOutputStream(new FileOutputStream(argv[i + 2]));
			SolutionModel model = SolutionModel.identifyTypeAndLoad(argv[i + 1]);
			if (predict_probability == 1)
				{
				if (model instanceof MultiClassModel)
					{
					if (!((MultiClassModel) model)
							.supportsOneVsOneProbability()) //svm.svm_check_probability_model(model)==0)
						{
						System.err.print("Model does not support probability estimates\n");
						System.exit(1);
						}
					}
				else if (model instanceof RegressionModel)
					{
					if (!((RegressionModel) model).supportsLaplace()) //svm.svm_check_probability_model(model)==0)
						{
						System.err.print("Model does not support probability estimates\n");
						System.exit(1);
						}
					}
				else
					{
					System.err.print("Model does not support probability estimates\n");
					System.exit(1);
					}
				}
			else
				{
				if (model instanceof MultiClassModel && ((MultiClassModel) model).supportsOneVsOneProbability())
					{
					System.out.print("Model supports probability estimates, but disabled in prediction.\n");
					}
				else if (model instanceof RegressionModel && ((RegressionModel) model).supportsLaplace())
					{
					System.out.print("Model supports Laplace parameter estimation, but disabled in prediction.\n");
					}
				}
			predict(input, output, model, predict_probability);
			input.close();
			output.close();
			}
		catch (FileNotFoundException e)
			{
			exit_with_help();
			}
		catch (ArrayIndexOutOfBoundsException e)
			{
			exit_with_help();
			}
		}

	private static void predict(BufferedReader input, DataOutputStream output, SolutionModel model,
	                            int predict_probability) throws IOException
		{
		int correct = 0;
		int total = 0;
		double error = 0;
		double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;

		//	int svm_type = svm.svm_get_svm_type(model);
		//	int nr_class = svm.svm_get_nr_class(model);


		if (predict_probability == 1)
			{
			if (model instanceof RegressionModel) //svm_type == SvmParameter.EPSILON_SVR || svm_type == SvmParameter.NU_SVR)
				{
				System.out
						.print("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma="
								+ ((RegressionModel) model).laplaceParameter + "\n");
				}
			else
				{
				output.writeBytes("labels");
				for (Object i : model.getLabels()) // in insertion order!
					{
					output.writeBytes(" " + i);
					}

				output.writeBytes("\n");
				}
			}


		while (true)
			{
			String line = input.readLine();
			if (line == null)
				{
				break;
				}

			StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

			Float target = Float.parseFloat(st.nextToken());
			int m = st.countTokens() / 2;
			SparseVector x = new SparseVector(m);
			for (int j = 0; j < m; j++)
				{
				//x[j] = new svm_node();
				x.indexes[j] = Integer.parseInt(st.nextToken());
				x.values[j] = Float.parseFloat(st.nextToken());
				}

			Object prediction;
			if (predict_probability == 1
					&& model instanceof MultiClassModel) //(svm_type == SvmParameter.C_SVC || svm_type == SvmParameter.NU_SVC))
				{
				Map<Integer, Float> prob_estimates =
						((MultiClassModel<Integer, SparseVector>) model).predictProbability(x); //null;
				//v = svm.svm_predict_probability(model, x, prob_estimates);
				prediction = ((MultiClassModel<Integer, SparseVector>) model).bestProbabilityLabel(prob_estimates);
				output.writeBytes(prediction + " ");

				SortedMap<Integer, Float> prob_estimates_sorted = new TreeMap<Integer, Float>(prob_estimates);
				for (float prob_estimate : prob_estimates_sorted.values())
					{
					output.writeBytes(prob_estimate + " ");
					}
				output.writeBytes("\n");
				}
			else if (predict_probability == 1 && model instanceof RegressionModel)
				{
				prediction = ((RegressionModel) model).predictValue(x); //svm.svm_predict(model, x);
				output.writeBytes(prediction + " " + ((RegressionModel) model).laplaceParameter);
				}
			else if (model instanceof DiscreteModel)
					{
					prediction = ((DiscreteModel) model).predictLabel(x); //svm.svm_predict(model, x);
					output.writeBytes(prediction + "\n");
					}
				else if (model instanceof ContinuousModel)
						{
						prediction = ((ContinuousModel) model).predictValue(x); //svm.svm_predict(model, x);
						output.writeBytes(prediction + "\n");
						}
					else
						{
						throw new SvmException("Don't know how to predict using model: " + model.getClass());
						}

			if (prediction.equals(target))
				{
				++correct;
				}
			if (prediction instanceof Float)
				{
				Float v = (Float) prediction;
				error += (v - target) * (v - target);
				sumv += v;
				sumy += target;
				sumvv += v * v;
				sumyy += target * target;
				sumvy += v * target;
				}
			++total;
			}
		if (model instanceof RegressionModel) // OK we include OneClassSVC here too //svm_type == SvmParameter.EPSILON_SVR || svm_type == SvmParameter.NU_SVR)
			{
			System.out.print("Mean squared error = " + error / total + " (regression)\n");
			System.out.print("Squared correlation coefficient = "
					+ ((total * sumvy - sumv * sumy) * (total * sumvy - sumv * sumy)) / ((total * sumvv - sumv * sumv)
					* (total * sumyy - sumy * sumy)) + " (regression)\n");
			}
		else
			{
			System.out.print("Accuracy = " + (double) correct / total * 100 + "% (" + correct + "/" + total
					+ ") (classification)\n");
			}
		}

	private static void exit_with_help()
		{
		System.err.print("usage: svm_predict [options] test_file model_file output_file\n" + "options:\n"
				+ "-b probability_estimates: whether to predict probability estimates, 0 or 1 (default 0); one-class SVM not supported yet\n");
		System.exit(1);
		}
	}
