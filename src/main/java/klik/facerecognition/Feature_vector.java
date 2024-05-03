package klik.facerecognition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import klik.util.Logger;

public class Feature_vector {
    public double[] features;

    public Feature_vector(double[] values) {
        features = values;
    }

    public static Feature_vector parse_json(String response, Logger logger)
    {
        Gson gson = new GsonBuilder().create();
        Feature_vector fv = gson.fromJson(response, Feature_vector.class);
        logger.log("parsed a feature vector, length: " + fv.features.length);
        return fv;
    }

    public String to_string() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < features.length; i++) {
            sb.append(features[i]);
            sb.append(", ");
        }
        return sb.toString();
    }

    public double distance(Feature_vector featureVector)
    {
        if ( this.features.length != featureVector.features.length)
        {
            throw new IllegalArgumentException("Feature vectors have different lengths");
        }
        double returned_distance = 0;
        for ( int i = 0; i < features.length; i++)
        {
            double f1 = this.features[i];
            double f2 = featureVector.features[i];
            double diff = f1 - f2;
            double diff2 = diff * diff;
            returned_distance += diff2;
        }
        return Math.sqrt(returned_distance);
    }
}
