package klik.image_ml;

//**********************************************************
public class Feature_vector
//**********************************************************
{


    public double[] features;

    public Feature_vector(double[] values) {
        features = values;
    }

    //**********************************************************
    public static Feature_vector from_string(String in) throws NumberFormatException
    //**********************************************************
    {
        String[] parts = in.split(" ");
        double[] returned = new double[parts.length];
        for ( int i = 0; i < parts.length; i++)
        {
            returned[i] = Double.parseDouble(parts[i]);
        }
        return new Feature_vector(returned);
    }

    //**********************************************************
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
    public double compare(Feature_vector feature_vector)
    //**********************************************************
    {
        return cosine_similarity(feature_vector);
        //return hamming_similarity(feature_vector);
    }

    //**********************************************************
    private double compare_max(Feature_vector feature_vector)
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
    private double compare_sum(Feature_vector feature_vector)
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
    public double cosine_similarity(Feature_vector feature_vector)
    //**********************************************************
    {
        int n = features.length;
        double dotProduct = 0.0;
        double magnitudeVec1 = 0.0;
        double magnitudeVec2 = 0.0;

        for (int i = 0; i < n; i++) {
            dotProduct += features[i] * feature_vector.features[i];
            magnitudeVec1 += features[i] * features[i];
            magnitudeVec2 += feature_vector.features[i] * feature_vector.features[i];
        }
        if (magnitudeVec1 == 0.0 || magnitudeVec2 == 0.0) {
            return 0.0; // avoid NaN
        }

        double mag = Math.sqrt(magnitudeVec1*magnitudeVec2);

        double cosineSimilarity = dotProduct / mag;
        return 1 - cosineSimilarity;
    }



    //**********************************************************
    public double hamming_similarity(Feature_vector feature_vector)
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
    public double euclidian(Feature_vector featureVector)
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
