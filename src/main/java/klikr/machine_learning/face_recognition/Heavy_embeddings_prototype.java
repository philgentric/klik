// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.face_recognition;

import javafx.scene.image.Image;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.machine_learning.feature_vector.Feature_vector_double;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

//**********************************************************
public class Heavy_embeddings_prototype implements Embeddings_prototype
//**********************************************************
{
    private final Image face; // this is what makes it heavy
    private final Feature_vector fv;
    private final String label;
    private final String tag;

    //**********************************************************
    public Heavy_embeddings_prototype(Image face, Feature_vector fv, String label, String tag)
    //**********************************************************
    {
        this.face = face;
        this.fv = fv;
        this.label = label;
        this.tag = tag;
    }



    //**********************************************************
    @Override
    public Image face_image(Path face_recognizer_path, Logger logger)
    //**********************************************************
    {
        return face;
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
        // "for human only":
        // we write the face image to disk, if it gets deleted by aa human,
        // the prototype will be deleted next time the set is reloaded
        Path path_to_prototype_image = Face_recognition_service.write_tmp_image(face_image(face_recognizer_path,logger), face_recognizer_path, tag,logger);
        if ( path_to_prototype_image == null)
        {
            return false;
        }

        String filename = Embeddings_prototype.make_prototype_path(face_recognizer_path, tag()).toAbsolutePath().toString();
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename)))
        {
            writer.println(label());
            Feature_vector_double fvd = (Feature_vector_double)feature_vector();
            writer.println(fvd.features.length );
            for ( double d : fvd.features)
            {
                writer.println(d);
            }
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return false;
        }
        return true;
    }
}
