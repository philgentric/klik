// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.ONNX_java_ML;

import klikr.util.Kontext;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.javacpp.indexer.UByteIndexer;

import java.nio.file.Path;

//**********************************************************
// Loads an image with OpenCV and turns it into the float tensor each model
// expects. Mirrors the Python preprocessing:
//
//   MobileNetV3 : load RGB, resize 224x224, NHWC, Keras mobilenet_v3
//                 preprocess_input (identity on [0,255] for V3).
//   FaceNet     : load RGB, resize 160x160, /255, NCHW.
//
// OpenCV reads BGR; we convert to RGB to match the Python/Keras path.
//**********************************************************
public final class Image_preprocessor
//**********************************************************
{
    private Image_preprocessor() {}

    //**********************************************************
    // Build the model input float array from an image file on disk.
    // Returns null (and logs) if the image cannot be read.
    //**********************************************************
    public static float[] preprocess_file(Path image_path, Onnx_embedding_engine.Kind kind, Kontext context)
    //**********************************************************
    {
        Mat bgr = opencv_imgcodecs.imread(image_path.toAbsolutePath().toString(), opencv_imgcodecs.IMREAD_COLOR);
        if (bgr == null || bgr.empty())
        {
            context.log("Image_preprocessor: failed to read image: " + image_path);
            return null;
        }
        try
        {
            return preprocess_mat(bgr, kind);
        }
        finally
        {
            bgr.release();
        }
    }

    //**********************************************************
    // Build the model input float array from an in-memory BGR Mat (e.g. a
    // cropped face from the detector). The Mat is NOT released here.
    //**********************************************************
    public static float[] preprocess_mat(Mat bgr, Onnx_embedding_engine.Kind kind)
    //**********************************************************
    {
        Mat resized = new Mat();
        Mat rgb = new Mat();
        try
        {
            opencv_imgproc.resize(bgr, resized, new Size(kind.width, kind.height));
            opencv_imgproc.cvtColor(resized, rgb, opencv_imgproc.COLOR_BGR2RGB);

            int w = kind.width;
            int h = kind.height;
            float[] out = new float[3 * w * h];
            UByteIndexer idx = rgb.createIndexer();

            boolean div255 = (kind.normalization == Onnx_embedding_engine.Normalization.DIV_255);
            boolean nchw   = (kind.layout == Onnx_embedding_engine.Layout.NCHW);

            for (int y = 0; y < h; y++)
            {
                for (int x = 0; x < w; x++)
                {
                    for (int c = 0; c < 3; c++)
                    {
                        int v = idx.get(y, x, c) & 0xFF;
                        float f = div255 ? (v / 255.0f) : (float) v;
                        int dst = nchw
                                ? (c * h * w + y * w + x)   // NCHW
                                : ((y * w + x) * 3 + c);    // NHWC
                        out[dst] = f;
                    }
                }
            }
            idx.release();
            return out;
        }
        finally
        {
            resized.release();
            rgb.release();
        }
    }
}
