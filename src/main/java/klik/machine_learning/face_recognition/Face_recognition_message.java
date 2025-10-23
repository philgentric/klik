package klik.machine_learning.face_recognition;

import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Message;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Face_recognition_message implements Message
//**********************************************************
{
    private final Aborter aborter;
    public final File file;
    public final String label;
    public final boolean do_face_detection;
    public final boolean display_face_reco_window;
    public final AtomicInteger files_in_flight;
    public final Face_detection_type face_detection_type;


    //**********************************************************
    public Face_recognition_message(File file,
                                    Face_detection_type face_detection_type,
                                    boolean do_face_detection,
                                    String label_for_training, // if null, this is "only" recognition, otherwise if recognition result is NOT this label, training will happen
                                    boolean display_face_reco_window,
                                    Aborter aborter,
                                    AtomicInteger files_in_flight)
    //**********************************************************
    {
        this.file = file;
        this.face_detection_type = face_detection_type;
        this.do_face_detection = do_face_detection;
        this.label = label_for_training;
        this.display_face_reco_window = display_face_reco_window;
        this.aborter = aborter;
        this.files_in_flight = files_in_flight;
    }

    @Override
    public String to_string() {
        return "Face_recognition_message";
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
