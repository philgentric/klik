// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.ONNX_java_ML;

import klikr.machine_learning.face_recognition.Face_detection_result;
import klikr.machine_learning.feature_vector.Feature_vector_double;
import klikr.util.Kontext;
import klikr.util.log.Simple_logger;

import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
// Standalone smoke test for the pure-JVM ML package.
//
// Guarded on model presence: if the .onnx files are not on the classpath
// (i.e. export_models.py has not been run yet), it reports SKIPPED and exits
// cleanly rather than failing. This lets the package compile and the test run
// in CI before the models are produced.
//
// Run with an image path to exercise detection + embeddings:
//     gradle full_java_ML_smoke --args="/path/to/face.jpg"
//**********************************************************
public final class Smoke_test
//**********************************************************
{
    //**********************************************************
    public static void main(String[] args)
    //**********************************************************
    {
        Kontext context = new Kontext(null, null, new Simple_logger());

        context.log("full_java_ML smoke test starting");

        // 1. Can we load the embedding engines? (needs the .onnx files)
        Optional<Onnx_feature_vector_source> mobilenet =
                Onnx_feature_vector_source.create(Onnx_embedding_engine.Kind.MOBILENET, context);
        Optional<Onnx_feature_vector_source> facenet =
                Onnx_feature_vector_source.create(Onnx_embedding_engine.Kind.FACENET, context);

        if (mobilenet.isEmpty() || facenet.isEmpty())
        {
            context.log("SKIPPED: ONNX models not present. Run scripts/export_models.py "
                    + "to produce src/main/resources/ml_models/*.onnx, then re-run.");
            return;
        }
        context.log("OK: both embedding engines loaded.");

        // 2. If an image path was given, run the full pipeline on it.
        if (args.length < 1)
        {
            context.log("No image path given; engine-load check only. "
                    + "Pass an image path to exercise detection + embeddings.");
            return;
        }

        Path image = Path.of(args[0]);
        context.log("Running on: " + image);

        // 2a. image-similarity embedding (MobileNet)
        Optional<Feature_vector_double> mv = mobilenet.get().get_feature_vector(image, context);
        context.log(mv.isPresent()
                ? "OK: MobileNet embedding length=" + mv.get().features.length
                : "FAIL: MobileNet produced no vector");

        // 2b. face detection (YuNet) -> crop
        Face_detection_result det =
                Onnx_face_detector.detect_face(image, context);
        context.log("Face detection status: " + det.status());

        // 2c. face embedding (FaceNet) on the whole image (direct mode)
        Optional<Feature_vector_double> fv = facenet.get().get_feature_vector(image, context);
        context.log(fv.isPresent()
                ? "OK: FaceNet embedding length=" + fv.get().features.length
                : "FAIL: FaceNet produced no vector");

        context.log("full_java_ML smoke test done.");
    }

    private Smoke_test() {}
}
