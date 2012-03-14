package edu.berkeley.compbio.jlibsvm.kernel;

import edu.berkeley.compbio.jlibsvm.util.SparseVector;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class GaussianRBFKernelTest {
// ------------------------------ FIELDS ------------------------------

    private static final Logger logger = Logger.getLogger(GaussianRBFKernelTest.class);


// -------------------------- OTHER METHODS --------------------------

    @Test
    public void explicitAndCompositeKernelsAreEqual() {
        float gamma = 1f;

        CompositeGaussianRBFKernel<SparseVector> composite =
                new CompositeGaussianRBFKernel<SparseVector>(gamma, new LinearKernel());

        GaussianRBFKernel explicit = new GaussianRBFKernel(gamma);

        // need a lot of iterations to use enough time for profiling (e.g. 1000)

        for (int i = 0; i < 100; i++) {
            SparseVector sv1 = new SparseVector(100, .5f, 1);
            SparseVector sv2 = new SparseVector(100, .5f, 1);

            // those vectors are likely far apart, and the RBF is always near zero for those.  Interpolate to test closer distances.

            for (int j = 0; j < 100; j++) {
                SparseVector sv3 = new SparseVector(sv1, 1f - (j / 1000f), sv2, (j / 1000f));
                final double compositeResult = composite.evaluate(sv1, sv3);
                final double explicitResult = explicit.evaluate(sv1, sv3);
                final double diff = explicitResult - compositeResult;
                assert Math.abs(diff) < 1e-10;
            }
        }
    }

    // no need to waste cycles on the build server
    /*
     @Test
     public void explicitRBFKernelSpeedTest()
         {		// poor man's profiling
         long startTime = System.currentTimeMillis();

         float gamma = 1f;


         GaussianRBFKernel explicit = new GaussianRBFKernel(gamma);

         // need a lot of iterations to use enough time for profiling (e.g. 1000)

         for (int i = 0; i < 100; i++)
             {
             SparseVector sv1 = new SparseVector(100, .5f, 1);
             SparseVector sv2 = new SparseVector(100, .5f, 1);

             // those vectors are likely far apart, and the RBF is always near zero for those.  Interpolate to test closer distances.

             for (int j = 0; j < 1000; j++)
                 {
                 SparseVector sv3 = new SparseVector(100, sv1, 1f - (j / 1000f), sv2, (j / 1000f));
                 final double explicitResult = explicit.evaluate(sv1, sv3);
                 }
             }

         long endTime = System.currentTimeMillis();
         float time = (endTime - startTime) / 1000f;
         logger.debug("Explicit RBF kernel time = " + time + " sec");
         }


     @Test
     public void compositeRBFKernelSpeedTest()
         {		// poor man's profiling
         long startTime = System.currentTimeMillis();

         float gamma = 1f;


         CompositeGaussianRBFKernel<SparseVector> composite =
                 new CompositeGaussianRBFKernel<SparseVector>(gamma, new LinearKernel());

         // need a lot of iterations to use enough time for profiling (e.g. 1000)

         for (int i = 0; i < 100; i++)
             {
             SparseVector sv1 = new SparseVector(100, .5f, 1);
             SparseVector sv2 = new SparseVector(100, .5f, 1);

             // those vectors are likely far apart, and the RBF is always near zero for those.  Interpolate to test closer distances.

             for (int j = 0; j < 1000; j++)
                 {
                 SparseVector sv3 = new SparseVector(100, sv1, 1f - (j / 1000f), sv2, (j / 1000f));
                 final double compositeResult = composite.evaluate(sv1, sv3);
                 }
             }

         long endTime = System.currentTimeMillis();
         float time = (endTime - startTime) / 1000f;
         logger.debug("Composite RBF kernel time = " + time + " sec");
         }
         */
}
