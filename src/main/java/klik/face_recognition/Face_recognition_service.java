package klik.face_recognition;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.icons.JavaFX_to_Swing;
import klik.files_and_paths.Guess_file_type;
import klik.properties.Static_application_properties;
import klik.search.Show_running_man_frame;
import klik.search.Show_running_man_frame_with_abort_button;
import klik.util.Fx_batch_injector;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Face_recognition_service
//**********************************************************
{
    public static final boolean dbg = false;
    public final static String EXTENSION_FOR_EP = "prototype";
    public static final int K_of_KNN = 5;
    private static Face_recognition_service instance = null;
    final Logger logger;
    private final Browser browser;
    ConcurrentLinkedQueue<Embeddings_prototype> embeddings_prototypes = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<String> labels = new ConcurrentLinkedQueue<>();
    Map<String,Embeddings_prototype> names_to_embeddings = new ConcurrentHashMap<>();
    public final String face_recognizer_name;
    public final Path face_recognizer_path;
    Recognition_stats recognition_stats;
    Training_stats training_stats;
    long last_report;


    //**********************************************************
    private Face_recognition_service(String name_, Browser browser)
    //**********************************************************
    {
        face_recognizer_name = name_;
        this.browser = browser;
        this.logger = browser.browser_ui.logger;
        this.face_recognizer_path = Static_application_properties.get_absolute_dir_on_user_home(face_recognizer_name,true, logger);
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
        String local = get_face_recognition_name(browser.my_Stage.the_Stage);
        instance = new Face_recognition_service(local, browser);
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
    private static String get_face_recognition_name(Stage stage)
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Give recognition system tag");
        dialog.setHeaderText("Give recognition system tag");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) return result.get();
        return null;
    }

    //**********************************************************
    public static void auto(Browser browser)
    //**********************************************************
    {
        Face_recognition_service fr = Face_recognition_service.get_instance(browser);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                fr.auto_internal(browser);
            }
        };
        Actor_engine.execute(r,new Aborter("auto face recog",fr.logger), fr.logger);
    }

    //**********************************************************
    public void auto_internal(Browser browser)
    //**********************************************************
    {
        AtomicInteger files_in_flight = new AtomicInteger(0);
        Show_running_man_frame_with_abort_button running_man = Show_running_man_frame_with_abort_button.show_running_man("Wait for auto train to complete",20*60,logger);
        Aborter aborter_for_auto_train = running_man.aborter;

        last_report = System.currentTimeMillis();
        recognition_stats = new Recognition_stats();
        training_stats = new Training_stats();
        Path target = browser.displayed_folder_path;
        File check = new File (target.toFile(),".folder_name_is_recognition_label");
        if ( !check.exists())
        {
            logger.log("skipping "+target);
            return;
        }
        logger.log("doing "+target);

        File files[] = target.toFile().listFiles();
        for ( File f : files)
        {
            if ( !f.isDirectory())
            {
                logger.log("skipping "+f.getAbsolutePath());
                continue;
            }
            String label = f.getName();
            AtomicInteger count = new AtomicInteger(0);
            process_folder(f,label, aborter_for_auto_train, files_in_flight, count);
        }

        running_man.wait_and_block_until_finished(files_in_flight);
    }

    //**********************************************************
    private void process_folder(File dir, String label, Aborter aborter_for_auto_train, AtomicInteger files_in_flight, AtomicInteger count)
    //**********************************************************
    {
        Face_recognition_actor actor = new Face_recognition_actor(this);
        Job_termination_reporter tr = (message, job) -> {
            files_in_flight.decrementAndGet();
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
        for ( File f: files)
        {
            if ( f.isDirectory()) process_folder(f,label,aborter_for_auto_train, files_in_flight, count);
            if (Guess_file_type.is_file_an_image(f))
            {
                while ( files_in_flight.get() > 10)
                {
                    logger.log("auto train more than 10 files in flight, sleeping: "+files_in_flight.get());
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        logger.log(Stack_trace_getter.get_stack_trace(""+e));
                    }
                }
                Face_recognition_message msg = new Face_recognition_message(f,label,false,aborter_for_auto_train, files_in_flight);
                Actor_engine.run(actor,msg,tr,logger);
                if ( count.incrementAndGet() > 10) break;
            }
        }
        save_internal();
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
    public boolean train_manual(Path reference_face)
    //**********************************************************
    {
        Face_detector.Face_detection_result status = Face_detector.detect_face(reference_face, true, logger);

        if (status.status() == Face_recognition_status.server_not_reacheable)
        {
            Face_detector.warn_about_face_detector_server(logger);
            return false;
        }
        if (status.status() != Face_recognition_status.face_detected)
        {
            Face_detector.warn_about_no_face_detected(logger);
            return false;
        }
        if (dbg) show_face_recognition_window(status.image(),null, true, null,new ArrayList<>());
        return true;
    }


    //**********************************************************
    public void show_face_recognition_window(
            Image face,
            String label,
            boolean enable_adding,
            String name, // only present in case of an exact match
            List<Image> closest_faces
    )
    //**********************************************************
    {
        if (Platform.isFxApplicationThread())
        {
            show_face_recognition_window_internal(face,label,enable_adding,name,closest_faces);
        }
        else {
            Fx_batch_injector.inject(()->show_face_recognition_window_internal(face,label,enable_adding,name,closest_faces),logger);
        }
    }
    //**********************************************************
    public void show_face_recognition_window_internal(
            Image face,
            String label,
            boolean enable_adding,
            String name, // only present in case of an exact match
            List<Image> closest_faces
    )
    //**********************************************************
    {
        Stage stage = new Stage();
        Label status_label = new Label();

        if ( label != null)
        {
            stage.setTitle("Recognized as: "+label);
        }
        VBox vBox = new VBox();
        vBox.getChildren().add(new Label("Extracted face looks like this:"));

        {
            ImageView iv = new ImageView();
            if ( face != null)
            {
                iv.setImage(face);
            }
            else
            {
                stage.setTitle("Face Detection failed");
                status_label.setText("Face Detection failed");
            }
            iv.setPreserveRatio(true);
            iv.setFitWidth(100);
            Pane image_pane = new StackPane(iv);
            vBox.getChildren().add(image_pane);
        }
        if ( !closest_faces.isEmpty())
        {
            vBox.getChildren().add(new Label("Closests prototypes found: "));

            HBox hBox = new HBox();
            for ( Image ii : closest_faces)
            {
                ImageView iv = new ImageView(ii);
                iv.setPreserveRatio(true);
                iv.setFitWidth(100);
                Pane image_pane = new StackPane(iv);
                hBox.getChildren().add(image_pane);
            }
            vBox.getChildren().add(hBox);

        }
        TextField textField = new TextField();
        textField.setDisable(!enable_adding);

        {
            HBox hBox = new HBox();
            Label label5 = new Label("Enter the recognition label from list:");
            hBox.getChildren().add(label5);
            vBox.getChildren().add(hBox);
        }
        {
            HBox hBox = new HBox();
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setDisable(!enable_adding);

            comboBox.getItems().addAll(Face_recognition_service.get_instance(browser).get_prototype_labels());
            if ( label != null)
            {
                comboBox.setValue(label);
                textField.setText(label);
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
        if (!enable_adding)
        {
            if ( face !=null)
            {
                stage.setTitle("Exact match! ");
                status_label.setText("prototype was recognized at distance zero, no need to add it ");
            }
        }

        {
            HBox hBox = new HBox();
            Button add = new Button("Add to training set");
            add.setDisable(!enable_adding);
            add.setOnAction(e -> {
                String image_label = textField.getText();
                if ( image_label.trim().isEmpty())
                {
                    status_label.setText("Error: no label!");
                    return;
                }
                Prototype_adder_actor actor = new Prototype_adder_actor(this);
                Prototype_adder_message msg = new Prototype_adder_message(face,image_label.trim(),new Aborter("bidon",logger));
                Job_termination_reporter tr = (message, job) -> {
                    Face_recognition_status s = Face_recognition_status.valueOf(message);
                    if (s != Face_recognition_status.feature_vector_ready)
                    {
                        status_label.setText("prototype fabrication error "+s);
                    }
                    else
                    {
                        save_internal();
                        stage.close();
                    }
                };
                Actor_engine.run(actor,msg,tr,logger);

                //Face_recognition_status s = add_prototype_image_face(face, image_label.trim(),false);

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
        {
            HBox hBox = new HBox();
            Button skip = new Button("REMOVE this face from the training set (bad face or wrong label)");
            skip.setDisable(!enable_adding);
            skip.setOnAction(e -> {
                Embeddings_prototype guilty = names_to_embeddings.get(name);
                embeddings_prototypes.remove(guilty);
                names_to_embeddings.remove(name);
                try {
                    Path p = make_image_path(face_recognizer_path,name,logger);
                    Files.delete(p);
                    logger.log("deleted: "+p);
                } catch (IOException ex) {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }
                try {
                    Path p = make_prototype_path(face_recognizer_path,name);
                    Files.delete(p);
                    logger.log("deleted: "+p);
                } catch (IOException ex) {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }
                save_internal();
                stage.close();
            });
            hBox.getChildren().add(skip);
            vBox.getChildren().add(hBox);
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
        logger.log("saving "+embeddings_prototypes.size()+ "prototypes");
        for (Embeddings_prototype ep : embeddings_prototypes)
        {
            save_ep(ep);
        }
    }
    //**********************************************************
    private void load_internal()
    //**********************************************************
    {
        Aborter ab = new Aborter("face reco load",logger);

        CountDownLatch x = Show_running_man_frame.show_running_man("Loading face recognition prototypes", 20_100,ab, logger);

        Face_recognition_service frs = this;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Path p = Path.of(face_recognizer_path.toAbsolutePath().toString());
                File[] files = p.toFile().listFiles();
                for (File f: files)
                {
                    if ( f.isDirectory()) continue;
                    String ext = FilenameUtils.getExtension(f.getName());
                    if ( !ext.equals(EXTENSION_FOR_EP)) continue;
                    String name = FilenameUtils.getBaseName(f.getName());

                    Embeddings_prototype ep = load_ep(f,name);
                    if ( ep == null)
                    {
                        logger.log("loading failed for "+f.getAbsolutePath());
                        continue;
                    }
                    String label = ep.label();
                    frs.embeddings_prototypes.add(ep);
                    if ( !labels.contains(label)) labels.add(label);
                }
                x.countDown();
            }
        };
        Actor_engine.execute(r,ab,logger);


    }

    //**********************************************************
    private Embeddings_prototype load_ep(File f, String name)
    //**********************************************************
    {
        Path image_path = make_image_path(face_recognizer_path,name,logger);

        logger.log("trying to load image ->"+image_path+"<-");
        File image_file = new File(image_path.toAbsolutePath().toString());
        Image face = null;
        try
        {
            face = new Image(image_file.toURI().toString());
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            // remove the prototype
            try {
                Files.delete(f.toPath());
            } catch (IOException ex) {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
            }
            logger.log("face not found while loading prototype: prototype erased from disk : "+f);
            return null;
        }


        try (BufferedReader reader = new BufferedReader(new FileReader(f)))
        {
            String label = null;

            {
                String line =  reader.readLine();
                if ( line == null)
                {
                    logger.log("error reading ep label");
                    return null;
                }
                label = line.trim();
            }

            int size = 0;
            {
                String line =  reader.readLine();
                if ( line == null)
                {
                    logger.log("error reading ep fv size");
                    return null;
                }
                size = Integer.valueOf(line.trim());
            }
            double[] values = new double[size];
            for ( int i = 0; i < size ;i++)
            {
                String line = reader.readLine();
                if ( line == null)
                {
                    logger.log("error reading fv #"+i);
                    return null;
                }
                try {
                    values[i] = Double.valueOf(line.trim());
                }
                catch (NumberFormatException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }
            }
            Feature_vector fv = new Feature_vector(values);
            // now read the face from the same folder

            //Path cache_dir = Files_and_Paths.get_face_recognition_cache_dir(logger);



            return new Embeddings_prototype(face, fv, label, name);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //**********************************************************
    static Path make_prototype_path(Path folder_path, String name)
    //**********************************************************
    {
        return Path.of(folder_path.toAbsolutePath().toString() , name+"."+EXTENSION_FOR_EP);
    }


    //**********************************************************
    private void save_ep(Embeddings_prototype prototype)
    //**********************************************************
    {
        String filename = make_prototype_path(face_recognizer_path, prototype.name()).toAbsolutePath().toString();
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename)))
        {
            writer.println(prototype.label());
            writer.println(prototype.feature_vector().features.length );
            for ( double d : prototype.feature_vector().features)
            {
                writer.println(d);
            }
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
    }


    //**********************************************************
    @Deprecated
    public static void display(Image image, String label)
    //**********************************************************
    {
        Stage stage = new Stage();
        stage.setTitle("Face Recognition Result");
        VBox vBox = new VBox();
        {
            ImageView iv = new ImageView(image);
            iv.setPreserveRatio(true);
            iv.setFitWidth(100);
            Pane image_pane = new StackPane(iv);
            vBox.getChildren().add(image_pane);
        }

        HBox hBox = new HBox();
        Label ll = new Label(label);
        hBox.getChildren().add(ll);
        vBox.getChildren().add(hBox);
        stage.setScene(new Scene(vBox));
        stage.show();
    }

    //**********************************************************
    static Path make_image_path(Path folder, String name, Logger logger)
    //**********************************************************
    {
        Path path =  Path.of(folder.toString(),name+".png");
        return path;
    }

    //**********************************************************
    public static Path write_tmp_image(Image face, Path folder_path, String name, Logger logger)
    //**********************************************************
    {
        Path path =  make_image_path(folder_path,name,logger);
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


}
