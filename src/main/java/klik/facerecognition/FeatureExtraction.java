package klik.facerecognition;

import java.util.ArrayList;


public abstract class FeatureExtraction {
	ArrayList<Matrix> trainingSet;
	ArrayList<String> labels;
	int numOfComponents;
	Matrix meanMatrix;
	// Output
	Matrix W;
	ArrayList<ProjectedTrainingMatrix> projectedTrainingSet;

	public abstract Matrix getW();

	public abstract ArrayList<ProjectedTrainingMatrix> getProjectedTrainingSet();

	public abstract Matrix getMeanMatrix();

	public abstract int addFace(Matrix face, String label);
}
