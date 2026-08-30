// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.face_recognition;

import klikr.machine_learning.ML_server_type;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Face_recognition_message implements Message
//**********************************************************
{
    private final Aborter aborter;
    public final File file;
    public final String label;
    public final boolean display_face_reco_window;
    public final AtomicInteger files_in_flight;
    public final ML_server_type face_detection_type; // none if null

    //**********************************************************
    public Face_recognition_message(File file,
                                    ML_server_type face_detection_type, // maybe null
                                    String label_for_training, // if null, this is "only" recognition, otherwise if recognition result is NOT this label, training will happen
                                    boolean display_face_reco_window,
                                    Aborter aborter,
                                    AtomicInteger files_in_flight)
    //**********************************************************
    {
        this.file = file;
        this.face_detection_type = face_detection_type;
        this.label = label_for_training;
        this.display_face_reco_window = display_face_reco_window;
        this.aborter = aborter;
        this.files_in_flight = files_in_flight;
    }

    @Override
    public String thread_name() {
        return "Face_recognition";
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
