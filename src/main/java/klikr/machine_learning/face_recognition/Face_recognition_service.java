// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Embeddings_prototype.java
//SOURCES ./Recognition_stats.java
//SOURCES ./Training_stats.java
//SOURCES ./Face_recognition_actor.java
//SOURCES ./Face_recognition_message.java
//SOURCES ./Prototype_adder_actor.java
//SOURCES ./Prototype_adder_message.java
//SOURCES ./Load_one_prototype_actor.java
//SOURCES ./Load_one_prototype_message.java
//SOURCES ./Light_embeddings_prototype.java
//SOURCES ./Utils.java



package klikr.machine_learning.face_recognition;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.Window_builder;
import klikr.Window_type;
import klikr.browser_core.Image_and_properties;
import klikr.browser_core.icons.image_properties_cache.Image_properties;
import klikr.browser_core.icons.image_properties_cache.Rotation;
import klikr.look.Look_and_feel_manager;
import klikr.machine_learning.ML_server_type;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.util.cache.Cache_folder;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job_termination_reporter;
//import klik.browser.icons.JavaFX_to_Swing;
import klikr.util.image.Static_image_utilities;
import klikr.machine_learning.feature_vector.Feature_vector;
import klikr.machine_learning.feature_vector.Feature_vector_double;
import klikr.util.files_and_paths.Extensions;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.File_logger;
import klikr.util.ui.Popups;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

//**********************************************************
public class Face_recognition_service
//**********************************************************
{
    public static final boolean dbg = false;

    public static final int K_of_KNN = 3;
    public static final int LIMIT_PER_LABEL = 3;

    public final static String EXTENSION_FOR_EP = "prototype";
    private static final int MAX_THREADS = 50;
    private static volatile Face_recognition_service instance = null;

    private final Logger logger;
    ConcurrentLinkedQueue<Embeddings_prototype> embeddings_prototypes = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<String> labels = new ConcurrentLinkedQueue<>();
    Map<String,Embeddings_prototype> tag_to_prototype = new ConcurrentHashMap<>();
    Map<String,Integer> label_to_prototype_count = new ConcurrentHashMap<>();
    public final String face_recognizer_name;
    public Path face_recognizer_path;
    Recognition_stats recognition_stats;
    Training_stats training_stats;
    long last_report;
    private final Window owner;
    Application application;
    private static Feature_vector_source feature_vector_source;
    private final Face_recognition_actor face_recognition_actor;
    //**********************************************************
    private Face_recognition_service(Application application, String name, Window owner, Logger logger_)
    //**********************************************************
    {
        this.application = application;
        face_recognizer_name = name;
        this.owner = owner;
        this.logger = new File_logger("facereco");//logger;
        Path face_reco_folder = Static_files_and_paths_utilities.get_face_reco_folder(owner,logger);
        face_recognizer_path = Path.of(face_reco_folder.toAbsolutePath().toString(),face_recognizer_name);
        Window_builder.additional_no_past(application,Window_type.File_system_2D,new Path_list_provider_for_file_system(face_recognizer_path,owner,logger_),owner,logger_);

        last_report = System.currentTimeMillis();
        recognition_stats = new Recognition_stats();
        training_stats = new Training_stats();
        feature_vector_source = new Feature_vector_source_for_face_recognition(owner,logger);

        face_recognition_actor = new Face_recognition_actor(this,logger);

    }
    


    //**********************************************************
    public static Face_recognition_service get_instance(Application application,Window owner,Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Face_recognition_service.class)
            {
                if (instance == null)
                {
                    start_new(application,owner,logger);
                }
            }
        }
        return instance;
    }

    //**********************************************************
    public static void start_new(Application application, Window owner, Logger logger)
    //**********************************************************
    {
        Optional<String> localo = get_Face_recognition_model_name(owner,logger);
        if ( localo.isEmpty()) return;
        instance = new Face_recognition_service(application,localo.get(), owner,logger);
        instance.load_internal();
    }

    //**********************************************************
    public static void save()
    //**********************************************************
    {
        if ( instance != null) instance.save_internal();
    }

    //**********************************************************
    public static void load(Application application,Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance != null) instance.load_internal();
        else start_new(application,owner,logger);
    }

    //**********************************************************
    Path detect_face_and_recognize(File file, ML_server_type face_detection_type, String label, boolean display_face_reco_window, Aborter aborter)
    //**********************************************************
    {
        //logger.log("process_file FILE before: "+file.getAbsolutePath());
        Face_recognition_results face_recognition_results = detect_and_recognize(
                file.toPath(),
                face_detection_type,
                display_face_reco_window,
                aborter);
        //logger.log("process_file FILE after : "+file.getAbsolutePath()+ " "+ Face_recognition_results.status);
        switch (face_recognition_results.face_recognition_in_image_status())
        {
            case server_not_reacheable:
                logger.log("detect_face_and_recognize:server_not_reacheable");
                server_error();

                break;
            case  error:
                logger.log("detect_face_and_recognize:error");
                server_error();
                break;
            case no_face_detected:
                // happens a lot
                logger.log("detect_face_and_recognize:no_face_detected for: "+file);
                no_face_detected();
                break;
            case face_detected:
                logger.log("detect_face_and_recognize:should not happen 1");
                should_not_happen();
                break;
            case no_feature_vector:
                logger.log("detect_face_and_recognize:should not happen 2");
                should_not_happen();
                break;
            case feature_vector_ready:
                logger.log("detect_face_and_recognize:should not happen 3");
                should_not_happen();
                break;
            case exact_match, face_recognized :
                logger.log("detect_face_and_recognize:face_recognized");
                recognition_stats.face_recognized.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // no training
                    break;
                }
                if ( label.equals(face_recognition_results.label()))
                {
                    training_stats.face_correctly_recognized_not_recorded.incrementAndGet();
                    training_stats.done.incrementAndGet();
                    logger.log("label was correct, skipping "+file.getName() );
                    break;
                }
                logger.log("ADDING "+file.getName()+" as label was NOT correct: "+ face_recognition_results.label());

                add_prototype_to_set(file,label,face_recognition_results,aborter);
                break;
            case no_face_recognized :
                logger.log("detect_face_and_recognize: NO face_recognized");
                recognition_stats.face_not_recognized.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // this is a "pure" recognition task
                    //logger.log(Stack_trace_getter.get_stack_trace("process_file: NO face_recognized, nolabel ?????????"));
                    // no training
                    break;
                }
                training_stats.done.incrementAndGet();
                add_prototype_to_set(file,label,face_recognition_results,aborter);
                break;


            default:
                logger.log(Stack_trace_getter.get_stack_trace("detect_face_and_recognize: should not happen"));
                return null;

        }
        return face_recognition_results.image_path();
    }

    //**********************************************************
    Path just_recognize(File file, String label, boolean display_face_reco_window, Aborter aborter)
    //**********************************************************
    {
        //logger.log("process_file FILE before: "+file.getAbsolutePath());
        Face_recognition_results Face_recognition_results = just_recognize_a_face(file.toPath(), display_face_reco_window,aborter);
        //logger.log("process_file FILE after : "+file.getAbsolutePath()+ " "+ Face_recognition_results.status);
        switch (Face_recognition_results.face_recognition_in_image_status())
        {
            case server_not_reacheable:
                logger.log("just_recognize:server_not_reacheable");
                recognition_stats.error.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.error.incrementAndGet();
                training_stats.done.incrementAndGet();
                break;
            case  error:
                logger.log("just_recognize:error");
                recognition_stats.error.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.error.incrementAndGet();
                training_stats.done.incrementAndGet();
                break;
            case no_face_detected:
                // happens a lot
                logger.log("just_recognize:no_face_detected "+file);
                recognition_stats.done.incrementAndGet();
                recognition_stats.no_face_detected.incrementAndGet();
                training_stats.no_face_detected.incrementAndGet();
                training_stats.done.incrementAndGet();
                break;
            case face_detected:
                logger.log("just_recognize:should not happen 1");
                recognition_stats.error.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.error.incrementAndGet();
                training_stats.done.incrementAndGet();
                break;
            case no_feature_vector:
                logger.log("just_recognize:should not happen 2");
                recognition_stats.error.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.error.incrementAndGet();
                training_stats.done.incrementAndGet();
                break;
            case feature_vector_ready:
                logger.log("just_recognize:should not happen 3");
                recognition_stats.error.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.error.incrementAndGet();
                training_stats.done.incrementAndGet();
                break;
            case exact_match:
                logger.log("just_recognize:exact_match");
                recognition_stats.face_recognized.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                // no training
                break;
            case face_recognized :
                logger.log("just_recognize:face_recognized");
                recognition_stats.face_recognized.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // no training
                    break;
                }
                if ( label.equals(Face_recognition_results.label()))
                {
                    training_stats.face_correctly_recognized_not_recorded.incrementAndGet();
                    training_stats.done.incrementAndGet();
                    logger.log("label was correct, skipping "+file.getName() );
                    break;
                }
                logger.log("ADDING "+file.getName()+" as label was NOT correct: "+ Face_recognition_results.label());
                add_prototype_to_set(file,label,Face_recognition_results,aborter);
                break;
            case no_face_recognized :
                logger.log("just_recognize: NO face_recognized");
                recognition_stats.face_not_recognized.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                if ( label == null)
                {
                    // this is a "pure" recognition task
                    //logger.log(Stack_trace_getter.get_stack_trace("process_file: NO face_recognized, nolabel ?????????"));
                    // no training
                    break;
                }
                training_stats.face_wrongly_recognized_recorded.incrementAndGet();
                training_stats.done.incrementAndGet();
                add_prototype_to_set(file,label,Face_recognition_results,aborter);
                break;
            default:
                logger.log(Stack_trace_getter.get_stack_trace("just_recognize: should not happen"));
                break;

        }
        return Face_recognition_results.image_path();
    }
    //**********************************************************
    private boolean add_prototype_to_set(File f, String label, Face_recognition_results Face_recognition_results, Aborter aborter)
    //**********************************************************
    {
        boolean check_this_is_a_face = false;

        if (check_this_is_a_face)
        {
            //make a last check: but is this a face ????
            Face_detector.Face_detection_result face_detection_result = Face_detector.detect_face(Face_recognition_results.image_path(), ML_server_type.Haar_alt1, owner, logger);

            if (face_detection_result.status() != Face_recognition_in_image_status.face_detected)
            {
                skipped();
                logger.log("NOT adding prototype as the final face check fails , for: "+Face_recognition_results.image_path());
                return false;
            }
            else
            {
                logger.log("✅ ADDING prototype as the final face check is OK , status is: "+Face_recognition_results.image_path());
            }
        }

        if ( label_to_prototype_count.get(label) > LIMIT_PER_LABEL)
        {
            skipped();
            logger.log("Face_recognition_actor, NOT storing "+f.getName()+" with label: "+label+ " as there are too many prototypes already "+label_to_prototype_count.get(label));
            return false;
        }
        else
        {
            logger.log("✅ STORING face recognition prototype "+f.getAbsolutePath());
        }

        training_stats.face_wrongly_recognized_recorded.incrementAndGet();
        training_stats.done.incrementAndGet();

        Prototype_adder_actor actor = new Prototype_adder_actor(this,logger);
        Prototype_adder_message msg = new Prototype_adder_message(label,Face_recognition_results.image(),Face_recognition_results.feature_vector() , aborter);
        Actor_engine.run(actor,msg,null, logger);
        return true;
    }


    //**********************************************************
    private static Optional<String> get_Face_recognition_model_name(Window owner, Logger logger)
    //**********************************************************
    {

        Path p = Static_files_and_paths_utilities.get_face_reco_folder(owner,logger);
        File[] files = p.toFile().listFiles();

        ChoiceDialog<String> cd = new ChoiceDialog<>("Select face recognition model");
        cd.initOwner(owner);
        Look_and_feel_manager.set_dialog_look(cd,owner,logger);
        ObservableList<String> list = cd.getItems();
        for ( File f : files)
        {
            list.add(f.getName());
        }
        String new_model = "new model";
        list.add(new_model);
        cd.setTitle("Select face recognition model");
        Optional<String> x = cd.showAndWait();
        if ( x.get().equals(new_model))
        {
            TextInputDialog dialog = new TextInputDialog();
            Look_and_feel_manager.set_dialog_look(dialog,owner,logger);

            dialog.setTitle("Give recognition system tag");
            dialog.setHeaderText("Give recognition system tag");
            return dialog.showAndWait();
        }
        return x;


    }

    //**********************************************************
    public static void auto(Application application,Path displayed_folder_path, Window owner,Logger logger)
    //**********************************************************
    {
        Face_recognition_service fr = Face_recognition_service.get_instance(application,owner,logger);
        Actor_engine.execute(() -> fr.auto_internal(displayed_folder_path), "face recognition auto",fr.logger);
    }


    //**********************************************************
    public static void do_folder(Application application,Path folder, Window owner, Logger logger)
    //**********************************************************
    {
        Face_recognition_service fr = Face_recognition_service.get_instance(application,owner,logger);
        Actor_engine.execute(() -> fr.do_folder_internal(folder), "face recognition do 1 folder",fr.logger);
    }

    //**********************************************************
    public static void recognize(Face_recognition_message msg)
    //**********************************************************
    {
        Actor_engine.run(instance.face_recognition_actor, msg,null,instance.logger);
    }

    //**********************************************************
    private void auto_internal(Path displayed_folder_path)
    //**********************************************************
    {
        logger.log("AUTO STARTED");
        AtomicInteger files_in_flight = new AtomicInteger();
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Aborter aborter = new Aborter("face_recog_auto", logger);
        Optional<Hourglass> hourglass = Progress_window.show_with_in_flight_and_aborter(
                files_in_flight,
                aborter,
                "Wait for auto train to complete",
                3600*60,
                x,
                y,
                owner,
                logger);


        last_report = System.currentTimeMillis();
        recognition_stats = new Recognition_stats();
        training_stats = new Training_stats();
        Path target = displayed_folder_path;
        File check = new File (target.toFile(),".folder_name_is_recognition_label");
        if ( !check.exists())
        {
            Platform.runLater(()->
            {
                boolean reply = Popups.popup_ask_for_confirmation(
                        "❗ Folder does not contain a file named '.folder_name_is_recognition_label'",
                        "Do you want to create this file?",
                        owner, logger);
                if (reply) {
                    try {
                        Files.createDirectories(check.toPath().getParent());
                        Files.createFile(check.toPath());
                    } catch (IOException e) {
                        logger.log("❌ auto_internal FATAL cannot create file '.folder_name_is_recognition_label' in folder " + target);
                        return;
                    }
                }
                logger.log("auto_internal skipping1 " + target + " as it does not contain a file named '.folder_name_is_recognition_label'");
            });
            return;
        }
        logger.log("doing AUTO on: "+target);

        File files[] = target.toFile().listFiles();
        List<File> folders = new ArrayList<>();
        for ( File f : files)
        {
            if (f.isDirectory())
            {
                folders.add(f);
            }
        }
        Collections.sort(folders);
        int i = 0;
        for ( File f : folders)
        {
            if ( aborter.should_abort()) return;

            String label = f.getName();
            double percent = 100.0*(double)i/(double)folders.size();
            String done =  String.format("%.1f",percent);
            if ( hourglass.isPresent() ) {
                Progress_window progress_window = ((Progress_window)hourglass.get());
                progress_window.set_text(label + ", " + done + "% of total");
            }
            i++;
            Integer N = label_to_prototype_count.get(label);
            if ( N == null)
            {
                N = Integer.valueOf(0);
                label_to_prototype_count.put(label,N);
            }
            else
            {
                if (N > LIMIT_PER_LABEL)
                {
                    skipped();
                    logger.log("Face_recognition_service, NOT scheduling "+f.getName()+" with label: "+label+ " as there are too many prototypes already "+ label_to_prototype_count.get(label));
                    continue;
                }
            }

            for(;;)
            {
                int in_flight = Actor_engine.how_many_threads_are_in_flight(logger);
                if (in_flight < MAX_THREADS) break;
                {
                    try {
                        logger.log("\n\nAUTO going to sleep :1s, too many threads");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.log("" + e);
                        return;
                    }
                }
            }

            LongAdder label_in_flight = new LongAdder();
            auto_folder_for_one_label(f,label, aborter, files_in_flight, label_in_flight);
        }

        // DONT save_internal(aborter_for_auto_train);

        //running_film.report_progress_and_close_when_finished(files_in_flight);
        logger.log("Finished Face Recognition AUTO: "+recognition_stats.to_string());
    }


    //**********************************************************
    private boolean auto_folder_for_one_label(File dir, String label,
                                              Aborter aborter_may_be_null,
                                              AtomicInteger files_in_flight,
                                              LongAdder label_in_flight)
    //**********************************************************
    {
        for(;;)
        {
            int N = Actor_engine.how_many_threads_are_in_flight(logger);
            if (N < MAX_THREADS) break;
            {
                try {
                    logger.log("\n\nAUTO going to sleep : 1s, too many threads");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.log("" + e);
                    return false;
                }
            }
        }

        logger.log("auto_folder: "+dir);
        Job_termination_reporter tr = (message, job) -> {
            files_in_flight.decrementAndGet();
            label_in_flight.decrement();
            long now = System.currentTimeMillis();
            if (now-last_report> 10000)
            {
                last_report = now;
                logger.log("\n\n\n\n\n");
                logger.log("Recognition:"+recognition_stats.to_string());
                logger.log("Training:"+training_stats.to_string());
            }
        };
        logger.log("auto train folder: "+dir.getAbsolutePath()+ "files in flight: "+files_in_flight.doubleValue());
        File files[] = dir.listFiles();
        if ( files == null) return true;
        if ( files.length == 0) return true;
        for ( File f: files)
        {
            if ( aborter_may_be_null != null)
            {
                if ( aborter_may_be_null.should_abort())
                {
                    logger.log("auto aborted");
                    return false;
                }
            }


            if ( f.isDirectory())
            {
                if ( !auto_folder_for_one_label(f,label, aborter_may_be_null, files_in_flight,label_in_flight))
                {
                    logger.log("auto_folder returns false, aborting folder "+dir);
                    return false;
                }
            }
            if (Guess_file_type.is_this_file_extension_an_image(f,owner, logger))
            {
                label_in_flight.increment();
                Aborter local_never_null = aborter_may_be_null;
                if ( local_never_null == null) local_never_null = new Aborter("dummy",logger);
                Face_recognition_message msg = new Face_recognition_message(f, ML_server_type.MTCNN, label, false, local_never_null, files_in_flight);
                Actor_engine.run(face_recognition_actor, msg, tr, logger);
           }
        }
        logger.log("Folder done: "+dir.getAbsolutePath());
        return true;
    }





    //**********************************************************
    public List<String> get_prototype_labels()
    //**********************************************************
    {
        List<String> returned = new ArrayList<>(labels);
        Collections.sort(returned);
        return returned;
    }


    //**********************************************************
    public void show_face_recognition_window(Image face, Eval_results eval_result, ML_server_type ml_server_type)
    //**********************************************************
    {

        int size = 1600/K_of_KNN;
        if ( size > 200) size = 200;
        if (Platform.isFxApplicationThread())
        {
            logger.log("HAPPENS1 show_face_recognition_window");
            show_Face_recognition_window_internal(application,size,face,eval_result,ml_server_type,owner);
        }
        else {
            int size2 = size;
            Jfx_batch_injector.inject(()->show_Face_recognition_window_internal(application,size2,face,eval_result,ml_server_type,owner),logger);
        }
    }

    private static double x = 10;
    private static double y = 10;

    //**********************************************************
    public void show_Face_recognition_window_internal(
            Application application,
            int size,
            Image face_image,
            Eval_results eval_result,
            ML_server_type ml_server_type,
            Window owner)
    //**********************************************************
    {
        Stage stage = new Stage();

        stage.initOwner(owner);
        Label status_label = new Label();
        Look_and_feel_manager.set_label_look(status_label,stage,logger);

        stage.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent key_event)
            {
                if (key_event.getCode() == KeyCode.ESCAPE)
                {
                    key_event.consume();
                    stage.close();
                }
            }
        });

        if ( eval_result != null)
        {
            stage.setTitle("Recognized as: "+eval_result.label());
        }
        else
        {
            stage.setTitle("Not Recognized");
        }
        VBox vBox = new VBox();

        {
            if ( face_image != null)
            {
                String txt = "Extracted face looks like this: ";
                if (ml_server_type!= null ) txt += " Detector: "+ml_server_type.name();
                Label l= new Label(txt);
                Look_and_feel_manager.set_label_look(l,stage,logger);
                vBox.getChildren().add(l);
                ImageView iv = new ImageView();
                iv.setImage(face_image);
                iv.setPreserveRatio(true);
                iv.setFitWidth(size);
                Pane image_pane = new StackPane(iv);
                Look_and_feel_manager.set_region_look(image_pane,stage,logger);
                vBox.getChildren().add(image_pane);
            }
            else
            {
                stage.setTitle("Face Detection failed for detector "+ml_server_type.name());
                stage.setMinWidth(400);
                stage.setMinHeight(400);
                status_label.setText("Face Detection failed for detector "+ml_server_type.name());
            }
        }
        if ( face_image !=null)
        {
            if (eval_result != null) {
                logger.log("eval results SIZE="+eval_result.list().size());
                if (!eval_result.list().isEmpty())
                {
                    Label l = new Label("Closests prototypes found: ");
                    Look_and_feel_manager.set_label_look(l,stage,logger);
                    vBox.getChildren().add(l);
                    HBox hBox = new HBox();
                    Border border = new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID,new CornerRadii(1),new BorderWidths(0.5)));

                    for (Eval_result_for_one_prototype res : eval_result.list())
                    {
                        VBox vb = new VBox();
                        vb.setBorder(border);

                        {
                            Label lab = new Label("At: "+String.format("%.3f",res.distance()));
                            Look_and_feel_manager.set_label_look(lab,stage,logger);
                            lab.setMaxWidth(size);
                            lab.setWrapText(true);
                            vb.getChildren().add(lab);
                        }
                        {
                            Image image = res.embeddings_prototype().face_image(face_recognizer_path,logger);
                            ImageView iv = new ImageView(image);
                            iv.setPreserveRatio(true);
                            iv.setFitWidth(size);
                            Pane image_pane = new StackPane(iv);
                            Look_and_feel_manager.set_region_look(image_pane,stage,logger);

                            vb.getChildren().add(image_pane);
                        }
                        {
                            Label lab = new Label(res.embeddings_prototype().label());
                            Look_and_feel_manager.set_label_look(lab,stage,logger);
                            lab.setMaxWidth(size);
                            lab.setWrapText(true);
                            vb.getChildren().add(lab);
                        }

                        hBox.getChildren().add(vb);
                    }
                    vBox.getChildren().add(hBox);

                }
            }
            TextField textField = new TextField();
            Look_and_feel_manager.set_TextField_look(textField,false,stage,logger);

            if (eval_result != null) textField.setDisable(!eval_result.adding());

            {
                HBox hBox = new HBox();
                Label label5 = new Label("Enter the recognition label from list:");
                Look_and_feel_manager.set_label_look(label5,stage,logger);
                hBox.getChildren().add(label5);
                vBox.getChildren().add(hBox);
            }
            {
                HBox hBox = new HBox();
                ComboBox<String> comboBox = new ComboBox<>();
                Look_and_feel_manager.set_region_look(comboBox,stage,logger);
                if (eval_result != null) comboBox.setDisable(!eval_result.adding());

                comboBox.getItems().addAll(Face_recognition_service.get_instance(application,stage,logger).get_prototype_labels());
                if (eval_result != null) {
                    if (eval_result.label() != null) {
                        comboBox.setValue(eval_result.label());
                        textField.setText(eval_result.label());
                    }
                }
                comboBox.onActionProperty().set(e -> {
                    String selected = comboBox.getSelectionModel().getSelectedItem();
                    textField.setText(selected);
                });
                hBox.getChildren().add(comboBox);

                Label label2 = new Label("Or introduce a new label:");
                Look_and_feel_manager.set_label_look(label2,stage,logger);

                hBox.getChildren().add(label2);

                hBox.getChildren().add(textField);
                vBox.getChildren().add(hBox);
            }
            if (eval_result != null) {
                if (!eval_result.adding())
                {
                    if (face_image != null)
                    {
                        if ( eval_result.label() != null)
                        {
                            stage.setTitle("Exact match! " + eval_result.label());
                            status_label.setText("prototype was recognized at distance zero, no need to add it ");
                        }
                        else
                        {
                            stage.setTitle("Not recognized");
                            status_label.setText("prototype was not recognized ");
                        }
                    }
                }
            }

            {
                HBox hBox = new HBox();
                Button add = new Button("Add to training set");
                Look_and_feel_manager.set_button_look(add,true,stage,logger);

                if (eval_result != null) add.setDisable(!eval_result.adding());
                add.setOnAction(e -> {
                    String image_label = textField.getText();

                    if (image_label.trim().isEmpty()) {
                        status_label.setText("Error: no label!");
                        return;
                    }
                    Prototype_adder_actor actor = new Prototype_adder_actor(this,logger);
                    Feature_vector fv = eval_result.feature_vector();
                    Prototype_adder_message msg = new Prototype_adder_message(image_label.trim(), face_image, fv,new Aborter("bidon", logger));
                    Job_termination_reporter tr = (message, job) -> {
                        Face_recognition_in_image_status s = Face_recognition_in_image_status.valueOf(message);
                        if (s != Face_recognition_in_image_status.feature_vector_ready) {
                            Jfx_batch_injector.inject(() -> status_label.setText("prototype fabrication error " + s), logger);
                        } else {
                            //save_internal();
                            Jfx_batch_injector.inject(() -> stage.close(), logger);
                        }
                    };
                    Actor_engine.run(actor, msg, tr, logger);
                });
                hBox.getChildren().add(add);
                vBox.getChildren().add(hBox);
            }
            {
                HBox hBox = new HBox();
                Button skip = new Button("Skip this face, do not add it to the training set");
                Look_and_feel_manager.set_button_look(skip,true,stage,logger);
                skip.setOnAction(e -> {
                    stage.close();
                });
                hBox.getChildren().add(skip);
                vBox.getChildren().add(hBox);
            }
            if (eval_result != null) {
                HBox hBox = new HBox();
                Button remove = new Button("REMOVE this face from the training set (bad face or wrong label)");
                Look_and_feel_manager.set_button_look(remove,true,stage,logger);
                remove.setDisable(!eval_result.adding());
                remove.setOnAction(e -> {

                    Embeddings_prototype guilty = tag_to_prototype.get(eval_result.tag());
                    embeddings_prototypes.remove(guilty);
                    tag_to_prototype.remove(eval_result.tag());
                    try {
                        Path p = Embeddings_prototype.make_image_path(face_recognizer_path, eval_result.tag(), logger);
                        Files.delete(p);
                        logger.log("deleted: " + p);
                    } catch (IOException ex) {
                        logger.log(Stack_trace_getter.get_stack_trace("" + e));
                    }
                    try {
                        Path p = Embeddings_prototype.make_prototype_path(face_recognizer_path, eval_result.tag());
                        Files.delete(p);
                        logger.log("deleted: " + p);
                    } catch (IOException ex) {
                        logger.log(Stack_trace_getter.get_stack_trace("" + e));
                    }
                    //save_internal();
                    stage.close();
                });
                hBox.getChildren().add(remove);
                vBox.getChildren().add(hBox);
            }
        }
        {
            HBox hBox = new HBox();
            hBox.getChildren().add(status_label);
            vBox.getChildren().add(hBox);
        }
        Scene scene = new Scene(vBox);
        Look_and_feel_manager.set_scene_look(scene,stage,logger);

        stage.setScene(scene);
        stage.show();
        stage.setX(x);
        stage.setY(y);
        stage.setAlwaysOnTop(true);
        x += 20;
        if ( x > 200)
        {
            x = 10;
        }
        y += 20;
        if ( y > 200)
        {
            y = 10;
        }
    }


    //**********************************************************
    private void save_internal()
    //**********************************************************
    {
        logger.log("save_internal : saving "+embeddings_prototypes.size()+ " prototypes");
        for (Embeddings_prototype ep : embeddings_prototypes)
        {
            Actor_engine.execute(()->save_ep(ep),"Save face recognition prototypes",logger);
        }
    }
    //**********************************************************
    private void load_internal()
    //**********************************************************
    {
        logger.log("load_internal : loading prototypes");
        AtomicInteger in_flight = new AtomicInteger();
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Aborter aborter = new Aborter("face recognition load",logger);
        Optional<Hourglass> hourglass = Progress_window.show_with_in_flight_and_aborter(
                in_flight,
                aborter,
                "Loading face recognition prototypes",
                3600*60,
                x,
                y,
                owner,
                logger);
        Load_one_prototype_actor actor = new Load_one_prototype_actor();
        Runnable r = () -> {
            Path p = Path.of(face_recognizer_path.toAbsolutePath().toString());
            File[] files = p.toFile().listFiles();
            if ( files == null)
            {
                try {
                    logger.log("going to create folder: "+p.toAbsolutePath());
                    Files.createDirectory(p);
                } catch (IOException e) {
                    logger.log("cannot create folder: "+p.toAbsolutePath());
                }
                return;
            }

            for (File f: files)
            {
                if ( aborter.should_abort()) return;
                if ( f.isDirectory()) continue;
                in_flight.incrementAndGet();
                Job_termination_reporter tr = (message, job) -> in_flight.decrementAndGet();

                Actor_engine.run(actor,
                        new Load_one_prototype_message(f,this,aborter),
                        tr,
                        logger);
            }
        };
        Actor_engine.execute(r,"Load face recognition prototypes",logger);

        hourglass.ifPresent(Hourglass::close);
    }

    //**********************************************************
    public void load_one_prototype(File f)
    //**********************************************************
    {
        String ext = Extensions.get_extension(f.getName());
        if ( !ext.equals(EXTENSION_FOR_EP)) return;
        String tag = Extensions.get_base_name(f.getName());

        Embeddings_prototype ep = load_ep(f,tag);
        if ( ep == null)
        {
            //logger.log("loading failed for "+ f.getAbsolutePath());
            return;
        }
        String label = ep.label();
        embeddings_prototypes.add(ep);
        if ( !labels.contains(label)) labels.add(label);
        Integer x = label_to_prototype_count.get(tag);
        if ( x == null) x = Integer.valueOf(1);
        else x++;
        label_to_prototype_count.put(tag,x);
        tag_to_prototype.put(tag,ep);

    }

    //**********************************************************
    private Embeddings_prototype load_ep(File f, String tag)
    //**********************************************************
    {
        Image local_face = Embeddings_prototype.is_image_present(face_recognizer_path,tag,logger);
        if ( local_face == null)
        {
            // no image, remove the prototype
           delete_prototype(f);
            return null;
        }


        try (BufferedReader reader = new BufferedReader(new FileReader(f)))
        {
            boolean ok = true;
            String label = null;

            {
                String line =  reader.readLine();
                if ( line == null)
                {
                    logger.log("error reading ep label");
                    ok = false;
                }
                else
                {
                    label = line.trim();
                }
            }

            int size = 0;
            if (ok)
            {
                {
                    String line = reader.readLine();
                    if (line == null)
                    {
                        logger.log("error reading ep fv length");
                        ok = false;
                    }
                    else
                    {
                        size = Integer.valueOf(line.trim());
                    }
                }
            }
            double[] values = null;
            if (ok)
            {
                values = new double[size];
                for (int i = 0; i < size; i++)
                {

                    String line = reader.readLine();
                    if (line == null)
                    {
                        logger.log("error reading fv: missing component #" + i + ", length of fv was " + size + " for: " + f);
                        ok = false;
                        break;
                    }
                    try
                    {
                        values[i] = Double.valueOf(line.trim());
                    }
                    catch (NumberFormatException e)
                    {
                        logger.log(Stack_trace_getter.get_stack_trace(f+"   =>  " + e));
                        ok = false;
                        break;
                    }
                }
            }
            if ( ok)
            {
                Feature_vector fv = new Feature_vector_double(values,"face_reco");
                return new Light_embeddings_prototype(fv,label,tag);
                //return new Heavy_embeddings_prototype(face, fv, label, tag);
            }
        } catch (FileNotFoundException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        // the prototype file is corrupted, let us remove it and the image too
        delete_prototype(f);
        delete_prototype(Embeddings_prototype.make_image_path(face_recognizer_path,tag,logger).toFile());
        return null;
    }



    //**********************************************************
    public Face_recognition_results just_recognize_a_face(Path path_of_face, boolean display_face_reco_window, Aborter aborter)
    //**********************************************************
    {
        Eval_results eval_result = eval_a_face(path_of_face, aborter);
        Image face = Utils.get_image(path_of_face);
        if (face == null)
        {
            // TODO: these error messages are not accurate
            if ( display_face_reco_window) Face_detector.warn_about_no_face_detected(owner,logger);
            else logger.log("❌ fatal : cannot load face image");
            return new Face_recognition_results(null,null, path_of_face,null, Face_recognition_in_image_status.no_face_detected);
        }

        if (display_face_reco_window)
        {
            show_face_recognition_window(face,eval_result,ML_server_type.FaceNet);
        }

        String display_label = eval_result.label();
        if ( eval_result.label() == null)
        {
            display_label = "not recognized";
        }
        logger.log("face reco result = "+display_label);
        //display(face, display_label);
        if ( eval_result.label() == null)
        {
            return new Face_recognition_results(null,face, path_of_face,null, Face_recognition_in_image_status.no_face_recognized);
        }
        else
        {
            if ( eval_result.eval_situation() == Eval_situation.exact_match)
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
    private void delete_prototype(File f)
    //**********************************************************
    {
        try {
            logger.log("deleting corrupted prototype: "+f);
            Files.delete(f.toPath());
        } catch (IOException ex) {
            logger.log(Stack_trace_getter.get_stack_trace("" + ex));
        }
    }

    //**********************************************************
    private String load_label_from_ep_file(File f)
    //**********************************************************
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(f)))
        {
            String line =  reader.readLine();
            if ( line == null)
            {
                logger.log("error reading ep label");
                return null;
            }
            String label = line.trim();
            return label;

        } catch (FileNotFoundException e) {
            logger.log(""+e);
        } catch (IOException e) {
            logger.log(""+e);
        }
        return null;
    }





    //**********************************************************
    private void save_ep(Embeddings_prototype prototype)
    //**********************************************************
    {
        prototype.save(face_recognizer_path,logger);

    }


    //**********************************************************
    public static Path write_tmp_image(Image face, Path folder_path, String tag, Logger logger)
    //**********************************************************
    {
        Path path =  Embeddings_prototype.make_image_path(folder_path,tag,logger);
        logger.log("writing tmp image to: "+path+" face="+face.getWidth()+"x"+face.getHeight());

        Image_and_properties iap = Image_and_properties.build(face,false);
        Static_image_utilities.write_png_to_disk(iap, path, logger);

        return path;
    }




    //**********************************************************
    private void do_folder_internal(Path target)
    //**********************************************************
    {
        AtomicInteger files_in_flight = new AtomicInteger(0);
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Aborter aborter =  new Aborter("face recog do_folder",logger);
        Optional<Hourglass> hourglass = Progress_window.show_with_in_flight_and_aborter(
                files_in_flight,
                aborter,
                "Wait for SELF face recognition to complete",
                3600*60,
                x,
                y,
                owner,
                logger);

        last_report = System.currentTimeMillis();
        recognition_stats = new Recognition_stats();
        //Path target = face_recognizer_path;
        logger.log("doing SELF on: "+target);

        do_folder(target,null,aborter,recognition_stats);



        //running_film.report_progress_and_close_when_finished(files_in_flight);
        logger.log("Finished Face Recognition "+recognition_stats.to_string());
    }

    //**********************************************************
    private void do_folder(Path target, String tag, Aborter aborter, Recognition_stats recognition_stats)
    //**********************************************************
    {
        File files[] = target.toFile().listFiles();
        for ( File f : files)
        {
            logger.log("do_folder doing file= "+f.getAbsolutePath());
            if ( f.isDirectory())
            {
                logger.log("self_internal diving in folder "+f.getAbsolutePath());
                do_folder(f.toPath(), f.getName(),aborter, recognition_stats);
                continue;
            }
            if ( aborter.should_abort()) return;
            //if ( ! Extensions.get_extension(f.getName()).equals(EXTENSION_FOR_EP)) continue;
            int N = Actor_engine.how_many_threads_are_in_flight(logger);
            if ( N > MAX_THREADS)
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.log(""+e);
                    return;
                }

            }

            do_one_file(f,tag,aborter, recognition_stats);
        }
    }

    //**********************************************************
    private void do_one_file(File file, String self_target_tag, Aborter aborter, Recognition_stats recognition_stats)
    //**********************************************************
    {
        logger.log("Face recognition test on " + file.getName());

        Face_recognition_results face_recognition_results = null;
        boolean face_detected_at_least_by_one_method =  false;
        for( ML_server_type face_detection_type : ML_server_type.face_detectors())
        {
            face_recognition_results =
                detect_and_recognize(
                file.toPath(),
                face_detection_type,
                false,
                        aborter);

            logger.log("face detection results: "+face_recognition_results.to_string()+"\n\n\n");


            if ( face_recognition_results.face_recognition_in_image_status() == Face_recognition_in_image_status.no_face_detected)
            {
                logger.log("No face detected with "+face_detection_type);
                continue;
            }
            face_detected_at_least_by_one_method = true;

            if ( face_recognition_results.label().equals(self_target_tag))
            {
                // FULL SUCCESS
                logger.log("FULL SUCCESS: " + self_target_tag + " CORRECTLY recognized as " + face_recognition_results.label());
                recognition_stats.done.incrementAndGet();
                recognition_stats.face_recognized.incrementAndGet();
                return;
            }
            logger.log(face_detection_type+ " face detected, but recognition failed: " + self_target_tag + " recognition error, recognized as " + face_recognition_results.label() + " for file :" + file.getAbsolutePath());
        }
        recognition_stats.done.incrementAndGet();
        if ( face_detected_at_least_by_one_method)
        {
            recognition_stats.face_not_recognized.incrementAndGet();
        }
        else
        {
            recognition_stats.no_face_detected.incrementAndGet();
        }
        logger.log(
                "face detection result:"+face_detected_at_least_by_one_method+
                "finally failed: " + self_target_tag + " not recognized for file :" + file.getAbsolutePath());
        if ( face_recognition_results.image() == null)
        {
            logger.log("face recognition results has no image");
            return;
        }
        Utils.display(
                200,
                face_recognition_results.image(),
                null,
                null,
                self_target_tag + " NOT recognized",
                "recognized as: " + face_recognition_results.label(),
                logger);
    }


    //**********************************************************
    public Face_recognition_results detect_and_recognize(Path tested, ML_server_type face_detection_type, boolean display_face_reco_window, Aborter aborter)
    //**********************************************************
    {
        Face_detector.Face_detection_result face_detection_result = Face_detector.detect_face(tested, face_detection_type, owner,logger);
        if (face_detection_result.status() == Face_recognition_in_image_status.server_not_reacheable)
        {
            logger.log("face detection server unreachable");
            if ( display_face_reco_window) Face_detector.warn_about_face_detector_server(owner,logger);
            return new Face_recognition_results(null, null, null,null, Face_recognition_in_image_status.server_not_reacheable);
        }
        if (face_detection_result.status() != Face_recognition_in_image_status.face_detected)
        {
            if ( display_face_reco_window) show_face_recognition_window(face_detection_result.image(),null,face_detection_type);
            return new Face_recognition_results(null,null, null,null, Face_recognition_in_image_status.no_face_detected);
        }
        Image image_face = face_detection_result.image();

        if (image_face == null)
        {
            if ( display_face_reco_window) Face_detector.warn_about_no_face_detected(owner,logger);
            else logger.log("no face detected");
            return new Face_recognition_results(null,null,null,null, Face_recognition_in_image_status.no_face_detected);
        }

        if ( dbg) logger.log("detect_and_recognize: face detected");

        // write the image to disk, the tmp path will be passed to the embedding server
        Path face_reco_cache_folder = Cache_folder.get_cache_dir(Cache_folder.face_recognition_cache,owner,logger);
        if ( dbg) logger.log("face_reco_folder = "+face_reco_cache_folder);

        String file_name_base = "tmp_unknown_face_"+ UUID.randomUUID();
        Path tmp_path_to_face = Face_recognition_service.write_tmp_image(image_face, face_reco_cache_folder,file_name_base,logger);
        if ( dbg) logger.log("tmp_path_to_face = "+tmp_path_to_face);

        Eval_results eval_result = eval_a_face(tmp_path_to_face, aborter);
        if (display_face_reco_window) show_face_recognition_window(image_face,eval_result,face_detection_type);

        String display_label = eval_result.label();
        if ( eval_result.label() == null)
        {
            display_label = "not recognized";
        }
        logger.log("face recognition result = "+display_label);

        if ( eval_result.label() == null)
        {
            return new Face_recognition_results(null,image_face,tmp_path_to_face,eval_result.feature_vector(), Face_recognition_in_image_status.no_face_recognized);
        }
        else
        {
            if ( eval_result.eval_situation() == Eval_situation.exact_match)
            {
                return new Face_recognition_results(eval_result.label(), image_face, tmp_path_to_face, eval_result.feature_vector(), Face_recognition_in_image_status.exact_match);
            }
            else
            {
                return new Face_recognition_results(eval_result.label(), image_face, tmp_path_to_face, eval_result.feature_vector(), Face_recognition_in_image_status.face_recognized);
            }
        }
    }


    static long start;
    static long count = 0;
    static double feature_vector_total_ns = 0;
    static double total_ns = 0;

    //**********************************************************
    static Comparator<? super Eval_result_for_one_prototype> comp = new Comparator<Eval_result_for_one_prototype>() {
        @Override
        public int compare(Eval_result_for_one_prototype o1, Eval_result_for_one_prototype o2) {
            return o1.distance().compareTo(o2.distance());
        }
    };


    //**********************************************************
    Eval_results eval_a_face(Path face, Aborter aborter)
    //**********************************************************
    {
        logger.log("eval_a_face "+face);

        start = System.nanoTime();
        boolean error = true;
        Feature_vector the_feature_vector_to_be_identified = null;
        for ( int i = 0; i < 3; i ++)
        {
            Optional<Feature_vector_double> op = feature_vector_source.get_feature_vector(face, owner, aborter, logger);
            if (op.isPresent())
            {
                error = false;
                the_feature_vector_to_be_identified = op.get();
                break;
            }

            logger.log("Warning: FaceNet embeddings failed ! waiting for the servers to start...");
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
                break;
            }
        }
        if ( error)
        {
            return new Eval_results("error", null, Eval_situation.nothing_found, false, "error", new ArrayList<>());
        }
        long fv_time = System.nanoTime()-start;
        feature_vector_total_ns += fv_time;

        List<Eval_result_for_one_prototype> results = new ArrayList<>();
        for (Embeddings_prototype embeddings_prototype : embeddings_prototypes)
        {

            double distance = the_feature_vector_to_be_identified.distance(embeddings_prototype.feature_vector(),logger);
            //nearests.put(distance,embeddings_prototype);
            Eval_result_for_one_prototype r = new Eval_result_for_one_prototype(distance,embeddings_prototype);
            if ( dbg) logger.log("   at distance ="+String.format("%.4f",distance)+"  =>   "+embeddings_prototype.label());
            results.add(r);
        }

        if (results.isEmpty())
        {
            logger.log("no results at all (happens when there are no prototypes in the set)");
            return new Eval_results(null, the_feature_vector_to_be_identified, Eval_situation.nothing_found, true,"empty",new ArrayList<>());
        }

        Collections.sort(results,comp);

        double min_distance = results.get(0).distance();
        Embeddings_prototype winner = results.get(0).embeddings_prototype();
        if ( min_distance < 0.001)
        {
            logger.log("EXACT MATCH DETECTED 1 nearest "+winner.label()+ " at "+String.format("%.2f",min_distance));
            List<Eval_result_for_one_prototype> l = new ArrayList<>();
            l.add(new Eval_result_for_one_prototype(min_distance,winner));
            report_time( fv_time);
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
            logger.log("     d="+String.format("%.3f",res.distance())+ " "+ res.embeddings_prototype().tag());
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

            report_time( fv_time);
            //list_of_Eval_result_for_one_prototype.clear();
            //list_of_Eval_result_for_one_prototype.add(win);
            return new Eval_results(win.embeddings_prototype().label(), the_feature_vector_to_be_identified, Eval_situation.ex_aequo,true,win.embeddings_prototype().tag(),list_of_Eval_result_for_one_prototype);
        }


        if ( winner != null) logger.log("1 nearest "+winner.label()+ " at "+String.format("%.2f",min_distance));
        logger.log(K_of_KNN+" nearest "+label5+ " average distance = "+String.format("%.2f",average_distance));


        report_time( fv_time);

        return new Eval_results(label5, the_feature_vector_to_be_identified, Eval_situation.normal,true,null,list_of_Eval_result_for_one_prototype);
    }


    //**********************************************************
    private void report_time(long fv_time)
    //**********************************************************
    {
        long end = System.nanoTime();
        double x = (end-start);
        total_ns += x;
        count++;
        logger.log("\n==> evaluating one face took "
                +String.format("%.2f",x/1_000_000.0)
                +" milliseconds of which "+ String.format("%.2f",fv_time/1_000_000.0)
                +" was computing the feature vector ("+ String.format("%.2f",100.0*fv_time/x)
                +"%), the averages are "+String.format("%.2f",total_ns/(double) count/1_000_000.0)
                +" / "+String.format("%.2f",feature_vector_total_ns/(double) count/1_000_000.0));
    }


    public void skipped()
    {
        recognition_stats.skipped.incrementAndGet();
        recognition_stats.done.incrementAndGet();
        training_stats.skipped.incrementAndGet();
        training_stats.done.incrementAndGet();
    }

    public void server_error()
    {
        recognition_stats.error.incrementAndGet();
        recognition_stats.done.incrementAndGet();
        training_stats.error.incrementAndGet();
        training_stats.done.incrementAndGet();
    }

    public void no_face_detected()
    {
        recognition_stats.done.incrementAndGet();
        recognition_stats.no_face_detected.incrementAndGet();
        training_stats.no_face_detected.incrementAndGet();
        training_stats.done.incrementAndGet();

    }

    public void should_not_happen()
    {
        recognition_stats.error.incrementAndGet();
        recognition_stats.done.incrementAndGet();
        training_stats.error.incrementAndGet();
        training_stats.done.incrementAndGet();

    }

    public void run_face_recognition_actor(Face_recognition_message msg)
    {
        Face_recognition_actor actor = new Face_recognition_actor(this,logger);
        Actor_engine.run(actor,msg,null, logger);

    }
}
