package klikr.machine_learning;

import javafx.stage.Window;
import klikr.properties.Non_booleans_properties;
import klikr.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public enum ML_server_type
//**********************************************************
{
    MobileNet_image_similarity_embeddings_server,
    FaceNet_similarity_embeddings_server,
    MTCNN_face_detection_server,
    Haars_face_detection_server;

    //**********************************************************
    Path registry_path(Window owner, Logger logger)
    //**********************************************************
    {
        switch(this)
        {
            case FaceNet_similarity_embeddings_server, Haars_face_detection_server, MTCNN_face_detection_server:
            {
                return Non_booleans_properties.get_absolute_hidden_dir_on_user_home("face_recognition_server_registry", false, owner, logger);
            }
            case MobileNet_image_similarity_embeddings_server:
            {
                return Non_booleans_properties.get_absolute_hidden_dir_on_user_home("image_similarity_server_registry", false, owner, logger);
            }
        }
        return null;
    }
    //**********************************************************
    int target_server_count(Window owner)
    //**********************************************************
    {
        switch(this)
        {
            case FaceNet_similarity_embeddings_server:
            {
                return 2;
            }
            case Haars_face_detection_server:
            {
                return 2;
            }
            case MTCNN_face_detection_server:
            {
                return 2;
            }
            case MobileNet_image_similarity_embeddings_server:
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
            case FaceNet_similarity_embeddings_server:
            {
            return "FaceNet_embeddings_server.py";
            }
            case Haars_face_detection_server:
            {
            return "haars_face_detection_server.py";
            }
            case MTCNN_face_detection_server:
            {
            return "MTCNN_face_detection_server.py";
            }
            case MobileNet_image_similarity_embeddings_server:
            {
                return "MobileNet_embeddings_server.py";
            }
        }
        return null;
    }
}
