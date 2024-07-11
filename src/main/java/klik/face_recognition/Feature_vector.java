package klik.face_recognition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Path;
import java.util.Random;

//**********************************************************
public class Feature_vector
//**********************************************************
{
    // server's port to get embeddings:
    static int[] port = {8020, 8021};//, 8022, 8023, 8024};//, 8025, 8026, 8027, 8028, 8029};
    static Random random = new Random();
    //**********************************************************
    public static int get_random_port()
    //**********************************************************
    {
        int returned = random.nextInt(port[0],port[0]+port.length);
        return returned;
    }

    public double[] features;

    public Feature_vector(double[] values) {
        features = values;
    }



    //**********************************************************
    public static Feature_vector get_feature_vector_from_server(Path path, Logger logger)
    //**********************************************************
    {
        if ( path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("BAD WARNING"));
            return null;
        }

        String url_string = null;
        try {
            String encodedPath = URLEncoder.encode(path.toAbsolutePath().toString(), "UTF-8");
            url_string = "http://localhost:" + get_random_port() + "/" + encodedPath;
        } catch (UnsupportedEncodingException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        URL url = null;
        try {
            url = new URL(url_string);
        } catch (MalformedURLException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
         try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(120_000);
        } catch (ProtocolException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        try {
            connection.connect();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        try {
            int response_code = connection.getResponseCode();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        try {
            String response_message = connection.getResponseMessage();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }

        // Read the JSON response one character at a time
        StringBuffer sb = new StringBuffer();
        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream()))
        {
            for(;;)
            {
                int c = bufferedInputStream.read();
                if ( c == -1) break;
                //System.out.print((char)c);
                sb.append((char)c);
            }
        } catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }

        // Use a JSON parser library (e.g., Jackson) to parse the JSON string
        String json = sb.toString();
        //logger.log("json ="+json);
        Feature_vector fv = Feature_vector.parse_json(json,logger);
        if ( fv == null) {
            logger.log("feature vector is null");
        }
        else {
            //logger.log("feature vector size:"+fv.features.length);
        }

        return fv;
    }

    //**********************************************************
    public static Feature_vector parse_json(String response, Logger logger)
    //**********************************************************
    {
        Gson gson = new GsonBuilder().create();
        Feature_vector fv = gson.fromJson(response, Feature_vector.class);
        //logger.log("parsed a feature vector, length: " + fv.features.length);
        return fv;
    }

    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < features.length; i++) {
            sb.append(features[i]);
            sb.append(", ");
        }
        return sb.toString();
    }

    // with VGG the best distance seems to be cosine...
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
