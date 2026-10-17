// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.full_java_ML;

import klikr.machine_learning.feature_vector.Feature_vector_double;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.util.Kontext;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
// Pure-JVM replacement for the Python embedding HTTP servers.
//
// Implements the existing Feature_vector_source interface, so it is a drop-in
// for the HTTP-based Feature_vector_source_server: same Optional<Feature_vector_double>
// contract, same double[] + cosine distance downstream, caches unchanged in
// shape (bump a cache version so ONNX vectors are not compared against old
// Python ones).
//
// Create one instance per model kind (MOBILENET for image similarity,
// FACENET for face recognition). The underlying OrtSession is thread-safe,
// so a single instance can be shared across the app's worker threads.
//**********************************************************
public final class Onnx_feature_vector_source implements Feature_vector_source
//**********************************************************
{
    private final Onnx_embedding_engine engine;
    private final String who_are_you;

    //**********************************************************
    private Onnx_feature_vector_source(Onnx_embedding_engine engine, String who_are_you)
    //**********************************************************
    {
        this.engine = engine;
        this.who_are_you = who_are_you;
    }

    //**********************************************************
    // Factory. Returns empty if the ONNX model is missing / fails to load.
    //**********************************************************
    public static Optional<Onnx_feature_vector_source> create(Onnx_embedding_engine.Kind kind, Kontext context)
    //**********************************************************
    {
        Onnx_embedding_engine engine = Onnx_embedding_engine.create(kind, context);
        if (engine == null)
        {
            context.log(Logger.error+"Onnx_embedding_engine create failed");
            return Optional.empty();
        }
        return Optional.of(new Onnx_feature_vector_source(engine, "onnx_" + kind.name().toLowerCase()));
    }

    //**********************************************************
    @Override
    public Optional<Feature_vector_double> get_feature_vector(Path path, Kontext context)
    //**********************************************************
    {
        if (context.should_abort())
        {
            context.log("aborting Onnx_feature_vector_source.get_feature_vector, reason: " + context.abort_reason());
            return Optional.empty();
        }
        if (path == null)
        {
            context.log_stack_trace("Onnx_feature_vector_source.get_feature_vector called with null path");
            return Optional.empty();
        }
        if (!path.toFile().exists())
        {
            context.log("Onnx_feature_vector_source: file does not exist: " + path);
            return Optional.empty();
        }

        float[] input = Image_preprocessor.preprocess_file(path, engine.kind(), context);
        if (input == null)
        {
            context.log("Onnx_feature_vector_source: preprocessing failed (file MAY BE CORRUPTED): " + path);
            return Optional.empty();
        }

        double[] values = engine.embed(input, context);
        if (values == null)
        {
            context.log("Onnx_feature_vector_source: inference failed for: " + path);
            return Optional.empty();
        }
        return Optional.of(new Feature_vector_double(values, who_are_you));
    }
}
