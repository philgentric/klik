package klik.face_recognition;

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
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.icons.JavaFX_to_Swing;
import klik.files_and_paths.Guess_file_type;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

//**********************************************************
public class oldFace_recognizer
//**********************************************************
{
    public static final boolean dbg = false;
    public final static String EXTENSION_FOR_EP = "prototype";
    public static final int K_of_KNN = 5;
    private static oldFace_recognizer instance = null;
    private final Logger logger;
    private final Browser browser;
    List<Embeddings_prototype> embeddings_prototypes = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    Map<String,Embeddings_prototype> names_to_embeddings = new HashMap<>();
    public final String face_recognizer_name;
    public final Path face_recognizer_path;

    //**********************************************************
    private oldFace_recognizer(String name_, Browser browser)
    //**********************************************************
    {
        face_recognizer_name = name_;
        this.browser = browser;
        this.logger = browser.browser_ui.logger;
        this.face_recognizer_path = Static_application_properties.get_absolute_dir_on_user_home(face_recognizer_name,true, logger);
        Browser_creation_context.additional_different_folder(face_recognizer_path,browser,logger);
    }


    //**********************************************************
    public static oldFace_recognizer get_instance(Browser browser)
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
        instance = new oldFace_recognizer(local, browser);
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
        oldFace_recognizer fr = oldFace_recognizer.get_instance(browser);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                fr.auto_internal(browser);
            }
        };
        Actor_engine.execute(r,new Aborter("auto face recog",fr.logger), fr.logger);
    }

    private Recognition_stats recognition_stats;
    private Training_stats training_stats;
    long last_report;

    //**********************************************************
    public void auto_internal(Browser browser)
    //**********************************************************
    {
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
            process_folder(f,label);
        }
    }

    //**********************************************************
    private void process_folder(File dir, String label)
    //**********************************************************
    {
        logger.log("auto train folder: "+dir.getAbsolutePath());
        File files[] = dir.listFiles();
        for ( File f: files)
        {
            if ( f.isDirectory()) process_folder(f,label);
            if (Guess_file_type.is_file_an_image(f))
            {
                process_file(f, label);
                long now = System.currentTimeMillis();
                if (now-last_report> 10000)
                {
                    last_report = now;
                    logger.log("\n\n\n\n\n");
                    logger.log("Recognition:"+recognition_stats.to_string());
                    logger.log("Training:"+training_stats.to_string());
                }
            }
        }
        save_internal();
    }

    //**********************************************************
    private void process_file(File f, String label)
    //**********************************************************
    {
        logger.log("process_file FILE before: "+f.getAbsolutePath());
        Face_recognition_results result = recognize(f.toPath(), false);
        logger.log("process_file FILE after : "+f.getAbsolutePath()+ " "+ result.status);
        switch ( result.status)
        {
            case server_not_reacheable:
                logger.log("process_file:server_not_reacheable");
                recognition_stats.error.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.error.incrementAndGet();
                training_stats.done.incrementAndGet();
                return;
            case  error:
                logger.log("process_file:error");
                recognition_stats.error.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.error.incrementAndGet();
                training_stats.done.incrementAndGet();
                return;
            case no_face_detected:
                // happens a lot
                logger.log("process_file:no_face_detected");
                recognition_stats.done.incrementAndGet();
                recognition_stats.no_face_detected.incrementAndGet();
                training_stats.no_face_detected.incrementAndGet();
                training_stats.done.incrementAndGet();
                break;
            case face_detected:
                logger.log("process_file:should not happen 1");
                recognition_stats.error.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.error.incrementAndGet();
                training_stats.done.incrementAndGet();
                break;
            case no_feature_vector:
                logger.log("process_file:should not happen 2");
                recognition_stats.error.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.error.incrementAndGet();
                training_stats.done.incrementAndGet();
                break;
            case feature_vector_ready:
                logger.log("process_file:should not happen 2");
                recognition_stats.error.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.error.incrementAndGet();
                training_stats.done.incrementAndGet();
                break;
            case face_recognized :
                logger.log("process_file:face_recognized");
                recognition_stats.face_recognized.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                if ( label.equals(result.label))
                {
                    training_stats.face_correctly_recognized_not_recorded.incrementAndGet();
                    training_stats.done.incrementAndGet();
                    logger.log("skipping "+f.getAbsolutePath()+" label was correct: "+label);
                    return;
                }
                logger.log("adding "+f.getAbsolutePath()+" label was NOT correct: "+label);
                training_stats.face_wrongly_recognized_recorded.incrementAndGet();
                training_stats.done.incrementAndGet();
                add_prototype_to_set(f,label,result);
                break;
            case no_face_recognized :
                logger.log("process_file: NO face_recognized");
                recognition_stats.face_not_recognized.incrementAndGet();
                recognition_stats.done.incrementAndGet();
                training_stats.face_wrongly_recognized_recorded.incrementAndGet();
                training_stats.done.incrementAndGet();
                add_prototype_to_set(f,label,result);
                break;
            default:
                logger.log(Stack_trace_getter.get_stack_trace("process_file: should not happen"));
                break;

        }
    }



    //**********************************************************
    private void add_prototype_to_set(File f, String label, Face_recognition_results result)
    //**********************************************************
    {
        logger.log("ADDING "+f.getAbsolutePath()+" with label: "+label);
        train_force(f.toPath(),label);

    }

    //**********************************************************
    public List<String> get_prototype_labels()
    //**********************************************************
    {
        Collections.sort(labels);
        return new ArrayList<>(labels);
    }


    //**********************************************************
    public Face_recognition_status add_prototype_image_face(Image face, String label, boolean and_save)
    //**********************************************************
    {
        String name = label+ "_"+ UUID.randomUUID();
        Path path = write_tmp_image(face, face_recognizer_path, name,logger);
        Feature_vector fv = get_image_embeddings(path, logger);
        if ( fv ==null)
        {
            logger.log("FATAL: prototype not added as the feature vector is null");
            return Face_recognition_status.no_feature_vector;
        }

        Embeddings_prototype ep = new Embeddings_prototype(face, fv, label, name);
        names_to_embeddings.put(name,ep);
        embeddings_prototypes.add(ep);
        if ( !labels.contains(label)) labels.add(label);
        if ( and_save) save_ep(ep);
        logger.log("added prototype image face with label ="+label);
        //display(face,label);
        return Face_recognition_status.feature_vector_ready;
    }

    //**********************************************************
    private void save_ep(Embeddings_prototype prototype)
    //**********************************************************
    {
        String filename = Face_recognition_service.make_prototype_path(face_recognizer_path, prototype.name()).toAbsolutePath().toString();
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
    public boolean train_force(Path reference_face, String label)
    //**********************************************************
    {
        Face_detector.Face_detection_result status = Face_detector.detect_face(reference_face, false, logger);

        if (status.status() == Face_recognition_status.server_not_reacheable)
        {
            logger.log("Face_recognition_status.server_not_reacheable");
            return false;
        }
        if (status.status() != Face_recognition_status.face_detected)
        {
            logger.log("no face detected");
            return false;
        }

        add_prototype_image_face(status.image(),label, true);
        return true;
    }


    record Eval_result(String label, boolean enable_adding, String name, List<Image> list){};


    //**********************************************************
    private Eval_result eval(Path face, boolean verbose)
    //**********************************************************
    {
        Feature_vector fv = get_image_embeddings(face, logger);
        AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        Embeddings_prototype winner = null;

        ConcurrentSkipListMap<Double, Embeddings_prototype> nearests = new ConcurrentSkipListMap<>();
        CountDownLatch cdl = new CountDownLatch(embeddings_prototypes.size());
        for (Embeddings_prototype ep : embeddings_prototypes)
        {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    double distance = fv.distance(ep.feature_vector());
                    long local_distance = Double.doubleToLongBits(distance);
                    long local_min = min.get();
                    if ( local_distance < local_min ) min.set(local_distance);
                    nearests.put(distance,ep);
                    if ( verbose) logger.log("   at distance ="+(int)distance+"  =>   "+ep.label());
                    cdl.countDown();
                }
            };
            Actor_engine.execute(r,new Aborter("disatnces",logger),logger);
        }
        try {
            cdl.await(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.log("EVAL TIMEOUT "+e);
        }
        winner = nearests.get(Double.longBitsToDouble(min.get()));
        logger.log("min distance :"+min.get());
        if ( min.get() == 0)
        {
            logger.log("1 nearest "+winner.label()+ " at "+min.get());
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


        if ( winner != null) logger.log("1 nearest "+winner.label()+ " at "+min);
        logger.log("5 nearest "+label5);


        return new Eval_result(label5,true,null,list_of_faces);
    }


    //**********************************************************
    private void show_face_recognition_window(
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

        VBox vBox = new VBox();
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

            comboBox.getItems().addAll(oldFace_recognizer.get_instance(browser).get_prototype_labels());
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
                Face_recognition_status s = add_prototype_image_face(face, image_label.trim(),false);
                if (s != Face_recognition_status.feature_vector_ready)
                {
                    status_label.setText("prototype fabrication error "+s);
                }
                else
                {
                    save_internal();
                    stage.close();
                }
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
            this.embeddings_prototypes.add(ep);
            if ( !labels.contains(label)) labels.add(label);
        }
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
    private static Path make_prototype_path(Path folder_path, String name)
    //**********************************************************
    {
        return Path.of(folder_path.toAbsolutePath().toString() , name+"."+EXTENSION_FOR_EP);
    }




    //**********************************************************
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

    record Face_recognition_results(Image image, String label, Face_recognition_status status){}
    //**********************************************************
    public Face_recognition_results recognize(Path tested, boolean verbose)
    //**********************************************************
    {
        Face_detector.Face_detection_result status = Face_detector.detect_face(tested, verbose, logger);
        if (status.status() == Face_recognition_status.server_not_reacheable)
        {
            if ( verbose) Face_detector.warn_about_face_detector_server(logger);
            return new Face_recognition_results(null, null,Face_recognition_status.server_not_reacheable);
        }
        if (status.status() != Face_recognition_status.face_detected)
        {
            if ( verbose) show_face_recognition_window(null,null,false, null, new ArrayList<>());
            return new Face_recognition_results(null,null,Face_recognition_status.no_face_detected);
        }
        Image face = status.image();

        if (face == null)
        {
            if ( verbose) Face_detector.warn_about_no_face_detected(logger);
            else logger.log("no face dtetcetd");
            return new Face_recognition_results(null,null,Face_recognition_status.no_face_detected);
        }

        if ( face == null)
        {
            logger.log("NO face detected");
            return new Face_recognition_results(null,null,Face_recognition_status.no_face_detected);
        }

        logger.log("Face detected");

        String name = "tmp_unknown_face"+ "_"+ UUID.randomUUID();
        Path tmp_image_reco = Static_application_properties.get_trash_dir(face_recognizer_path,logger);
        Path path_to_face = oldFace_recognizer.write_tmp_image(face, tmp_image_reco,name,logger);

        Eval_result eval_result = eval(path_to_face, verbose);
        if (verbose) show_face_recognition_window(face,eval_result.label(),eval_result.enable_adding(), eval_result.name(), eval_result.list());

        String display_label = eval_result.label();
        if ( eval_result.label() == null)
        {
            display_label = "not recognized";
        }
        logger.log("face reco result = "+display_label);
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
