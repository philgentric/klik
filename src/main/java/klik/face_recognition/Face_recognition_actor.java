package klik.face_recognition;

import javafx.scene.image.Image;
import klik.actor.*;
import klik.properties.Static_application_properties;
import klik.util.Stack_trace_getter;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Face_recognition_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;
    public static final boolean verbose = false;
    public static final int K_of_KNN = 3;
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
        if ( frm.do_face_detection)
        {
            detect_face_and_recognize(frm.file, frm.label, frm.display_face_reco_window, frm.count_for_label, frm.get_aborter());
        }
        else
        {
            just_recognize(frm.file, frm.label, frm.display_face_reco_window, frm.count_for_label, frm.get_aborter());
        }
        return null;
    }

    //**********************************************************
    private void detect_face_and_recognize(File file, String label, boolean display_face_reco_window, AtomicInteger count_for_label, Aborter aborter)
    //**********************************************************
    {
        //service.logger.log("process_file FILE before: "+file.getAbsolutePath());
        Face_recognition_results face_recognition_results = detect_and_recognize(file.toPath(), display_face_reco_window,aborter);
        //service.logger.log("process_file FILE after : "+file.getAbsolutePath()+ " "+ face_recognition_results.status);
        switch ( face_recognition_results.status)
        {
            case server_not_reacheable:
                service.logger.log("detect_face_and_recognize:server_not_reacheable");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                return;
            case  error:
                service.logger.log("detect_face_and_recognize:error");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                return;
            case no_face_detected:
                // happens a lot
                service.logger.log("detect_face_and_recognize:no_face_detected "+file);
                service.recognition_stats.done.incrementAndGet();
                service.recognition_stats.no_face_detected.incrementAndGet();
                service.training_stats.no_face_detected.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case face_detected:
                service.logger.log("detect_face_and_recognize:should not happen 1");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case no_feature_vector:
                service.logger.log("detect_face_and_recognize:should not happen 2");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case feature_vector_ready:
                service.logger.log("detect_face_and_recognize:should not happen 2");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case face_recognized :
                service.logger.log("detect_face_and_recognize:face_recognized");
                service.recognition_stats.face_recognized.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // no training
                    return;
                }
                if ( label.equals(face_recognition_results.label))
                {
                    service.training_stats.face_correctly_recognized_not_recorded.incrementAndGet();
                    service.training_stats.done.incrementAndGet();
                    service.logger.log("skipping "+file.getAbsolutePath()+" label was correct: "+label);
                    return;
                }
                service.logger.log("adding "+file.getAbsolutePath()+" label was NOT correct: "+face_recognition_results.label);
                service.training_stats.face_wrongly_recognized_recorded.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                add_prototype_to_set(file,label,face_recognition_results,aborter, count_for_label);
                break;
            case exact_match, no_face_recognized :
                service.logger.log("detect_face_and_recognize: NO face_recognized");
                service.recognition_stats.face_not_recognized.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // this is a "pure" recognition task
                    //service.logger.log(Stack_trace_getter.get_stack_trace("process_file: NO face_recognized, nolabel ?????????"));
                    // no training
                    return;
                }
                service.training_stats.face_wrongly_recognized_recorded.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                add_prototype_to_set(file,label,face_recognition_results,aborter, count_for_label);
                break;


            default:
                service.logger.log(Stack_trace_getter.get_stack_trace("detect_face_and_recognize: should not happen"));
                break;

        }
    }

    //**********************************************************
    private void just_recognize(File file, String label, boolean display_face_reco_window, AtomicInteger count_for_label, Aborter aborter)
    //**********************************************************
    {
        //service.logger.log("process_file FILE before: "+file.getAbsolutePath());
        Face_recognition_results face_recognition_results = recognize_a_face(file.toPath(), display_face_reco_window,aborter,service);
        //service.logger.log("process_file FILE after : "+file.getAbsolutePath()+ " "+ face_recognition_results.status);
        switch ( face_recognition_results.status)
        {
            case server_not_reacheable:
                service.logger.log("just_recognize:server_not_reacheable");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                return;
            case  error:
                service.logger.log("just_recognize:error");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                return;
            case no_face_detected:
                // happens a lot
                service.logger.log("just_recognize:no_face_detected "+file);
                service.recognition_stats.done.incrementAndGet();
                service.recognition_stats.no_face_detected.incrementAndGet();
                service.training_stats.no_face_detected.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case face_detected:
                service.logger.log("just_recognize:should not happen 1");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case no_feature_vector:
                service.logger.log("just_recognize:should not happen 2");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case feature_vector_ready:
                service.logger.log("just_recognize:should not happen 3");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case exact_match:
                service.logger.log("just_recognize:exact_match");
                service.recognition_stats.face_recognized.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                // no training
                return;
            case face_recognized :
                service.logger.log("just_recognize:face_recognized");
                service.recognition_stats.face_recognized.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // no training
                    return;
                }
                if ( label.equals(face_recognition_results.label))
                {
                    service.training_stats.face_correctly_recognized_not_recorded.incrementAndGet();
                    service.training_stats.done.incrementAndGet();
                    service.logger.log("skipping "+file.getAbsolutePath()+" label was correct: "+label);
                    return;
                }
                service.logger.log("adding "+file.getAbsolutePath()+" label was NOT correct: "+face_recognition_results.label);
                service.training_stats.face_wrongly_recognized_recorded.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                add_prototype_to_set(file,label,face_recognition_results,aborter, count_for_label);
                break;
            case no_face_recognized :
                service.logger.log("just_recognize: NO face_recognized");
                service.recognition_stats.face_not_recognized.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // this is a "pure" recognition task
                    //service.logger.log(Stack_trace_getter.get_stack_trace("process_file: NO face_recognized, nolabel ?????????"));
                    // no training
                    return;
                }
                service.training_stats.face_wrongly_recognized_recorded.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                add_prototype_to_set(file,label,face_recognition_results,aborter, count_for_label);
                break;
            default:
                service.logger.log(Stack_trace_getter.get_stack_trace("just_recognize: should not happen"));
                break;

        }
    }


    //**********************************************************
    private boolean add_prototype_to_set1(File f, String label, Aborter aborter, AtomicInteger count_for_label)
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
        Prototype_adder_actor actor = new Prototype_adder_actor(service);
        Prototype_adder_message msg = new Prototype_adder_message(status.image(),label,aborter);
        Actor_engine.run(actor,msg,null, service.logger);
        count_for_label.incrementAndGet();
        return true;
    }


    //**********************************************************
    private boolean add_prototype_to_set(File f, String label, Face_recognition_results face_recognition_results, Aborter aborter, AtomicInteger count_for_label)
    //**********************************************************
    {
        service.logger.log("ADDING "+f.getAbsolutePath()+" with label: "+label);

        Prototype_adder_actor actor = new Prototype_adder_actor(service);
        Prototype_adder_message msg = new Prototype_adder_message(face_recognition_results.image(),label,aborter);
        Actor_engine.run(actor,msg,null, service.logger);
        count_for_label.incrementAndGet();
        return true;
    }


    record Eval_results(String label, boolean exact_match, boolean enable_adding, String name, List<Eval_result_for_one_prototype> list){};

    record Eval_result_for_one_prototype(Double distance, Embeddings_prototype embeddings_prototype){}

    static Comparator<? super Eval_result_for_one_prototype> comp = new Comparator<Eval_result_for_one_prototype>() {
        @Override
        public int compare(Eval_result_for_one_prototype o1, Eval_result_for_one_prototype o2) {
            return o1.distance.compareTo(o2.distance);
        }
    };

    //**********************************************************
    private static Eval_results eval_a_face(Path face, Face_recognition_service service)
    //**********************************************************
    {
        Feature_vector the_feature_vector_to_be_identified = Feature_vector.get_feature_vector_from_server(face, service.logger);
        if ( the_feature_vector_to_be_identified == null)
        {
            service.logger.log(Stack_trace_getter.get_stack_trace("PANIC: embeddings failed "));
            return new Eval_results("error",false,false,"error",new ArrayList<>());
        }


        Embeddings_prototype winner = null;
        ConcurrentLinkedQueue<Eval_result_for_one_prototype> out_queue = new ConcurrentLinkedQueue<>();
        CountDownLatch cdl = new CountDownLatch(service.embeddings_prototypes.size());
        for (Embeddings_prototype embeddings_prototype : service.embeddings_prototypes)
        {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    double distance = the_feature_vector_to_be_identified.distance(embeddings_prototype.feature_vector());
                    //nearests.put(distance,embeddings_prototype);
                    out_queue.add(new Eval_result_for_one_prototype(distance,embeddings_prototype));
                    if ( verbose) service.logger.log("   at distance ="+String.format("%.4f",distance)+"  =>   "+embeddings_prototype.label());
                    cdl.countDown();
                }
            };
            Actor_engine.execute(r,new Aborter("distances",service.logger),service.logger);
        }
        List<Eval_result_for_one_prototype> results = new ArrayList<>();
        for(;;)
        {
            Eval_result_for_one_prototype r = out_queue.poll();
            if ( r != null) results.add(r);
            else
            {
                if ( cdl.getCount() == 0 ) break;
            }
        }
        try {
            cdl.await(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            service.logger.log("EVAL TIMEOUT "+e);
        }
        if (results.isEmpty())
        {
            service.logger.log("not results ???????");
            return new Eval_results("empty",false, false,"empty",new ArrayList<>());
        }

        Collections.sort(results,comp);

        double min_distance = results.get(0).distance;
        winner = results.get(0).embeddings_prototype;
        if ( min_distance < 0.001)
        {
            service.logger.log("1 nearest "+winner.label()+ " at "+min_distance);
            List<Eval_result_for_one_prototype> l = new ArrayList<>();
            l.add(new Eval_result_for_one_prototype(min_distance,winner));
            return new Eval_results(winner.label(),true, false,winner.name(),l);
        }

        double average_distance = 0;
        Map<String,Integer> votes = new HashMap<>();
        List<Eval_result_for_one_prototype> list_of_Eval_result_for_one_prototype = new ArrayList<>();
        int max = K_of_KNN;
        if (results.size() < max) max = results.size();
        for ( int i = 0 ; i < max; i++)
        {
            Eval_result_for_one_prototype res = results.get(i);

            service.logger.log("     d="+String.format("%.3f",res.distance)+ " "+ res.embeddings_prototype().name());

            average_distance += res.distance;

            Embeddings_prototype ep = res.embeddings_prototype;
            list_of_Eval_result_for_one_prototype.add(res);

            String label2 = ep.label();
            Integer vote = votes.get(label2);
            if ( vote == null)
            {
                votes.put(label2, Integer.valueOf(1));
            }
            else
            {
                votes.put(label2, Integer.valueOf(vote+1));
            }
        }
        average_distance /= (double) K_of_KNN;

        int max_vote = 0;
        String label5 = null;

        for (Map.Entry<String,Integer> e : votes.entrySet())
        {
            String lab = e.getKey();
            Integer vote = e.getValue();
            if ( vote > max_vote)
            {
                max_vote = vote;
                label5 = lab;
            }
        }

        // if max_vote is ex aequo, the distance decides
        int ex_aequo = 0;
        List<String> ex_aequo_labels = new ArrayList<>();
        for (Map.Entry<String,Integer> e : votes.entrySet())
        {
            String lab = e.getKey();
            Integer vote = e.getValue();

            if ( vote == max_vote)
            {
                ex_aequo++;
                ex_aequo_labels.add(lab);
            }
        }
        if ( ex_aequo > 1)
        {
            // use distance to break the tie
            double min = Double.MAX_VALUE;
            Eval_result_for_one_prototype win =null;
            for ( String lab : ex_aequo_labels)
            {
                for (Eval_result_for_one_prototype r : results)
                {
                    if (r.embeddings_prototype.label().equals(lab))
                    {
                        if (r.distance() < min )
                        {
                            min = r.distance();
                            win = r;
                        }
                    }
                }
            }
            list_of_Eval_result_for_one_prototype.clear();
            list_of_Eval_result_for_one_prototype.add(win);
            return new Eval_results(win.embeddings_prototype().label(),false,true,win.embeddings_prototype().name(),list_of_Eval_result_for_one_prototype);
        }


        if ( winner != null) service.logger.log("1 nearest "+winner.label()+ " at "+min_distance);
        service.logger.log("5 nearest "+label5+ " average distance = "+average_distance);


        return new Eval_results(label5,false,true,null,list_of_Eval_result_for_one_prototype);
    }


    record Face_recognition_results(Image image, String label, Face_recognition_status status){
        public String to_string()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("label : ");
            sb.append(label);
            sb.append(" status: ");
            sb.append(status);
            return sb.toString();
        }
    }

    //**********************************************************
    public Face_recognition_results detect_and_recognize(Path tested, boolean display_face_reco_window, Aborter aborter)
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
            if ( display_face_reco_window) service.show_face_recognition_window(null,null,aborter);
            return new Face_recognition_results(null,null,Face_recognition_status.no_face_detected);
        }
        Image face = status.image();

        if (face == null)
        {
            if ( display_face_reco_window) Face_detector.warn_about_no_face_detected(service.logger);
            else service.logger.log("no face dtetcetd");
            return new Face_recognition_results(null,null,Face_recognition_status.no_face_detected);
        }

        service.logger.log("Face detected");

        String name = "tmp_unknown_face"+ "_"+ UUID.randomUUID();
        Path tmp_image_reco = Static_application_properties.get_trash_dir(service.face_recognizer_path,service.logger);
        Path path_to_face = Face_recognition_service.write_tmp_image(face, tmp_image_reco,name,service.logger);

        Eval_results eval_result = eval_a_face(path_to_face,service);
        if (display_face_reco_window) service.show_face_recognition_window(face,eval_result, aborter);

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
            if ( eval_result.exact_match())
            {
                return new Face_recognition_results(face,eval_result.label(),Face_recognition_status.exact_match);
            }
            else
            {
                return new Face_recognition_results(face,eval_result.label(),Face_recognition_status.face_recognized);
            }
        }
    }

    //**********************************************************
    public static Face_recognition_results recognize_a_face(Path path_of_face, boolean display_face_reco_window, Aborter aborter, Face_recognition_service service)
    //**********************************************************
    {
        Eval_results eval_result = eval_a_face(path_of_face, service);
        Image face = Utils.get_image(path_of_face);
        if (face == null)
        {
            // TODO: these error messages are not accurate
            if ( display_face_reco_window) Face_detector.warn_about_no_face_detected(service.logger);
            else service.logger.log("fatal : cannot load image");
            return new Face_recognition_results(null,null,Face_recognition_status.no_face_detected);
        }

        if (display_face_reco_window)
        {

            service.show_face_recognition_window(face,eval_result, aborter);
        }

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
            if ( eval_result.exact_match())
            {
                return new Face_recognition_results(face,eval_result.label(),Face_recognition_status.exact_match);
            }
            else
            {
                return new Face_recognition_results(face,eval_result.label(),Face_recognition_status.face_recognized);
            }
        }
    }
}
