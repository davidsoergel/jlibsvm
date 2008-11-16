package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.FoldSpec;
import edu.berkeley.compbio.jlibsvm.SVM;
import edu.berkeley.compbio.jlibsvm.SvmException;
import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;

import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Type;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class BinaryClassificationSVM extends SVM<Boolean, BinaryClassificationProblem>
	{
	protected BinaryClassificationSVM(KernelFunction kernel, SvmParameter<Boolean> param)
		{
		super(kernel, param);
		}

	public Boolean[] makeTArray(int length)
		{
		return new Boolean[length];
		}

	public BinaryModel train(BinaryClassificationProblem problem)
		{

		// convert arbitrary labels into +1 , -1
		//	assert param.weights.size() == 2;

		//	Iterator<Integer> it= param.weights.keySet().iterator();
		//	float label1 = it.next();
		//float label2 = it.next();
/*		Boolean label1 = problem.targetValues[0];  // should be int
		Boolean label2 = null;


		for (int i = 0; i < problem.targetValues.length; i++)
			{
			if (problem.targetValues[i] == label1)
				{
				problem.targetValues[i] = 1f;
				}
			else
				{
				if (label2 == null)
					{
					label2 = (int) problem.targetValues[i];
					}
				else if (!label2.equals((int) problem.targetValues[i]))
					{
					throw new SvmException("Can't do binary classification; more than two classes found");
					}
				problem.targetValues[i] = -1f;
				}
			}
		if (label2 == null)
			{
			throw new SvmException("Can't do binary classification; only one class was found");
			}*/
		if (problem.getNumLabels() == 1)
			{
			throw new SvmException("Can't do binary classification; only one class was found");
			}


		// calculate weighted C

		float weightedCp = param.C;
		float weightedCn = param.C;

		Float weightP = param.getWeight(true);
		if (weightP != null)
			{
			weightedCp *= weightP;
			}

		Float weightN = param.getWeight(false);
		if (weightN != null)
			{
			weightedCn *= weightN;
			}

		BinaryModel result = trainOne(problem, weightedCp, weightedCn);
		result.printSolutionInfo(problem);
		return result;
		}

	public abstract BinaryModel trainOne(BinaryClassificationProblem problem, float Cp, float Cn);

	/*
	static decision_function svm_train_one(svm_problem prob, svm_parameter param, float Cp, float Cn)
		{
		float[] alpha = new float[prob.examples.length];
		SolutionInfo si = new SolutionInfo();
		switch (param.svm_type)
			{
			case svm_parameter.C_SVC:
				solve_c_svc(prob, param, alpha, si, Cp, Cn);
				break;
			case svm_parameter.NU_SVC:
				solve_nu_svc(prob, param, alpha, si);
				break;
			case svm_parameter.ONE_CLASS:
				solve_one_class(prob, param, alpha, si);
				break;
			case svm_parameter.EPSILON_SVR:
				solve_epsilon_svr(prob, param, alpha, si);
				break;
			case svm_parameter.NU_SVR:
				solve_nu_svr(prob, param, alpha, si);
				break;
			}

		printSolutionInfo(prob, alpha, si);

		decision_function f = new decision_function();
		f.alpha = alpha;
		f.rho = si.rho;
		return f;
		}
		*/

	public FoldSpec separateFolds(BinaryClassificationProblem problem, int numberOfFolds)
		{
		FoldSpec fs = new FoldSpec(problem.examples.length, numberOfFolds);

		//problem.groupClasses(problem, fs.perm);


		BinaryClassificationProblem.GroupedClasses groupedExamples = problem.groupClasses(fs.perm);
		//	int nr_class = tmp_nr_class[0];
		//	int[] label = tmp_label[0];
		//	int[] start = tmp_start[0];
		//	int[] count = tmp_count[0];
		int numberOfClasses = groupedExamples.numberOfClasses;
		//	List<Integer> label = groupedExamples.label;
		int[] start = groupedExamples.start;
		List<Integer> count = groupedExamples.count;

		// random shuffle and then data grouped by fold using the array perm
		int[] foldCount = new int[numberOfFolds];

		int[] index = new int[fs.perm.length];
		for (int i = 0; i < fs.perm.length; i++)
			{
			index[i] = fs.perm[i];
			}
		for (int c = 0; c < numberOfClasses; c++)
			{
			for (int i = 0; i < count.get(c); i++)
				{
				int j = i + (int) (Math.random() * (count.get(c) - i));

				int swap = index[start[c] + j];
				index[start[c] + j] = index[start[c] + i];
				index[start[c] + i] = swap;
				}
			}
		for (int i = 0; i < numberOfFolds; i++)
			{
			foldCount[i] = 0;
			for (int c = 0; c < numberOfClasses; c++)
				{
				foldCount[i] += (i + 1) * count.get(c) / numberOfFolds - i * count.get(c) / numberOfFolds;
				}
			}
		fs.foldStart[0] = 0;
		for (int i = 1; i <= numberOfFolds; i++)
			{
			fs.foldStart[i] = fs.foldStart[i - 1] + foldCount[i - 1];
			}
		for (int c = 0; c < numberOfClasses; c++)
			{
			for (int i = 0; i < numberOfFolds; i++)
				{
				int begin = start[c] + i * count.get(c) / numberOfFolds;
				int end = start[c] + (i + 1) * count.get(c) / numberOfFolds;
				for (int j = begin; j < end; j++)
					{
					fs.perm[fs.foldStart[i]] = index[j];
					fs.foldStart[i]++;
					}
				}
			}
		fs.foldStart[0] = 0;
		for (int i = 1; i <= numberOfFolds; i++)
			{
			fs.foldStart[i] = fs.foldStart[i - 1] + foldCount[i - 1];
			}
		return fs;
		}

	@Override
	public Type getGenericType()
		{
		return Boolean.class; // ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		}

	protected Boolean[] foldPredict(BinaryClassificationProblem subprob, Iterator<SvmPoint> foldIterator, int length)
		{
		BinaryModel model = train(subprob);
		Boolean[] result = new Boolean[length];

		int i = 0;
		while (foldIterator.hasNext())
			{
			result[i] = model.predictLabel(foldIterator.next());
			i++;
			}
		return result;
		}


	// Cross-validation decision values for probability estimates
	// ** unify with SVM.crossValidation?

	public float[] svcProbability(BinaryClassificationProblem problem, float Cp, float Cn)
		{

		int i;
		int numberOfFolds = 5;
		int[] perm = new int[problem.examples.length];
		float[] decisionValues = new float[problem.examples.length];

		// random shuffle
		for (i = 0; i < problem.examples.length; i++)
			{
			perm[i] = i;
			}
		for (i = 0; i < problem.examples.length; i++)
			{
			int j = i + (int) (Math.random() * (problem.examples.length - i));

			int swap = perm[i];
			perm[i] = perm[j];
			perm[j] = swap;
			}
		for (i = 0; i < numberOfFolds; i++)
			{
			int begin = i * problem.examples.length / numberOfFolds;
			int end = (i + 1) * problem.examples.length / numberOfFolds;
			int j, k;
			int subprobLength = problem.examples.length - (end - begin);
			BinaryClassificationProblem subprob = new BinaryClassificationProblem(subprobLength);

			//subprob.examples = new SvmPoint[subprobLength];
			//subprob.targetValues = new float[subprobLength];

			k = 0;
			for (j = 0; j < begin; j++)
				{
				subprob.examples[k] = problem.examples[perm[j]];
				subprob.putTargetValue(k, problem.getTargetValue(perm[j]));
				++k;
				}
			for (j = end; j < problem.examples.length; j++)
				{
				subprob.examples[k] = problem.examples[perm[j]];
				subprob.putTargetValue(k, problem.getTargetValue(perm[j]));
				++k;
				}
			int positiveCount = 0, negativeCount = 0;
			for (j = 0; j < k; j++)
				{
				if (subprob.getTargetValue(j))
					{
					positiveCount++;
					}
				else
					{
					negativeCount++;
					}
				}

			if (positiveCount == 0 && negativeCount == 0)
				{
				for (j = begin; j < end; j++)
					{
					decisionValues[perm[j]] = 0;
					}
				}
			else if (positiveCount > 0 && negativeCount == 0)
				{
				for (j = begin; j < end; j++)
					{
					decisionValues[perm[j]] = 1;
					}
				}
			else if (positiveCount == 0 && negativeCount > 0)
					{
					for (j = begin; j < end; j++)
						{
						decisionValues[perm[j]] = -1;
						}
					}
				else
					{
					SvmParameter<Boolean> subparam = new SvmParameter<Boolean>(param);
					subparam.probability = false;
					subparam.C = 1.0f;
					//subparam.nr_weight = 2;
					subparam.putWeight(true, Cp);
					subparam.putWeight(false, Cn);

					SvmParameter origParam = param;
					param = subparam;

					BinaryModel submodel = train(subprob);

					param = origParam;

					for (j = begin; j < end; j++)
						{
						Float decisionValue = submodel.predictValue(problem.examples[perm[j]]);
						decisionValues[perm[j]] = decisionValue;
						// ensure +1 -1 order; reason not using CV subroutine
						//	decisionValues[perm[j]] *= submodel.label[0];  // Huh?  this was always 1, right?
						// Yep, decisionValue > 0 => true
						}
					}
			}

		return sigmoidTrain(problem.examples.length, decisionValues, problem.getTargetValues());
		}


	// Platt's binary SVM Probablistic Output: an improvement from Lin et al.
	protected float[] sigmoidTrain(int l, float[] decisionValues, Boolean[] labels)
		{
		float A, B;
		float prior1 = 0, prior0 = 0;
		//int i;

		for (Boolean b : labels)
			{
			if (b)
				{
				prior1 += 1;
				}
			else
				{
				prior0 += 1;
				}
			}

		int maximumIterations = 100;// Maximal number of iterations
		float minStep = 1e-10f;// Minimal step taken in line search
		float sigma = 1e-12f;// For numerically strict PD of Hessian
		float eps = 1e-5f;
		float hiTarget = (prior1 + 1.0f) / (prior1 + 2.0f);
		float loTarget = 1 / (prior0 + 2.0f);
		float[] t = new float[l];
		double p, q;
		float fApB, h11, h22, h21, g1, g2, det, dA, dB, gd, stepsize;
		float newA, newB, newf, d1, d2;
		//	int iter;

		// Initial Point and Initial Fun Value
		A = 0.0f;
		B = (float) Math.log((prior0 + 1.0f) / (prior1 + 1.0f)); // PERF
		float fval = 0.0f;

		for (int i = 0; i < l; i++)
			{
			if (labels[i])
				{
				t[i] = hiTarget;
				}
			else
				{
				t[i] = loTarget;
				}
			fApB = decisionValues[i] * A + B;
			if (fApB >= 0)
				{
				fval += t[i] * fApB + Math.log(1 + Math.exp(-fApB));
				}
			else
				{
				fval += (t[i] - 1) * fApB + Math.log(1 + Math.exp(fApB));
				}
			}


		int iter;
		for (iter = 0; iter < maximumIterations; iter++)
			{			// Update Gradient and Hessian (use H' = H + sigma I)
			h11 = sigma;// numerically ensures strict PD
			h22 = sigma;
			h21 = 0.0f;
			g1 = 0.0f;
			g2 = 0.0f;
			for (int i = 0; i < l; i++)
				{
				fApB = decisionValues[i] * A + B;

				// PERF

				if (fApB >= 0)
					{
					p = Math.exp(-fApB) / (1.0f + Math.exp(-fApB));
					q = 1.0f / (1.0f + Math.exp(-fApB));
					}
				else
					{
					p = 1.0f / (1.0f + Math.exp(fApB));
					q = Math.exp(fApB) / (1.0f + Math.exp(fApB));
					}
				d2 = (float) (p * q);
				h11 += decisionValues[i] * decisionValues[i] * d2;
				h22 += d2;
				h21 += decisionValues[i] * d2;
				d1 = (float) (t[i] - p);
				g1 += decisionValues[i] * d1;
				g2 += d1;
				}

			// Stopping Criteria
			if (Math.abs(g1) < eps && Math.abs(g2) < eps)
				{
				break;
				}

			// Finding Newton direction: -inv(H') * g
			det = h11 * h22 - h21 * h21;
			dA = -(h22 * g1 - h21 * g2) / det;
			dB = -(-h21 * g1 + h11 * g2) / det;
			gd = g1 * dA + g2 * dB;


			stepsize = 1;// Line Search
			while (stepsize >= minStep)
				{
				newA = A + stepsize * dA;
				newB = B + stepsize * dB;

				// New function value
				newf = 0.0f;
				for (int i = 0; i < l; i++)
					{
					fApB = decisionValues[i] * newA + newB;
					if (fApB >= 0)
						{
						newf += t[i] * fApB + Math.log(1 + Math.exp(-fApB));
						}
					else
						{
						newf += (t[i] - 1) * fApB + Math.log(1 + Math.exp(fApB));
						}
					}				// Check sufficient decrease
				if (newf < fval + 0.0001 * stepsize * gd)
					{
					A = newA;
					B = newB;
					fval = newf;
					break;
					}
				else
					{
					stepsize = stepsize / 2.0f;
					}
				}

			if (stepsize < minStep)
				{
				System.err.print("Line search fails in two-class probability estimates\n");
				break;
				}
			}

		if (iter >= maximumIterations)
			{
			System.err.print("Reaching maximal iterations in two-class probability estimates\n");
			}
		float[] probAB = new float[2];
		probAB[0] = A;
		probAB[1] = B;
		return probAB;
		}
	}
