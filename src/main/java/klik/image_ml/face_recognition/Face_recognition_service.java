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



package klik.image_ml.face_recognition;

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
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.icons.JavaFX_to_Swing;
import klik.image_ml.Feature_vector;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.ui.Show_running_man_frame_with_abort_button;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Face_recognition_service
//**********************************************************
{
    public static final boolean dbg = false;
    public final static String EXTENSION_FOR_EP = "prototype";
    private static Face_recognition_service instance = null;
    final Logger logger;
    private final Browser browser;
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


    //**********************************************************
    private Face_recognition_service(String name_, Browser browser)
    //**********************************************************
    {
        face_recognizer_name = name_;
        this.browser = browser;
        this.logger = browser.browser_ui.logger;
        Path face_reco_folder = Static_files_and_paths_utilities.get_face_reco_folder(logger);
        face_recognizer_path = Path.of(face_reco_folder.toAbsolutePath().toString(),face_recognizer_name);
        Browser_creation_context.additional_different_folder(face_recognizer_path,browser,logger);

        last_report = System.currentTimeMillis();
        recognition_stats = new Recognition_stats();
        training_stats = new Training_stats();

    }


    //**********************************************************
    public static Face_recognition_service get_instance(Browser browser)
    //**********************************************************
    {
        if ( instance == null) start_new(browser);
        return instance;
    }

    //**********************************************************
    public static void start_new(Browser browser)
    //**********************************************************
    {
        Optional<String> localo = get_face_recognition_model_name(browser.logger);

        if ( localo.isEmpty()) return;

        instance = new Face_recognition_service(localo.get(), browser);
        instance.load_internal();
    }

    //**********************************************************
    public static void save()
    //**********************************************************
    {
        if ( instance != null) instance.save_internal();
    }

    //**********************************************************
    public static void load(Browser browser)
    //**********************************************************
    {
        if ( instance != null) instance.load_internal();
        else start_new(browser);
    }



    //**********************************************************
    private static Optional<String> get_face_recognition_model_name(Logger logger)
    //**********************************************************
    {

        Path p = Static_files_and_paths_utilities.get_face_reco_folder(logger);
        File[] files = p.toFile().listFiles();

        ChoiceDialog<String> cd = new ChoiceDialog<>("Select face recognition model");
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
            dialog.setTitle("Give recognition system tag");
            dialog.setHeaderText("Give recognition system tag");
            return dialog.showAndWait();
        }
        return x;


    }

    //**********************************************************
    public static void auto(Browser browser)
    //**********************************************************
    {
        Face_recognition_service fr = Face_recognition_service.get_instance(browser);
        Actor_engine.execute(() -> fr.auto_internal(browser), fr.logger);
    }


    //**********************************************************
    public static void self(Browser browser)
    //**********************************************************
    {
        Face_recognition_service fr = Face_recognition_service.get_instance(browser);
        Actor_engine.execute(() -> fr.self_internal(browser), fr.logger);
    }

    //**********************************************************
    private void auto_internal(Browser browser)
    //**********************************************************
    {
        AtomicInteger files_in_flight = new AtomicInteger(0);
        Show_running_man_frame_with_abort_button running_man = Show_running_man_frame_with_abort_button.show_running_man(files_in_flight,"Wait for auto train to complete",20*3600,logger);
        Aborter aborter_for_auto_train = running_man.aborter;


        Face_recognition_actor face_recognition_actor = new Face_recognition_actor(this);


        last_report = System.currentTimeMillis();
        recognition_stats = new Recognition_stats();
        training_stats = new Training_stats();
        Path target = browser.displayed_folder_path;
        File check = new File (target.toFile(),".folder_name_is_recognition_label");
        if ( !check.exists())
        {
            logger.log("auto_internal skipping1 "+target+" as it does not contain a file named .folder_name_is_recognition_label");
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
            if ( !f.isDirectory())
            {
                logger.log("auto_internal skipping2 "+f.getAbsolutePath());
                continue;
            }
            if ( aborter_for_auto_train.should_abort()) return;

            String label = f.getName();
            double percent = 100.0*(double)i/(double)folders.size();
            String done =  String.format("%.1f",percent);
            running_man.set_title(label+", "+done+"% of total");
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
                int T = Actor_engine.how_many_threads_are_in_flight(logger);
                if (T < MAX_THREADS) break;
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

            AtomicInteger label_in_flight = new AtomicInteger(0);
            auto_folder_for_one_label(f,label, face_recognition_actor, aborter_for_auto_train, files_in_flight, label_in_flight);
        }

        // DONT save_internal(aborter_for_auto_train);

        //running_man.report_progress_and_close_when_finished(files_in_flight);
        logger.log("Finished Face Recognition AUTO: "+recognition_stats.to_string());
    }


    //**********************************************************
    private boolean auto_folder_for_one_label(File dir, String label,
                                              Face_recognition_actor face_recognition_actor,
                                              Aborter aborter_for_auto_train,
                                              AtomicInteger files_in_flight,
                                              AtomicInteger label_in_flight)
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
            label_in_flight.decrementAndGet();
            long now = System.currentTimeMillis();
            if (now-last_report> 10000)
            {
                last_report = now;
                logger.log("\n\n\n\n\n");
                logger.log("Recognition:"+recognition_stats.to_string());
                logger.log("Training:"+training_stats.to_string());
            }
        };
        logger.log("auto train folder: "+dir.getAbsolutePath()+ "files in flight: "+files_in_flight.get());
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
                if ( !auto_folder_for_one_label(f,label, face_recognition_actor, aborter_for_auto_train, files_in_flight,label_in_flight))
                {
                    logger.log("auto_folder returns false, aborting folder "+dir);
                    return false;
                }
            }
            if (Guess_file_type.is_file_an_image(f))
            {
                label_in_flight.incrementAndGet();
                Face_recognition_message msg = new Face_recognition_message(f, Face_detection_type.MTCNN, true, label, false, aborter_for_auto_train, files_in_flight);
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
    public void show_face_recognition_window(
            Image face,
            Face_recognition_actor.Eval_results eval_result,
            Aborter aborter
    )
    //**********************************************************
    {
        int size = 1600/Face_recognition_actor.K_of_KNN;
        if ( size > 200) size = 200;
        if (Platform.isFxApplicationThread())
        {
            show_face_recognition_window_internal(size,face,eval_result);
        }
        else {
            int size2 = size;
            Jfx_batch_injector.inject(()->show_face_recognition_window_internal(size2,face,eval_result),logger);
        }
    }
    //**********************************************************
    public void show_face_recognition_window_internal(
            int size,
            Image face_image,
            Face_recognition_actor.Eval_results eval_result)
    //**********************************************************
    {


        Stage stage = new Stage();
        Label status_label = new Label();

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
                vBox.getChildren().add(new Label("Extracted face looks like this:"));
                ImageView iv = new ImageView();
                iv.setImage(face_image);
                iv.setPreserveRatio(true);
                iv.setFitWidth(size);
                Pane image_pane = new StackPane(iv);
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
                    vBox.getChildren().add(new Label("Closests prototypes found: "));

                    HBox hBox = new HBox();
                    Border border = new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID,new CornerRadii(1),new BorderWidths(0.5)));

                    for (Eval_result_for_one_prototype res : eval_result.list())
                    {
                        VBox vb = new VBox();
                        vb.setBorder(border);

                        {
                            Label lab = new Label("At: "+String.format("%.3f",res.distance()));
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
                            vb.getChildren().add(image_pane);
                        }
                        {
                            Label lab = new Label(res.embeddings_prototype().label());
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
            if (eval_result != null) textField.setDisable(!eval_result.enable_adding());

            {
                HBox hBox = new HBox();
                Label label5 = new Label("Enter the recognition label from list:");
                hBox.getChildren().add(label5);
                vBox.getChildren().add(hBox);
            }
            {
                HBox hBox = new HBox();
                ComboBox<String> comboBox = new ComboBox<>();
                if (eval_result != null) comboBox.setDisable(!eval_result.enable_adding());

                comboBox.getItems().addAll(Face_recognition_service.get_instance(browser).get_prototype_labels());
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
                hBox.getChildren().add(label2);

                hBox.getChildren().add(textField);
                vBox.getChildren().add(hBox);
            }
            if (eval_result != null) {
                if (!eval_result.enable_adding())
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
                if (eval_result != null) add.setDisable(!eval_result.enable_adding());
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
                        Face_recognition_status s = Face_recognition_status.valueOf(message);
                        if (s != Face_recognition_status.feature_vector_ready) {
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
                skip.setOnAction(e -> {
                    stage.close();
                });
                hBox.getChildren().add(skip);
                vBox.getChildren().add(hBox);
            }
            if (eval_result != null) {
                HBox hBox = new HBox();
                Button remove = new Button("REMOVE this face from the training set (bad face or wrong label)");
                remove.setDisable(!eval_result.enable_adding());
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
        stage.setScene(new Scene(vBox));
        stage.show();
    }


    //**********************************************************
    private void save_internal()
    //**********************************************************
    {
        logger.log("save_internal : saving "+embeddings_prototypes.size()+ " prototypes");
        for (Embeddings_prototype ep : embeddings_prototypes)
        {
            Actor_engine.execute(()->save_ep(ep),logger);
        }
    }
    //**********************************************************
    private void load_internal()
    //**********************************************************
    {
        AtomicInteger in_flight = new AtomicInteger(0);
        Show_running_man_frame_with_abort_button x = Show_running_man_frame_with_abort_button.show_running_man(in_flight,"Loading face recognition prototypes", 20_100, logger);
        Load_one_prototype_actor actor = new Load_one_prototype_actor();
        Runnable r = () -> {
            Path p = Path.of(face_recognizer_path.toAbsolutePath().toString());
            File[] files = p.toFile().listFiles();
            for (File f: files)
            {
                if ( f.isDirectory()) continue;
                in_flight.incrementAndGet();
                Job_termination_reporter tr = (message, job) -> in_flight.decrementAndGet();

                Actor_engine.run(actor,
                        new Load_one_prototype_message(f,this,x.aborter),
                        tr,
                        logger);
            }
        };
        Actor_engine.execute(r,logger);

        //x.report_progress_and_close_when_finished(in_flight);
    }

    //**********************************************************
    public void load_one_prototype(File f)
    //**********************************************************
    {
        String ext = FilenameUtils.getExtension(f.getName());
        if ( !ext.equals(EXTENSION_FOR_EP)) return;
        String tag = FilenameUtils.getBaseName(f.getName());

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
                Feature_vector fv = new Feature_vector(values);
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
        try {
            BufferedImage bi = JavaFX_to_Swing.fromFXImage(face, null, logger);
            ImageIO.write(bi, "png", path.toFile());
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        return path;
    }




    //**********************************************************
    private void self_internal(Browser browser)
    //**********************************************************
    {
        AtomicInteger files_in_flight = new AtomicInteger(0);
        Show_running_man_frame_with_abort_button running_man = Show_running_man_frame_with_abort_button.show_running_man(files_in_flight,"Wait for SELF face recognition to complete",20*60,logger);
        Aborter aborter_for_self = running_man.aborter;

        last_report = System.currentTimeMillis();
        recognition_stats = new Recognition_stats();
        Path target = face_recognizer_path;
        logger.log("doing SELF on: "+target);

        File files[] = target.toFile().listFiles();
        for ( File f : files)
        {
            if ( f.isDirectory())
            {
                logger.log("self_internal skipping2 "+f.getAbsolutePath());
                continue;
            }
            if ( aborter_for_self.should_abort()) return;
            if ( ! FilenameUtils.getExtension(f.getName()).equals(EXTENSION_FOR_EP)) continue;
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

            self_file(f,files,aborter_for_self);
        }

        //running_man.report_progress_and_close_when_finished(files_in_flight);
        logger.log("Finished Face Recognition");
    }

    //**********************************************************
    private void self_file(File f, File[] files, Aborter aborter_for_self)
    //**********************************************************
    {
        String ext = FilenameUtils.getExtension(f.getName());
        if ( !ext.equals(EXTENSION_FOR_EP)) return;
        String self_target_tag = FilenameUtils.getBaseName(f.getName());

        boolean part2 = true;
        String label = null;
        Feature_vector fv  = null;
        Image ref = null;
        if ( part2)
        {
            Embeddings_prototype ep = load_ep(f,self_target_tag);
            fv = ep.feature_vector();
            label = ep.label();
            ref= ep.face_image(face_recognizer_path,logger);
        }
        else
        {
            label = load_label_from_ep_file(f);
            if (label == null) {
                logger.log("loading failed for " + f.getAbsolutePath());
                return;
            }
            logger.log("self file: " + self_target_tag + " label:" + label);
        }

        double min_distance = Double.MAX_VALUE;
        Embeddings_prototype ep_min = null;
        Embeddings_prototype ep_max = null;
        double max_distance = 0;
        double average_distance =0;
        int count =0;
        for ( File f2 : files)
        {
            if ( f.getAbsolutePath().equals(f2.getAbsolutePath())) continue;
            if ( ! FilenameUtils.getExtension(f2.getName()).equals(EXTENSION_FOR_EP)) continue;
            Feature_vector fv2 = null;
            String tag2 = FilenameUtils.getBaseName(f2.getName());
            String label2 = load_label_from_ep_file(f2);
            if ( label2 == null) continue;
            if ( !label2.equals(label)) continue;

            Embeddings_prototype ep = null;
            if ( part2)
            {
                ep = load_ep(f2,tag2);
                fv2 = ep.feature_vector();
            }
            logger.log("self file: "+self_target_tag+ " tag2:"+tag2);
            Path face_path2 = Embeddings_prototype.make_image_path(face_recognizer_path,tag2,logger);

            boolean part1 = false;
            if ( part1)
            {
                // part1: ask the full set==> must return an exact match
                Face_recognition_actor.Face_recognition_results face_recognition_results = Face_recognition_actor.recognize_a_face(face_path2,false,aborter_for_self, this);

                logger.log(face_recognition_results.to_string()+"\n\n\n");

                if ( face_recognition_results.face_recognition_status() == Face_recognition_status.exact_match)
                {
                    // this is what is expected of course !
                }
                else
                {
                    logger.log("ERROR: "+self_target_tag+" tested with "+tag2+ " does not give exact match");
                }
            }
            if (part2)
            {
                //part2, compute the distances
                double distance = fv.cosine_similarity(fv2);
                count++;
                if ( distance < min_distance)
                {
                    ep_min = ep;
                    min_distance = distance;
                }
                if ( distance > max_distance)
                {
                    ep_max = ep;
                    max_distance = distance;
                }
                average_distance += distance;
            }

        }
        average_distance /= count;
        logger.log(self_target_tag+" min: "+min_distance+ " ave: "+average_distance+" max: "+max_distance);
        if (( ep_min != null) && ( ep_max != null))
        {
            String desc = " min:"+ep_min.tag()+ "at: "+String.format("%.2f",min_distance);
            desc += "\n max:"+ep_max.tag()+ "at: "+String.format("%.2f",max_distance);

            Utils.display(200,ref,ep_min.face_image(face_recognizer_path,logger),ep_max.face_image(face_recognizer_path,logger),self_target_tag +"recognized as:", desc,logger);
        }
        else
        {
            logger.log(Stack_trace_getter.get_stack_trace("WTF?"));
        }
    }


    public void skipped() {
        recognition_stats.skipped.incrementAndGet();
        recognition_stats.done.incrementAndGet();
        training_stats.skipped.incrementAndGet();
        training_stats.done.incrementAndGet();
    }

    public void server_error() {
        recognition_stats.error.incrementAndGet();
        recognition_stats.done.incrementAndGet();
        training_stats.error.incrementAndGet();
        training_stats.done.incrementAndGet();
    }

    public void no_face_detected() {
        recognition_stats.done.incrementAndGet();
        recognition_stats.no_face_detected.incrementAndGet();
        training_stats.no_face_detected.incrementAndGet();
        training_stats.done.incrementAndGet();

    }

    public void should_not_happen() {
        recognition_stats.error.incrementAndGet();
        recognition_stats.done.incrementAndGet();
        training_stats.error.incrementAndGet();
        training_stats.done.incrementAndGet();

    }
}
