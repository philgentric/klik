//SOURCES animated_gifs/Ffmpeg_utils.java
//SOURCES ../Image_and_properties.java
//SOURCES ../items/Iconifiable_item_type.java
//SOURCES ../../util/files_and_paths/Static_files_and_paths_utilities.java
//SOURCES ../../images/decoding/Fast_image_property_from_exif_metadata_extractor.java
//SOURCES ../../util/execute/Execute_command.java
//SOURCES animated_gifs/Animated_gif_from_folder_content.java

package klik.browser.icons;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klik.actor.*;
import klik.browser.icons.animated_gifs.Ffmpeg_utils;
import klik.browser.Image_and_properties;
import klik.browser.icons.image_properties_cache.*;
import klik.browser.items.Iconifiable_item_type;
import klik.look.Jar_utils;
import klik.properties.boolean_features.Booleans;
import klik.properties.Cache_folder;
import klik.properties.Non_booleans_properties;
import klik.util.image.Icons_from_disk;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.execute.Execute_command;
import klik.util.image.icon_cache.Icon_caching;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


// an actor-style asynchronous icon factory
//**********************************************************
public class Icon_factory_actor implements Actor
//**********************************************************
{
    private static final boolean verbose_dbg = false;
    private static final boolean dbg = true;
    private static final boolean pdf_dbg = true;
    private static final boolean aborting_dbg = false;

    Logger logger;
    Icon_writer_actor writer;
    private final Window owner;
    private final Aborter aborter;
    private final Image_properties_RAM_cache image_properties_RAM_cache;

    private final Path icon_cache_dir;

    //**********************************************************
    public Icon_factory_actor(Image_properties_RAM_cache image_properties_RAM_cache,
                              Window owner,
                              Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.image_properties_RAM_cache = image_properties_RAM_cache;
        this.aborter = aborter;
        this.owner = owner;
        logger = logger_;
        if (dbg) logger.log("Icon_factory created");
        icon_cache_dir = Static_files_and_paths_utilities.get_cache_dir( Cache_folder.klik_icon_cache,owner,logger);
        writer = new Icon_writer_actor(icon_cache_dir, owner, logger);
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Icon_factory_actor";
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

        Optional<Image_and_properties> image_and_properties = Optional.empty();
        for(;;) {
            image_and_properties = try_once(destination, icon_factory_request);
            if ( image_and_properties.isEmpty())
            {
                // DONT RETRY
                return "no icon";
            }
            if (image_and_properties.get().image() != null) {
                double w = image_and_properties.get().image().getWidth();
                double h = image_and_properties.get().image().getHeight();
                if ((w > 0.0)&&(h > 0.0)) {
                    // SUCCESS!!
                    break;
                }
            }

            // retry a few times
            if (icon_factory_request.retry_count < Icon_factory_request.max_retry) {
                icon_factory_request.retry_count++;
                logger.log("RETRYING : " + icon_factory_request.retry_count + " times, after empty icon for : " + destination.get_item_path() );
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            logger.log("too many retries after empty icon for : " + destination.get_item_path() );
            return "icon failed";
        }

        check(image_and_properties.get());

        destination.receive_icon(image_and_properties.get());
        Image_properties image_properties = image_and_properties.get().properties();
        if ( image_properties != null)
        {
            image_properties_RAM_cache.inject(destination.get_item_path(),image_properties,true);
        }
        //logger.log("Icon_factory_actor: "+ instance.decrementAndGet());

        return "icon done";
    }

    //**********************************************************
    private void check(Image_and_properties image_and_properties)
    //**********************************************************
    {
        if ( image_and_properties.image() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("image_and_properties.image() == null"));
            return;
        }
        if ( image_and_properties.properties() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("image_and_properties.properties() == null"));
            return;
        }
        if ( image_and_properties.properties().rotation() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("image_and_properties.properties().rotation() == null"));
        }
    }


    //**********************************************************
    private Optional<Image_and_properties> try_once(Icon_destination destination, Icon_factory_request icon_factory_request )
    //**********************************************************
    {
        switch (destination.get_item_type())
        {
            case pdf -> {
                if (dbg) logger.log(destination.get_item_path() + " type is PDF");
                return Optional.ofNullable(process_pdf(icon_factory_request, destination));
            }
            case video -> {
                if (dbg) logger.log(destination.get_item_path() + " type is VIDEO");
                return Optional.ofNullable(process_video(icon_factory_request, destination));
            }

            case folder, symbolic_link_on_folder -> {
                // for folder the path of the image chosen to represent the folder as an icon is needed
                return process_image(destination.get_item_type(), icon_factory_request, destination);
            }

            case //image_fits,
                 non_javafx_image, image_gif , image_png , javafx_image_not_gif_not_png -> {
                return process_image(destination.get_item_type(), icon_factory_request, destination);
            }
            case no_path, other -> {
                logger.log(Stack_trace_getter.get_stack_trace("HAPPENS in Icon_factory_actor"+icon_factory_request.destination));
                return process_image(destination.get_item_type(), icon_factory_request, destination);
            }

        }
        logger.log("WARNING icon is null for "+destination.get_item_path());

        return null;
    }


    //**********************************************************
    private Optional<Image_and_properties> process_image(Iconifiable_item_type item_type, Icon_factory_request icon_factory_request, Icon_destination destination )
    //**********************************************************
    {
        if ( dbg)  logger.log("Icon_factory thread: process_image:" + destination.get_string());

        Path path = destination.get_path_for_display_icon_destination();
        if (path == null)
        {
            if (dbg)
                logger.log("Icon_factory thread: returning null icon because icon path is null for item:" + destination.get_string() + "\ntypically happens when there is no image to use as a icon in that folder");
            return Optional.empty();
        }
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg)
                logger.log("Icon_factory thread: aborting0 "+icon_factory_request.aborter.reason());
            return Optional.empty();
        }

        {
            Image image_from_cache = Icons_from_disk.load_icon_from_disk_cache(
                    path,
                    icon_factory_request.icon_size,
                    String.valueOf(icon_factory_request.icon_size),
                    Icon_caching.png_extension,
                    false, icon_factory_request.owner,logger);
            if (icon_factory_request.aborter.should_abort())
            {
                if (aborting_dbg)
                    logger.log("Icon_factory thread: aborting2 " + icon_factory_request.aborter.name + " reason: " + icon_factory_request.aborter.reason());
                return Optional.empty();
            }
            if (image_from_cache != null) {
                if (dbg)
                    logger.log("Icon_factory thread: found in cache: " + path.getFileName());

                Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
                return Optional.of(new Image_and_properties(image_from_cache, properties));
            }
        }
        if (dbg)
            logger.log("\n\nLoading icon from cache FAILED for " + path.toAbsolutePath());


        Optional<Image> op = Icons_from_disk.read_original_image_from_disk_and_return_icon(path, item_type, icon_factory_request.icon_size, true, icon_factory_request.aborter, icon_factory_request.owner, logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg)
                logger.log("Icon_factory thread: aborting3");
            return Optional.empty();
        }
        boolean write_icon_to_cache = true;
        Image image_from_disk = null;
        if (op.isEmpty()) {
            //if (dbg)
            logger.log("Making an icon FAILED (1) for " + path.getFileName());
            return Optional.empty();

            //image_from_disk = Jar_utils.get_broken_icon(300,icon_factory_request.owner,logger);
            //write_icon_to_cache = false; // do not write the broken icon to the cache
        }
        else
        {
            image_from_disk = op.get();
        }
        if (image_from_disk.getWidth() < 1.0) {
            // this "should not happen" as it was seen when there was a multithreading bug: too many icon requests were arriving at the same time
            logger.log("Making an icon FAILED (2) getWidth() ==0 for " + path.getFileName());
            return Optional.empty();
        }
        if (image_from_disk.getHeight() ==0) {
            // this "should not happen" as it was seen when there was a multithreading bug: too many icon requests were arriving at the same time
            logger.log("Making an icon FAILED (3) getHeight() ==0 for " + path.getFileName());
            return Optional.empty();
        }

        switch (item_type) {
            case image_gif:
                // dont try to disk-cache for gifs, they are either small or animated
            case symbolic_link_on_folder:
            case folder:
                // no need for folders
                break;
            default:
                Path icon_cache_dir = Static_files_and_paths_utilities.get_cache_dir( Cache_folder.klik_icon_cache,owner,logger);
                if (path.getParent().toAbsolutePath().toString().equals(icon_cache_dir.toAbsolutePath().toString()))
                {
                    // the user is browsing the icon cache. if we save a file for the icon, it will trigger a new icon request...
                    // ad nauseam ! ==> storm avoidance = dont save the icon.
                    if (dbg) logger.log("Icon_factory thread: (storm avoidance) not saving the icon for a file which is in the icons' cache folder " + path.getFileName());
                    break;
                }

                if (dbg)
                    logger.log("Icon_factory thread: sending icon write to file in cache dir for " + path.getFileName());

                if ( write_icon_to_cache)
                {
                    Icon_write_message iwm = new Icon_write_message(image_from_disk, icon_factory_request.icon_size, path, aborter);
                    writer.push(iwm);
                }
                break;
        }

        Image_properties properties = image_properties_RAM_cache.get_from_cache(destination.get_path_for_display_icon_destination(),null);
        if (properties == null)
        {
            properties = new Image_properties(image_from_disk.getWidth(), image_from_disk.getHeight(), Rotation.normal);
            image_properties_RAM_cache.inject(destination.get_path_for_display_icon_destination(), properties,true);
        }
        return Optional.of(new Image_and_properties(image_from_disk,properties));
    }


    //**********************************************************
    private Optional<Image> process_image_with_no_cached_icon(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        if ( dbg) logger.log("Icon_factory thread: process_image_with_no_cached_icon:" + destination.get_string());

        Path path = destination.get_path_for_display_icon_destination();
        if (path == null)
        {
            if (dbg)
                logger.log("Icon_factory thread: returning null icon because icon path is null for item:" + destination.get_string() + "\ntypically happens when there is no image to use as a icon in that folder");

            return Optional.empty();//Look_and_feel_manager.get_large_folder_icon(icon_factory_request.icon_size);
        }
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting1");
            return Optional.empty();
        }
        if ( icon_factory_request.destination.get_item_type() == Iconifiable_item_type.pdf)
        {
            logger.log("SHOULD NOT HAPPEN: process_image_with_no_cached_icon: target is PDF");
        }

        Optional<Image> op = Icons_from_disk.read_original_image_from_disk_and_return_icon(path, icon_factory_request.destination.get_item_type(), icon_factory_request.icon_size, true, icon_factory_request.aborter, icon_factory_request.owner,logger);
        //Image image = Icons_from_disk.load_native_resolution_image_from_disk(path, true, icon_factory_request.aborter, logger);
        if (op.isEmpty()) {
            if (dbg)
                logger.log("Making an icon FAILED (4) for " + path.getFileName());
            return Optional.empty();
        }

        return op;
    }



    //**********************************************************
    private Image_and_properties process_video(Icon_factory_request icon_factory_request, Icon_destination destination)
    //**********************************************************
    {
        if (verbose_dbg) logger.log("Icon_factory thread:  process_video " + destination.get_item_path().toAbsolutePath());

        // we are going to create an animated gif using ffmpeg
        // ... unless it is already in the icon cache
        //String tag = ""; // empty since we do not resize the frames to icon size
        {
            Image image_from_cache = Icons_from_disk.load_icon_from_disk_cache(destination.get_item_path(), icon_factory_request.icon_size, "",Icon_caching.gif_extension, dbg, icon_factory_request.owner,logger);
            if (icon_factory_request.aborter.should_abort()) {
                if (aborting_dbg) logger.log("Icon_factory thread: aborting4");
                return null;
            }
            if (image_from_cache != null) {
                if (dbg)
                    logger.log("Icon_factory thread: found in cache: " + destination.get_item_path().getFileName());

                Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
                return new Image_and_properties(image_from_cache, properties);
            }


            if (dbg)
                logger.log("Icon_factory thread:  load from GIF tmp FAILED for " + destination.get_item_path());
            double length = Non_booleans_properties.get_animated_gif_duration_for_a_video(icon_factory_request.owner);

            Path gif_animated_icon_file = Icon_caching.path_for_icon_caching(destination.get_path_for_display_icon_destination(), "", Icon_caching.gif_extension, owner, logger);
            //File gif_animated_icon_file = Icons_from_disk.file_for_cache(icon_cache_dir, destination.get_icon_path(), ""+icon_factory_request.icon_size+"_"+length, gif_extension);
            if (icon_factory_request.aborter.should_abort()) {
                if (aborting_dbg) logger.log("Icon_factory thread: aborting5");
                return null;
            }

            Path destination_gif_full_path = Paths.get(icon_cache_dir.toAbsolutePath().toString(), gif_animated_icon_file.getFileName().toString());

            double skip = 0;
            {
                Double duration_in_seconds = Ffmpeg_utils.get_media_duration(destination.get_item_path(), icon_factory_request.owner, logger);
                if (duration_in_seconds != null) {
                    if (duration_in_seconds > 3 * 3600) {
                        logger.log("WARNING: ffprobe reports duration that looks wrong");
                        duration_in_seconds = 1800.0;
                    }

                    if (duration_in_seconds < 0) {
                        duration_in_seconds = length;
                    }

                    if (duration_in_seconds < length) {
                        length = duration_in_seconds;
                    } else {
                        // jump to the middle of the movie
                        skip = duration_in_seconds / 2 - length;
                    }
                }
            }


            Ffmpeg_utils.video_to_gif(icon_factory_request.owner, destination.get_item_path(), icon_factory_request.icon_size, 10,destination_gif_full_path, length, skip, 0,icon_factory_request.get_aborter(), logger);
            if (icon_factory_request.aborter.should_abort()) {
                if (aborting_dbg) logger.log("Icon_factory thread: aborting6");
                return null;
            }

            if (destination_gif_full_path.toFile().length() == 0) {
                logger.log("ERROR animated gif empty " + destination_gif_full_path.toAbsolutePath());
                return null;
            }
            if (verbose_dbg)
                logger.log("Icon_factory Animated gif icon MADE for " + destination.get_item_path().getFileName() + " as " + destination_gif_full_path.toAbsolutePath());
        }
        {
            Image image_from_cache = Icons_from_disk.load_icon_from_disk_cache(destination.get_path_for_display_icon_destination(), icon_factory_request.icon_size, "",Icon_caching.gif_extension, dbg, icon_factory_request.owner,logger);

            if (icon_factory_request.aborter.should_abort()) {
                if (aborting_dbg) logger.log("Icon_factory thread: aborting7");
                return null;
            }
            if (image_from_cache == null) {
                logger.log("Icon_factory thread: load from file FAILED (5) for " + destination.get_item_path().getFileName());
                return null;
            }
            if ((image_from_cache.getHeight() == 0) && (image_from_cache.getWidth() == 0)) {
                logger.log("Icon_factory thread: load from file FAILED (6) for " + destination.get_item_path().getFileName());
                return null;
            }
            //logger.log("Icon_factory returning image for :" + destination.get_item_path().getFileName());

            Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
            return new Image_and_properties(image_from_cache, properties);
        }
    }



    //**********************************************************
    private Image_and_properties process_pdf(Icon_factory_request icon_factory_request, Icon_destination icon_destination)
    //**********************************************************
    {

        if (pdf_dbg) logger.log("Icon_factory thread:  process_pdf " + icon_destination.get_item_path().toAbsolutePath());


        Image image_from_cache = Icons_from_disk.load_icon_from_disk_cache(icon_destination.get_path_for_display_icon_destination(), icon_factory_request.icon_size, "",Icon_caching.png_extension, false,icon_factory_request.owner,logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting8");
            return null;
        }
        if (image_from_cache != null)
        {
            if (pdf_dbg) logger.log("Icon_factory thread: found in cache for: " + icon_destination.get_item_path().getFileName());
            Image_properties properties = new Image_properties(image_from_cache.getWidth(),image_from_cache.getHeight(),Rotation.normal);
            return new Image_and_properties(image_from_cache,properties);
        }

        if (pdf_dbg)
            logger.log("Icon_factory thread:  load from disk cache FAILED for " + icon_destination.get_item_path().getFileName() + " MAKING IT NOW");

        // we are going to create the PNG using pdfbox!

        File file_in = icon_destination.get_item_path().toFile();
        Path resulting_png_name = Icon_caching.path_for_icon_caching(icon_destination.get_item_path(), "", Icon_caching.png_extension, icon_factory_request.owner, logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting9");
            return null;
        }

       // boolean use_GraphicsMagick = true;
       // if (use_GraphicsMagick)
        {
            // gm convert -density 300 -resize 256x256 -quality 90 input.pdf output.png
            List<String> command_line_for_GraphicsMagic = new ArrayList<>();
            command_line_for_GraphicsMagic.add("gm");
            command_line_for_GraphicsMagic.add("convert");
            command_line_for_GraphicsMagic.add("-density");
            command_line_for_GraphicsMagic.add("300");
            command_line_for_GraphicsMagic.add("-resize");
            command_line_for_GraphicsMagic.add(""+icon_factory_request.icon_size+"x"+icon_factory_request.icon_size);
            command_line_for_GraphicsMagic.add("-quality");
            command_line_for_GraphicsMagic.add("90");
            command_line_for_GraphicsMagic.add(file_in.getAbsolutePath().toString());
            command_line_for_GraphicsMagic.add(resulting_png_name.toAbsolutePath().toString());
            StringBuilder sb = null;
            if ( pdf_dbg) sb = new StringBuilder();
            File wd = file_in.getParentFile();
            if ( Execute_command.execute_command_list(command_line_for_GraphicsMagic, wd, 2000, sb,logger) == null)
            {
                Booleans.manage_show_graphicsmagick_install_warning(icon_factory_request.owner,logger);
                return null;
            }
            if ( pdf_dbg) logger.log(sb.toString());
        }
        /*
        else
        {
            String deb = null;
            try (PDDocument document = Loader.loadPDF(file_in, deb)) {

                if (icon_factory_request.aborter.should_abort()) {
                    if (aborting_dbg) logger.log("Icon_factory thread: aborting10");
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
                    //int dpi = Non_booleans_properties.get_icon_size(logger);
                    BufferedImage image = renderer.renderImage(i);
                    if (pdf_dbg)
                        logger.log("PDF = " + image.getWidth() + "x" + image.getHeight() + " aspect ratio = " + ((double) (image.getWidth()) / (double) (image.getHeight())));
                    if (aspect_ratio_cache != null)
                        aspect_ratio_cache.inject(icon_destination.get_item_path(), ((double) (image.getWidth()) / (double) (image.getHeight())), true);

                    //BufferedImage image = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                    if (icon_factory_request.aborter.should_abort()) {
                        if (aborting_dbg) logger.log("Icon_factory thread: aborting11");
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
                                if (icon_factory_request.aborter.should_abort()) {
                                    if (aborting_dbg) logger.log("Icon_factory thread: aborting12");
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
                            if (icon_factory_request.aborter.should_abort()) {
                                if (aborting_dbg) logger.log("Icon_factory thread: aborting13");
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
        }*/

        if (pdf_dbg) logger.log("image of PDF write done (2)" + resulting_png_name);
        image_from_cache = Icons_from_disk.load_icon_from_disk_cache(icon_destination.get_item_path(),icon_factory_request.icon_size, "", Icon_caching.png_extension, dbg, icon_factory_request.owner,logger);
        if (icon_factory_request.aborter.should_abort())
        {
            if ( aborting_dbg) logger.log("Icon_factory thread: aborting14");
            return null;
        }
        if (image_from_cache == null)
        {
            logger.log("Icon_factory thread: load from file FAILED (7) for " + icon_destination.get_item_path().getFileName());
            return null;
        }
        else
        {
            if (pdf_dbg) logger.log("image of PDF found on disk OK (3)" + resulting_png_name);
        }

        Image_properties properties = new Image_properties(image_from_cache.getWidth(),image_from_cache.getHeight(),Rotation.normal);
        return new Image_and_properties(image_from_cache,properties);
    }



}
