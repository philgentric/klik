package klik.facerecognition;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.files_and_paths.Guess_file_type;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;


//**********************************************************
public class MyFaceRecognizer
//**********************************************************
{

    private static MyFaceRecognizer instance;
    private CascadeClassifier faceDetector;
    Trainer trainer;
    public final Logger logger;
    private boolean trained = false;

    //**********************************************************
    public MyFaceRecognizer(Logger logger)
    //**********************************************************
    {
        this.logger = logger;
    }

    //**********************************************************
    public static MyFaceRecognizer get_instance(Logger logger)
    //**********************************************************
    {
        if ( instance != null) return instance;
        instance = new MyFaceRecognizer(logger);
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
        try
        {
            logger.log("NATIVE_LIBRARY_NAME="+ Core.NATIVE_LIBRARY_NAME);
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        }
        catch (UnsatisfiedLinkError e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return false;
        }
        faceDetector = new CascadeClassifier("haarcascade_frontalface_default.xml");

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
        int count  = 0;
        for (;;)
        {
            Prototype prototype = prototypes.poll();
            if ( prototype == null) break;
            trainer.add(prototype.matrix(), prototype.label());
            count++;
            if ( count%1000 == 0) logger.log("trainer: "+count);
        }
        logger.log("trainer: "+count);

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
            if (f.isDirectory())
            {
                add_all_pictures_to_training_set_in_a_thread(f.toPath());
            }
            else {
                if ( count_images < 4)
                {
                    if (Guess_file_type.is_file_an_image(f)) {
                        add_to_training_set_thread_safe(f.toPath(), label);
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
        logger.log("training with:"+label);
        Matrix vector = image_to_vector(p);
        if (vector == null) return;
        Prototype prototype = new Prototype(vector,label);
        prototypes.add(prototype);
    }

    //**********************************************************
    public boolean add_to_training_set(Path p, String label)
    //**********************************************************
    {
        logger.log("training with:"+label);
        Matrix vector = image_to_vector(p);
        if (vector == null) return false;
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
        Mat gray = to_grey(p);
        Mat face = extract_face(gray,faceDetector,logger);
        if ( face == null) return null;
        Matrix matrix = make_matrix(face,logger);
        if ( matrix == null) return null;

        Matrix vector = TrainerTest.vectorize(matrix);
        return vector;
    }


    //**********************************************************
    private static Mat extract_face(Mat gray, CascadeClassifier faceDetector, Logger logger)
    //**********************************************************
    {
        MatOfRect faceRects = detect_face(gray,faceDetector);
        if (faceRects.toArray().length == 0) { // No faces detected
            logger.log("No faces detected in the image.");
            return null; // or throw an exception, depending on your requirements
        }
        Mat face = new Mat(gray.rows(), gray.cols(), CvType.CV_8U);
        gray.submat(faceRects.toArray()[0]).copyTo(face);
        logger.log("face info: " + face.type());
        if (face.empty())
        {
            System.err.println("Error: face is empty!");
            return null;
        }
        else
        {
            int dataType = face.type();
            if (dataType != CvType.CV_8U && dataType != CvType.CV_32F) {
                System.err.println("Error: Incompatible face1 data type: " + dataType);
            }
        }
        return face;
    }

    //**********************************************************
    private static MatOfRect detect_face(Mat gray, CascadeClassifier faceDetector)
    //**********************************************************
    {
        MatOfRect faceRects = new MatOfRect();
        synchronized (faceDetector) {
            faceDetector.detectMultiScale(gray, faceRects);
        }
        return faceRects;
    }

    //**********************************************************
    static Mat to_grey(Path path)
    //**********************************************************
    {
        Mat image = Imgcodecs.imread(path.toAbsolutePath().toString());
        // Convert images to grayscale
        Mat grey = new Mat();//image.rows(), image.cols(), CvType.CV_8U);
        Imgproc.cvtColor(image, grey, Imgproc.COLOR_BGR2GRAY);
        return grey;
    }


    //**********************************************************
    static Matrix make_matrix(Mat face_in, Logger logger)
    //**********************************************************
    {
        Mat face = resize(face_in);
        int picHeight = face.rows();
        logger.log("picHeight"+picHeight);
        int picWidth = face.cols();
        logger.log("picWidth"+picWidth);
        double[][] data2D = new double[picHeight][picWidth];
        byte[] pixels = new byte[picHeight * picWidth];
        face.get(0, 0, pixels); // Get all pixels at once
        int idx = 0;
        for (int row = 0; row < picHeight; row++) {
            for (int col = 0; col < picWidth; col++) {
                data2D[row][col] = (double) pixels[idx++] / 255.0;
            }
        }
        return new Matrix(data2D);
    }

    //**********************************************************
    private static Mat resize(Mat face)
    //**********************************************************
    {
        int targetWidth = 100;
        int targetHeight = 100;

        // Resize the face to the target size
        Mat resized_face = new Mat();
        Imgproc.resize(face, resized_face, new Size(targetWidth, targetHeight));

        return resized_face;
    }
}