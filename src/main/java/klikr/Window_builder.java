// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Window;
import klikr.browsers.*;
import klikr.browser_core.comparators.Last_access_comparator;
import klikr.browser_core.virtual_landscape.Scroll_position_cache;
import klikr.browser_core.virtual_landscape.Shutdown_target;
import klikr.path_lists.Path_list_provider;
import klikr.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Window_builder
//**********************************************************
{
    private static final boolean dbg = false;
    public final Window_type window_type;
    public final Path_list_provider path_list_provider;
    public final Rectangle2D rectangle;
    public final Shutdown_target shutdown_target; // if null, there is no previous guy to shutdown
    public final Window owner;
    public final Application application;

    //**********************************************************
    private Window_builder(
            Application application,
            Window_type window_type,
            Path_list_provider path_list_provider,
            Rectangle2D rectangle,
            Shutdown_target shutdown_target,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        this.application = application;
        this.window_type = window_type;
        this.rectangle = rectangle;
        this.shutdown_target = shutdown_target;
        this.owner = owner;
        this.path_list_provider = path_list_provider;
    }

    //**********************************************************
    private String to_string()
    //**********************************************************
    {
        return "Window builder for shutdown_target="+shutdown_target;
    }


    //**********************************************************
    public static Owner_provider additional_no_past(Application application, Window_type window_type, Path_list_provider path_list_provider, Window owner, Logger logger)
    //**********************************************************
    {
        record_last_access(path_list_provider, logger);

        Window_builder window_builder = new Window_builder(
                application,
                window_type,
                path_list_provider,
                null,
                null,
                owner,
                logger);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ window_builder.to_string() ));
        return get_one_new(window_builder,logger);
    }


    //**********************************************************
    public static void additional_same_folder(
            Application application,
            Window_type window_type,
            Path_list_provider path_list_provider,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        // make sure the new window is scrolled at the same position
        if ( top_left != null)
        {
            Scroll_position_cache.scroll_position_cache_write(path_list_provider.get_key(),top_left.toAbsolutePath().normalize().toString(),"additional same folder",logger);
        };

        Rectangle2D rectangle = new Rectangle2D(originator.getX()+100,originator.getY()+100,originator.getWidth()-100,originator.getHeight()-100);

        Window_builder window_builder =  new Window_builder(
                application,
                window_type,
                path_list_provider,
                rectangle,
                null,
                originator,
                logger);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ window_builder.to_string() ));
        get_one_new(window_builder,logger);
    }


    //**********************************************************
    public static void additional_same_folder_fat_tall(
            Application application,
            Window_type window_type,
            Path_list_provider path_list_provider,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(application,window_type,path_list_provider,5,top_left,originator ,logger);

    }
    //**********************************************************
    public static void additional_same_folder_twin(
            Application application,
            Window_type window_type,
            Path_list_provider path_list_provider,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(application,window_type,path_list_provider,2,top_left,originator,logger);
    }
    //**********************************************************
    public static void additional_same_folder_ratio(
            Application application,
            Window_type window_type,
            Path_list_provider path_list_provider,
            int ratio,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        if ( top_left != null)
        {
            Scroll_position_cache.scroll_position_cache_write(path_list_provider.get_key(),top_left.toAbsolutePath().normalize().toString(),"additional same folder ratio",logger);
        }

        ObservableList<Screen> intersecting_screens = Screen.getScreensForRectangle(originator.getX(), originator.getY(), originator.getWidth(), originator.getHeight());

        Screen s = intersecting_screens.get(0);
        logger.log("    getBounds" + s.getBounds());
        Rectangle2D rectangle = s.getBounds();
        originator.setX(rectangle.getMinX());
        originator.setY(rectangle.getMinY());
        double h = s.getBounds().getHeight();

        // adjust existing window to "fat"
        double ratio_fat = ((double) ratio - 1.0)/ (double) ratio;
        double w_fat = s.getBounds().getWidth() * ratio_fat;
        originator.setWidth(w_fat);
        originator.setHeight(h);

        // create new "tall" window
        double ratio_tall = 1.0 / (double) ratio;
        double w2 = s.getBounds().getWidth() * ratio_tall;
        rectangle = new Rectangle2D(rectangle.getMinX()+w_fat, rectangle.getMinY(), w2, h);

        Window_builder window_builder = new Window_builder(
                application,
                window_type,
                path_list_provider,
                rectangle,
                null,
                originator,
                logger);
        if (dbg) logger.log(("\nadditional_same_folder\n" + window_builder.to_string()));
        get_one_new(window_builder,logger);
    }


    //**********************************************************
    public static void replace_same_folder(
            Application application,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Path_list_provider what_to_browse,
            String key_for_scroll_position_cache,
            Path top_left, // maybe null
            Window originator,
            Logger logger)
    //**********************************************************
    {
        if ( top_left != null)
        {
            Scroll_position_cache.scroll_position_cache_write(
                    key_for_scroll_position_cache,
                    top_left.toAbsolutePath().normalize().toString(),
                    "replace same folder",logger);
        };

        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        Window_builder window_builder =  new Window_builder(
                application,
                window_type,
                what_to_browse,
                rectangle,
                shutdown_target,
                originator,
                logger);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ window_builder.to_string() ));
        get_one_new(window_builder,logger);
    }

    //**********************************************************
    public static void replace_different_folder(
            Application application,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Path_list_provider path_list_provider,
            Path key_for_scroll_position_cache,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        if ( top_left != null)
        {
            Scroll_position_cache.scroll_position_cache_write(
                    key_for_scroll_position_cache.toAbsolutePath().normalize().toString(),
                    top_left.toAbsolutePath().normalize().toString(),
                    "replace_different_folder",logger);
        };

        Path folder_path = path_list_provider.get_folder_path();
        if(folder_path != null)
        {
            // this is a file system
            if ( dbg) logger.log("replace_different_folder new path: " + folder_path.toAbsolutePath());
            Last_access_comparator.set_last_access(folder_path,logger);
        }

        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        Window_builder window_builder =  new Window_builder(
                application,
                window_type,
                path_list_provider,
                rectangle,
                shutdown_target,
                originator,
                logger);
        if ( dbg)
            logger.log(("\nreplace_different_folder\n"+ window_builder.to_string() ));
        get_one_new(window_builder,logger);

    }

    //**********************************************************
    private static Owner_provider get_one_new(Window_builder window_builder, Logger logger)
    //**********************************************************
    {
        Owner_provider returned = null;

        switch (window_builder.window_type)
        {
            case File_system_2D -> returned = new Browser_for_file_system_in_2D(window_builder,logger);
            case File_system_3D -> returned = new Browser_for_file_system_in_3D(window_builder,logger);
            case File_system_diskview -> returned = new Browser_for_disk_footprint(window_builder,logger);
            case Song_playlist -> returned = new Browser_for_song_playlist(window_builder,logger);
            case Image_playlist -> returned = new Browser_for_image_playlist(window_builder,logger);
            case Search_results -> returned = new Browser_for_search_results(window_builder,logger);
        }
        if (window_builder.shutdown_target != null)
        {
            if ( dbg) logger.log("closing previous window");
            window_builder.shutdown_target.shutdown();
        }
        return returned;
    }

    //**********************************************************
    private static void record_last_access(Path_list_provider path_list_provider, Logger logger)
    //**********************************************************
    {
        Path p = path_list_provider.get_folder_path();
        if ( p != null)
        {
            Last_access_comparator.set_last_access(p, logger);
        };
    }

}
