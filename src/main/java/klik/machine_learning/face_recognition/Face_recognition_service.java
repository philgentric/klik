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



package klik.machine_learning.face_recognition;

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
import klik.Window_type;
import klik.Instructions;
import klik.look.Look_and_feel_manager;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.execute.actor.Job_termination_reporter;
//import klik.browser.icons.JavaFX_to_Swing;
import klik.util.image.Static_image_utilities;
import klik.machine_learning.feature_vector.Feature_vector;
import klik.machine_learning.feature_vector.Feature_vector_double;
import klik.util.files_and_paths.Extensions;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.ui.Popups;
import klik.util.ui.progress.Progress_window;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

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
    public final static String EXTENSION_FOR_EP = "prototype";
    private static Face_recognition_service instance = null;
    final Logger logger;
    ConcurrentLinkedQueue<Embeddings_prototype> embeddings_prototypes = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<String> labels = new ConcurrentLinkedQueue<>();
    Map<String,Embeddings_prototype> tag_to_prototype = new ConcurrentHashMap<>();
    Map<String,Integer> label_to_prototype_count = new ConcurrentHashMap<>();
    public final String face_recognizer_name;
    public static Path face_recognizer_path;
    Recognition_stats recognition_stats;
    Training_stats training_stats;
    long last_report;
    private static final int MAX_THREADS = 50;
    Window owner;

    //**********************************************************
    private Face_recognition_service(String name, Window owner,Logger logger)
    //**********************************************************
    {
        face_recognizer_name = name;
        this.owner = owner;
        this.logger = logger;
        Path face_reco_folder = Static_files_and_paths_utilities.get_face_reco_folder(owner,logger);
        face_recognizer_path = Path.of(face_reco_folder.toAbsolutePath().toString(),face_recognizer_name);
        Instructions.additional_no_past(Window_type.File_system_2D,new Path_list_provider_for_file_system(face_recognizer_path,owner,logger),owner,logger);

        last_report = System.currentTimeMillis();
        recognition_stats = new Recognition_stats();
        training_stats = new Training_stats();

    }


    //**********************************************************
    public static Face_recognition_service get_instance(Window owner,Logger logger)
    //**********************************************************
    {
        if ( instance == null) start_new(owner,logger);
        return instance;
    }

    //**********************************************************
    public static void start_new(Window owner, Logger logger)
    //**********************************************************
    {
        Optional<String> localo = get_Face_recognition_model_name(owner,logger);

        if ( localo.isEmpty()) return;

        instance = new Face_recognition_service(localo.get(), owner,logger);
        instance.load_internal();
    }

    //**********************************************************
    public static void save()
    //**********************************************************
    {
        if ( instance != null) instance.save_internal();
    }

    //**********************************************************
    public static void load(Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance != null) instance.load_internal();
        else start_new(owner,logger);
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
    public static void auto(Path displayed_folder_path, Window owner,Logger logger)
    //**********************************************************
    {
        Face_recognition_service fr = Face_recognition_service.get_instance(owner,logger);
        Actor_engine.execute(() -> fr.auto_internal(displayed_folder_path, logger), "face recognition auto",fr.logger);
    }


    //**********************************************************
    public static void do_folder(Path folder, Window owner, Logger logger)
    //**********************************************************
    {
        Face_recognition_service fr = Face_recognition_service.get_instance(owner,logger);
        Actor_engine.execute(() -> fr.do_folder_internal(folder), "face recognition do 1 folder",logger);
    }

    //**********************************************************
    private void auto_internal(Path displayed_folder_path, Logger logger)
    //**********************************************************
    {
        AtomicInteger files_in_flight = new AtomicInteger();
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Progress_window progress_window = Progress_window.show(
                files_in_flight,
                "Wait for auto train to complete",
                3600*60,
                x,
                y,
                owner,
                logger);
        Aborter aborter_for_auto_train = progress_window.aborter;

        Face_recognition_actor Face_recognition_actor = new Face_recognition_actor(this);

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
                logger.log("auto_internal skipping1 " + target + " as it does not contain a file named .folder_name_is_recognition_label");
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
            if ( aborter_for_auto_train.should_abort()) return;

            String label = f.getName();
            double percent = 100.0*(double)i/(double)folders.size();
            String done =  String.format("%.1f",percent);
            progress_window.set_text(label+", "+done+"% of total");
            i++;
            Integer N = label_to_prototype_count.get(label);
            if ( N == null)
            {
                N = Integer.valueOf(0);
                label_to_prototype_count.put(label,N);
            }
            else
            {
                if (N > Face_recognition_actor.LIMIT_PER_LABEL)
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
            auto_folder_for_one_label(f,label, Face_recognition_actor, aborter_for_auto_train, files_in_flight, label_in_flight);
        }

        // DONT save_internal(aborter_for_auto_train);

        //running_film.report_progress_and_close_when_finished(files_in_flight);
        logger.log("Finished Face Recognition AUTO: "+recognition_stats.to_string());
    }


    //**********************************************************
    private boolean auto_folder_for_one_label(File dir, String label,
                                              Face_recognition_actor Face_recognition_actor,
                                              Aborter aborter_for_auto_train,
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
            if ( aborter_for_auto_train.should_abort())
            {
                logger.log("auto aborted");
                return false;
            }


            if ( f.isDirectory())
            {
                if ( !auto_folder_for_one_label(f,label, Face_recognition_actor, aborter_for_auto_train, files_in_flight,label_in_flight))
                {
                    logger.log("auto_folder returns false, aborting folder "+dir);
                    return false;
                }
            }
            if (Guess_file_type.is_this_file_an_image(f,owner, logger))
            {
                label_in_flight.increment();
                Face_recognition_message msg = new Face_recognition_message(f, Face_detection_type.MTCNN, true, label, false, aborter_for_auto_train, files_in_flight);
                Actor_engine.run(Face_recognition_actor, msg, tr, logger);
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
    public void show_face_recognition_window(
            Image face,
            Face_recognition_actor.Eval_results eval_result,
            Window owner,
            Aborter aborter
    )
    //**********************************************************
    {
        int size = 1600/Face_recognition_actor.K_of_KNN;
        if ( size > 200) size = 200;
        if (Platform.isFxApplicationThread())
        {
            show_Face_recognition_window_internal(size,face,eval_result,owner);
        }
        else {
            int size2 = size;
            Jfx_batch_injector.inject(()->show_Face_recognition_window_internal(size2,face,eval_result,owner),logger);
        }
    }
    //**********************************************************
    public void show_Face_recognition_window_internal(
            int size,
            Image face_image,
            Face_recognition_actor.Eval_results eval_result,
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
                Label l= new Label("Extracted face looks like this:");
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
                stage.setTitle("Face Detection failed");
                stage.setMinWidth(400);
                stage.setMinHeight(400);
                status_label.setText("Face Detection failed");
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

                comboBox.getItems().addAll(Face_recognition_service.get_instance(stage,logger).get_prototype_labels());
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
                    Prototype_adder_actor actor = new Prototype_adder_actor(this);
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
        AtomicInteger in_flight = new AtomicInteger();
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Progress_window progress_window = Progress_window.show(
                in_flight,
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
                    Files.createDirectory(p);
                } catch (IOException e) {
                    logger.log("cannot create folder: "+p.toAbsolutePath());
                }
                return;
            }
            for (File f: files)
            {
                if ( f.isDirectory()) continue;
                in_flight.incrementAndGet();
                Job_termination_reporter tr = (message, job) -> in_flight.decrementAndGet();

                Actor_engine.run(actor,
                        new Load_one_prototype_message(f,this,progress_window.aborter),
                        tr,
                        logger);
            }
        };
        Actor_engine.execute(r,"Load face recognition prototypes",logger);

        //x.report_progress_and_close_when_finished(in_flight);
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
                        logger.log("error reading ep fv size");
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
                        logger.log("error reading fv: missing component #" + i + ", size of fv was " + size + " for: " + f);
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
                Feature_vector fv = new Feature_vector_double(values);
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

        Static_image_utilities.write_png_to_disk(face, path, logger);
        /*
        try {
            BufferedImage bi = JavaFX_to_Swing.fromFXImage(face, null, logger);
            logger.log("BufferedImage="+bi.getWidth()+"x"+bi.getHeight());
            boolean status = ImageIO.write(bi, "png", path.toFile());
            // RICK boolean status = ImageIO.write(bi, "png", new File("/Users/philippegentric/Desktop/toto/titi.png"));//path.toFile());
            if ( !status)
            {
                logger.log("could not write tmp image to: "+path);
                return null;
            }
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }*/
        return path;
    }




    //**********************************************************
    private void do_folder_internal(Path target)
    //**********************************************************
    {
        AtomicInteger files_in_flight = new AtomicInteger(0);
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Progress_window progress_window = Progress_window.show(
                files_in_flight,
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

        do_folder(target,null,progress_window.aborter,recognition_stats);



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
    private void do_one_file(File file, String self_target_tag, Aborter aborter_for_self, Recognition_stats recognition_stats)
    //**********************************************************
    {
        logger.log("Face recognition test on " + file.getName());

        Face_recognition_actor.Face_recognition_results face_recognition_results = null;
        boolean face_detected_at_least_by_one_method =  false;
        for( Face_detection_type face_detection_type : Face_detection_type.values())
        {
            face_recognition_results =
                Face_recognition_actor.detect_and_recognize(
                this,
                file.toPath(),
                face_detection_type,
                false,
                aborter_for_self, owner,logger);

            logger.log(face_recognition_results.to_string()+"\n\n\n");


            if ( face_recognition_results.face_recognition_in_image_status() == Face_recognition_in_image_status.no_face_detected)
            {
                logger.log("No face detected with "+face_detection_type);
                continue;
            }
            face_detected_at_least_by_one_method = true;

            if ( face_recognition_results.label().equals(self_target_tag))
            {
                logger.log("OK: " + self_target_tag + " CORRECTLY recognized as " + face_recognition_results.label());
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
        if ( face_recognition_results.image() == null) return;
        Utils.display(
                200,
                face_recognition_results.image(),
                null,
                null,
                self_target_tag + " NOT recognized",
                "recognized as: " + face_recognition_results.label(),
                logger);
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
}
