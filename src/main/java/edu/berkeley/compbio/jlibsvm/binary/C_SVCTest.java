package edu.berkeley.compbio.jlibsvm.binary;

import edu.berkeley.compbio.jlibsvm.SvmParameter;
import edu.berkeley.compbio.jlibsvm.SvmPoint;
import edu.berkeley.compbio.jlibsvm.kernel.KernelFunction;
import edu.berkeley.compbio.jlibsvm.kernel.RBFKernel;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class C_SVCTest
	{
	public void basicTest()
		{
		float gamma = .5f;
		KernelFunction k = new RBFKernel(gamma);
		SvmParameter param = new SvmParameter();

		// setup parameters...
		param.cache_size = 500;  //MB

		C_SVC csvc = new C_SVC(k, param);

		BinaryClassificationProblem problem = new BinaryClassificationProblem(10);

		BinaryModel m = csvc.train(problem);

		SvmPoint testpoint = new SvmPoint(10);

		assert m.predictLabel(testpoint).equals(Boolean.TRUE);
		}
	}
