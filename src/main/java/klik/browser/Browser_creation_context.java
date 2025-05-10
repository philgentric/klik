package klik.browser;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Browser_creation_context
//**********************************************************
{
    private static final boolean dbg = false;
    public final String folder_path;
    public final Rectangle2D rectangle;
    public final Shutdown_target shutdown_target; // if null, there is no previous guy to shutdown
    //**********************************************************
    private Browser_creation_context(
            String dir,
            Rectangle2D rectangle,
            Shutdown_target shutdown_target)
    //**********************************************************
    {
        this.rectangle = rectangle;
        this.shutdown_target = shutdown_target;
        this.folder_path = dir;
    }

    //**********************************************************
    private String to_string()
    //**********************************************************
    {
        String returned = "shutdown_target="+shutdown_target;
        return returned;
    }

    //**********************************************************
    public static void first(String path, Logger logger)
    //**********************************************************
    {
        Browser_creation_context context = new Browser_creation_context(
                path,
                null,
                null);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        new Browser(context, logger);
    }

    //**********************************************************
    public static void additional_no_past(String path, Logger logger)
    //**********************************************************
    {
        Browser_creation_context context = new Browser_creation_context(
                path,
                null,
                null);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        new Browser(context, logger);
    }

    //**********************************************************
    public static void additional_same_folder(String parent_displayed_folder_path, Window owner, Path top_left,Logger logger)
    //**********************************************************
    {
        Rectangle2D rectangle = new Rectangle2D(owner.getX()+100,owner.getY()+100,owner.getWidth()-100,owner.getHeight()-100);

        // make sure the new window is scrolled at the same position
        Virtual_landscape.scroll_position_cache.put(parent_displayed_folder_path,top_left);
        Browser_creation_context context =  new Browser_creation_context(
                parent_displayed_folder_path,
                rectangle,
                null);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }


    //**********************************************************
    public static void additional_same_folder_fat_tall(String parent_displayed_folder_path, Window parent_window,  Path top_left,Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(parent_displayed_folder_path,parent_window ,5,top_left,logger);

    }
    //**********************************************************
    public static void additional_same_folder_twin(String parent_displayed_folder_path, Window parent_window,  Path top_left,Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(parent_displayed_folder_path,parent_window,2,top_left,logger);
    }
    //**********************************************************
    public static void additional_same_folder_ratio(
            String parent_displayed_folder_path,
            Window parent_window,
            int ratio,  Path top_left,Logger logger)
    //**********************************************************
    {
        Virtual_landscape.scroll_position_cache.put(parent_displayed_folder_path,top_left);

        ObservableList<Screen> intersecting_screens = Screen.getScreensForRectangle(parent_window.getX(), parent_window.getY(), parent_window.getWidth(), parent_window.getHeight());

        Screen s = intersecting_screens.get(0);
        logger.log("    getBounds" + s.getBounds());
        Rectangle2D rectangle = s.getBounds();
        parent_window.setX(rectangle.getMinX());
        parent_window.setY(rectangle.getMinY());
        double h = s.getBounds().getHeight();

        // adjust existing window to "fat"
        double ratio_fat = ((double) ratio - 1.0)/ (double) ratio;
        double w_fat = s.getBounds().getWidth() * ratio_fat;
        parent_window.setWidth(w_fat);
        parent_window.setHeight(h);

        // create new "tall" window
        double ratio_tall = 1.0 / (double) ratio;
        double w2 = s.getBounds().getWidth() * ratio_tall;
        rectangle = new Rectangle2D(rectangle.getMinX()+w_fat, rectangle.getMinY(), w2, h);

        Browser_creation_context context = new Browser_creation_context(
                parent_displayed_folder_path,
                rectangle,
                null);
        if (dbg) logger.log(("\nadditional_same_folder\n" + context.to_string()));
        new Browser(context, logger);
    }


    //**********************************************************
    public static void additional_different_folder(
            String path,
            Window parent_window,
            Logger logger)
    //**********************************************************
    {
        Rectangle2D rectangle = new Rectangle2D(parent_window.getX()+100,parent_window.getY()+100,parent_window.getWidth()-100,parent_window.getHeight()-100);
        Browser_creation_context context =  new Browser_creation_context(
                path,
                rectangle,
                null);
        if ( dbg) logger.log(("\nadditional_different_folder\n"+ context.to_string() ));
        new Browser(context, logger);
    }

    //**********************************************************
    public static void replace_same_folder(
            Shutdown_target shutdown_target,
            String path,
            Window parent_window,
            Path top_left,
            Logger logger)
    //**********************************************************
    {
        Virtual_landscape.scroll_position_cache.put(path,top_left);

        Rectangle2D rectangle = new Rectangle2D(parent_window.getX(),parent_window.getY(),parent_window.getWidth(),parent_window.getHeight());
        Browser_creation_context context =  new Browser_creation_context(
                path,
                rectangle,
                shutdown_target);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ context.to_string() ));
        new Browser(context, logger);
    }

    //**********************************************************
    public static void replace_different_folder(
            Shutdown_target shutdown_target,
            String path,
            Window parent_window,
            Logger logger)
    //**********************************************************
    {
        logger.log(("\nreplace_different_folder\nshutdown_target="+shutdown_target+"\npath="+path+"\nparent_window= "+parent_window  ));

        Rectangle2D rectangle = new Rectangle2D(parent_window.getX(),parent_window.getY(),parent_window.getWidth(),parent_window.getHeight());
        Browser_creation_context context =  new Browser_creation_context(
                path,
                rectangle,
                shutdown_target);
        if ( dbg) logger.log(("\nreplace_different_folder\n"+ context.to_string() ));
        new Browser(context, logger);

    }

}
