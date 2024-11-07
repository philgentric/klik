module klikmodule {


    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.media;
    requires org.apache.commons.io;
    requires java.desktop;
    requires com.github.benmanes.caffeine;
    requires metadata.extractor;
    requires com.google.gson;
    requires javafx.web;


    exports klik.image_ml.face_recognition; // required for parsing json from embeddings server
    exports klik;
    exports klik.image_ml;
    exports klik.image_ml.image_similarity;
}