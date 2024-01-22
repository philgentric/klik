package klik.browser;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import klik.files_and_paths.Files_and_Paths;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.System_out_logger;

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
    public final Path top_left_in_parent;
    public final Path scroll_to;
    public final Browser old_browser; // if null, there is no previous guy
    //public final My_Stage stage; // if null, there is no previous guy

    //**********************************************************
    Browser_creation_context(My_Stage previous_stage, Path dir, Rectangle2D rectangle, boolean keepOffset, boolean additional_window, boolean move_a_bit_, Path previousTopLeft, Path scrollTo, Browser oldBrowser)
    //**********************************************************
    {
        //this.stage = stage;
        this.rectangle = rectangle;
        keep_offset = keepOffset;
        this.additional_window = additional_window;
        if ( !additional_window) previous_stage.the_Stage.close();
        move_a_bit = move_a_bit_;
        top_left_in_parent = previousTopLeft;
        //System.out.println("top_left_in_parent="+top_left_in_parent);
        scroll_to = scrollTo;
        //System.out.println("scroll_to="+scroll_to);
        old_browser = oldBrowser;
        Path icon_cache_dir = Files_and_Paths.get_icon_cache_dir(new System_out_logger());
        if (dir.toAbsolutePath().equals(icon_cache_dir.toAbsolutePath()))
        {
            if ( Popups.popup_ask_for_confirmation(
                    previous_stage.the_Stage,
                    "Browsing klik icon cache is not such a good idea!",
                    "Because it causes an explosion (recursive) of new icons... of icons ... of icons....\n" +
                            "If you insist, ok, but remember to clear the icon cache ASAP after! ",new System_out_logger()))
            {
                // ok, its your disk man
                this.folder_path = dir;
            }
            else
            {
                this.folder_path = dir.getParent();

            }
        }
        else
        {
            this.folder_path = dir;
        }
    }

    //**********************************************************
    private String to_string()
    //**********************************************************
    {
        String returned = "keep_offset="+keep_offset+"\nadditional_window="+additional_window+"\nsame_place="+ move_a_bit;
        if ( top_left_in_parent!=null) returned += "\ntop_left_in_parent="+top_left_in_parent.toAbsolutePath();
        else returned += "\ntop_left_in_parent is NULL";
        if ( scroll_to!=null) returned += "\nscroll_to="+scroll_to;
        else returned += "\nscroll_to is NULL";
        return returned;
    }

    //**********************************************************
    public static Browser first(My_Stage stage, Path path, Logger logger)
    //**********************************************************
    {
        Browser_creation_context context = new Browser_creation_context(stage, path, null, false, true, false,
                null,
                null,
                null);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        return new Browser(context, logger);
    }

    //**********************************************************
    public static void additional_no_past(Path path, Logger logger)
    //**********************************************************
    {
        My_Stage stage = new My_Stage(new Stage(),logger);
        Browser_creation_context context = new Browser_creation_context(stage, path, null,
                false, true, true,
                null,
                null,
                null);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }

    //**********************************************************
    public static void additional_same_folder(Browser parent, Logger logger)
    //**********************************************************
    {
        My_Stage stage = new My_Stage(new Stage(),logger);
        Browser_creation_context context =  new Browser_creation_context(
                stage,
                parent.displayed_folder_path,
                parent.get_rectangle(),
                false,true,true,
                parent.get_top_left(),
                parent.get_top_left(),
                null);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }

    //**********************************************************
    public static void additional_same_folder_twin(Browser parent, Logger logger)
    //**********************************************************
    {
        Stage parent_stage = parent.my_Stage.the_Stage;
        ObservableList<Screen> intersecting_screens = Screen.getScreensForRectangle(parent_stage.getX(), parent_stage.getY(), parent_stage.getWidth(), parent_stage.getHeight());
        //ObservableList<Screen> screens = Screen.getScreens();
        //for (int i = 0; i < intersecting_screens.size(); i++)

        Screen s = intersecting_screens.get(0);
        logger.log("    getBounds" + s.getBounds());
        Rectangle2D r = s.getBounds();
        parent_stage.setX(r.getMinX());
        parent_stage.setY(r.getMinY());
        double w = s.getBounds().getWidth() / 2;
        parent_stage.setWidth(w);
        double h = s.getBounds().getHeight();
        parent_stage.setHeight(h);

        r = new Rectangle2D(r.getMinX()+w, r.getMinY(), w, h);

        My_Stage stage = new My_Stage(new Stage(), logger);
        Browser_creation_context context = new Browser_creation_context(
                stage,
                parent.displayed_folder_path,
                r,
                false, true, false,
                parent.get_top_left(),
                parent.get_top_left(),
                null);
        if (dbg) logger.log(("\nadditional_same_folder\n" + context.to_string()));
        Browser b = new Browser(context, logger);
    }

    //**********************************************************
    public static void additional_different_folder(Path path, Browser browser, Logger logger)
    //**********************************************************
    {
        My_Stage stage = new My_Stage(new Stage(),logger);
        Browser_creation_context context =  new Browser_creation_context(stage, path, browser.get_rectangle(), false,true,true,
                browser.get_top_left(),
                null,null);
        if ( dbg) logger.log(("\nadditional_different_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }

    //**********************************************************
    public static void replace_same_folder(Browser browser, Logger logger)
    //**********************************************************
    {
        Browser_creation_context context =  new Browser_creation_context(browser.my_Stage,
                browser.displayed_folder_path, browser.get_rectangle(), true,false,false,
                browser.get_top_left(),
                browser.get_top_left(),browser);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }

    //**********************************************************
    public static void replace_different_folder(Path path, Browser browser, Path scroll_to, Logger logger)
    //**********************************************************
    {
        Browser_creation_context context =  new Browser_creation_context(browser.my_Stage, path, browser.get_rectangle(), true,false,false,
                browser.get_top_left(),
                scroll_to,
                browser);
        if ( dbg) logger.log(("\nreplace_different_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);

    }

}
