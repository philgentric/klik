// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../Icon_writer_actor.java

package klikr.util.animated_gifs;

import javafx.stage.Window;
import klikr.util.External_application;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.util.cache.Cache_folder;
import klikr.util.cache.Clearable_disk_caches;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.Execute_result;
import klikr.util.execute.actor.Aborter;
import klikr.browser.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.browser.comparators.Aspect_ratio_comparator;
import klikr.properties.*;
import klikr.properties.boolean_features.Booleans;
import klikr.util.execute.Execute_command;
import klikr.util.image.icon_cache.Icon_caching;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
animated-GIF making utility:
makes an animated gif from the pictures inside a folder
 */

//**********************************************************
public class Animated_gif_from_folder_content
//**********************************************************
{

    private final static boolean dbg = false;
    private static boolean dbg_GraphicsMagick_call = true;

    public static final int HOW_MANY_IMAGES_SAMPLED_IN_FOLDER_TO_MAKE_ANIMATED_GIF = 10;

    public static final String FRAME1 = "Frame_";
    public static final String FRAME2 = "_"+FRAME1;
    public static final String PNG = ".png";


    //**********************************************************
    public static Path make_animated_gif_from_images_in_folder(
            Window owner,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            List<File> images_in_folder,
            Klikr_cache<Path, Image_properties> image_properties_cache,
            Aborter aborter,Logger logger)
    //**********************************************************
    {
        //System.out.println(Stack_trace_getter.get_stack_trace("make_animated_gif_from_images_in_folder "+target_folder));
        //if (!Files.isDirectory(target_folder)) {
        //    if (dbg) logger.log("FATAL not a folder " + target_folder);
        //    return null;
        //}

        int icon_size = Non_booleans_properties.get_icon_size(owner);

        String output_animated_gif_name = Icon_caching.make_cache_name(path_list_provider.get_name(), "ANIMATED_FOLDER_" + icon_size, "gif");
        Path folder_icon_cache_dir = Clearable_disk_caches.get_cache_dir(Cache_folder.folder_icon_cache,owner,logger);
        Path returned_path_of_animated_gif = Path.of(folder_icon_cache_dir.toAbsolutePath().toString(), output_animated_gif_name);
        if (Files.exists(returned_path_of_animated_gif)) {
            if (dbg) logger.log(" make_animated_gif_from_all_images_in_folder found in cache ");
            return returned_path_of_animated_gif;
        }
        if (dbg) logger.log(" make_animated_gif_from_all_images_in_folder in = " + path_list_provider.get_name() + " target out = " + returned_path_of_animated_gif);

        if ( aborter.should_abort()) return null;

        Path icon_cache_dir = Clearable_disk_caches.get_cache_dir(Cache_folder.icon_cache,owner, logger);
        //List<Path> to_be_cleaned_up = new ArrayList<>();
        List<Path> paths = new ArrayList<>();
        for (File f : images_in_folder) paths.add(f.toPath());
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Comparator<? super Path> local_comp = Sort_files_by.get_image_comparator(path_list_provider,path_comparator_source,image_properties_cache, owner, x,y,new Aborter("dummy",logger),logger);
        Collections.sort(paths, local_comp);
        if ( dbg) if ( local_comp instanceof Aspect_ratio_comparator) logger.log("comparator = Aspect_ratio_comparator");

        for (int i = 0; i < Math.min(HOW_MANY_IMAGES_SAMPLED_IN_FOLDER_TO_MAKE_ANIMATED_GIF,paths.size()); i++)
        {
            Path p = paths.get(i);
            String local = Icon_caching.make_cache_name(path_list_provider.get_name(), FRAME1 + i, Icon_caching.png_extension);
            Path destination = Path.of(icon_cache_dir.toAbsolutePath().toString(),local);
            generate_padded_icon(p,icon_size,destination,owner,logger);
            //to_be_cleaned_up.add(destination);
        }

        {
            List<String> graphicsMagick_command_line = new ArrayList<>();
            // call GraphicsMagick
            graphicsMagick_command_line.add(External_application.GraphicsMagick.get_command(owner,logger));
            graphicsMagick_command_line.add("convert");
            graphicsMagick_command_line.add("-delay");
            graphicsMagick_command_line.add("30"); // in centiseconds
            String frames = Icon_caching.make_cache_name_raw(path_list_provider.get_name()) + FRAME2 + "*" + PNG;
            graphicsMagick_command_line.add(frames);
            graphicsMagick_command_line.add(returned_path_of_animated_gif.toAbsolutePath().toString());

            if ( dbg_GraphicsMagick_call) logger.log("\n\nexecute = "+graphicsMagick_command_line);
            StringBuilder sb = null;
            if (dbg_GraphicsMagick_call) sb = new StringBuilder();
            Execute_result res = Execute_command.execute_command_list(graphicsMagick_command_line, icon_cache_dir.toFile(), 2000, sb, logger);
            if ( !res.status())
            {
                List<String> verify = new ArrayList<>();
                verify.add(External_application.GraphicsMagick.get_command(owner,logger));
                verify.add("--version");
                String home = System.getProperty(String_constants.USER_HOME);
                Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, logger);
                if ( !res2.status())
                {
                    Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
                }
            }
            else
            {
                logger.log(" make_animated_gif_from_all_images_in_folder "+graphicsMagick_command_line);
            }
            if (dbg_GraphicsMagick_call) logger.log("GraphicsMagick: "+sb.toString());
        }


        /*
        STEP3: clean up the temporary frames
         */
        //cleanup_frames(logger, to_be_cleaned_up);

        if ( dbg) logger.log(" make_animated_gif_from_all_images_in_folder DONE "+returned_path_of_animated_gif.toAbsolutePath().toString());

        return returned_path_of_animated_gif;
    }

    //**********************************************************
    private static void generate_padded_icon(Path p, int icon_ize, Path destination, Window owner, Logger logger)
    //**********************************************************
    {
        // gm convert in.jpg -resize 256x256 -gravity center -background transparent -extent 256x256 padded_icon.png
        List<String> graphicsMagick_command_line = new ArrayList<>();
        // call GraphicsMagick
        graphicsMagick_command_line.add(External_application.GraphicsMagick.get_command(owner,logger));
        graphicsMagick_command_line.add("convert");
        graphicsMagick_command_line.add(p.toAbsolutePath().toString());
        graphicsMagick_command_line.add("-resize");
        graphicsMagick_command_line.add(icon_ize+"x"+icon_ize);
        graphicsMagick_command_line.add("-gravity");
        graphicsMagick_command_line.add("center");
        graphicsMagick_command_line.add("-background");
        graphicsMagick_command_line.add("black");
        graphicsMagick_command_line.add("-extent");
        graphicsMagick_command_line.add(icon_ize+"x"+icon_ize);
        graphicsMagick_command_line.add(destination.toAbsolutePath().toString());

        StringBuilder sb = null;
        if (dbg_GraphicsMagick_call) sb = new StringBuilder();
        Execute_result res = Execute_command.execute_command_list(graphicsMagick_command_line, new File("."), 2000, sb, logger);
        if ( !res.status())
        {
            List<String> verify = new ArrayList<>();
            verify.add(External_application.GraphicsMagick.get_command(owner,logger));
            verify.add("--version");
            String home = System.getProperty(String_constants.USER_HOME);
            Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, logger);
            if ( !res2.status())
            {
                Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
            }
        }
        else
        {
            logger.log("OK: make_animated_gif_from_all_images_in_folder "+graphicsMagick_command_line);
        }
        if (dbg_GraphicsMagick_call) logger.log("GraphicsMagick: "+sb.toString());
    }

    //**********************************************************
    private static void cleanup_frames(Logger logger, List<Path> to_be_cleaned_up)
    //**********************************************************
    {
        for (Path p : to_be_cleaned_up)
        {
            System.out.println("erasing tmp frame : "+p.toAbsolutePath());
            try {
                Files.delete(p);
            } catch (IOException e) {
                if (dbg) logger.log(Stack_trace_getter.get_stack_trace("WARNING: cleanup failed "+e));
                else logger.log(("WARNING: cleanup failed "+e));

            }
        }
    }
}
