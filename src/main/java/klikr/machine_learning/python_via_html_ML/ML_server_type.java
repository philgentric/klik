package klikr.machine_learning.python_via_html_ML;

import javafx.stage.Window;
import klikr.settings.Non_booleans_properties;
import klikr.util.Kontext;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;

import java.nio.file.Path;


//**********************************************************
public enum ML_server_type
//**********************************************************
{
    // Convolutional Neural Network = CNN
    MobileNet, // feature vectors (CNN embeddings) for general purpose image similarity
    FaceNet, //  feature vectors (CNN embeddings) specialized for face similarity (=> recognition assuming faces with labels are recorded)
    MTCNN, // Convolutional Neural Network for face detection
    // "old tech" face detector
    Haar_tree,
    Haar_default,
    Haar_alt1,
    Haar_alt2;

    //**********************************************************
    public static ML_server_type[] face_detectors()
    //**********************************************************
    {
        return new ML_server_type[]{MTCNN, Haar_alt1, Haar_alt2, Haar_default, Haar_tree};
    }

    //**********************************************************
    public Path registry_path(Kontext context)
    //**********************************************************
    {
        switch(this)
        {
            case FaceNet, MTCNN, Haar_alt1, Haar_alt2, Haar_default, Haar_tree:
            {
                return Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home("face_recognition_server_registry", false, context);
            }
            case MobileNet:
            {
                return Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home("image_similarity_server_registry", false,context);
            }
        }
        return null;
    }

    private int number_of_image_similarity_servers = -1;
    //**********************************************************
    public int quota(Window owner)
    //**********************************************************
    {
        // for the face recog servers we count '1' for a call to start ... which may start more than one
        switch(this)
        {
            case FaceNet, MTCNN:
            {
                return 3;
            }
            case Haar_alt1, Haar_alt2, Haar_default, Haar_tree:
            {
                return 1;
            }
            case MobileNet:
            {
                if ( number_of_image_similarity_servers < 0 ) number_of_image_similarity_servers = Non_booleans_properties.get_number_of_image_similarity_servers();
                return number_of_image_similarity_servers;
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
            case MobileNet:
            {
                return "MobileNet_embeddings_server.py";
            }
            case FaceNet:
            {
            return "FaceNet_embeddings_server.py";
            }
            case MTCNN:
            {
            return "MTCNN_face_detection_server.py";
            }
            case Haar_alt1, Haar_alt2, Haar_default, Haar_tree:
            {
                return "Haar_face_detection_server.py";
            }

        }
        return null;
    }


    //**********************************************************
    public String get_xml_file_name()
    //**********************************************************
    {
        switch(this)
        {
            case Haar_default:
                return "default.xml";
            case Haar_tree:
                return "tree.xml";
            case Haar_alt1:
                return "alt1.xml";
            case Haar_alt2:
                return "alt2.xml";
        }
        return null;
    }
}
