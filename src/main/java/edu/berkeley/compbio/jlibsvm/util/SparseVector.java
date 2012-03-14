package edu.berkeley.compbio.jlibsvm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This acts something like a Map from int to float
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class SparseVector {
// ------------------------------ FIELDS ------------------------------

    public int[] indexes;
    public float[] values;


// --------------------------- CONSTRUCTORS ---------------------------

    public SparseVector(int dimensions) {
        indexes = new int[dimensions];
        values = new float[dimensions];
    }

    // don't want to put this here since it assumes the usual dot product, but we might want a different one
    /*
     private static final float NOT_COMPUTED_YET = -1;
     private float normSquared = NOT_COMPUTED_YET;

     public float getNormSquared()
         {
         if (normSquared == -1)
             {
             normSquared = 0;
             int xlen = values.length;
             int i = 0;
             while (i < xlen)
                 {
                 normSquared += values[i] * values[i];
                 ++i;
                 }
             }
         return normSquared;
         }*/

    /**
     * Create randomized vectors for testing
     *
     * @param maxDimensions
     * @param nonzeroProbability
     * @param maxValue
     */
    public SparseVector(int maxDimensions, float nonzeroProbability, float maxValue) {
        List<Integer> indexList = new ArrayList<Integer>();

        for (int i = 0; i < maxDimensions; i++) {
            if (Math.random() < nonzeroProbability) {
                indexList.add(i);
            }
        }

        indexes = new int[indexList.size()];
        values = new float[indexes.length];


        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = indexList.get(i);
            values[i] = (float) (Math.random() * maxValue);
        }
    }

    public int maxIndex() {
        return indexes[indexes.length - 1];
    }

    public SparseVector(SparseVector sv1, float p1, SparseVector sv2, float p2) {
        // need the resulting indexes to be sorted; just brute force through the possible indexes
        // note this works for sparse subclasses that e.g. provide a default value

        int maxDimensions = Math.max(sv1.maxIndex(), sv2.maxIndex());

        List<Integer> indexList = new ArrayList<Integer>();
        List<Float> valueList = new ArrayList<Float>();

        for (int i = 0; i < maxDimensions; i++) {
            float v = sv1.get(i) * p1 + sv2.get(i) * p2;
            if (v != 0) {
                indexList.add(i);
                valueList.add(v);
            }
        }

        indexes = new int[indexList.size()];
        values = new float[indexes.length];

        for (int i = 0; i < indexList.size(); i++) {
            indexes[i] = indexList.get(i);
            values[i] = valueList.get(i);
        }
    }

    public float get(int i) {
        int j = Arrays.binarySearch(indexes, i);
        if (j < 0) {
            return 0;
        } else {
            return values[j];
        }
    }

// ------------------------ CANONICAL METHODS ------------------------

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < indexes.length; j++) {
            sb.append(indexes[j] + ":" + values[j] + " ");
        }
        return sb.toString();
    }

// -------------------------- OTHER METHODS --------------------------

    public void normalizeL2() {
        double sumOfSquares = 0;
        for (float value : values) {
            sumOfSquares += value * value;
        }

        double total = Math.sqrt(sumOfSquares);
        for (int i = 0; i < values.length; i++) {
            values[i] /= total;
        }
    }
}
