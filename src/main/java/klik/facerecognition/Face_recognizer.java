package klik.facerecognition;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klik.browser.Browser;
import klik.browser.icons.JavaFX_to_Swing;
import klik.files_and_paths.Files_and_Paths;
import klik.images.Image_window;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

//**********************************************************
public class Face_recognizer
//**********************************************************
{
    public final static String EXTENSION_FOR_EP = "prototype";
    private static Face_recognizer instance = null;
    private final Logger logger;
    private final Browser browser;
    List<Embeddings_prototype> embeddings_prototypes = new ArrayList<>();
    List<String> labels = new ArrayList<>();

    public static void reset()
    {
        if ( instance != null) {
            instance.embeddings_prototypes.clear();
            instance.labels.clear();
        }
    }

    public List<String> get_prototype_labels() {
        return new ArrayList<>(labels);
    }

    //**********************************************************
    public static Face_recognizer get_instance(Browser browser, Logger logger)
    //**********************************************************
    {
        if ( instance == null) {
            instance = new Face_recognizer(browser, logger);
        }
        return instance;
    }

    //**********************************************************
    private Face_recognizer(Browser browser, Logger logger)
    //**********************************************************
    {
        this.browser = browser;
        this.logger = logger;
    }

    //**********************************************************
    public void add_prototype_image_face(Image face, String label)
    //**********************************************************
    {
        String name = label+ "_"+ UUID.randomUUID();
        Path path = write_tmp_image(face, name,logger);
        Feature_vector fv = get_image_embeddings(path, logger);
        embeddings_prototypes.add(new Embeddings_prototype(face, fv, label, name));
        if ( !labels.contains(label)) labels.add(label);
        logger.log("added prototype image face with label ="+label);
        display(face,label);
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


    public void train(Path reference_face)
    {
        Image face = Face_detector.detect_face(reference_face, logger);

        Stage stage = new Stage();
        stage.setTitle("Face Detection Result");
        VBox vBox = new VBox();
        {
            if ( face == null) {
                Label label = new Label("No face detected in this image");
                vBox.getChildren().add(label);
                stage.setScene(new Scene(vBox));
                stage.show();
                return;
            }
            ImageView iv = new ImageView(face);
            iv.setPreserveRatio(true);
            iv.setFitWidth(100);
            Pane image_pane = new StackPane(iv);
            vBox.getChildren().add(image_pane);
        }
        TextField textField = new TextField();
        {
            HBox hBox = new HBox();
            Label label = new Label("Enter the recognition label from list:");
            hBox.getChildren().add(label);

            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.getItems().addAll(Face_recognizer.get_instance(browser, logger).get_prototype_labels());
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
        {
            HBox hBox = new HBox();
            Button add = new Button("Add to training set");
            add.setOnAction(e -> {
                String image_label = textField.getText();
                add_prototype_image_face(face, image_label);
                stage.close();
            });
            hBox.getChildren().add(add);
            Button skip = new Button("Skip this face if the above picture is NOT a representative face");
            skip.setOnAction(e -> {
                stage.close();
            });
            hBox.getChildren().add(skip);
            vBox.getChildren().add(hBox);
        }
        stage.setScene(new Scene(vBox));
        stage.show();

    }

    //**********************************************************
    public String eval(Path face)
    //**********************************************************
    {
        Feature_vector fv = get_image_embeddings(face, logger);
        double min = Double.MAX_VALUE;
        String label = null;

        ConcurrentSkipListMap<Double, Embeddings_prototype> nearests = new ConcurrentSkipListMap<>();

        for (Embeddings_prototype ep : embeddings_prototypes)
        {
            double distance = fv.distance(ep.feature_vector());
            nearests.put(distance,ep);
            logger.log("          distance ="+distance);
            if (distance < min) {
                logger.log("best distance : "+ep.label()+" at: "+distance);
                label = ep.label();
                min = distance;
            }
        }
        // vote
        Map<String,Integer> votes = new HashMap<>();
        int count = 0;
        for (Map.Entry<Double,Embeddings_prototype> e : nearests.entrySet())
        {
            double distance = e.getKey();
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
            count++;
            if ( count > 5) break;
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

        logger.log("1 nearest "+label+ " at "+min);
        logger.log("5 nearest "+label5);
        return label5;
    }

    //**********************************************************
    public void save()
    //**********************************************************
    {
        for (Embeddings_prototype ep : embeddings_prototypes)
        {
            save_ep(ep);
        }
    }
    //**********************************************************
    public void load()
    //**********************************************************
    {
        reset();
        Path p = Path.of(Files_and_Paths.get_face_recognition_cache_dir(logger).toAbsolutePath().toString());

        File[] files = p.toFile().listFiles();
        for (File f: files)
        {
            if ( f.isDirectory()) continue;
            String ext = FilenameUtils.getExtension(f.getName());
            if ( !ext.equals(EXTENSION_FOR_EP)) continue;

            Embeddings_prototype ep = load_ep(f);
            this.embeddings_prototypes.add(ep);
            String label = ep.label();
            if ( !labels.contains(label)) labels.add(label);
        }
    }

    //**********************************************************
    private Embeddings_prototype load_ep(File f)
    //**********************************************************
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(f)))
        {
            String label = null;
            String name = null;

            {
                String line =  reader.readLine();
                if ( line == null)
                {
                    logger.log("error reading ep label");
                    return null;
                }
                label = line.trim();
            }
            {
                String line =  reader.readLine();
                if ( line == null)
                {
                    logger.log("error reading ep name");
                    return null;
                }
                name = line.trim();
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
                values[i] = Double.valueOf(line.trim());
            }
            Feature_vector fv = new Feature_vector(values);
            // now read the face from the same folder
            Image face = new Image(make_image_path(name,logger).toAbsolutePath().toString());

            return new Embeddings_prototype(face, fv, label, name);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //**********************************************************
    private void save_ep(Embeddings_prototype prototype)
    //**********************************************************
    {
        Path p = Path.of(Files_and_Paths.get_face_recognition_cache_dir(logger).toAbsolutePath().toString() , prototype.name()+"."+EXTENSION_FOR_EP);
        String filename = p.toAbsolutePath().toString();
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename)))
        {
            writer.println(prototype.label());
            writer.println(prototype.name());
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
    static Path make_image_path(String name, Logger logger)
    //**********************************************************
    {
        Path cache_dir = Files_and_Paths.get_face_recognition_cache_dir(logger);
        Path path =  Path.of(cache_dir.toString(),name+".png");
        return path;
    }

    //**********************************************************
    public static Path write_tmp_image(Image face, String name, Logger logger)
    //**********************************************************
    {
        Path path =  make_image_path(name,logger);
        try {
            BufferedImage bi = JavaFX_to_Swing.fromFXImage(face, null, logger);
            ImageIO.write(bi, "png", path.toFile());
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return null;
        }
        return path;
    }

    //**********************************************************
    public String recognize(Path tested)
    //**********************************************************
    {
        Image face = Face_detector.detect_face(tested,logger);
        if ( face != null)
        {
            if ( face.getWidth() > 0)
            {
                logger.log("Face detected");
                Path path_to_face = Face_recognizer.write_tmp_image(face,"unknown",logger);
                String label = eval(path_to_face);
                logger.log("result = "+label);
                display(face, label);
                return label;
            }
        }
        logger.log("NO face detected");
        return "no face detected";
    }
}
