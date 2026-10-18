// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.ONNX_java_ML;

import klikr.util.Kontext;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

//**********************************************************
// Locates the model files the pure-JVM ML needs.
//
// These are expected on the classpath under /ml_models/ :
//   - mobilenet_v3_large.onnx   (image-similarity embeddings)
//   - facenet_vggface2.onnx     (face embeddings)
//   - face_detection_yunet.onnx (YuNet NN face detector, replaces Haar + MTCNN)
//
// ONNX Runtime and OpenCV both need a real filesystem path (they load native
// side), so classpath resources are extracted once to a temp dir and cached.
//**********************************************************
public final class ML_model_paths
//**********************************************************
{
    public static final String MOBILENET_ONNX  = "mobilenet_v3_large.onnx";
    public static final String FACENET_ONNX     = "facenet_vggface2.onnx";
    public static final String YUNET_ONNX       = "face_detection_yunet.onnx";

    // classpath root for the .onnx models
    private static final String MODELS_CP_ROOT  = "/ml_models/";

    private ML_model_paths() {}

    //**********************************************************
    // Resolve an .onnx model from /ml_models/ on the classpath,
    // extracting it to a temp file the native runtime can open.
    //**********************************************************
    public static Path onnx_model(String file_name, Kontext context)
    //**********************************************************
    {
        return extract_resource(MODELS_CP_ROOT + file_name, file_name, context);
    }

    //**********************************************************
    private static Path extract_resource(String classpath_location, String file_name, Kontext context)
    //**********************************************************
    {
        try
        {
            Path cache_dir = Files.createDirectories(
                    Path.of(System.getProperty("java.io.tmpdir"), "klikr_ml_models"));
            Path target = cache_dir.resolve(file_name);
            if (Files.exists(target) && Files.size(target) > 0)
            {
                return target;
            }
            try (InputStream in = ML_model_paths.class.getResourceAsStream(classpath_location))
            {
                if (in == null)
                {
                    context.log("ML_model_paths: resource NOT found on classpath: " + classpath_location
                            + " (did you run export_models.py and place the .onnx files in src/main/resources/ml_models/ ?)");
                    return null;
                }
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        }
        catch (Exception e)
        {
            context.log_with_stack_trace_from_throwable("ML_model_paths.extract_resource failed for " + classpath_location + " ",
                    e instanceof Exception ex ? ex : new Exception(e));
            return null;
        }
    }
}
