package klikr.machine_learning.face_recognition;

import javafx.scene.image.Image;
import klikr.machine_learning.feature_vector.Feature_vector;

import java.nio.file.Path;



//**********************************************************
public record Face_recognition_results(String label, Image image,
                                Path image_path,
                                Feature_vector feature_vector,
                                Face_recognition_in_image_status face_recognition_in_image_status)
//**********************************************************
{
    public String to_string()
    {
        return "label : " +
                label +
                " Face_recognition_in_image_status: " +
                face_recognition_in_image_status;
    }
}