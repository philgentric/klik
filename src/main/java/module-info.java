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


    exports klik.face_recognition; // required for parsing json from embeddings server
    exports klik;
}