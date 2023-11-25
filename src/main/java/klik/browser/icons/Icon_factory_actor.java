package klik.browser.icons;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import klik.actor.*;
import klik.animated_gifs_from_videos.Animated_gif_generator;
import klik.browser.items.Iconifiable_item_type;
import klik.files_and_paths.Files_and_Paths;
import klik.experimental.performance_monitoring.Sample_collector;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


// an actor-style asynchronous icon factory
//**********************************************************
public class Icon_factory_actor implements Actor
//**********************************************************
{
    private static final boolean verbose = false;
    private static final boolean dbg = false;
    private static final boolean pdf_dbg = true;
    private static final boolean dbg_aborting = false;

    Logger logger;
    private final Stage owner;
    Icon_writer_actor writer;
    public Path icon_cache_dir;
    public static final String gif_extension = "gif";
    public static final String png_extension = "png";

    static Icon_factory_actor icon_factory = null;
    public final static Sample_collector sample_collector = new Sample_collector();
    ConcurrentLinkedQueue<Job> jobs;
    Job_termination_reporter termination_reporter;


    static public List<Path> videos_for_which_giffing_failed = new ArrayList<>();

    //**********************************************************
    public static void reset_videos_for_which_giffing_failed()
    //**********************************************************
    {
        videos_for_which_giffing_failed.clear();
    }
    //**********************************************************
    public static Icon_factory_actor get_icon_factory(Stage owner, Logger logger)
    //**********************************************************
    {
        if (icon_factory == null) {
            icon_factory = new Icon_factory_actor(owner, logger);
        }
        return icon_factory;
    }
    //**********************************************************
    private Icon_factory_actor(Stage owner_, Logger logger_)
    //**********************************************************
    {
        owner = owner_;
        logger = logger_;
        if (dbg) logger.log("Icon_factory created");
        icon_cache_dir = Files_and_Paths.get_icon_cache_dir(logger);
        writer = new Icon_writer_actor(icon_cache_dir, logger);
        //writer = Icon_writer_actor.launch_icon_writer(icon_cache_dir, logger);
        jobs = new ConcurrentLinkedQueue<>();
        termination_reporter = (message, job) -> jobs.remove(job);
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Icon_factory_request ifr = (Icon_factory_request) m;
        process(ifr);
        return "icon done";
    }



    //**********************************************************
    private void process(Icon_factory_request icon_factory_request)
    //**********************************************************
    {
        if (dbg) logger.log("icon request processing starts ");

        Icon_destination destination = icon_factory_request.destination;
        if (destination == null) {
            logger.log("icon factory : cancel! destination==null");
            return;
        }

        Image image = null;

        switch (destination.get_item_type()) {
            case video -> {
                image = process_video(icon_factory_request, destination);
                if (image == null) {
                    logger.log("process-video failed for " + icon_factory_request.destination.get_item_path());
                    videos_for_which_giffing_failed.add(icon_factory_request.destination.get_item_path());
                }
            }
            case pdf -> {
                if (dbg) logger.log(destination.get_item_path() + " type is PDF");
                image = process_pdf(icon_factory_request, destination);
            }
            default -> image = process_image(icon_factory_request, destination);
        }


        if (dbg) logger.log("Icon_factory icon ready");

        if (image == null) {
            // must treat as non_image
            if (dbg) logger.log("making an icon failed for : " + destination.get_item_path());
            return;
        }
        destination.receive_icon(image);
    }



    //**********************************************************
    public Job make_icon(Icon_factory_request icon_factory_request)
    //**********************************************************
    {
        if (dbg) logger.log("icon request made ");
        if (icon_factory_request.destination.get_icon_status() == Icon_status.true_icon_in_the_making) {
            if (dbg) logger.log("icon request : cancel, request already done ");
            return null;
        }
        if (icon_factory_request.destination.get_icon_status() == Icon_status.true_icon) {
            if (dbg) logger.log("icon request : cancel, icon already done ");
            return null;
        }
        if (dbg) logger.log("icon request : queued! ");

        Job job = Actor_engine.run(this, icon_factory_request, termination_reporter,logger);
        icon_factory_request.destination.set_icon_status(Icon_status.true_icon_in_the_making);
        jobs.add(job);
        return job;
    }
    //**********************************************************
    private Image process_image(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        Path p = destination.get_path_for_display();
        if (p == null)
        {
            //if (dbg)
                logger.log("Icon_factory thread: returning large folder icon because icon path is null for item:" + destination.get_string() + "\ntypically happens when there is no image to use as a icon in that folder");

            return null;//Look_and_feel_manager.get_large_folder_icon(icon_factory_request.icon_size);
        }
        if (icon_factory_request.aborter.should_abort())
        {
            if ( dbg_aborting) logger.log("Icon_factory thread: aborting1");
            return null;
        }
        String tag = String.valueOf(icon_factory_request.icon_size);
        if ( icon_factory_request.destination.get_item_type() == Iconifiable_item_type.pdf)
        {
            // for PDF we do not minimize the generated png, ImageView will do it
            // so a side effect is it is not necessary to regenerate it if the icon size was changed
            tag = "";
        }

        //long start = System.currentTimeMillis();
        Image image = From_disk.load_icon_from_disk_cache(p, icon_cache_dir, icon_factory_request.icon_size, tag, png_extension, dbg,logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( dbg_aborting) logger.log("Icon_factory thread: aborting2");
            return null;
        }
        if (image == null) {
            if (dbg) logger.log("Icon_factory thread:  load from cache FAILED for " + p.getFileName());

            image = From_disk.read_original_image_from_disk_and_return_icon(p, icon_factory_request.icon_size, icon_factory_request.aborter, dbg, logger);
            if (icon_factory_request.aborter.should_abort())
            {
                if ( dbg_aborting) logger.log("Icon_factory thread: aborting3");
                return null;
            }
            if (image == null) {
                if (dbg) logger.log("WARNING: Icon_factory thread: load from file FAILED for " + p.getFileName());
                return null;
            }

            //long pixels = (long)(image.getHeight()* image.getWidth());
            //sample_collector.add_sample(System.currentTimeMillis()-start,pixels);

            switch (destination.get_item_type()) {
                case image_gif:
                    // dont try to disk-cache for gifs, they are either small or animated
                case folder:
                    // no need for folders, the icon is generated and saved by imagemagic
                    break;
                default:
                    if (dbg)
                        logger.log("Icon_factory thread: sending icon write to file in cache dir for " + p.getFileName());
                    Icon_write_message iwm = new Icon_write_message(image, icon_factory_request.icon_size, png_extension, p);
                    writer.push(iwm);
                    break;
            }
        } else {
            if (dbg) logger.log("Icon_factory thread: found in cache: " + p.getFileName());
        }
        return image;
    }

    //**********************************************************
    private Image process_video(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        if (verbose) logger.log("Icon_factory thread:  process_video " + destination.get_item_path().toAbsolutePath());

        // we are going to create an animated gif using ffmpeg
        // ... unless it is already in the icon cache
        String tag = ""; // empty since we do not resize the frames to icon size
        Image image = From_disk.load_icon_from_disk_cache(destination.get_item_path(), icon_cache_dir, icon_factory_request.icon_size, tag,gif_extension, dbg, logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( dbg_aborting) logger.log("Icon_factory thread: aborting4");
            return null;
        }
        if ( image != null)
        {
            if (dbg) logger.log("Icon_factory thread: found in cache: " + destination.get_item_path().getFileName());
            return image;
        }

        if (dbg)
            logger.log("Icon_factory thread:  load from GIF tmp FAILED for " + destination.get_item_path());
        int length = Static_application_properties.get_animated_gif_duration_for_a_video(logger);

        File gif_animated_icon_file = From_disk.file_for_icon_cache(icon_cache_dir, destination.get_path_for_display(), tag, gif_extension);
        //File gif_animated_icon_file = From_disk.file_for_cache(icon_cache_dir, destination.get_icon_path(), ""+icon_factory_request.icon_size+"_"+length, gif_extension);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( dbg_aborting) logger.log("Icon_factory thread: aborting5");
            return null;
        }

        Path destination_gif_full_path = Paths.get(icon_cache_dir.toAbsolutePath().toString(), gif_animated_icon_file.getName());

        int skip = 0;
        int duration_in_seconds = Animated_gif_generator.get_video_duration(owner, destination.get_item_path(), logger);
        if ( duration_in_seconds > 3*3600)
        {
            logger.log("WARNING: ffprobe reports duration that looks wrong");
            duration_in_seconds = 3600/2;
        }

        if ( duration_in_seconds < 0)
        {
            duration_in_seconds = length;
        }

        if (duration_in_seconds < length)
        {
            length = duration_in_seconds;
        }
        else
        {
            // jump to the middle of the movie
            skip = duration_in_seconds / 2 - length;
        }

        Animated_gif_generator.video_to_gif(owner, destination.get_item_path(), destination_gif_full_path, length, skip, icon_factory_request.get_aborter(),logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( dbg_aborting) logger.log("Icon_factory thread: aborting6");
            return null;
        }

        if (destination_gif_full_path.toFile().length() == 0)
        {
            logger.log("ERROR animated gif empty " + destination_gif_full_path.toAbsolutePath());
            return null;
        }
        if (verbose)
            logger.log("Icon_factory Animated gif icon MADE for " + destination.get_item_path().getFileName() + " as " + destination_gif_full_path.toAbsolutePath());
        image = From_disk.load_icon_from_disk_cache(destination.get_path_for_display(), icon_cache_dir, icon_factory_request.icon_size, tag,gif_extension, dbg, logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( dbg_aborting) logger.log("Icon_factory thread: aborting7");
            return null;
        }
        if (image == null)
        {
            logger.log("Icon_factory thread: load from file FAILED (1) for " + destination.get_item_path().getFileName());
            return null;
        }
        if ((image.getHeight() == 0) && (image.getWidth() == 0))
        {
            logger.log("Icon_factory thread: load from file FAILED (2) for " + destination.get_item_path().getFileName());
            return null;
        }
        //logger.log("Icon_factory returning image for :" + destination.get_item_path().getFileName());

        return image;
    }



    //**********************************************************
    private Image process_pdf(Icon_factory_request icon_factory_request, Icon_destination item_image)
    //**********************************************************
    {
        if (pdf_dbg) logger.log("Icon_factory thread:  process_pdf " + item_image.get_item_path().toAbsolutePath());

        // we are going to create the PNG using pdfbox!

        String tag = "";//+icon_factory_request.icon_size;
        Image image_from_cache = From_disk.load_icon_from_disk_cache(item_image.get_path_for_display(), icon_cache_dir, icon_factory_request.icon_size,tag, png_extension, dbg,logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( dbg_aborting) logger.log("Icon_factory thread: aborting8");
            return null;
        }
        if (image_from_cache != null)
        {
            if (pdf_dbg) logger.log("Icon_factory thread: found in cache for: " + item_image.get_item_path().getFileName());
            return image_from_cache;
        }

        if (pdf_dbg)
            logger.log("Icon_factory thread:  load from disk cache FAILED for " + item_image.get_item_path().getFileName() + " MAKING IT NOW");

        File file_in = item_image.get_item_path().toFile();
        File resulting_png_name = From_disk.file_for_icon_cache(icon_cache_dir, item_image.get_item_path(), tag, png_extension);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( dbg_aborting) logger.log("Icon_factory thread: aborting9");
            return null;
        }

        String deb = null;
        try (PDDocument document = Loader.loadPDF(file_in, deb))
        {
            if (icon_factory_request.aborter.should_abort())
            {
                if ( dbg_aborting) logger.log("Icon_factory thread: aborting10");
                return null;
            }
            if (pdf_dbg) logger.log("Icon_factory thread:  PDF loaded");
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null && acroForm.getNeedAppearances()) {
                acroForm.refreshAppearances();
            }
            PDFRenderer renderer = new PDFRenderer(document);
            renderer.setSubsamplingAllowed(true);
            int i = 0;
            {
                int dpi = Static_application_properties.get_icon_size(logger);
                BufferedImage image = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                if (icon_factory_request.aborter.should_abort())
                {
                    if ( dbg_aborting) logger.log("Icon_factory thread: aborting11");
                    return null;
                }
                try (OutputStream output = new BufferedOutputStream(new FileOutputStream(resulting_png_name))) {
                    ImageOutputStream imageOutput = null;
                    ImageWriter writer = null;
                    try {
                        // find suitable image writer
                        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(png_extension);
                        ImageWriteParam param = null;
                        IIOMetadata metadata = null;
                        // Loop until we get the best driver, i.e. one that supports
                        // setting dpi in the standard metadata format; however we'd also
                        // accept a driver that can't, if a better one can't be found
                        while (writers.hasNext()) {
                            if (icon_factory_request.aborter.should_abort())
                            {
                                if ( dbg_aborting) logger.log("Icon_factory thread: aborting12");
                                return null;
                            }
                            if (writer != null) {
                                writer.dispose();
                            }
                            writer = writers.next();
                            if (writer != null) {
                                param = writer.getDefaultWriteParam();
                                metadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), param);
                                if (metadata != null
                                        && !metadata.isReadOnly()
                                        && metadata.isStandardMetadataFormatSupported()) {
                                    break;
                                }
                            }
                        }
                        if (writer == null) {
                            logger.log("No ImageWriter found for jpeg format");
                            return null;
                        }
                        // compression
                        if (param != null && param.canWriteCompressed()) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            {
                                param.setCompressionType(param.getCompressionTypes()[0]);
                                param.setCompressionQuality(1);
                            }
                        }
                        imageOutput = ImageIO.createImageOutputStream(output);
                        if (icon_factory_request.aborter.should_abort())
                        {
                            if ( dbg_aborting) logger.log("Icon_factory thread: aborting13");
                            return null;
                        }
                        writer.setOutput(imageOutput);
                        writer.write(null, new IIOImage(image, null, metadata), param);
                        if (pdf_dbg) logger.log("image of PDF write done (1) " + resulting_png_name);
                    } finally {
                        if (writer != null) {
                            writer.dispose();
                        }
                        if (imageOutput != null) {
                            imageOutput.close();
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            logger.log("Error converting document [" + ioe.getClass().getSimpleName() + "]: " + ioe.getMessage());
            return null;
        }

        if (pdf_dbg) logger.log("image of PDF write done (2)" + resulting_png_name);

        image_from_cache = From_disk.load_icon_from_disk_cache(item_image.get_item_path(), icon_cache_dir, icon_factory_request.icon_size, tag, png_extension, dbg, logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( dbg_aborting) logger.log("Icon_factory thread: aborting14");
            return null;
        }

        if (image_from_cache == null) {
            logger.log("Icon_factory thread: load from file FAILED for " + item_image.get_item_path().getFileName());
        }
        else
        {
            if (pdf_dbg) logger.log("image of PDF found on disk OK (3)" + resulting_png_name);
        }
        return image_from_cache;
    }



    //**********************************************************
    public void cancel_all()
    //**********************************************************
    {
        Actor_engine.get(logger).cancel_all(jobs);
        jobs.clear();
    }
}
