// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning.face_recognition;

import javafx.scene.image.Image;
import klik.machine_learning.feature_vector.Feature_vector;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Light_embeddings_prototype implements Embeddings_prototype
//**********************************************************
{
    private final Feature_vector fv;
    private final String label;
    private final String tag;
    //**********************************************************
    public Light_embeddings_prototype(Feature_vector fv, String label, String tag)
    //**********************************************************
    {
        this.fv = fv;
        this.tag = tag;
        this.label = label;
    }

    //**********************************************************
    @Override
    public Image face_image(Path face_recognizer_path, Logger logger)
    //**********************************************************
    {
        return Embeddings_prototype.is_image_present(face_recognizer_path,tag, logger);
    }

    //**********************************************************
    @Override
    public Feature_vector feature_vector()
    //**********************************************************
    {
        return fv;
    }

    //**********************************************************
    @Override
    public String label()
    //**********************************************************
    {
        return label;
    }

    //**********************************************************
    @Override
    public String tag()
    //**********************************************************
    {
        return tag;
    }

    //**********************************************************
    @Override
    public boolean save(Path face_recognizer_path, Logger logger)
    //**********************************************************
    {
        logger.log("❌ FATAL: saving light weight prototypes NOT implemented");
        return false;
    }
}
