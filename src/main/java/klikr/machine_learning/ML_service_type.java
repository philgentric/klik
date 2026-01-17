package klikr.machine_learning;

import klikr.machine_learning.face_recognition.Face_detection_type;

//**********************************************************
public record ML_service_type(ML_server_type ml_server_type, Face_detection_type face_detection_type)
//**********************************************************
{

    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        String returned =  ml_server_type().name();
        if ( face_detection_type() !=null) returned += " and "+face_detection_type().name();
        return returned;
    }
};
