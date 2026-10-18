package klikr.machine_learning.face_recognition;

import klikr.machine_learning.python_via_html_ML.ML_server_type;
import klikr.util.execute.actor.Aborter;

import java.io.File;
import java.nio.file.Path;

public interface Face_recognition_service
{

    void skipped();

    Path get_face_recognizer_path();

    void tag_to_prototype_put(String tag, Embeddings_prototype ep);

    Integer label_to_prototype_count_get(String label);

    void label_to_prototype_count_put(String label, Integer x);

    void embeddings_prototypes_add(Embeddings_prototype ep);

    boolean labels_contains(String label);

    void labels_add(String label);

    void load_one_prototype(File f);

    Path detect_face_and_recognize(File file, ML_server_type faceDetectionType, String label, boolean displayFaceRecoWindow, Aborter aborter);

    Path just_recognize(File file, String label, boolean displayFaceRecoWindow, Aborter aborter);
}
