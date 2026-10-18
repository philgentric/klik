package klikr.machine_learning.face_recognition;

import javafx.scene.image.Image;

public record Face_detection_result(Image image, Face_recognition_in_image_status status){}
