// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.full_java_ML;

import javafx.scene.image.Image;
import klikr.machine_learning.face_recognition.Face_recognition_in_image_status;
import klikr.util.Kontext;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.FaceDetectorYN;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

//**********************************************************
// Pure-JVM replacement for ALL the Python face-detection HTTP servers
// (Haar_face_detection_server.py and MTCNN_face_detection_server.py).
//
// Both the Haar cascades and MTCNN are replaced by a single detector:
// OpenCV's YuNet (FaceDetectorYN), a small bundled ONNX NN detector that
// outperforms Haar and MTCNN while being faster and simpler. This removes the
// 4 Haar variants, the cascade/confidence logic, and the MTCNN dependency.
//
// Returns the cropped face as a JavaFX Image plus a status, mirroring the
// existing Face_detector.Face_detection_result contract so this is a drop-in.
//**********************************************************
public final class Opencv_face_detector
//**********************************************************
{
    static final int MINIMUM_ACCEPTABLE_FACE_SIZE = 200;

    //**********************************************************
    public record Face_detection_result(Image image, Face_recognition_in_image_status status) {}
    //**********************************************************

    private Opencv_face_detector() {}

    //**********************************************************
    // Detect the most prominent face in the image file and return it cropped.
    //**********************************************************
    public static Face_detection_result detect_face(Path path, Kontext context)
    //**********************************************************
    {
        if (context.should_abort())
        {
            context.log("aborting Opencv_face_detector.detect_face, reason: " + context.abort_reason());
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }
        if (path == null || !path.toFile().exists())
        {
            context.log("Opencv_face_detector: file does not exist: " + path);
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }

        Mat bgr = opencv_imgcodecs.imread(path.toAbsolutePath().toString(), opencv_imgcodecs.IMREAD_COLOR);
        if (bgr == null || bgr.empty())
        {
            context.log("Opencv_face_detector: failed to read image: " + path);
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }
        try
        {
            return detect_with_yunet(bgr, context);
        }
        finally
        {
            bgr.release();
        }
    }

    //**********************************************************
    private static Face_detection_result detect_with_yunet(Mat bgr, Kontext context)
    //**********************************************************
    {
        Path model = ML_model_paths.onnx_model(ML_model_paths.YUNET_ONNX, context);
        if (model == null)
        {
            return new Face_detection_result(null, Face_recognition_in_image_status.error);
        }

        FaceDetectorYN detector = FaceDetectorYN.create(
                model.toAbsolutePath().toString(), "",
                new Size(bgr.cols(), bgr.rows()));
        Mat faces = new Mat();
        try
        {
            detector.setInputSize(new Size(bgr.cols(), bgr.rows()));
            detector.detect(bgr, faces);

            if (faces.rows() < 1)
            {
                return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
            }

            // YuNet rows: [x, y, w, h, 5 landmarks (10 values), score].
            // Pick the highest-scoring face.
            int cols = faces.cols();
            FloatPointer fp = new FloatPointer(faces.data());
            int best = 0;
            float best_score = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < faces.rows(); i++)
            {
                float score = fp.get((long) i * cols + (cols - 1));
                if (score > best_score) { best_score = score; best = i; }
            }

            long base = (long) best * cols;
            int x = Math.max(0, Math.round(fp.get(base)));
            int y = Math.max(0, Math.round(fp.get(base + 1)));
            int w = Math.round(fp.get(base + 2));
            int h = Math.round(fp.get(base + 3));
            w = Math.min(w, bgr.cols() - x);
            h = Math.min(h, bgr.rows() - y);
            if (w <= 0 || h <= 0)
            {
                return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
            }

            Mat roi = new Mat(bgr, new Rect(x, y, w, h));
            try
            {
                Image img = mat_to_image(roi, context);
                if (img == null) return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
                if (img.getHeight() < MINIMUM_ACCEPTABLE_FACE_SIZE || img.getWidth() < MINIMUM_ACCEPTABLE_FACE_SIZE)
                {
                    context.log("Opencv_face_detector: YuNet face smaller than " + MINIMUM_ACCEPTABLE_FACE_SIZE + " px discarded");
                    return new Face_detection_result(null, Face_recognition_in_image_status.no_face_detected);
                }
                context.log("Opencv_face_detector: face detected by YuNet (score=" + best_score + ")");
                return new Face_detection_result(img, Face_recognition_in_image_status.face_detected);
            }
            finally
            {
                roi.release();
            }
        }
        finally
        {
            faces.release();
            detector.close();
        }
    }

    //**********************************************************
    // Encode an OpenCV Mat to PNG and wrap it as a JavaFX Image.
    //**********************************************************
    private static Image mat_to_image(Mat mat, Kontext context)
    //**********************************************************
    {
        BytePointer buf = new BytePointer();
        try
        {
            boolean ok = opencv_imgcodecs.imencode(".png", mat, buf);
            if (!ok)
            {
                context.log("Opencv_face_detector: imencode(.png) failed");
                return null;
            }
            byte[] bytes = new byte[(int) buf.limit()];
            buf.get(bytes);
            return new Image(new ByteArrayInputStream(bytes));
        }
        finally
        {
            buf.deallocate();
        }
    }
}
