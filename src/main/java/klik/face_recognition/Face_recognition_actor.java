package klik.face_recognition;

import javafx.scene.image.Image;
import klik.actor.*;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Face_recognition_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;

    public final static String EXTENSION_FOR_EP = "prototype";
    public static final int K_of_KNN = 5;
    private final Face_recognition_service service;

    //**********************************************************
    public Face_recognition_actor(Face_recognition_service service_)
    //**********************************************************
    {
        service = service_;
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Face_recognition_message frm = (Face_recognition_message) m;

        if ( frm.files_in_flight !=null) frm.files_in_flight.incrementAndGet();
        process_file(frm.file, frm.label, frm.display_face_reco_window, frm.get_aborter());
        return null;
    }



    //**********************************************************
    private void process_file(File f, String label, boolean display_face_reco_window, Aborter aborter)
    //**********************************************************
    {
        service.logger.log("process_file FILE before: "+f.getAbsolutePath());
        Face_recognition_results result = recognize(f.toPath(), display_face_reco_window);
        service.logger.log("process_file FILE after : "+f.getAbsolutePath()+ " "+ result.status);
        switch ( result.status)
        {
            case server_not_reacheable:
                service.logger.log("process_file:server_not_reacheable");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                return;
            case  error:
                service.logger.log("process_file:error");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                return;
            case no_face_detected:
                // happens a lot
                service.logger.log("process_file:no_face_detected");
                service.recognition_stats.done.incrementAndGet();
                service.recognition_stats.no_face_detected.incrementAndGet();
                service.training_stats.no_face_detected.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case face_detected:
                service.logger.log("process_file:should not happen 1");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case no_feature_vector:
                service.logger.log("process_file:should not happen 2");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case feature_vector_ready:
                service.logger.log("process_file:should not happen 2");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case face_recognized :
                service.logger.log("process_file:face_recognized");
                service.recognition_stats.face_recognized.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // no training
                    return;
                }
                if ( label.equals(result.label))
                {
                    service.training_stats.face_correctly_recognized_not_recorded.incrementAndGet();
                    service.training_stats.done.incrementAndGet();
                    service.logger.log("skipping "+f.getAbsolutePath()+" label was correct: "+label);
                    return;
                }
                service.logger.log("adding "+f.getAbsolutePath()+" label was NOT correct: "+label);
                service.training_stats.face_wrongly_recognized_recorded.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                add_prototype_to_set(f,label,result,aborter);
                break;
            case no_face_recognized :
                service.logger.log("process_file: NO face_recognized");
                service.recognition_stats.face_not_recognized.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // no training
                    return;
                }
                service.training_stats.face_wrongly_recognized_recorded.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                add_prototype_to_set(f,label,result,aborter);
                break;
            default:
                service.logger.log(Stack_trace_getter.get_stack_trace("process_file: should not happen"));
                break;

        }
    }


    //**********************************************************
    private boolean add_prototype_to_set(File f, String label, Face_recognition_results result, Aborter aborter)
    //**********************************************************
    {
        service.logger.log("ADDING "+f.getAbsolutePath()+" with label: "+label);

        Path reference_face = f.toPath();
        Face_detector.Face_detection_result status = Face_detector.detect_face(reference_face, false,service.logger);

        if (status.status() == Face_recognition_status.server_not_reacheable)
        {
            service.logger.log("Face_recognition_status.server_not_reacheable");
            return false;
        }
        if (status.status() != Face_recognition_status.face_detected)
        {
            service.logger.log("no face detected");
            return false;
        }

        //add_prototype_image_face(status.image(),label, true);
        Prototype_adder_actor actor = new Prototype_adder_actor(service);
        Prototype_adder_message msg = new Prototype_adder_message(status.image(),label,aborter);
        Actor_engine.run(actor,msg,null, service.logger);

        return true;
    }



    //**********************************************************
    public static Feature_vector get_image_embeddings(Path path, Logger logger)
    //**********************************************************
    {
        String url_string = null;
        try {
            String encodedPath = URLEncoder.encode(path.toAbsolutePath().toString(), "UTF-8");
            url_string = "http://localhost:8001/" + encodedPath;
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
        logger.log("Connection established: "+connection.toString());
        // Send a GET request to the server
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

        // Get the response code and message
        try {
            int responseCode = connection.getResponseCode();
            logger.log("Response Code: " + responseCode);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }

        try {
            String responseMessage = connection.getResponseMessage();
            logger.log("Response Message: " + responseMessage);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }

        // Read the response from the server
        BufferedInputStream bufferedInputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(connection.getInputStream());
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }

        StringBuffer sb = new StringBuffer();
        for(;;)
        {
            try {
                int c = bufferedInputStream.read();
                if ( c == -1) break;
                //System.out.print((char)c);
                sb.append((char)c);
            } catch (IOException e) {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
                return null;
            }
        }

        // Use a JSON parser library (e.g., Jackson) to parse the JSON string
        String json = sb.toString();
        //logger.log("json ="+json);
        Feature_vector fv = Feature_vector.parse_json(json,logger);
        if ( fv == null) {
            logger.log("feature vector is null");
        }
        else {
            //logger.log("feature vector ="+fv.to_string());
            logger.log("feature vector size:"+fv.features.length);
        }

        return fv;
    }

    record Eval_result(String label, boolean enable_adding, String name, List<Image> list){};

    //**********************************************************
    private Eval_result eval(Path face, boolean verbose)
    //**********************************************************
    {
        Feature_vector fv = get_image_embeddings(face, service.logger);
        AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        Embeddings_prototype winner = null;

        ConcurrentSkipListMap<Double, Embeddings_prototype> nearests = new ConcurrentSkipListMap<>();
        CountDownLatch cdl = new CountDownLatch(service.embeddings_prototypes.size());
        for (Embeddings_prototype ep : service.embeddings_prototypes)
        {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    double distance = fv.distance(ep.feature_vector());
                    long local_distance = Double.doubleToLongBits(distance);
                    long local_min = min.get();
                    if ( local_distance < local_min ) min.set(local_distance);
                    nearests.put(distance,ep);
                    if ( verbose) service.logger.log("   at distance ="+(int)distance+"  =>   "+ep.label());
                    cdl.countDown();
                }
            };
            Actor_engine.execute(r,new Aborter("disatnces",service.logger),service.logger);
        }
        try {
            cdl.await(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            service.logger.log("EVAL TIMEOUT "+e);
        }
        winner = nearests.get(Double.longBitsToDouble(min.get()));
        service.logger.log("min distance :"+min.get());
        if ( min.get() == 0)
        {
            service.logger.log("1 nearest "+winner.label()+ " at "+min.get());
            List<Image> l = new ArrayList<>();
            l.add(winner.face());
            return new Eval_result(winner.label(),false,winner.name(),l);
        }

        // vote
        Map<String,Integer> votes = new HashMap<>();
        int count = 0;
        List<Image> list_of_faces = new ArrayList<>();
        for (Map.Entry<Double,Embeddings_prototype> e : nearests.entrySet())
        {
            Embeddings_prototype ep = e.getValue();
            String label2 = e.getValue().label();
            Integer vote = votes.get(label2);
            if ( vote == null)
            {
                votes.put(label2, Integer.valueOf(1));
            }
            else
            {
                votes.put(label2, Integer.valueOf(vote+1));
            }
            list_of_faces.add(ep.face());
            count++;
            if ( count > K_of_KNN) break;
        }
        int max_vote = 0;
        String label5 = null;
        for (Map.Entry<String,Integer> e : votes.entrySet())
        {
            String l = e.getKey();
            Integer i = e.getValue();
            if ( i > max_vote)
            {
                max_vote = i;
                label5 = l;
            }
        }


        if ( winner != null) service.logger.log("1 nearest "+winner.label()+ " at "+min);
        service.logger.log("5 nearest "+label5);


        return new Eval_result(label5,true,null,list_of_faces);
    }


    record Face_recognition_results(Image image, String label, Face_recognition_status status){}

    //**********************************************************
    public Face_recognition_results recognize(Path tested, boolean display_face_reco_window)
    //**********************************************************
    {
        Face_detector.Face_detection_result status = Face_detector.detect_face(tested, display_face_reco_window,service.logger);
        if (status.status() == Face_recognition_status.server_not_reacheable)
        {
            if ( display_face_reco_window) Face_detector.warn_about_face_detector_server(service.logger);
            return new Face_recognition_results(null, null,Face_recognition_status.server_not_reacheable);
        }
        if (status.status() != Face_recognition_status.face_detected)
        {
            if ( display_face_reco_window) service.show_face_recognition_window(null,null,false, null, new ArrayList<>());
            return new Face_recognition_results(null,null,Face_recognition_status.no_face_detected);
        }
        Image face = status.image();

        if (face == null)
        {
            if ( display_face_reco_window) Face_detector.warn_about_no_face_detected(service.logger);
            else service.logger.log("no face dtetcetd");
            return new Face_recognition_results(null,null,Face_recognition_status.no_face_detected);
        }

        if ( face == null)
        {
            service.logger.log("NO face detected");
            return new Face_recognition_results(null,null,Face_recognition_status.no_face_detected);
        }

        service.logger.log("Face detected");

        String name = "tmp_unknown_face"+ "_"+ UUID.randomUUID();
        Path tmp_image_reco = Static_application_properties.get_trash_dir(service.face_recognizer_path,service.logger);
        Path path_to_face = Face_recognition_service.write_tmp_image(face, tmp_image_reco,name,service.logger);

        Eval_result eval_result = eval(path_to_face, display_face_reco_window);
        if (display_face_reco_window) service.show_face_recognition_window(face,eval_result.label(),eval_result.enable_adding(), eval_result.name(), eval_result.list());

        String display_label = eval_result.label();
        if ( eval_result.label() == null)
        {
            display_label = "not recognized";
        }
        service.logger.log("face reco result = "+display_label);
        //display(face, display_label);
        if ( eval_result.label() == null)
        {
            return new Face_recognition_results(face,null,Face_recognition_status.no_face_recognized);
        }
        else
        {
            return new Face_recognition_results(face,eval_result.label(),Face_recognition_status.face_recognized);
        }
    }
}
