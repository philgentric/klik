// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Heavy_embeddings_prototype.java
package klikr.machine_learning.face_recognition;

import javafx.scene.image.Image;
import klikr.machine_learning.python_via_html_ML.Python_HTML_Face_recognition_service;
import klikr.util.Kontext;
import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.util.log.Logger;

import java.util.*;

//**********************************************************
public class Prototype_adder_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;
    private final Face_recognition_service service;
    private final Logger logger;

    //**********************************************************
    public Prototype_adder_actor(Face_recognition_service service, Logger logger)
    //**********************************************************
    {
        this.service = service;
        this.logger = logger;
    }



    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Prototype_adder_actor";
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Prototype_adder_message pam = (Prototype_adder_message)m;
        Face_recognition_in_image_status frs = add_prototype_image_face(pam.label,pam.face,pam.feature_vector, pam.context);
        return frs.name();
    }


    //**********************************************************
    public Face_recognition_in_image_status add_prototype_image_face(String label, Image face, Feature_vector fv, Kontext context)
    //**********************************************************
    {
        String tag = label+ "_"+ UUID.randomUUID();

        Embeddings_prototype heavy_ep = new Heavy_embeddings_prototype(face, fv, label, tag);
        heavy_ep.save(service.get_face_recognizer_path(), logger);

        // what we store is the version WITHOUT the image, to save RAM
        Embeddings_prototype ep = new Light_embeddings_prototype(fv, label, tag);
        service.tag_to_prototype_put(tag,ep);
        Integer x = service.label_to_prototype_count_get(label);
        if ( x == null) x = Integer.valueOf(0);
        x++;
        service.label_to_prototype_count_put(label,x);
        service.embeddings_prototypes_add(ep);
        if ( !service.labels_contains(label)) service.labels_add(label);
        logger.log("added prototype image face with tag ="+tag);
        // for debug
        Utils.display(300, face,null,null,"debug",label,context);
        return Face_recognition_in_image_status.feature_vector_ready;
    }

}
