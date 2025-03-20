package klik.browser;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Browser_creation_context
//**********************************************************
{
    private static final boolean dbg = false;
    public final Path folder_path;
    public final Rectangle2D rectangle;
    public final boolean keep_offset;
    public final boolean additional_window; // if false, the old_browser is closed and de-registered, then a new one is used in the SAME window
    public final boolean move_a_bit; // if true the new instance is at the same location on screen s the old one
    public Browser old_browser; // if null, there is no previous guy
    //**********************************************************
    private Browser_creation_context(
            My_Stage previous_stage,
            Path dir,
            Rectangle2D rectangle,
            boolean keepOffset,
            boolean additional_window,
            boolean move_a_bit_,
            Browser oldBrowser)
    //**********************************************************
    {
        this.rectangle = rectangle;
        keep_offset = keepOffset;
        this.additional_window = additional_window;
        if ( !additional_window) previous_stage.the_Stage.close();
        move_a_bit = move_a_bit_;
        old_browser = oldBrowser;
        this.folder_path = dir;
    }

    //**********************************************************
    private String to_string()
    //**********************************************************
    {
        String returned = "keep_offset="+keep_offset+"\nadditional_window="+additional_window+"\nsame_place="+ move_a_bit;
        return returned;
    }

    //**********************************************************
    public static Browser first(My_Stage stage, Path path, Logger logger)
    //**********************************************************
    {
        Browser_creation_context context = new Browser_creation_context(
                stage,
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
    public static Browser additional_no_past(Path path, Logger logger)
    //**********************************************************
    {
        My_Stage stage = new My_Stage(new Stage(),logger);
        Browser_creation_context context = new Browser_creation_context(
                stage,
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
    public static void additional_same_folder(Browser parent, Logger logger)
    //**********************************************************
    {
        My_Stage stage = new My_Stage(new Stage(),logger);
        Browser.scroll_position_cache.put(parent.displayed_folder_path,parent.get_top_left());
        Browser_creation_context context =  new Browser_creation_context(
                stage,
                parent.displayed_folder_path,
                parent.get_rectangle(),
                false,true,true,
                null);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }


    //**********************************************************
    public static void additional_same_folder_fat_tall(Browser parent, Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(parent,5,logger);

    }
    //**********************************************************
    public static void additional_same_folder_twin(Browser parent, Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(parent,2,logger);
    }
    //**********************************************************
    public static void additional_same_folder_ratio(Browser parent, int ratio, Logger logger)
    //**********************************************************
    {
        Browser.scroll_position_cache.put(parent.displayed_folder_path,parent.get_top_left());

        Stage parent_stage = parent.my_Stage.the_Stage;
        ObservableList<Screen> intersecting_screens = Screen.getScreensForRectangle(parent_stage.getX(), parent_stage.getY(), parent_stage.getWidth(), parent_stage.getHeight());

        Screen s = intersecting_screens.get(0);
        logger.log("    getBounds" + s.getBounds());
        Rectangle2D r = s.getBounds();
        parent_stage.setX(r.getMinX());
        parent_stage.setY(r.getMinY());
        double h = s.getBounds().getHeight();

        // adjust existing window to "fat"
        double ratio_fat = ((double) ratio - 1.0)/ (double) ratio;
        double w_fat = s.getBounds().getWidth() * ratio_fat;
        parent_stage.setWidth(w_fat);
        parent_stage.setHeight(h);

        // create new "tall" window
        double ratio_tall = 1.0 / (double) ratio;
        double w2 = s.getBounds().getWidth() * ratio_tall;
        r = new Rectangle2D(r.getMinX()+w_fat, r.getMinY(), w2, h);

        My_Stage stage = new My_Stage(new Stage(), logger);
        Browser_creation_context context = new Browser_creation_context(
                stage,
                parent.displayed_folder_path,
                r,
                false, true, false,
                null);
        if (dbg) logger.log(("\nadditional_same_folder\n" + context.to_string()));
        Browser b = new Browser(context, logger);
    }


    //**********************************************************
    public static void additional_different_folder(
            Path path,
            Rectangle2D rectangle,
            Logger logger)
    //**********************************************************
    {

        My_Stage stage = new My_Stage(new Stage(),logger);
        Browser_creation_context context =  new Browser_creation_context(
                stage,
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
    public static void replace_same_folder(Browser parent, Logger logger)
    //**********************************************************
    {
        Browser.scroll_position_cache.put(parent.displayed_folder_path,parent.get_top_left());

        Browser_creation_context context =  new Browser_creation_context(
                parent.my_Stage,
                parent.displayed_folder_path,
                parent.get_rectangle(),
                true,
                false,
                false,
                parent);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }

    //**********************************************************
    public static void replace_different_folder(Path path, Browser parent, Logger logger)
    //**********************************************************
    {
        Browser.scroll_position_cache.put(parent.displayed_folder_path,parent.get_top_left());


        Browser_creation_context context =  new Browser_creation_context(
                parent.my_Stage,
                path,
                parent.get_rectangle(),
                true,
                false,
                false,
                parent);
        if ( dbg) logger.log(("\nreplace_different_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);

    }

}
