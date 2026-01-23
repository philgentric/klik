package klikr.machine_learning;

import javafx.stage.Window;
import klikr.properties.Non_booleans_properties;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public enum ML_server_type
//**********************************************************
{
    // Convolutional Neural Network = CNN
    MobileNet, // feature vectors (CNN embeddings) for general purpose image similarity
    FaceNet, //  feature vectors (CNN embeddings) specialized for face similarity (=> recognition assuming faces with labels are recorded)
    MTCNN, // Convolutional Neural Network for face detection
    Haars; // "old tech" face detector

    //**********************************************************
    Path registry_path(Window owner, Logger logger)
    //**********************************************************
    {
        switch(this)
        {
            case FaceNet, Haars, MTCNN:
            {
                return Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home("face_recognition_server_registry", false, owner, logger);
            }
            case MobileNet:
            {
                return Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home("image_similarity_server_registry", false, owner, logger);
            }
        }
        return null;
    }
    //**********************************************************
    int quota(Window owner)
    //**********************************************************
    {
        // for the face recog servers we count '1' for a call to start ... which may start more than one
        switch(this)
        {
            case FaceNet, MTCNN:
            {
                return 1;
            }
            case Haars:
            {
                return 4; // one of each sub type
            }
            case MobileNet:
            {
                return Non_booleans_properties.get_number_of_image_similarity_servers(owner);
            }
        }
        return -1;
    }

    //**********************************************************
    String python_file_name()
    //**********************************************************
    {
        switch(this)
        {
            case FaceNet:
            {
            return "FaceNet_embeddings_server.py";
            }
            case Haars:
            {
            return "haars_face_detection_server.py";
            }
            case MTCNN:
            {
            return "MTCNN_face_detection_server.py";
            }
            case MobileNet:
            {
                return "MobileNet_embeddings_server.py";
            }
        }
        return null;
    }
}
