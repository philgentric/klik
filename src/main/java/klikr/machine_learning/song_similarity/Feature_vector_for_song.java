// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.song_similarity;

import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

//**********************************************************
public class Feature_vector_for_song implements Feature_vector
//**********************************************************
{
    public static final String FINGERPRINT = "FINGERPRINT=";

    public final String original_string;
    private final double[] features;
    Logger logger;

    //**********************************************************
    public Feature_vector_for_song(String result, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        original_string = result;

        // the "raw format is like this:
        // DURATION=120
        // FINGERPRINT=2027482382,2044243274,2044239307,2077859785,2069544841,2071639944,2033825688,2100869052,2098814892,1595620284,1595550140,3726322094,3

        int fingerprint_index = result.indexOf(FINGERPRINT);
        if ( fingerprint_index == -1)
        {
            logger.log("fpcalc parsing failed: no FINGERPRINT=  found");
            features = null;
            return;
        }
        String array_string = result.substring(fingerprint_index + FINGERPRINT.length()).trim();
        if ( array_string.isEmpty())
        {
            logger.log("fpcalc parsing failed: empty FINGERPRINT array");
            features = null;
            return;
        }
        String[] parts = array_string.split(",");
        features = new double[parts.length];
        for ( int i = 0; i < parts.length; i++)
        {
            try
            {
                features[i] = Double.parseDouble(parts[i].trim());
            }
            catch ( NumberFormatException e)
            {
                logger.log(Stack_trace_getter.get_stack_trace("parse_json: NumberFormatException for part="+parts[i]+" "+e));
                return;
            }
        }

    }

    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        return original_string;
    }

    //**********************************************************
    public double distance(Feature_vector feature_vector)
    //**********************************************************
    {
        if ( feature_vector instanceof Feature_vector_for_song other)
        {
            return cosine_distance(other);
        }
        else
        {
            logger.log("❌ PANIC feature_vector_bitmap.compare called with non eature_vector_bitmap");
            return 0;
        }
    }
    //**********************************************************
    public double cosine_distance(Feature_vector other_feature_vector_)
    //**********************************************************
    {
        Feature_vector_for_song other_feature_vector = (Feature_vector_for_song) other_feature_vector_;
        int n = features.length;
        if ( other_feature_vector.features.length < n)
        {
            n = other_feature_vector.features.length;
            logger.log("WARNING: chromaprint fingerprints differ in length "+n+" vs "+features.length);
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
    @Override
    public double size()
    //**********************************************************
    {
        return features.length*Double.SIZE/8.0;
    }
}
