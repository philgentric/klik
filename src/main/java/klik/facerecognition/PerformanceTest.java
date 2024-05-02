package klik.facerecognition;




import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static klik.facerecognition.PCA.get_PCA;


public class PerformanceTest {

    public void testPerformance() throws IOException {
        //Test Different Methods
        //Notice that the second parameter which is a measurement of energy percentage does not apply to LDA and LPP

        if(test(2, 101, 0, 3, 2) > 0.6)
        {
            System.out.println("ok perf 1");
        }
        if(test(2, 60, 2, 3, 2) > 0.6)
        {
            System.out.println("ok perf 2");
        }
    }

    /*metricType:
     * 	0: CosineDissimilarity
     * 	1: L1Distance
     * 	2: EuclideanDistance
     *
     * energyPercentage:
     *  PCA: components = samples * energyPercentage
     *  LDA: components = (c-1) *energyPercentage
     *  LLP: components = (c-1) *energyPercentage
     *
     * featureExtractionMode
     * 	0: PCA
     *	1: LDA
     * 	2: LLP
     *
     * trainNums: how many numbers in 1..10 are assigned to be training faces
     * for each class, randomly generate the set
     *
     * knn_k: number of K for KNN algorithm
     *
     * */
    double test(int metricType, int componentsRetained, int featureExtractionMode, int trainNums, int knn_k) throws IOException {


        return -1;
    }

    static ArrayList<Integer> generateTrainNums(int trainNum) {
        Random random = new Random();
        ArrayList<Integer> result = new ArrayList<Integer>();

        while (result.size() < trainNum) {
            int temp = random.nextInt(10) + 1;
            while (result.contains(temp)) {
                temp = random.nextInt(10) + 1;
            }
            result.add(temp);
        }

        return result;
    }

    static ArrayList<Integer> generateTestNums(ArrayList<Integer> trainSet) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int i = 1; i <= 10; i++) {
            if (!trainSet.contains(i))
                result.add(i);
        }
        return result;
    }

    //Convert a m by n matrix into a m*n by 1 matrix
    static Matrix vectorize(Matrix input) {
        int m = input.getRowDimension();
        int n = input.getColumnDimension();

        Matrix result = new Matrix(m * n, 1);
        for (int p = 0; p < n; p++) {
            for (int q = 0; q < m; q++) {
                result.set(p * m + q, 0, input.get(q, p));
            }
        }
        return result;
    }
}
