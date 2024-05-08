package klik.face_recognition;

import klik.actor.Aborter;
import klik.actor.Message;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class Face_recognition_message implements Message {
    private final Aborter aborter;
    public final File file;
    public final String label;
    public final boolean display_face_reco_window;
    public final AtomicInteger files_in_flight;

    public Face_recognition_message(File file, String label, boolean display_face_reco_window, Aborter aborter, AtomicInteger files_in_flight) {
        this.file = file;
        this.label = label;
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
