// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Eval_result_for_one_prototype.java
//SOURCES ../Feature_vector.java
//SOURCES ../Feature_vector_source.java
//SOURCES ./Feature_vector_source_for_Face_recognition.java
//SOURCES ./Face_detection_type.java
//SOURCES ./Face_recognition_in_image_status.java
//SOURCES ./Face_detector.java

package klik.machine_learning.face_recognition;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klik.util.execute.actor.*;
import klik.machine_learning.feature_vector.Feature_vector;
import klik.machine_learning.feature_vector.Feature_vector_source;
import klik.properties.Cache_folder;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Face_recognition_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;
    public static final boolean verbose = false;
    public static final int K_of_KNN = 3;
    public static final int LIMIT_PER_LABEL = 3;
    private final Face_recognition_service service;
    private static Feature_vector_source feature_vector_source;
    //**********************************************************
    public Face_recognition_actor(Face_recognition_service service_)
    //**********************************************************
    {
        service = service_;
        feature_vector_source = new Feature_vector_source_for_face_recognition(service.owner,service.logger);
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Face_recognition_actor";
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Face_recognition_message frm = (Face_recognition_message) m;
        // we need to ALWAYS increment because the actor termination reporter will decrement it (even in case of abort)
        if ( frm.files_in_flight !=null) frm.files_in_flight.incrementAndGet();

        if ( frm.label != null)
        {
            if ( service.label_to_prototype_count.get(frm.label) > LIMIT_PER_LABEL)
            {
                service.skipped();
                service.logger.log("Face_recognition_actor, NOT performing recognition "+frm.file.getName()+" with label: "+frm.label+ " as there are too many prototypes already "+service.label_to_prototype_count.get(frm.label));
                return null;
            }
        }

        Path tmp_image_to_be_deleted = null;
        if ( frm.do_face_detection)
        {
            tmp_image_to_be_deleted = detect_face_and_recognize(frm.file, frm.face_detection_type, frm.label, frm.display_face_reco_window, frm.get_aborter());
        }
        else
        {
            tmp_image_to_be_deleted = just_recognize(frm.file,frm.label, frm.display_face_reco_window, frm.get_aborter());
        }
        if ( tmp_image_to_be_deleted != null)
        {
            try {
                //service.logger.log("deleting tmp image :"+tmp_image_to_be_deleted);
                Files.delete(tmp_image_to_be_deleted);
            } catch (IOException e) {
                service.logger.log(Stack_trace_getter.get_stack_trace(""+e));
            }
        }
        return null;
    }

    //**********************************************************
    private Path detect_face_and_recognize(File file, Face_detection_type face_detection_type, String label, boolean display_face_reco_window, Aborter aborter)
    //**********************************************************
    {
        //service.logger.log("process_file FILE before: "+file.getAbsolutePath());
        Face_recognition_results face_recognition_results = detect_and_recognize(
                service,
                file.toPath(),
                face_detection_type,
                display_face_reco_window,
                aborter,
                service.owner,
                service.logger);
        //service.logger.log("process_file FILE after : "+file.getAbsolutePath()+ " "+ Face_recognition_results.status);
        switch ( face_recognition_results.face_recognition_in_image_status)
        {
            case server_not_reacheable:
                service.logger.log("detect_face_and_recognize:server_not_reacheable");
                service.server_error();

                break;
            case  error:
                service.logger.log("detect_face_and_recognize:error");
                service.server_error();
                break;
            case no_face_detected:
                // happens a lot
                service.logger.log("detect_face_and_recognize:no_face_detected for: "+file);
                service.no_face_detected();
                break;
            case face_detected:
                service.logger.log("detect_face_and_recognize:should not happen 1");
                service.should_not_happen();
                break;
            case no_feature_vector:
                service.logger.log("detect_face_and_recognize:should not happen 2");
                service.should_not_happen();
                break;
            case feature_vector_ready:
                service.logger.log("detect_face_and_recognize:should not happen 3");
                service.should_not_happen();
                break;
            case exact_match, face_recognized :
                service.logger.log("detect_face_and_recognize:face_recognized");
                service.recognition_stats.face_recognized.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // no training
                    break;
                }
                if ( label.equals(face_recognition_results.label))
                {
                    service.training_stats.face_correctly_recognized_not_recorded.incrementAndGet();
                    service.training_stats.done.incrementAndGet();
                    service.logger.log("label was correct, skipping "+file.getName() );
                    break;
                }
                service.logger.log("ADDING "+file.getName()+" as label was NOT correct: "+face_recognition_results.label);

                add_prototype_to_set(file,label,face_recognition_results,aborter);
                break;
            case no_face_recognized :
                service.logger.log("detect_face_and_recognize: NO face_recognized");
                service.recognition_stats.face_not_recognized.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // this is a "pure" recognition task
                    //service.logger.log(Stack_trace_getter.get_stack_trace("process_file: NO face_recognized, nolabel ?????????"));
                    // no training
                    break;
                }
                service.training_stats.done.incrementAndGet();
                add_prototype_to_set(file,label,face_recognition_results,aborter);
                break;


            default:
                service.logger.log(Stack_trace_getter.get_stack_trace("detect_face_and_recognize: should not happen"));
                return null;

        }
        return face_recognition_results.image_path();
    }

    //**********************************************************
    private Path just_recognize(File file, String label, boolean display_face_reco_window, Aborter aborter)
    //**********************************************************
    {
        //service.logger.log("process_file FILE before: "+file.getAbsolutePath());
        Face_recognition_results Face_recognition_results = recognize_a_face(file.toPath(), display_face_reco_window,aborter,service);
        //service.logger.log("process_file FILE after : "+file.getAbsolutePath()+ " "+ Face_recognition_results.status);
        switch ( Face_recognition_results.face_recognition_in_image_status)
        {
            case server_not_reacheable:
                service.logger.log("just_recognize:server_not_reacheable");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
            case  error:
                service.logger.log("just_recognize:error");
                service.recognition_stats.error.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                service.training_stats.error.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                break;
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
                break;
            case face_recognized :
                service.logger.log("just_recognize:face_recognized");
                service.recognition_stats.face_recognized.incrementAndGet();
                service.recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // no training
                    break;
                }
                if ( label.equals(Face_recognition_results.label))
                {
                    service.training_stats.face_correctly_recognized_not_recorded.incrementAndGet();
                    service.training_stats.done.incrementAndGet();
                    service.logger.log("label was correct, skipping "+file.getName() );
                    break;
                }
                service.logger.log("ADDING "+file.getName()+" as label was NOT correct: "+Face_recognition_results.label);
                 add_prototype_to_set(file,label,Face_recognition_results,aborter);
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
                    break;
                }
                service.training_stats.face_wrongly_recognized_recorded.incrementAndGet();
                service.training_stats.done.incrementAndGet();
                add_prototype_to_set(file,label,Face_recognition_results,aborter);
                break;
            default:
                service.logger.log(Stack_trace_getter.get_stack_trace("just_recognize: should not happen"));
                break;

        }
        return Face_recognition_results.image_path;
    }



    //**********************************************************
    enum Eval_situation
    //**********************************************************
    {
        nothing_found,
        exact_match,
        ex_aequo,
        normal
    }

    record Eval_results(String label, Feature_vector feature_vector, Eval_situation eval_situation, boolean adding, String tag, List<Eval_result_for_one_prototype> list){};


    //**********************************************************
    static Comparator<? super Eval_result_for_one_prototype> comp = new Comparator<Eval_result_for_one_prototype>() {
        @Override
        public int compare(Eval_result_for_one_prototype o1, Eval_result_for_one_prototype o2) {
            return o1.distance().compareTo(o2.distance());
        }
    };

    static long start;
    static long count = 0;
    static double feature_vector_total_ns = 0;
    static double total_ns = 0;

    //**********************************************************
    private static Eval_results eval_a_face(Path face, Face_recognition_service service, Aborter aborter)
    //**********************************************************
    {
        service.logger.log("eval_a_face "+face);

        start = System.nanoTime();
        Optional<Feature_vector> op = feature_vector_source.get_feature_vector(face, service.owner, aborter,service.logger);
        if ( op.isEmpty())
        {
            service.logger.log(Stack_trace_getter.get_stack_trace("❌ PANIC: embeddings failed ! are the servers started ?"));
            return new Eval_results("error",null,Eval_situation.nothing_found,false,"error",new ArrayList<>());
        }
        Feature_vector the_feature_vector_to_be_identified = op.get();
        long fv_time = System.nanoTime()-start;
        feature_vector_total_ns += fv_time;

        List<Eval_result_for_one_prototype> results = new ArrayList<>();
        for (Embeddings_prototype embeddings_prototype : service.embeddings_prototypes)
        {

            double distance = the_feature_vector_to_be_identified.distance(embeddings_prototype.feature_vector());
            //nearests.put(distance,embeddings_prototype);
            Eval_result_for_one_prototype r = new Eval_result_for_one_prototype(distance,embeddings_prototype);
            if ( verbose) service.logger.log("   at distance ="+String.format("%.4f",distance)+"  =>   "+embeddings_prototype.label());
            results.add(r);
        }

        if (results.isEmpty())
        {
            service.logger.log("no results at all (happens when there are no prototypes in the set)");
            return new Eval_results(null, the_feature_vector_to_be_identified,Eval_situation.nothing_found, true,"empty",new ArrayList<>());
        }

        Collections.sort(results,comp);

        double min_distance = results.get(0).distance();
        Embeddings_prototype winner = results.get(0).embeddings_prototype();
        if ( min_distance < 0.001)
        {
            service.logger.log("EXACT MATCH DETECTED 1 nearest "+winner.label()+ " at "+String.format("%.2f",min_distance));
            List<Eval_result_for_one_prototype> l = new ArrayList<>();
            l.add(new Eval_result_for_one_prototype(min_distance,winner));
            report_time(service, fv_time);
            return new Eval_results(winner.label(), winner.feature_vector(), Eval_situation.exact_match, false,winner.tag(),l);
        }

        double average_distance = 0;
        Map<String,Integer> votes = new HashMap<>();
        List<Eval_result_for_one_prototype> list_of_Eval_result_for_one_prototype = new ArrayList<>();
        int max = K_of_KNN;
        if (results.size() < max) max = results.size();
        for ( int i = 0 ; i < max; i++)
        {
            Eval_result_for_one_prototype res = results.get(i);
            service.logger.log("     d="+String.format("%.3f",res.distance())+ " "+ res.embeddings_prototype().tag());
            average_distance += res.distance();
            Embeddings_prototype ep = res.embeddings_prototype();
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
                    if (r.embeddings_prototype().label().equals(lab))
                    {
                        if (r.distance() < min )
                        {
                            min = r.distance();
                            win = r;
                        }
                    }
                }
            }

            report_time(service, fv_time);
            //list_of_Eval_result_for_one_prototype.clear();
            //list_of_Eval_result_for_one_prototype.add(win);
            return new Eval_results(win.embeddings_prototype().label(), the_feature_vector_to_be_identified, Eval_situation.ex_aequo,true,win.embeddings_prototype().tag(),list_of_Eval_result_for_one_prototype);
        }


        if ( winner != null) service.logger.log("1 nearest "+winner.label()+ " at "+String.format("%.2f",min_distance));
        service.logger.log(K_of_KNN+" nearest "+label5+ " average distance = "+String.format("%.2f",average_distance));


        report_time(service, fv_time);

        return new Eval_results(label5, the_feature_vector_to_be_identified,Eval_situation.normal,true,null,list_of_Eval_result_for_one_prototype);
    }


    //**********************************************************
    private static void report_time(Face_recognition_service service, long fv_time)
    //**********************************************************
    {
        long end = System.nanoTime();
        double x = (end-start);
        total_ns += x;
        count++;
        service.logger.log("\n==> evaluating one face took "
                +String.format("%.2f",x/1_000_000.0)
                +" milliseconds of which "+ String.format("%.2f",fv_time/1_000_000.0)
                +" was computing the feature vector ("+ String.format("%.2f",100.0*fv_time/x)
                +"%), the averages are "+String.format("%.2f",total_ns/(double) count/1_000_000.0)
                +" / "+String.format("%.2f",feature_vector_total_ns/(double) count/1_000_000.0));
    }


    //**********************************************************
    record Face_recognition_results(String label, Image image,
                                    Path image_path,
                                    Feature_vector feature_vector,
                                    Face_recognition_in_image_status face_recognition_in_image_status)
    //**********************************************************
    {
        public String to_string()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("label : ");
            sb.append(label);
            sb.append(" Face_recognition_in_image_status: ");
            sb.append(face_recognition_in_image_status);
            return sb.toString();
        }
    }

    //**********************************************************
    public static Face_recognition_results detect_and_recognize(Face_recognition_service service,Path tested, Face_detection_type face_detection_type, boolean display_face_reco_window, Aborter aborter, Window owner, Logger logger)
    //**********************************************************
    {
        Face_detector.Face_detection_result face_detection_result = Face_detector.detect_face(tested, face_detection_type, display_face_reco_window,logger);
        if (face_detection_result.status() == Face_recognition_in_image_status.server_not_reacheable)
        {
            logger.log("face detection server unreachable");
            if ( display_face_reco_window) Face_detector.warn_about_face_detector_server(owner,logger);
            return new Face_recognition_results(null, null, null,null, Face_recognition_in_image_status.server_not_reacheable);
        }
        if (face_detection_result.status() != Face_recognition_in_image_status.face_detected)
        {
            if ( display_face_reco_window) service.show_face_recognition_window(face_detection_result.image(),null,owner,aborter);
            return new Face_recognition_results(null,null, null,null, Face_recognition_in_image_status.no_face_detected);
        }
        Image image_face = face_detection_result.image();

        if (image_face == null)
        {
            if ( display_face_reco_window) Face_detector.warn_about_no_face_detected(owner,logger);
            else logger.log("no face detected");
            return new Face_recognition_results(null,null,null,null, Face_recognition_in_image_status.no_face_detected);
        }

        logger.log("Face detected");

        // write the image to disk, the tmp path will be passed to the embedding server
        Path face_reco_cache_folder = Static_files_and_paths_utilities.get_cache_dir(Cache_folder.klik_face_reco_cache,owner,logger);
        logger.log("face_reco_folder = "+face_reco_cache_folder);

        String file_name_base = "tmp_unknown_face_"+ UUID.randomUUID();
        Path tmp_path_to_face = Face_recognition_service.write_tmp_image(image_face, face_reco_cache_folder,file_name_base,logger);
        logger.log("tmp_path_to_face = "+tmp_path_to_face);

        Eval_results eval_result = eval_a_face(tmp_path_to_face,service, aborter);
        if (display_face_reco_window) service.show_face_recognition_window(image_face,eval_result, owner,aborter);

        String display_label = eval_result.label();
        if ( eval_result.label() == null)
        {
            display_label = "not recognized";
        }
        logger.log("face reco result = "+display_label);

        if ( eval_result.label() == null)
        {
            return new Face_recognition_results(null,image_face,tmp_path_to_face,eval_result.feature_vector(), Face_recognition_in_image_status.no_face_recognized);
        }
        else
        {
            if ( eval_result.eval_situation == Eval_situation.exact_match)
            {
                return new Face_recognition_results(eval_result.label(), image_face, tmp_path_to_face, eval_result.feature_vector(), Face_recognition_in_image_status.exact_match);
            }
            else
            {
                return new Face_recognition_results(eval_result.label(), image_face, tmp_path_to_face, eval_result.feature_vector(), Face_recognition_in_image_status.face_recognized);
            }
        }
    }

    //**********************************************************
    public static Face_recognition_results recognize_a_face(Path path_of_face, boolean display_face_reco_window, Aborter aborter, Face_recognition_service service)
    //**********************************************************
    {
        Eval_results eval_result = eval_a_face(path_of_face, service, aborter);
        Image face = Utils.get_image(path_of_face);
        if (face == null)
        {
            // TODO: these error messages are not accurate
            if ( display_face_reco_window) Face_detector.warn_about_no_face_detected(service.owner,service.logger);
            else service.logger.log("❌ fatal : cannot load face image");
            return new Face_recognition_results(null,null, path_of_face,null, Face_recognition_in_image_status.no_face_detected);
        }

        if (display_face_reco_window)
        {
            service.show_face_recognition_window(face,eval_result, service.owner,aborter);
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
            return new Face_recognition_results(null,face, path_of_face,null, Face_recognition_in_image_status.no_face_recognized);
        }
        else
        {
            if ( eval_result.eval_situation == Eval_situation.exact_match)
            {
                return new Face_recognition_results(eval_result.label(),face,path_of_face,eval_result.feature_vector(), Face_recognition_in_image_status.exact_match);
            }
            else
            {
                return new Face_recognition_results(eval_result.label(),face,path_of_face,eval_result.feature_vector(), Face_recognition_in_image_status.face_recognized);
            }
        }
    }




    //**********************************************************
    private boolean add_prototype_to_set(File f, String label, Face_recognition_results Face_recognition_results, Aborter aborter)
    //**********************************************************
    {
        boolean check_this_is_a_face = false;

        if (check_this_is_a_face)
        {
            //make a last check: but is this a face ????
            Face_detector.Face_detection_result face_detection_result = Face_detector.detect_face(Face_recognition_results.image_path(), Face_detection_type.haars_alt1, false,service.logger);

            if (face_detection_result.status() != Face_recognition_in_image_status.face_detected)
            {
                service.skipped();
                service.logger.log("NOT adding prototype as the final face check fails , for: "+Face_recognition_results.image_path());
                return false;
            }
            else
            {
                service.logger.log("✅ ADDING prototype as the final face check is OK , status is: "+Face_recognition_results.image_path());
            }
        }

        if ( service.label_to_prototype_count.get(label) > LIMIT_PER_LABEL)
        {
            service.skipped();
            service.logger.log("Face_recognition_actor, NOT storing "+f.getName()+" with label: "+label+ " as there are too many prototypes already "+service.label_to_prototype_count.get(label));
            return false;
        }
        else
        {
            service.logger.log("✅ STORING "+f.getAbsolutePath()+" as: "+name());
        }

        service.training_stats.face_wrongly_recognized_recorded.incrementAndGet();
        service.training_stats.done.incrementAndGet();

        Prototype_adder_actor actor = new Prototype_adder_actor(service);
        Prototype_adder_message msg = new Prototype_adder_message(label,Face_recognition_results.image(),Face_recognition_results.feature_vector() , aborter);
        Actor_engine.run(actor,msg,null, service.logger);
        return true;
    }

}
