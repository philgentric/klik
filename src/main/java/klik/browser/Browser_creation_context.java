package klik.browser;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Screen;
import javafx.stage.Window;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans;
import klik.util.log.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public class Browser_creation_context
//**********************************************************
{
    private static final boolean dbg = false;
    public final String target; // this can be an absolute folder path or a image play list name path
    public final Rectangle2D rectangle;
    public final Shutdown_target shutdown_target; // if null, there is no previous guy to shutdown
    //**********************************************************
    private Browser_creation_context(
            String target,
            Rectangle2D rectangle,
            Shutdown_target shutdown_target)
    //**********************************************************
    {
        this.rectangle = rectangle;
        this.shutdown_target = shutdown_target;
        this.target = target;
    }

    //**********************************************************
    public static void open_new_image_playlist(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        Rectangle2D rectangle = new Rectangle2D(owner.getX()+100,owner.getY()+100,owner.getWidth()-100,owner.getHeight()-100);

        String name = path.toAbsolutePath().toString();

        Browser_creation_context context = new Browser_creation_context(name, rectangle, null);
        new Image_playlist(context, logger);
    }

    //**********************************************************
    public static void replace_image_playlist(
            Shutdown_target shutdown_target,
            Path path,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        Rectangle2D rectangle = new Rectangle2D(owner.getX()+100,owner.getY()+100,owner.getWidth()-100,owner.getHeight()-100);

        String name = path.toAbsolutePath().toString();

        Browser_creation_context context = new Browser_creation_context(name, rectangle, shutdown_target);
        new Image_playlist(context, logger);
    }

    //**********************************************************
    public static void create_new_image_playlist(Window owner, Logger logger)
    //**********************************************************
    {
        Rectangle2D rectangle = new Rectangle2D(owner.getX()+100,owner.getY()+100,owner.getWidth()-100,owner.getHeight()-100);

        Path p = create_new_playlist_file(owner, logger);
        String name = p.toAbsolutePath().toString();

        Browser_creation_context context = new Browser_creation_context(name, rectangle, null);
        new Image_playlist(context, logger);
    }

    //**********************************************************
    public static Path create_new_playlist_file(Window owner, Logger logger)
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(My_I18n.get_I18n_string("New_Image_Playlist_File", logger));
        Look_and_feel_manager.set_dialog_look(dialog);
        dialog.initOwner(owner);
        dialog.setWidth(1000);
        dialog.setTitle(My_I18n.get_I18n_string("New_Image_Playlist_File", logger));
        dialog.setHeaderText(My_I18n.get_I18n_string("Enter_Name_Of_New_Image_Playlist_File", logger));
        dialog.setContentText(My_I18n.get_I18n_string("New_Image_Playlist_File_Name", logger));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();

            for (int i = 0; i < 10; i++) {
                try {
                    String local = new_name;
                    if ( !local.endsWith("." + Playlist_path_list_provider.KLIK_IMAGE_PLAYLIST_EXTENSION)) local += "." + Playlist_path_list_provider.KLIK_IMAGE_PLAYLIST_EXTENSION;
                    String home = System.getProperty(Non_booleans.USER_HOME);
                    Path new_playlist_file = Path.of( home, local);
                    Files.createFile(new_playlist_file); //Files.createDirectory(new_dir);
                    Virtual_landscape.scroll_position_cache.put(Path.of( Non_booleans.USER_HOME).toAbsolutePath().toString(), new_playlist_file);
                    return new_playlist_file;
                } catch (IOException e) {
                    logger.log("new directory creation FAILED: " + e);
                    new_name += "_";
                }
            }

        }
        return null;
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
