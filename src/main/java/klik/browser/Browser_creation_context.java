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
    public final boolean keep_offset;
    //public final Stage primary_stage;
    public final boolean additional_window; // if false, the old_browser is closed and de-registered, then a new one is used in the SAME window
    public final boolean move_a_bit; // if true the new instance is at the same location on screen s the old one
    public final Shutdown_target shutdown_target; // if null, there is no previous guy to shutdown
    //**********************************************************
    private Browser_creation_context(
            //Stage primary_stage,
            String dir,
            Rectangle2D rectangle,
            boolean keepOffset,
            boolean additional_window,
            boolean move_a_bit_,
            Shutdown_target shutdown_target)
    //**********************************************************
    {
        /*if ( primary_stage  == null)
        {
            System.out.println("FATAL Browser_creation_context: primary_stage is null");
            System.exit(-1);
        }
        this.primary_stage = primary_stage;*/
        this.rectangle = rectangle;
        keep_offset = keepOffset;
        this.additional_window = additional_window;
        move_a_bit = move_a_bit_;
        this.shutdown_target = shutdown_target;
        this.folder_path = dir;
    }

    //**********************************************************
    private String to_string()
    //**********************************************************
    {
        String returned = "keep_offset="+keep_offset+"\nadditional_window="+additional_window+"\nsame_place="+ move_a_bit+" shutdown_target="+shutdown_target;
        return returned;
    }

    //**********************************************************
    public static Browser first(
            //Stage primary_stage,
            String path, Logger logger)
    //**********************************************************
    {
        Browser_creation_context context = new Browser_creation_context(
                //primary_stage,
                path,
                null,
                false,
                true,
                false,
                null);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        return new Browser(context, logger);
    }

    //**********************************************************
    public static Browser additional_no_past(
            //Stage primary_stage,
            String path, Logger logger)
    //**********************************************************
    {
        //My_Stage stage = new My_Stage(new Stage(),null,logger);
        Browser_creation_context context = new Browser_creation_context(
                //primary_stage,
                path,
                null,
                false,
                true,
                true,
                null);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        return new Browser(context, logger);
    }

    //**********************************************************
    public static void additional_same_folder(Rectangle2D rectangle, String parent_displayed_folder_path, Path top_left,Logger logger)
    //**********************************************************
    {
        //My_Stage stage = new My_Stage(new Stage(),null,logger);
        Virtual_landscape.scroll_position_cache.put(parent_displayed_folder_path,top_left);
        Browser_creation_context context =  new Browser_creation_context(
                parent_displayed_folder_path,
                rectangle,
                false,true,true,
                null);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }


    //**********************************************************
    public static void additional_same_folder_fat_tall(String parent_displayed_folder_path, Window parent_window, Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(parent_displayed_folder_path,parent_window ,5,logger);

    }
    //**********************************************************
    public static void additional_same_folder_twin(String parent_displayed_folder_path, Window parent_window, Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(parent_displayed_folder_path,parent_window,2,logger);
    }
    //**********************************************************
    public static void additional_same_folder_ratio(
            String parent_displayed_folder_path,
            Window parent_window,
            int ratio, Logger logger)
    //**********************************************************
    {
        //Virtual_landscape.scroll_position_cache.put(parent.displayed_folder_path,parent.get_top_left());

        ObservableList<Screen> intersecting_screens = Screen.getScreensForRectangle(parent_window.getX(), parent_window.getY(), parent_window.getWidth(), parent_window.getHeight());

        Screen s = intersecting_screens.get(0);
        logger.log("    getBounds" + s.getBounds());
        Rectangle2D r = s.getBounds();
        parent_window.setX(r.getMinX());
        parent_window.setY(r.getMinY());
        double h = s.getBounds().getHeight();

        // adjust existing window to "fat"
        double ratio_fat = ((double) ratio - 1.0)/ (double) ratio;
        double w_fat = s.getBounds().getWidth() * ratio_fat;
        parent_window.setWidth(w_fat);
        parent_window.setHeight(h);

        // create new "tall" window
        double ratio_tall = 1.0 / (double) ratio;
        double w2 = s.getBounds().getWidth() * ratio_tall;
        r = new Rectangle2D(r.getMinX()+w_fat, r.getMinY(), w2, h);

        My_Stage stage = new My_Stage(new Stage(),logger);
        Browser_creation_context context = new Browser_creation_context(
                //parent.primary_stage,
                parent_displayed_folder_path,
                r,
                false, true, false,
                null);
        if (dbg) logger.log(("\nadditional_same_folder\n" + context.to_string()));
        Browser b = new Browser(context, logger);
    }


    //**********************************************************
    public static void additional_different_folder(
            String path,
            Window owner,
            Logger logger)
    //**********************************************************
    {

        Rectangle2D rectangle = new Rectangle2D(owner.getX()+100,owner.getY()+100,owner.getWidth(),owner.getHeight());
        //My_Stage stage = new My_Stage(new Stage(), null,logger);
        Browser_creation_context context =  new Browser_creation_context(
                //primary_stage,
                path,
                rectangle,
                false,
                true,
                true,
                null);
        if ( dbg) logger.log(("\nadditional_different_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }

    //**********************************************************
    public static void replace_same_folder(
            Shutdown_target shutdown_target,
            String path,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        //Virtual_landscape.scroll_position_cache.put(parent.displayed_folder_path,parent.get_top_left());

        Rectangle2D rectangle = new Rectangle2D(owner.getX(),owner.getY(),owner.getWidth(),owner.getHeight());
        Browser_creation_context context =  new Browser_creation_context(
                path,
                rectangle,
                true,
                false,
                false,
                shutdown_target);
        //if ( dbg)
            logger.log(("\nreplace_same_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }

    //**********************************************************
    public static void replace_different_folder(
            Shutdown_target shutdown_target,
            String path,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        logger.log(("\nreplace_different_folder\nshutdown_target="+shutdown_target+"\npath="+path+"\nowner= "+owner  ));

        Rectangle2D rectangle = new Rectangle2D(owner.getX(),owner.getY(),owner.getWidth(),owner.getHeight());

        Browser_creation_context context =  new Browser_creation_context(
                path,
                rectangle,
                true,
                false,
                false,
                shutdown_target);
        //if ( dbg)
        logger.log(("\nreplace_different_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);

    }

}
