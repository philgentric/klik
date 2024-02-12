package klik.browser.icons;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import klik.actor.*;
import klik.browser.icons.animated_gifs.Ffmpeg_utils;
import klik.browser.Image_and_rotation;
import klik.browser.icons.caches.Aspect_ratio_cache;
import klik.browser.icons.caches.Rotation_cache;
import klik.browser.items.Iconifiable_item_type;
import klik.files_and_paths.Files_and_Paths;
import klik.properties.Static_application_properties;
import klik.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
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
import java.util.concurrent.atomic.AtomicInteger;


// an actor-style asynchronous icon factory
//**********************************************************
public class Icon_factory_actor implements Actor
//**********************************************************
{
    private static final boolean verbose_dbg = false;
    private static final boolean dbg = false;
    private static final boolean pdf_dbg = false;
    private static final boolean aborting_dbg = false;
    private final Aspect_ratio_cache aspect_ratio_cache;
    private final Rotation_cache rotation_cache;

    Logger logger;
    private final Stage owner;
    Icon_writer_actor writer;
    public Path icon_cache_dir;
    public static final String gif_extension = "gif";
    public static final String png_extension = "png";
    private final Aborter aborter;


    public List<Path> videos_for_which_giffing_failed = new ArrayList<>();

    //private static AtomicInteger instance = new AtomicInteger(0);
    //**********************************************************
    public  void reset_videos_for_which_giffing_failed()
    //**********************************************************
    {
        videos_for_which_giffing_failed.clear();
    }


    //**********************************************************
    public Icon_factory_actor(Aspect_ratio_cache aspect_ratio_cache, Rotation_cache rotation_cache, Stage owner_, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aspect_ratio_cache = aspect_ratio_cache;
        this.rotation_cache = rotation_cache;
        this.aborter = aborter;
        owner = owner_;
        logger = logger_;
        if (dbg) logger.log("Icon_factory created");
        icon_cache_dir = Files_and_Paths.get_icon_cache_dir(logger);
        writer = new Icon_writer_actor(icon_cache_dir, logger);
        //writer = Icon_writer_actor.launch_icon_writer(icon_cache_dir, logger);
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Icon_factory_request icon_factory_request = (Icon_factory_request) m;
        if (dbg) logger.log("icon request processing starts ");

        Icon_destination destination = icon_factory_request.destination;
        if (destination == null) {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN icon factory : cancel! destination==null"));
            return null;
        }

        //instance.incrementAndGet();
        Image_and_rotation image_and_rotation = null;
        for(;;) {
            image_and_rotation = try_once(destination, icon_factory_request);
            if (image_and_rotation.image() == null) {
                // must treat as non_image
                if (dbg) logger.log("making an icon failed for : " + destination.get_item_path());
                //instance.decrementAndGet();
                return "icon fabrication failed";
            }
            double w = image_and_rotation.image().getWidth();
            double h = image_and_rotation.image().getHeight();
            if ((w > 0.0)&&(h > 0.0)) {
                // SUCCESS!!
                break;
            }

            // will retry, but let us yield before retrying
            get_ticket(icon_factory_request);

            // retry a few times
            if (icon_factory_request.retry_count < Icon_factory_request.max_retry) {
                icon_factory_request.retry_count++;
                logger.log("RETRYING again: " + icon_factory_request.retry_count + " times, after empty icon for : " + destination.get_item_path() + " w=" + w+ " h=" + h);
                continue;
            }
            logger.log("too many retries after empty icon for : " + destination.get_item_path() + " w=" + w+ " h=" + h);
            break;
        }
        destination.receive_icon(image_and_rotation);
        if ( image_and_rotation.rotation()!=null)
        {
            double aspect_ratio = image_and_rotation.image().getWidth()/image_and_rotation.image().getHeight();
            if (( image_and_rotation.rotation()==90)||(image_and_rotation.rotation()==270))
            {
                aspect_ratio = 1/aspect_ratio;
            }
            if(dbg) logger.log("Icon_factory_actor "+destination.get_item_path()+" "+aspect_ratio+" w="+image_and_rotation.image().getWidth()+" h="+image_and_rotation.image().getHeight());
            if ( aspect_ratio_cache!=null) aspect_ratio_cache.inject(destination.get_item_path(),aspect_ratio,true);
        }
        //logger.log("Icon_factory_actor: "+ instance.decrementAndGet());

        return "icon done";
    }

    //**********************************************************
    private void get_ticket(Icon_factory_request icon_factory_request)
    //**********************************************************
    {
        if ( !Actor_engine.use_tickets) return;
        //if( icon_factory_request.ticket_queue.peek() ==null) logger.log("will block "+ icon_factory_request.to_string());
        try {
            // if there are no tickets, this will block the thread
            // if the thread is virtual, another one will step in
            // thus jobs that need a lot of tickets will be blocked more often
            // than those that need zero or only one
            // thus we have a priority system ...
            // the implementer just has to add "get_ticket"
            // it is a bit like YIELD
            icon_factory_request.ticket_queue.take();
        } catch (InterruptedException e) {
            logger.log("ticket queue take interrupted");
        }
    }

    //**********************************************************
    private Image_and_rotation try_once(Icon_destination destination, Icon_factory_request icon_factory_request)
    //**********************************************************
    {
        Image image = null;
        Double rotation = Double.valueOf(0.0);
        switch (destination.get_item_type())
        {
            case pdf -> {
                if (dbg) logger.log(destination.get_item_path() + " type is PDF");
                image = process_pdf(icon_factory_request, destination);

            }
            case video -> {
                if (dbg) logger.log(destination.get_item_path() + " type is VIDEO");
                image = process_video(icon_factory_request, destination);

            }

            case folder, symbolic_link_on_folder -> {
                // for folder the path of the image chosen to represent the folder as an icon is needed
                image = process_image(icon_factory_request, destination);
                if (image != null)
                {
                    rotation = rotation_cache.get_rotation(destination.get_path_for_display_icon_destination());
                    //rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(destination.get_path_for_display_icon_destination(), false, aborter, logger);
                }
            }
            case image_gif -> {
                image = process_image_with_no_cached_icon(icon_factory_request, destination);
                if (image != null)
                {
                    rotation = rotation_cache.get_rotation(destination.get_item_path());
                    //rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(destination.get_item_path(), false, aborter, logger);
                }
            }
            case image_png -> {
                image = process_image_with_no_cached_icon(icon_factory_request, destination);
                if (image != null)
                {
                    rotation = rotation_cache.get_rotation(destination.get_item_path());
                    //rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(destination.get_item_path(), false, aborter, logger);
                }
            }
            case image_not_gif_not_png -> {
                image = process_image(icon_factory_request, destination);
                if (image != null)
                {
                    rotation = rotation_cache.get_rotation(destination.get_item_path());
                    //rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(destination.get_item_path(), false, aborter, logger);
                }
            }
            case no_path -> {
                logger.log("HAPPENS?????: no path for icon destination???"+icon_factory_request.destination);
                image = process_image(icon_factory_request, destination);
                if (image != null)
                {
                    rotation = rotation_cache.get_rotation(destination.get_item_path());
                    //rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(destination.get_item_path(), false, aborter, logger);
                }
            }

        }
        return new Image_and_rotation(image,rotation);
    }


    //**********************************************************
    private Image process_image(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        if ( dbg)
            logger.log("Icon_factory thread: process_image:" + destination.get_string());

        Path path = destination.get_path_for_display_icon_destination();
        if (path == null)
        {
            if (dbg)
                logger.log("Icon_factory thread: returning null icon because icon path is null for item:" + destination.get_string() + "\ntypically happens when there is no image to use as a icon in that folder");

            return null;//Look_and_feel_manager.get_large_folder_icon(icon_factory_request.icon_size);
        }
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting1");
            return null;
        }
        String tag = String.valueOf(icon_factory_request.icon_size);
        if ( icon_factory_request.destination.get_item_type() == Iconifiable_item_type.pdf)
        {
            logger.log("SHOULD NOT HAPPEN: process_image: PDF");

            // for PDF we do not minimize the generated png, ImageView will do it
            // so a side effect is it is not necessary to regenerate it if the icon size was changed
            tag = "";
        }

        //long start = System.currentTimeMillis();
        Image image = From_disk.load_icon_from_disk_cache(path, icon_cache_dir, icon_factory_request.icon_size, tag, png_extension, false,logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting2");
            return null;
        }
        if (image != null)
        {
            if (dbg)
                logger.log("Icon_factory thread: found in cache: " + path.getFileName());
                return image;
        }

        if (dbg)
                logger.log("Icon_factory thread:  load from cache FAILED for " + path.getFileName());


        image = From_disk.read_original_image_from_disk_and_return_icon(path, icon_factory_request.icon_size, true, icon_factory_request.aborter, logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting3");
            return null;
        }
        if (image == null) {
            if (dbg)
                logger.log("WARNING: Icon_factory thread: load from file FAILED for " + path.getFileName());
            return null;
        }
        if (image.getWidth() < 1.0) {
            // this "should not happen" as it was seen when there was a multithreading bug: too many icon requests were arriving at the same time
            logger.log("WARNING1: Icon_factory thread: load from file FAILED getWidth() ==0 for " + path.getFileName());
            return null;
        }
        if (image .getHeight() ==0) {
            // this "should not happen" as it was seen when there was a multithreading bug: too many icon requests were arriving at the same time
            logger.log("WARNING1: Icon_factory thread: load from file FAILED getHeight() ==0 for " + path.getFileName());
            return null;
        }

        switch (destination.get_item_type()) {
            case image_gif:
                // dont try to disk-cache for gifs, they are either small or animated
            case symbolic_link_on_folder:
            case folder:
                // no need for folders
                break;
            default:
                if (dbg)
                        logger.log("Icon_factory thread: sending icon write to file in cache dir for " + path.getFileName());
                Icon_write_message iwm = new Icon_write_message(image, icon_factory_request.icon_size, png_extension, path,aborter);
                writer.push(iwm);
                break;
        }

        return image;
    }

    //**********************************************************
    private Image process_image_with_no_cached_icon(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        if ( dbg) logger.log("Icon_factory thread: process_image_gif:" + destination.get_string());

        Path path = destination.get_path_for_display_icon_destination();
        if (path == null)
        {
            if (dbg)
                logger.log("Icon_factory thread: returning null icon because icon path is null for item:" + destination.get_string() + "\ntypically happens when there is no image to use as a icon in that folder");

            return null;//Look_and_feel_manager.get_large_folder_icon(icon_factory_request.icon_size);
        }
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting1");
            return null;
        }
        if ( icon_factory_request.destination.get_item_type() == Iconifiable_item_type.pdf)
        {
            logger.log("SHOULD NOT HAPPEN: process_image_with_no_cached_icon: target is PDF");
        }

        //long start = System.currentTimeMillis();
        Image image = From_disk.read_original_image_from_disk_and_return_icon(path, icon_factory_request.icon_size, true, icon_factory_request.aborter, logger);
        if (image == null) {
            if (dbg)
                logger.log("WARNING: Icon_factory thread: load from file FAILED for " + path.getFileName());
            return null;
        }

        return image;
    }



    //**********************************************************
    private Image process_video(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        if (verbose_dbg) logger.log("Icon_factory thread:  process_video " + destination.get_item_path().toAbsolutePath());

        // we are going to create an animated gif using ffmpeg
        // ... unless it is already in the icon cache
        String tag = ""; // empty since we do not resize the frames to icon size
        Image image = From_disk.load_icon_from_disk_cache(destination.get_item_path(), icon_cache_dir, icon_factory_request.icon_size, tag,gif_extension, dbg, logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting4");
            return null;
        }
        if ( image != null)
        {
            if (dbg) logger.log("Icon_factory thread: found in cache: " + destination.get_item_path().getFileName());
            return image;
        }

        get_ticket(icon_factory_request);

        if (dbg)
            logger.log("Icon_factory thread:  load from GIF tmp FAILED for " + destination.get_item_path());
        int length = Static_application_properties.get_animated_gif_duration_for_a_video(logger);

        File gif_animated_icon_file = From_disk.file_for_icon_cache(icon_cache_dir, destination.get_path_for_display_icon_destination(), tag, gif_extension);
        //File gif_animated_icon_file = From_disk.file_for_cache(icon_cache_dir, destination.get_icon_path(), ""+icon_factory_request.icon_size+"_"+length, gif_extension);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting5");
            return null;
        }

        Path destination_gif_full_path = Paths.get(icon_cache_dir.toAbsolutePath().toString(), gif_animated_icon_file.getName());

        int skip = 0;
        int duration_in_seconds = Ffmpeg_utils.get_video_duration(owner, destination.get_item_path(), logger);
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

        get_ticket(icon_factory_request);

        Ffmpeg_utils.video_to_gif(owner, destination.get_item_path(), destination_gif_full_path, length, skip, icon_factory_request.get_aborter(),logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting6");
            return null;
        }

        if (destination_gif_full_path.toFile().length() == 0)
        {
            logger.log("ERROR animated gif empty " + destination_gif_full_path.toAbsolutePath());
            return null;
        }
        if (verbose_dbg)
            logger.log("Icon_factory Animated gif icon MADE for " + destination.get_item_path().getFileName() + " as " + destination_gif_full_path.toAbsolutePath());
        image = From_disk.load_icon_from_disk_cache(destination.get_path_for_display_icon_destination(), icon_cache_dir, icon_factory_request.icon_size, tag,gif_extension, dbg, logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting7");
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
    private Image process_pdf(Icon_factory_request icon_factory_request, Icon_destination icon_destination)
    //**********************************************************
    {
        get_ticket(icon_factory_request);

        if (pdf_dbg) logger.log("Icon_factory thread:  process_pdf " + icon_destination.get_item_path().toAbsolutePath());

        // we are going to create the PNG using pdfbox!

        String tag = "";//+icon_factory_request.icon_size;
        Image image_from_cache = From_disk.load_icon_from_disk_cache(icon_destination.get_path_for_display_icon_destination(), icon_cache_dir, icon_factory_request.icon_size,tag, png_extension, false,logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting8");
            return null;
        }
        if (image_from_cache != null)
        {
            if (pdf_dbg) logger.log("Icon_factory thread: found in cache for: " + icon_destination.get_item_path().getFileName());
            return image_from_cache;
        }

        if (pdf_dbg)
            logger.log("Icon_factory thread:  load from disk cache FAILED for " + icon_destination.get_item_path().getFileName() + " MAKING IT NOW");

        File file_in = icon_destination.get_item_path().toFile();
        File resulting_png_name = From_disk.file_for_icon_cache(icon_cache_dir, icon_destination.get_item_path(), tag, png_extension);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting9");
            return null;
        }

        String deb = null;
        try (PDDocument document = Loader.loadPDF(file_in, deb))
        {
            get_ticket(icon_factory_request);

            if (icon_factory_request.aborter.should_abort())
            {
                if ( aborting_dbg) logger.log("Icon_factory thread: aborting10");
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
                //int dpi = Static_application_properties.get_icon_size(logger);
                BufferedImage image = renderer.renderImage(i);
                if ( pdf_dbg)
                    logger.log("PDF = "+image.getWidth()+"x"+image.getHeight()+" aspect ratio = "+((double)(image.getWidth())/(double)(image.getHeight())));
                if ( aspect_ratio_cache!=null) aspect_ratio_cache.inject(icon_destination.get_item_path(),((double)(image.getWidth())/(double)(image.getHeight())),true);

                //BufferedImage image = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                if (icon_factory_request.aborter.should_abort())
                {
                    if ( aborting_dbg) logger.log("Icon_factory thread: aborting11");
                    return null;
                }
                get_ticket(icon_factory_request);

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
                                if ( aborting_dbg) logger.log("Icon_factory thread: aborting12");
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
                            if ( aborting_dbg) logger.log("Icon_factory thread: aborting13");
                            return null;
                        }
                        writer.setOutput(imageOutput);
                        get_ticket(icon_factory_request);

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
        get_ticket(icon_factory_request);

        if (pdf_dbg) logger.log("image of PDF write done (2)" + resulting_png_name);
        image_from_cache = From_disk.load_icon_from_disk_cache(icon_destination.get_item_path(), icon_cache_dir, icon_factory_request.icon_size, tag, png_extension, dbg, logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting14");
            return null;
        }
        if (image_from_cache == null)
        {
            logger.log("Icon_factory thread: load from file FAILED for " + icon_destination.get_item_path().getFileName());
        }
        else
        {
            if (pdf_dbg) logger.log("image of PDF found on disk OK (3)" + resulting_png_name);
        }
        return image_from_cache;
    }



}
