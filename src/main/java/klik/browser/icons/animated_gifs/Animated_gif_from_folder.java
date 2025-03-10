//SOURCES ../Icon_writer_actor.java

package klik.browser.icons.animated_gifs;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.Icon_writer_actor;
import klik.util.files_and_paths.From_disk;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.properties.Static_application_properties;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/*
animated-GIF making utility:
makes an animated gif from the pictures inside a folder
 */

//**********************************************************
public class Animated_gif_from_folder
//**********************************************************
{
    public static final String warning_GraphicsMagick = "This feature requires GraphicsMagick\n . for mac: brew install graphicsmagick\n . for all systems download from GraphicsMagick.org\n";

    private final static boolean dbg = false;
    public static final String FRAME1 = "Frame_";
    public static final String FRAME2 = "_"+FRAME1;
    public static final String PNG = ".png";
    private static boolean dbg_GraphicsMagick_call = true;

    //**********************************************************
    public static Path make_animated_gif_from_all_images_in_folder(Stage owner, Path in, List<File> images_in_folder, Logger logger)
    //**********************************************************
    {
        if (! Files.isDirectory(in))
        {
            if ( dbg) logger.log("FATAL not a folder "+in);
            return null;
        }


        Collections.sort(images_in_folder);
        Path actual_icon_cache_dir = Static_files_and_paths_utilities.get_icons_cache_dir(owner, logger);
        Path folder_icon_cache_dir = Static_files_and_paths_utilities.get_folders_icons_cache_dir(logger);

        int icon_size = Static_application_properties.get_icon_size(logger);
        String output_animated_gif_name = Icon_writer_actor.make_cache_name(in,"ANIMATED_FOLDER_"+icon_size, "gif");
        Path out = Path.of(folder_icon_cache_dir.toAbsolutePath().toString(),output_animated_gif_name);
        if ( Files.exists(out))
        {
            if ( dbg) logger.log(" make_animated_gif_from_all_images_in_folder found in cache ");
            return out;
        }

        if ( dbg) logger.log(" make_animated_gif_from_all_images_in_folder in = "+in+" target out = "+out);

        /*
        STEP1: for each image in the folder
        find the icon in the cache (if it is not present... too bad !)
         */
        List<String> graphicsMagick_command_line = new ArrayList<>();
        // call GraphicsMagick
        graphicsMagick_command_line.add("gm");
        graphicsMagick_command_line.add("convert");

        Double W = null;
        double H = 0;
        String tag = String.valueOf(icon_size);
        int MAX = 10;
        int inc = images_in_folder.size()/MAX;
        if ( inc == 0) inc = 1;
        int actual = 0;
        List<Path> to_be_cleaned_up = new ArrayList<>();
        for ( int i =0; i < images_in_folder.size(); i += inc)
        {
            File image_file = images_in_folder.get(i);
            Image image = From_disk.load_icon_from_disk_cache(image_file.toPath(), actual_icon_cache_dir, icon_size, tag, Icon_factory_actor.png_extension, dbg,logger);
            if ( image == null)
            {
                if ( dbg) logger.log("   fetching icon from cache FAILED for:"+image_file);
                continue;
            }
            // it seems that animated gif with frames of different sizes is not well supported
            // at least not by javafx ImageView
            // this happens in a folder with a mix of portrait and landscape pictures...
            // so we will take the first picture as a reference and skip others
            if ( W == null)
            {
                W = image.getWidth();
                H = image.getHeight();
            }
            else
            {
                if ( W != image.getWidth())
                {
                    if ( dbg) logger.log("skiping up as icon whas wrong width:"+W+ "!="+ image.getWidth());
                    continue;
                }
                if ( H != image.getHeight())
                {
                    if ( dbg) logger.log("skiping up as icon whas wrong height:"+H+ "!="+ image.getHeight());
                    continue;
                }
            }

            /* copy the icons to the folder_icon_folder */
            String local = Icon_writer_actor.make_cache_name(in,FRAME1+i, Icon_factory_actor.png_extension);
            Path icon_path2 = Path.of(folder_icon_cache_dir.toAbsolutePath().toString(),local);
            try {
                //String icon_name = make_cache_name(image_file.toPath(),tag, png_extension);
                //Path icon_path = Path.of(actual_icon_cache_dir.toAbsolutePath().toString(),icon_name);
                File icon_file= From_disk.file_for_icon_caching(actual_icon_cache_dir.toAbsolutePath(), image_file.toPath(), tag, Icon_factory_actor.png_extension);

                Files.copy(icon_file.toPath(), icon_path2, REPLACE_EXISTING);
                if ( dbg) logger.log("copy DONE "+icon_path2);
                to_be_cleaned_up.add(icon_path2);

            } catch (IOException e) {
                logger.log("WARNING: make_animated_gif_from_all_images_in_folder copy failed "+e);
               continue;
            }
            actual++;
            if ( actual> MAX) break;
        }

        if ( actual < 2)
        {
            if ( dbg) logger.log(" make_animated_gif_from_all_images_in_folder too few images, aborting ");
            return null; // abort
        }

        graphicsMagick_command_line.add("-delay");
        graphicsMagick_command_line.add("30"); // in centiseconds
        String frames = Icon_writer_actor.make_cache_name_raw(in.toAbsolutePath())+ FRAME2 + "*" + PNG;
        graphicsMagick_command_line.add(frames);
        graphicsMagick_command_line.add(out.getFileName().toString());

        if ( dbg_GraphicsMagick_call)
            logger.log("execute = "+graphicsMagick_command_line);

        {
            StringBuilder sb = null;
            if (dbg_GraphicsMagick_call) sb = new StringBuilder();
            if (!Execute_command.execute_command_list(graphicsMagick_command_line, folder_icon_cache_dir.toFile(), 2000, sb, logger))
            {
                Static_application_properties.manage_show_GraphicsMagick_install_warning(owner,logger);
                logger.log(warning_GraphicsMagick);
                logger.log(" make_animated_gif_from_all_images_in_folder convert call failed");
                return null;
            }
            if (dbg_GraphicsMagick_call) logger.log(sb.toString());
        }


        /*
        STEP3: clean up the temporary frames
         */
        for (Path p : to_be_cleaned_up)
        {
            try {
                Files.delete(p);
            } catch (IOException e) {
                if (dbg) logger.log(Stack_trace_getter.get_stack_trace("WARNING: cleanup failed "+e));
                else logger.log(("WARNING: cleanup failed "+e));

            }
        }

        if ( dbg) logger.log(" make_animated_gif_from_all_images_in_folder DONE "+out.toAbsolutePath().toString());

        return out;
    }
}
