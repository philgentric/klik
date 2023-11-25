package klik.browser;

import javafx.geometry.Rectangle2D;
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
    public final boolean same_place; // if true the new instance is at the same location on screen s the old one
    public final Path top_left_in_parent;
    public final Path scroll_to;
    public final Browser old_browser; // if null, there is no previous guy
    public final My_Stage stage; // if null, there is no previous guy

    //**********************************************************
    Browser_creation_context(My_Stage stage, Path dir, Rectangle2D rectangle, boolean keepOffset, boolean additional_window, boolean samePlace, Path previousTopLeft, Path scrollTo, Browser oldBrowser)
    //**********************************************************
    {
        this.stage = stage;
        this.rectangle = rectangle;
        keep_offset = keepOffset;
        this.additional_window = additional_window;
        same_place = samePlace;
        top_left_in_parent = previousTopLeft;
        //System.out.println("top_left_in_parent="+top_left_in_parent);
        scroll_to = scrollTo;
        //System.out.println("scroll_to="+scroll_to);
        old_browser = oldBrowser;
        Path icon_cache_dir = Files_and_Paths.get_icon_cache_dir(new System_out_logger());
        if (dir.toAbsolutePath().equals(icon_cache_dir.toAbsolutePath()))
        {
            if ( Popups.popup_ask_for_confirmation(
                    stage.the_Stage,
                    "Browsing klik icon cache is not such a good idea!",
                    "Because it causes an explosion of new icons or icons of icons....\n" +
                            "Remember to clean the icon cache ASAP after ",new System_out_logger()))
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
        String returned = "keep_offset="+keep_offset+"\nadditional_window="+additional_window+"\nsame_place="+same_place;
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
                false, true, false,
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
                false,true,false,
                parent.get_top_left(),
                parent.get_top_left(),
                null);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }

    //**********************************************************
    public static void additional_different_folder(Path path, Browser browser, Logger logger)
    //**********************************************************
    {
        My_Stage stage = new My_Stage(new Stage(),logger);
        Browser_creation_context context =  new Browser_creation_context(stage, path, browser.get_rectangle(), false,true,false,
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
                browser.displayed_folder_path, browser.get_rectangle(), true,false,true,
                browser.get_top_left(),
                browser.get_top_left(),browser);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);
    }

    //**********************************************************
    public static void replace_different_folder(Path path, Browser browser, Path scroll_to, Logger logger)
    //**********************************************************
    {
        Browser_creation_context context =  new Browser_creation_context(browser.my_Stage, path, browser.get_rectangle(), true,false,true,
                browser.get_top_left(),
                scroll_to,
                browser);
        if ( dbg) logger.log(("\nreplace_different_folder\n"+ context.to_string() ));
        Browser b = new Browser(context, logger);

    }

}
