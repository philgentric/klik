// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.ONNX_java_ML;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.providers.CoreMLFlags;
import klikr.util.Kontext;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

//**********************************************************
// In-process ONNX Runtime inference, replacing the Python embedding HTTP
// servers (MobileNet_embeddings_server.py and FaceNet_embeddings_server.py).
//
// One OrtEnvironment is shared process-wide; each model kind owns one
// thread-safe OrtSession. ONNX Runtime sessions ARE safe for concurrent
// run() calls, so the Java-side thread pool can hit a single session instead
// of load-balancing across many Python HTTP servers.
//**********************************************************
public final class Onnx_embedding_engine
//**********************************************************
{
    //**********************************************************
    public enum Kind
    //**********************************************************
    {
        // MobileNetV3Large, no top, avg pool. Input 224x224x3, NHWC, Keras
        // mobilenet_v3 preprocess_input (identity on [0,255] for V3).
        MOBILENET(ML_model_paths.MOBILENET_ONNX, 224, 224, Layout.NHWC, Normalization.KERAS_MOBILENET_V3),
        // FaceNet InceptionResnetV1 (vggface2). Input 160x160x3, NCHW, /255.
        FACENET(ML_model_paths.FACENET_ONNX, 160, 160, Layout.NCHW, Normalization.DIV_255);

        final String onnx_file;
        final int width;
        final int height;
        final Layout layout;
        final Normalization normalization;

        Kind(String onnx_file, int width, int height, Layout layout, Normalization normalization)
        {
            this.onnx_file = onnx_file;
            this.width = width;
            this.height = height;
            this.layout = layout;
            this.normalization = normalization;
        }
    }

    enum Layout { NHWC, NCHW }
    enum Normalization { DIV_255, KERAS_MOBILENET_V3 }

    private final Kind kind;
    private final OrtEnvironment env;
    private final OrtSession session;
    private final String input_name;

    //**********************************************************
    private Onnx_embedding_engine(Kind kind, OrtEnvironment env, OrtSession session, String input_name)
    //**********************************************************
    {
        this.kind = kind;
        this.env = env;
        this.session = session;
        this.input_name = input_name;
    }

    //**********************************************************
    // Build an engine for a model kind. Returns null (and logs) if the .onnx
    // file is missing or the session cannot be created.
    //**********************************************************
    public static Onnx_embedding_engine create(Kind kind, Kontext context)
    //**********************************************************
    {
        Path model = ML_model_paths.onnx_model(kind.onnx_file, context);
        if (model == null)
        {
            context.log("Onnx_embedding_engine: cannot create " + kind + " engine, model file missing.");
            return null;
        }
        try
        {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            options.addCoreML(EnumSet.of(CoreMLFlags.CREATE_MLPROGRAM, CoreMLFlags.ENABLE_ON_SUBGRAPH));
            options.setInterOpNumThreads(1);
            options.setIntraOpNumThreads(10);
            options.setMemoryPatternOptimization(true);


            // CoreML / CUDA execution providers could be added here per-OS.
            OrtSession session = env.createSession(model.toString(), options);
            String input_name = session.getInputNames().iterator().next();
            context.log("Onnx_embedding_engine: " + kind + " session ready (input='" + input_name + "')");
            return new Onnx_embedding_engine(kind, env, session, input_name);
        }
        catch (OrtException e)
        {
            context.log_with_stack_trace_from_throwable("Onnx_embedding_engine.create " + kind + " failed: ", e);
            return null;
        }
    }

    //**********************************************************
    // Run inference on an already-preprocessed CHW/HWC float array and return
    // the flattened embedding as double[]. Returns null on failure.
    //**********************************************************
    public double[] embed(float[] preprocessed, Kontext context)
    //**********************************************************
    {
        long[] shape = (kind.layout == Layout.NCHW)
                ? new long[]{1, 3, kind.height, kind.width}
                : new long[]{1, kind.height, kind.width, 3};
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(preprocessed), shape))
        {
            Map<String, OnnxTensor> inputs = Collections.singletonMap(input_name, input);
            try (OrtSession.Result result = session.run(inputs))
            {
                Object raw = result.get(0).getValue();
                float[] flat = flatten(raw);
                if (flat == null)
                {
                    context.log("Onnx_embedding_engine.embed: unexpected output type for " + kind);
                    return null;
                }
                double[] out = new double[flat.length];
                for (int i = 0; i < flat.length; i++) out[i] = flat[i];
                return out;
            }
        }
        catch (OrtException e)
        {
            context.log_with_stack_trace_from_throwable("Onnx_embedding_engine.embed " + kind + " failed: ", e);
            return null;
        }
    }

    public Kind kind() { return kind; }

    //**********************************************************
    // ONNX output is typically float[1][N] (embeddings). Flatten to float[N].
    //**********************************************************
    private static float[] flatten(Object raw)
    //**********************************************************
    {
        if (raw instanceof float[] f1) return f1;
        if (raw instanceof float[][] f2)
        {
            // [batch=1][N]
            int n = 0;
            for (float[] row : f2) n += row.length;
            float[] out = new float[n];
            int k = 0;
            for (float[] row : f2) for (float v : row) out[k++] = v;
            return out;
        }
        return null;
    }
}
