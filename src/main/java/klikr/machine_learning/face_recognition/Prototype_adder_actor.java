// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Heavy_embeddings_prototype.java
package klikr.machine_learning.face_recognition;

import javafx.scene.image.Image;
import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;
import klikr.machine_learning.feature_vector.Feature_vector;

import java.util.*;

//**********************************************************
public class Prototype_adder_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;
    private final Face_recognition_service service;

    //**********************************************************
    Prototype_adder_actor(Face_recognition_service service_)
    //**********************************************************
    {
        service = service_;
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
        Face_recognition_in_image_status frs = add_prototype_image_face(pam.label,pam.face,pam.feature_vector);
        return frs.name();
    }


    //**********************************************************
    public Face_recognition_in_image_status add_prototype_image_face(String label, Image face, Feature_vector fv)
    //**********************************************************
    {
        String tag = label+ "_"+ UUID.randomUUID();

        Embeddings_prototype heavy_ep = new Heavy_embeddings_prototype(face, fv, label, tag);
        heavy_ep.save(service.face_recognizer_path, service.logger);

        // what we store is the version WITHOUT the image, to save RAM
        Embeddings_prototype ep = new Light_embeddings_prototype(fv, label, tag);
        service.tag_to_prototype.put(tag,ep);
        Integer x = service.label_to_prototype_count.get(label);
        if ( x == null) x = Integer.valueOf(0);
        x++;
        service.label_to_prototype_count.put(label,x);
        service.embeddings_prototypes.add(ep);
        if ( !service.labels.contains(label)) service.labels.add(label);
        service.logger.log("added prototype image face with tag ="+tag);
        // for debug
        //Utils.display(300, face,null,null,"debug",label,service.logger);
        return Face_recognition_in_image_status.feature_vector_ready;
    }

}
