package klik.face_recognition;

public enum Face_recognition_status {
    server_not_reacheable,
    error,

    face_detected,
    no_face_detected,

    no_feature_vector,
    no_face_recognized,
    feature_vector_ready,
    face_recognized,
    exact_match // special case of face recognized
    ;

}
