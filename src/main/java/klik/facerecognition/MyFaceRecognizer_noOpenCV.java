package klik.facerecognition;

import klik.Klik_application;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.facerecognition.violajones.Viola_Jones_detector;
import klik.files_and_paths.Guess_file_type;
import klik.images.Simple_image_window;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


//**********************************************************
public class MyFaceRecognizer_noOpenCV
//**********************************************************
{
    private static final int SMALL_SIZE = 100;
    //private static final double MAX_IMAGE_SIZE_FOR_FACE_DETECTION = 512;
    private static MyFaceRecognizer_noOpenCV instance;
    private Viola_Jones_detector viola_jones_detector;
    Trainer trainer;
    public final Logger logger;
    private boolean trained = false;

    //**********************************************************
    public MyFaceRecognizer_noOpenCV(Logger logger)
    //**********************************************************
    {
        this.logger = logger;
    }

    //**********************************************************
    public static MyFaceRecognizer_noOpenCV get_instance(Logger logger)
    //**********************************************************
    {
        if ( instance != null) return instance;
        instance = new MyFaceRecognizer_noOpenCV(logger);
        if ( !instance.init(logger))
        {
            instance = null;
        }
        return instance;
    }

    //**********************************************************
    public static void reset()
    //**********************************************************
    {
        instance = null;
    }

    //**********************************************************
    private boolean init(Logger logger)
    //**********************************************************
    {
        InputStream haarXml = Klik_application.class.getResourceAsStream("haarcascade_frontalface_default.xml");
        if ( haarXml == null)
        {
            logger.log("cannot read ressource haarcascade_frontalface_default.xml");
            return false;
        }
        viola_jones_detector = new Viola_Jones_detector(haarXml);
        trainer = Trainer.builder()
                .metric(new CosineDissimilarity())
                .featureType(FeatureType.LDA)
                .numberOfComponents(3)
                .k(1);
        return true;
    }

    //**********************************************************
    public void add_all_pictures_to_training_set(Path folder)
    //**********************************************************
    {
        add_all_pictures_to_training_set_in_a_thread(folder);
    }

    //**********************************************************
    public void train()
    //**********************************************************
    {
        int prototype_count  = 0;
        for (;;)
        {
            Prototype prototype = prototypes.poll();
            if ( prototype == null) break;
            trainer.add(prototype.matrix(), prototype.label());
            prototype_count++;
            if ( prototype_count%1000 == 0) logger.log("trainer prototype_count: "+prototype_count);
        }
        logger.log("trainer prototype_count: "+prototype_count);

        if ( prototype_count == 0)
        {
            logger.log("training failed, no prototypes?");
            return;
        }

        try {
            if ( !trainer.train()) return;
        } catch (Exception e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        trained = true;
    }

    //**********************************************************
    private void add_all_pictures_to_training_set_in_a_thread(Path folder)
    //**********************************************************
    {
        Runnable r = () -> add_all_pictures_to_training_set_internal(folder);
        Actor_engine.execute(r,new Aborter("adding picture to face reco training set",logger),logger);
    }


    //**********************************************************
    private void add_all_pictures_to_training_set_internal(Path folder)
    //**********************************************************
    {

        String label = folder.getFileName().toString();
        File files[] = folder.toFile().listFiles();

        int count_images = 0;
        for ( File f : files)
        {
            logger.log("looking at file: "+f.getAbsolutePath());
            if (f.isDirectory())
            {
                add_all_pictures_to_training_set_in_a_thread(f.toPath());
            }
            else {
                //if ( count_images < 4)
                {
                    if (Guess_file_type.is_file_an_image(f)) {
                        Runnable r = () -> add_to_training_set_thread_safe(f.toPath(), label);
                        Actor_engine.execute(r,new Aborter("kiki22",logger),logger);
                        count_images++;
                    }
                }
            }
        }
    }

    record Prototype(Matrix matrix, String label){}
    ConcurrentLinkedQueue<Prototype> prototypes = new ConcurrentLinkedQueue<>();
    //**********************************************************
    public void add_to_training_set_thread_safe(Path p, String label)
    //**********************************************************
    {
        logger.log("add_to_training_set_thread_safe() with:"+p+" label: "+label);

        Matrix vector = image_to_vector(p);
        if (vector == null)
        {
            logger.log("no vector ? NOT training with:"+p+" label: "+label);
            return;
        }
        logger.log("adding prototype to LIST:"+p+" label: "+label);
        Prototype prototype = new Prototype(vector,label);
        prototypes.add(prototype);
    }

    //**********************************************************
    public boolean add_to_training_set(Path p, String label)
    //**********************************************************
    {
        logger.log("training with:"+label);
        Matrix vector = image_to_vector(p);
        if (vector == null)
        {
            logger.log("no vector ? NOT training with:"+p+" label: "+label);
            return false;
        }
        logger.log("training with prototype:"+p+" label: "+label);
        trainer.add(vector, label);
        return true;
    }
    //**********************************************************
    public String eval(Path p)
    //**********************************************************
    {
        if ( !trained)
        {
            train();
        }
        Matrix vector = image_to_vector(p);
        if (vector == null) return null;
        String result = trainer.recognize(vector);
        logger.log("Eval result:"+result);
        return  result;
    }



    //**********************************************************
    private Matrix image_to_vector(Path p)
    //**********************************************************
    {
        BufferedImage face = extract_face(p);
        if ( face == null)
        {
            logger.log("OHO no face detect in "+p);
            return null;
        }
        Matrix vector = resize_and_vectorize(face);
        if ( vector == null)
        {
            logger.log("conversion failed for "+p);
            return null;
        }
        return vector;
    }



    //**********************************************************
    public BufferedImage extract_face(Path p)
    //**********************************************************
    {
        BufferedImage gray = to_grey(p);
        {
            Simple_image_window.open_Simple_image_window(gray,"gray version",logger);

        }
        List<Rectangle> list = detect_face(gray);
        if (list == null) { // No faces detected
            logger.log("No faces detected (1) in the image.");
            return null; // or throw an exception, depending on your requirements
        }
        if (list.size() == 0) { // No faces detected
            logger.log("No faces detected (2) in the image.");
            return null; // or throw an exception, depending on your requirements
        }

        for(int i = 0;i < list.size(); i++)
        {
            Rectangle r = list.get(i);
            logger.log("Face detected in the image."+r.toString());
            BufferedImage extracted = gray.getSubimage(r.x,r.y,r.width,r.height);
            Simple_image_window.open_Simple_image_window(extracted, "Rectangle "+i, logger);
        }

        Rectangle r = list.get(0);

        BufferedImage originalImage = null;
        try {
            originalImage = ImageIO.read(new FileInputStream(p.toFile()));
            Simple_image_window.open_Simple_image_window(originalImage, "original version", logger);

            BufferedImage face_local =  originalImage.getSubimage(r.x,r.y,r.width,r.height);
            Simple_image_window.open_Simple_image_window(face_local, "face in original version", logger);

        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

        BufferedImage extracted = gray.getSubimage(r.x,r.y,r.width,r.height);
        Simple_image_window.open_Simple_image_window(extracted, "gray version", logger);

        return extracted;
    }

    //**********************************************************
    public List<Rectangle> detect_face(BufferedImage img)
    //**********************************************************
    {
        List<Rectangle> res = null;
        synchronized (viola_jones_detector) {
             res = viola_jones_detector.getFaces(img, 1, 1.25f, 0.1f,1,true);
        }
        return res;
    }


    //**********************************************************
     BufferedImage to_grey(Path path)
    //**********************************************************
    {
        BufferedImage originalImage = null;
        try {
            originalImage = ImageIO.read(new FileInputStream(path.toFile()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        int w = originalImage.getWidth();
        int h = originalImage.getHeight();
        /*
        double fac = 1.0;
        if ( w > MAX_IMAGE_SIZE_FOR_FACE_DETECTION)
        {
            w = (int)MAX_IMAGE_SIZE_FOR_FACE_DETECTION;
            fac = MAX_IMAGE_SIZE_FOR_FACE_DETECTION/(double)originalImage.getWidth();
            h = (int)((double)originalImage.getHeight()*fac);
        }
         */
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = gray.getGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        return gray;
    }



    private Matrix resize_and_vectorize(BufferedImage face)
    {
        BufferedImage resized = new BufferedImage(SMALL_SIZE, SMALL_SIZE, BufferedImage.TYPE_INT_RGB);

        Graphics g = resized.createGraphics();
        g.drawImage(face, 0, 0, SMALL_SIZE, SMALL_SIZE, null);
        g.dispose();

        int w = resized.getWidth();
        logger.log(face.getWidth()+" resized width: "+w);

        int h = resized.getHeight();
        logger.log(face.getHeight()+" resized height:  "+h);
        Matrix result = new Matrix(h * w, 1);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                result.set(x * h + y, 0, resized.getRGB(x, y));
            }
        }
        return result;
    }
}