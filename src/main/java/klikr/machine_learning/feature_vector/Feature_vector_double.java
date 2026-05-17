// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.feature_vector;

import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

//**********************************************************
public class Feature_vector_double implements Feature_vector
//**********************************************************
{
    private final static boolean dbg = false;

    public double[] features;
    public final String who_are_you;

    //**********************************************************
    public Feature_vector_double(double[] values, String who_are_you)
    //**********************************************************
    {
        this.who_are_you = who_are_you;
        if ( dbg)
        {
            if (values == null)
            {
                System.out.println("❌ PANIC Feature_vector constructor called with null values");
            }
        }
        features = values;
    }


    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < features.length; i++)
        {
            sb.append(features[i]);
            sb.append(" ");
        }
        return sb.toString();
    }

    //**********************************************************
    @Override
    public double distance(Feature_vector feature_vector, Logger logger)
    //**********************************************************
    {
        return cosine_similarity(feature_vector,logger);
        //return hamming_similarity(feature_vector);
    }

    //**********************************************************
    @Override
    public int size()
    //**********************************************************
    {
        return features.length*Double.SIZE/8;
    }

    //**********************************************************
    private double compare_max(Feature_vector_double feature_vector)
    //**********************************************************
    {
        int n = features.length;
        double largest_diff = 0;
        for (int i = 0; i < n; i++) {
            double diff = Math.abs(features[i] -feature_vector.features[i]);
            if ( diff > largest_diff) largest_diff = diff;
        }
        return largest_diff;

    }

    //**********************************************************
    private double compare_sum(Feature_vector_double feature_vector)
    //**********************************************************
    {
        int n = features.length;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            double diff = Math.abs(features[i] -feature_vector.features[i]);
            sum += diff;
        }
        return sum;

    }


    //**********************************************************
    public double cosine_similarity(Feature_vector other_feature_vector_, Logger logger)
    //**********************************************************
    {
        Feature_vector_double other_feature_vector = (Feature_vector_double) other_feature_vector_;
        int n = features.length;
        int n2 = other_feature_vector.features.length;
        if ( n2 != n)
        {
            // this can legitimately happen for song similarity
            // i.e. a short song creates a shorter vector ...
            if (dbg) logger.log(Stack_trace_getter.get_stack_trace("WARNING.... feature vector size mismatch "+n+" "+this.who_are_you+" vs "+n2+ " "+((Feature_vector_double) other_feature_vector_).who_are_you));
            // too bad ... let us try the short way
            if ( n2 < n) n = n2;
        }

        double dotProduct = 0.0;
        double magnitudeVec1 = 0.0;
        double magnitudeVec2 = 0.0;

        for (int i = 0; i < n; i++) {
            dotProduct += features[i] * other_feature_vector.features[i];
            magnitudeVec1 += features[i] * features[i];
            magnitudeVec2 += other_feature_vector.features[i] * other_feature_vector.features[i];
        }
        if (magnitudeVec1 == 0.0 || magnitudeVec2 == 0.0) {
            return 0.0; // avoid NaN
        }

        double mag = Math.sqrt(magnitudeVec1*magnitudeVec2);

        double cosineSimilarity = dotProduct / mag;
        return 1 - cosineSimilarity;
    }



    //**********************************************************
    public double hamming_similarity(Feature_vector_double feature_vector)
    //**********************************************************
    {
        int n = features.length;
        double sum = 0.0;

        for (int i = 0; i < n; i++)
        {
            double diff = Math.abs(features[i] -feature_vector.features[i]);
            sum += diff;
        }
        return sum;
    }



    //**********************************************************
    public double euclidian(Feature_vector_double featureVector)
    //**********************************************************
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
