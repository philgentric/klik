package klik.facerecognition;


import java.util.ArrayList;
import java.util.Objects;

import static klik.facerecognition.PCA.get_PCA;

//**********************************************************
public class Trainer
//**********************************************************
{
    Metric metric;
    FeatureType featureType;
    FeatureExtraction featureExtraction;
    int numberOfComponents;
    int k; // k specifies the number of neighbour to consider

    ArrayList<Matrix> trainingSet;
    ArrayList<String> trainingLabels;
    ArrayList<ProjectedTrainingMatrix> model;

    //**********************************************************
    public static Trainer builder() {
        return new Trainer();
    }

    //**********************************************************
    public void add(Matrix matrix, String label)
    //**********************************************************
    {
        if (Objects.isNull(trainingSet)) {
            trainingSet = new ArrayList<>();
            trainingLabels = new ArrayList<>();
        }

        trainingSet.add(matrix);
        trainingLabels.add(label);
    }

    //**********************************************************
    public void addFaceAfterTraining(Matrix matrix, String label) {
        featureExtraction.addFace(matrix, label);
    }

    //**********************************************************
    public boolean train() throws Exception
    //**********************************************************
    {

        switch (featureType) {
            case PCA:
                featureExtraction = get_PCA(trainingSet, trainingLabels, numberOfComponents);
                break;
            case LDA:
                featureExtraction = new LDA(trainingSet, trainingLabels, numberOfComponents);
                break;
            case LPP:
                featureExtraction = new LPP(trainingSet, trainingLabels, numberOfComponents);
                break;
        }
        if ( featureExtraction == null)
        {
            System.out.println("train failed");
            return false;
        }
        System.out.println("train still OK after feature extraction");
        model = featureExtraction.getProjectedTrainingSet();
        return true;
    }

    //**********************************************************
    public String recognize(Matrix matrix)
    //**********************************************************
    {
        if ( featureExtraction == null)
        {
            return null;
        }
        Matrix testCase = featureExtraction.getW().transpose().times(matrix.minus(featureExtraction.getMeanMatrix()));
        String result = KNN.assignLabel(model.toArray(new ProjectedTrainingMatrix[0]), testCase, k, metric);
        return result;
    }

    //**********************************************************
    public Trainer metric(CosineDissimilarity cosineDissimilarity)
    //**********************************************************
    {
        metric = cosineDissimilarity;
        return this;
    }

    //**********************************************************
    public Trainer featureType(FeatureType featureType)
    //**********************************************************
    {
        this.featureType = featureType;
        return this;
    }

    //**********************************************************
    public Trainer numberOfComponents(int i)
    //**********************************************************
    {
        this.numberOfComponents = i;
        return this;
    }

    //**********************************************************
    public Trainer k(int i)
    //**********************************************************
    {
        this.k = i;
        return this;
    }
}
