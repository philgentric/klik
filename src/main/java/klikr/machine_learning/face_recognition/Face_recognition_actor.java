// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Eval_result_for_one_prototype.java
//SOURCES ../Feature_vector.java
//SOURCES ../Feature_vector_source.java
//SOURCES ./Feature_vector_source_for_Face_recognition.java
//SOURCES ./Face_detection_type.java
//SOURCES ./Face_recognition_in_image_status.java
//SOURCES ./Python_HTML_Face_detector.java

package klikr.machine_learning.face_recognition;

import klikr.machine_learning.python_via_html_ML.Python_HTML_Face_recognition_service;
import klikr.util.execute.actor.*;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//**********************************************************
public class Face_recognition_actor implements Actor
//**********************************************************
{
    private final Logger logger;
    public static final boolean dbg = false;
    private final Face_recognition_service service;
    //**********************************************************
    public Face_recognition_actor(Face_recognition_service service, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.service = service;
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Face_recognition_actor";
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Face_recognition_message frm = (Face_recognition_message) m;
        // we need to ALWAYS increment because the actor termination reporter will decrement it (even in case of abort)
        if ( frm.files_in_flight !=null) frm.files_in_flight.incrementAndGet();

        if ( frm.label != null)
        {
            if ( service.label_to_prototype_count_get(frm.label) > Python_HTML_Face_recognition_service.LIMIT_PER_LABEL)
            {
                service.skipped();
                logger.log("Face_recognition_actor, NOT performing recognition "+frm.file.getName()+" with label: "+frm.label+ " as there are too many prototypes already "+service.label_to_prototype_count_get(frm.label));
                return null;
            }
        }

        Path tmp_image_to_be_deleted = null;
        if ( frm.face_detection_type != null)
        {
            tmp_image_to_be_deleted = service.detect_face_and_recognize(frm.file, frm.face_detection_type, frm.label, frm.display_face_reco_window, frm.get_aborter());
        }
        else
        {
            tmp_image_to_be_deleted = service.just_recognize(frm.file,frm.label, frm.display_face_reco_window, frm.get_aborter());
        }
        /*
        no clean up ...
        if ( tmp_image_to_be_deleted != null)
        {
            try {
                //logger.log("deleting tmp image :"+tmp_image_to_be_deleted);
                Files.delete(tmp_image_to_be_deleted);
            } catch (IOException e) {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
            }
        }*/
        return null;
    }














}
