// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.face_recognition;

import javafx.scene.image.Image;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.util.files_and_paths.Extensions;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.nio.file.Path;

//**********************************************************
public interface Embeddings_prototype
//**********************************************************
{
    Image face_image(Path face_recognizer_path, Logger logger);
    Feature_vector feature_vector();
    String label();
    String tag();

    boolean save(Path face_recognizer_path, Logger logger);

    //**********************************************************
    static Path make_prototype_path(Path face_recognizer_path, String name)
    //**********************************************************
    {
        return Path.of(face_recognizer_path.toAbsolutePath().toString() , Extensions.add(name,Face_recognition_service.EXTENSION_FOR_EP));
    }

    //**********************************************************
    static Path make_image_path(Path face_recognizer_path, String tag, Logger logger)
    //**********************************************************
    {
        //logger.log("make_image_path "+face_recognizer_path+" tag="+tag);
        Path path =  Path.of(face_recognizer_path.toString(),tag+".png");
        //logger.log("make_image_path "+path);

        return path;
    }

    //**********************************************************
    static Image is_image_present(Path face_recognizer_path, String tag, Logger logger)
    //**********************************************************
    {

        Image face = null;
        File image_file = null;
        {
            Path image_path = make_image_path(face_recognizer_path, tag, logger);

            //logger.log("trying to load image ->" + image_path + "<-");
            image_file = new File(image_path.toAbsolutePath().toString());
            if (!image_file.exists())
            {
                // typically the image has been discarded by a human reviewer
                // ... need to remove the prototype
                logger.log("face not found while loading prototype");
                return null;
            }
            try
            {
                face = Utils.get_image(image_path);
            }
            catch (Exception e)
            {
                logger.log(Stack_trace_getter.get_stack_trace("should not happen "+e));
                return null;
            }
        }
        return face;
    }

}
