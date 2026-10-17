// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.image_similarity;

import klikr.machine_learning.feature_vector.Feature_vector_double;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.machine_learning.full_java_ML.Onnx_embedding_engine;
import klikr.machine_learning.full_java_ML.Onnx_feature_vector_source;
import klikr.machine_learning.python_via_html_ML.Feature_vector_source_server;
import klikr.machine_learning.python_via_html_ML.Load_balancer;
import klikr.machine_learning.python_via_html_ML.ML_server_type;
import klikr.machine_learning.python_via_html_ML.monitoring.UDP_traffic_monitor;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.Kontext;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public class Onnx_Feature_vector_source_for_image_similarity implements Feature_vector_source
//**********************************************************
{
    private final Onnx_feature_vector_source onnx_feature_vector_source;

    //**********************************************************
    public Onnx_Feature_vector_source_for_image_similarity(Kontext context)
    //**********************************************************
    {
        Optional<Onnx_feature_vector_source> mobilenet =
                Onnx_feature_vector_source.create(Onnx_embedding_engine.Kind.MOBILENET, context);
        if (mobilenet.isPresent())
        {
            context.log("onnx mobilenet found");
            this.onnx_feature_vector_source = mobilenet.get();
        }
        else
        {
            context.log(Logger.error+" onnx mobilenet NOT found");
            this.onnx_feature_vector_source = null;
        }
    }

    //**********************************************************
    @Override
    public Optional<Feature_vector_double> get_feature_vector(Path path, Kontext context)
    //**********************************************************
    {
        return onnx_feature_vector_source.get_feature_vector(path, context);
    }

}
