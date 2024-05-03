package klik.facerecognition;

import javafx.scene.image.Image;

public record Embeddings_prototype(Image face, Feature_vector feature_vector, String label, String name) {}
