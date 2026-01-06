// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.feature_vector;

import klikr.util.log.Logger;

public class Feature_vector_mask
{
    public final double[] mins;
    public final double[] maxs;
    public final double[] diffs;
    public final boolean[] mask;

    public Feature_vector_mask(Feature_vector_double fv1, Feature_vector_double fv2, boolean not_same, Logger logger)
    {
        mins = new double[fv1.features.length];
        maxs = new double[fv1.features.length];
        diffs = new double[fv1.features.length];
        mask = new boolean[fv1.features.length];
        for ( int i = 0; i < fv1.features.length; i++)
        {
            mins[i] = Double.MAX_VALUE;
            maxs[i] = -Double.MAX_VALUE;
        }
        for ( int i = 0; i < fv1.features.length; i++)
        {
            if (fv1.features[i] < mins[i]) mins[i] = fv1.features[i];
            if (fv2.features[i] < mins[i]) mins[i] = fv2.features[i];
            if (fv1.features[i] > maxs[i]) maxs[i] = fv1.features[i];
            if (fv2.features[i] > maxs[i]) maxs[i] = fv2.features[i];
        }
        for ( int i = 0; i < fv1.features.length; i++)
        {
            double f1 = 0;
            double f2 = 0;
            if ( (maxs[i]-mins[i]) != 0)
            {
                f1 = (fv1.features[i]-mins[i])/(maxs[i]-mins[i]);
                f2 = (fv2.features[i]-mins[i])/(maxs[i]-mins[i]);
            }

            // when f2=max=1 and f1=min=0 ==> 1
            // when f2=min=0 and f1=max=1 ==> 0
            // if both are EQUAL (e.g min or max) ==> 0.5

            diffs[i] = (f2-f1+1)/2.0;
        }
        if ( not_same)
        {
            /*for (int i = 0; i < fv1.features.length; i++)
            {
                logger.log(mins[i] + " < " + fv1.features[i]+ " < " + maxs[i]);
                logger.log(mins[i] + " < " + fv2.features[i]+ " < " + maxs[i]);
                logger.log("diff === "+diffs[i]+"\n");
            }*/

            int count = 0;
            for (int i = 0; i < fv1.features.length; i++)
            {
                if (diffs[i] <= 0.5)
                {
                    mask[i] = true;
                    count++;
                }
                else mask[i] = false;
            }
            //logger.log("mask size = " + count);
        }
    }

    public Double similarity_with_mask(Feature_vector fv0, Feature_vector fv1)
    {
        return similarity_with_mask_shorter(fv0,fv1);
        //return similarity_with_mask_stronger(fv0,fv1);
    }
    public Double similarity_with_mask_shorter(Feature_vector fv0_, Feature_vector fv1_)
    {
        Feature_vector_double fv0 = (Feature_vector_double) fv0_;
        Feature_vector_double fv1 = (Feature_vector_double) fv1_;
        int size = 0;
        for ( int i = 0 ; i < mask.length; i++)
        {
            if (mask[i]) size++;
        }
        double[] fv0_short = new double[size];
        double[] fv1_short = new double[size];
        int j = 0;
        for ( int i = 0 ; i < mask.length; i++)
        {
            if (mask[i])
            {
                fv0_short[j] = fv0.features[i];
                fv1_short[j] = fv1.features[i];
                j++;
            }
        }
        Feature_vector_double fv0_masked = new Feature_vector_double(fv0_short);
        Feature_vector_double fv1_masked = new Feature_vector_double(fv1_short);
        return fv0_masked.cosine_similarity(fv1_masked);
    }

    public Double similarity_with_mask_stronger(Feature_vector_double fv0, Feature_vector_double fv1)
    {
        int size = mask.length;
        double[] fv0_short = new double[size];
        double[] fv1_short = new double[size];
        for ( int i = 0 ; i < mask.length; i++)
        {
            if (mask[i])
            {
                fv0_short[i] = fv0.features[i];
                fv1_short[i] = fv1.features[i];
            }
            else
            {
                fv0_short[i] = maxs[i];
                fv1_short[i] = mins[i];
            }
        }
        Feature_vector_double fv0_masked = new Feature_vector_double(fv0_short);
        Feature_vector_double fv1_masked = new Feature_vector_double(fv1_short);
        return fv0_masked.cosine_similarity(fv1_masked);
    }
}
