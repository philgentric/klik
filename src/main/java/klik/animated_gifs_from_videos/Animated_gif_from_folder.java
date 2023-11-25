package klik.animated_gifs_from_videos;

import javafx.scene.image.Image;
import klik.browser.icons.Icon_writer_actor;
import klik.files_and_paths.Files_and_Paths;
import klik.properties.Static_application_properties;
import klik.util.Execute_command;
import klik.util.From_disk;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static klik.browser.icons.Icon_factory_actor.png_extension;
import static klik.browser.icons.Icon_writer_actor.make_cache_name;

/*
animated-GIF making utility:
makes an animated gif from the pictures inside a folder
 */

//**********************************************************
public class Animated_gif_from_folder
//**********************************************************
{
    public static final String warning_IMAGEMAGIC = "This feature requires ImageMagic\n . for mac: brew install imagemagick\n . for other systems download from imagemagic.org\n";

    private final static boolean dbg = false;
    public static final String FRAME1 = "Frame_";
    public static final String FRAME2 = "_"+FRAME1;
    public static final String PNG = ".png";
    private static boolean dbg_imagemagic_call = true;

    //**********************************************************
    public static Path make_animated_gif_from_all_images_in_folder(Path in, List<File> images_in_folder, Logger logger)
    //**********************************************************
    {
        if (! Files.isDirectory(in))
        {
            if ( dbg) logger.log("FATAL not a folder "+in);
            return null;
        }

        Collections.sort(images_in_folder);
        Path actual_icon_cache_dir = Files_and_Paths.get_icon_cache_dir(logger);
        Path folder_icon_cache_dir = Files_and_Paths.get_folder_icon_cache_dir(logger);

        int icon_size = Static_application_properties.get_icon_size(logger);
        String output_animated_gif_name = make_cache_name(in,"ANIMATED_FOLDER_"+icon_size, "gif");
        Path out = Path.of(folder_icon_cache_dir.toAbsolutePath().toString(),output_animated_gif_name);
        if ( Files.exists(out))
        {
            if ( dbg)
                logger.log(" make_animated_gif_from_all_images_in_folder found in cache ");
            return out;
        }

        if ( dbg) logger.log(" make_animated_gif_from_all_images_in_folder in = "+in);
        if ( dbg) logger.log(" make_animated_gif_from_all_images_in_folder target out = "+out);

        List<String> l = new ArrayList<>();
        l.add("convert");

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
            File f = images_in_folder.get(i);
            String icon_name = make_cache_name(f.toPath(),tag, png_extension);
            Path icon_path = Path.of(actual_icon_cache_dir.toAbsolutePath().toString(),icon_name);
            //logger.log("   +icon_path = "+icon_path);

            Image image = From_disk.load_icon_from_disk_cache(f.toPath(), actual_icon_cache_dir, icon_size, tag, png_extension, false,logger);
            if ( image == null)
            {
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
            String local = make_cache_name(in,FRAME1+i, png_extension);
            Path icon_path2 = Path.of(folder_icon_cache_dir.toAbsolutePath().toString(),local);
            try {
                Files.copy(icon_path, icon_path2, REPLACE_EXISTING);
                if ( dbg) logger.log("copy DONE "+icon_path2);
                to_be_cleaned_up.add(icon_path2);

            } catch (IOException e) {
                logger.log("copy failed "+e);
               continue;
            }
            actual++;
            if ( actual> MAX) break;
        }

        if ( actual < 2) return null; // abort

        l.add("-delay");
        l.add("30"); // in centiseconds
        String frames = Icon_writer_actor.clean_name(in.toAbsolutePath().toString())+ FRAME2 + "*" + PNG;
        l.add(frames);
        l.add(out.getFileName().toString());

        if ( dbg_imagemagic_call)
            logger.log("execute = "+l);

        {
            StringBuilder sb = null;
            if (dbg_imagemagic_call) sb = new StringBuilder();
            if (!Execute_command.execute_command_list(l, folder_icon_cache_dir.toFile(), 2000, sb, logger)) {
                logger.log(warning_IMAGEMAGIC);
                return null;
            }
            if (dbg_imagemagic_call) logger.log(sb.toString());
        }


        // clean up
        for (Path p : to_be_cleaned_up) {
            try {
                Files.delete(p);
            } catch (IOException e) {
                logger.log(Stack_trace_getter.get_stack_trace("WARNING: cleanup failed "+e));
            }
        }
        
        return out;
    }
}
