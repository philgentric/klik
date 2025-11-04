// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Window;
import klik.audio.Song_playlist_app;
import klik.audio.Song_playlist_browser;
import klik.browser.classic.Browser;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.change.history.History_engine;
import klik.change.history.History_item;
import klik.in3D.Circle_3D;
import klik.path_lists.Path_list_provider;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

//**********************************************************
public class New_context
//**********************************************************
{
    private static final boolean dbg = false;
    public final Path_list_provider path_list_provider;
    public final Rectangle2D rectangle;
    public final Shutdown_target shutdown_target; // if null, there is no previous guy to shutdown
    public final Window originator;
    public final Context_type context_type;

    //**********************************************************
    private New_context(
            Context_type context_type,
            Path_list_provider path_list_provider,
            Rectangle2D rectangle,
            Shutdown_target shutdown_target,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        this.context_type = context_type;
        this.rectangle = rectangle;
        this.shutdown_target = shutdown_target;
        this.originator = originator;

/*        if ( path_list_provider == null) {
            Path target = Paths.get(System.getProperty(Non_booleans_properties.USER_HOME));
            if (Booleans.get_boolean_defaults_to_true(Feature.Reload_last_folder_on_startup.name(), originator)) {
                List<History_item> l = History_engine.get(originator).get_all_history_items();
                if (!l.isEmpty()) {
                    History_item h = History_engine.get(originator).get_all_history_items().get(0);
                    if (h != null) {
                        target = Path.of(h.value);
                        logger.log("reloading last folder from history:" + target);
                    }
                }
            }
        }*/
        this.path_list_provider = path_list_provider;
    }

    //**********************************************************
    private String to_string()
    //**********************************************************
    {
        String returned = "shutdown_target="+shutdown_target;
        return returned;
    }


    //**********************************************************
    public static void additional_no_past(Context_type context_type, Path_list_provider path_list_provider, Window originator, Logger logger)
    //**********************************************************
    {
        New_context context = new New_context(
                context_type,
                path_list_provider,
                null,
                null,
                originator,
                logger);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        get_one_new(context,logger);
    }

    //**********************************************************
    public static void additional_same_folder(
            Context_type context_type, Path_list_provider path_list_provider,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        // make sure the new window is scrolled at the same position
        Browsing_caches.scroll_position_cache_write(path_list_provider.get_folder_path(),top_left);

        Rectangle2D rectangle = new Rectangle2D(originator.getX()+100,originator.getY()+100,originator.getWidth()-100,originator.getHeight()-100);

        New_context context =  new New_context(
                context_type,
                path_list_provider,
                rectangle,
                null,
                originator,
                logger);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ context.to_string() ));
        get_one_new(context,logger);
    }


    //**********************************************************
    public static void additional_same_folder_fat_tall(
            Context_type context_type, Path_list_provider path_list_provider,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(context_type,path_list_provider,5,top_left,originator ,logger);

    }
    //**********************************************************
    public static void additional_same_folder_twin(
            Context_type context_type, Path_list_provider path_list_provider,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(context_type,path_list_provider,2,top_left,originator,logger);
    }
    //**********************************************************
    public static void additional_same_folder_ratio(
            Context_type context_type, Path_list_provider path_list_provider,
            int ratio,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        Browsing_caches.scroll_position_cache_write(path_list_provider.get_folder_path(),top_left);

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

        New_context context = new New_context(
                context_type,
                path_list_provider,
                rectangle,
                null,
                originator,
                logger);
        if (dbg) logger.log(("\nadditional_same_folder\n" + context.to_string()));
        get_one_new(context,logger);
    }


    //**********************************************************
    public static void replace_same_folder(
            Shutdown_target shutdown_target,
            Context_type context_type,
            Path_list_provider path_list_provider,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        Browsing_caches.scroll_position_cache_write(path_list_provider.get_folder_path(),top_left);

        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        New_context context =  new New_context(
                context_type,
                path_list_provider,
                rectangle,
                shutdown_target,
                originator,
                logger);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ context.to_string() ));
        get_one_new(context,logger);
    }

    //**********************************************************
    public static void replace_different_folder(
            Shutdown_target shutdown_target,
            Context_type context_type, Path_list_provider path_list_provider,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        if ( dbg) logger.log("replace_different_folder new path: " + path_list_provider.get_folder_path().toAbsolutePath());
        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        New_context context =  new New_context(
                context_type,
                path_list_provider,
                rectangle,
                shutdown_target,
                originator,
                logger);
        if ( dbg) logger.log(("\nreplace_different_folder\n"+ context.to_string() ));
        get_one_new(context,logger);

    }

    private static void get_one_new(New_context context, Logger logger)
    {
        switch (context.context_type)
        {
            case File_system_2D -> new Browser(context,logger);
            case File_system_3D -> new Circle_3D(context,logger);
            case Song_playlist_1D -> new Song_playlist_browser(context,logger);

        }
    }

}
