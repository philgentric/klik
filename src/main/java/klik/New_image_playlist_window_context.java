package klik;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Screen;
import javafx.stage.Window;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.experimental.image_playlist.Image_playlist_browser;
import klik.experimental.image_playlist.Playlist_path_list_provider;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public class New_image_playlist_window_context
//**********************************************************
{
    private static final boolean dbg = false;
    public final Path path; // this can be an absolute folder path or a image play list FILE  path
    public final Rectangle2D rectangle;
    public final Shutdown_target shutdown_target; // if null, there is no previous guy to shutdown
    public final Window originator;

    //**********************************************************
    private New_image_playlist_window_context(
            Path target,
            Rectangle2D rectangle,
            Shutdown_target shutdown_target,
            Window originator)
    //**********************************************************
    {
        this.path = target;
        this.rectangle = rectangle;
        this.shutdown_target = shutdown_target;
        this.originator = originator;
    }

    //**********************************************************
    private String to_string()
    //**********************************************************
    {
        String returned = "shutdown_target="+shutdown_target;
        return returned;
    }


    //**********************************************************
    public static Window_provider additional_no_past(Path new_path, Window originator, Logger logger)
    //**********************************************************
    {
        New_image_playlist_window_context context = new New_image_playlist_window_context(
                new_path,
                null,
                null,
                originator);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        return new Image_playlist_browser(context.path,context.shutdown_target, context.rectangle, logger);
    }

    //**********************************************************
    public static void additional_same_folder(
            Path new_and_old_path,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        // make sure the new window is scrolled at the same position
        Browsing_caches.scroll_position_cache_write(new_and_old_path,top_left);

        Rectangle2D rectangle = new Rectangle2D(originator.getX()+100,originator.getY()+100,originator.getWidth()-100,originator.getHeight()-100);

        New_image_playlist_window_context context =  new New_image_playlist_window_context(
                new_and_old_path,
                rectangle,
                null,
                originator);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ context.to_string() ));
        new Image_playlist_browser(context.path,context.shutdown_target, context.rectangle, logger);
    }


    //**********************************************************
    public static void additional_same_folder_fat_tall(
            Path new_and_old_path,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(new_and_old_path,5,top_left,originator ,logger);

    }
    //**********************************************************
    public static void additional_same_folder_twin(
            Path new_and_old_path,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(new_and_old_path,2,top_left,originator,logger);
    }
    //**********************************************************
    public static void additional_same_folder_ratio(
            Path new_and_old_path,
            int ratio,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        Browsing_caches.scroll_position_cache_write(new_and_old_path,top_left);

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

        New_image_playlist_window_context context = new New_image_playlist_window_context(
                new_and_old_path,
                rectangle,
                null,
                originator);
        if (dbg) logger.log(("\nadditional_same_folder\n" + context.to_string()));
        new Image_playlist_browser(context.path,context.shutdown_target, context.rectangle, logger);
    }


    //**********************************************************
    public static void replace_same_folder(
            Shutdown_target shutdown_target,
            Path old_and_new_path,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        Browsing_caches.scroll_position_cache_write(old_and_new_path,top_left);

        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        New_image_playlist_window_context context =  new New_image_playlist_window_context(
                old_and_new_path,
                rectangle,
                shutdown_target,
                originator);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ context.to_string() ));
        new Image_playlist_browser(context.path,context.shutdown_target, context.rectangle, logger);
    }

    //**********************************************************
    public static void replace_different_folder(
            Shutdown_target shutdown_target,
            Path new_path,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        if ( dbg) logger.log("replace_different_folder new path: " + new_path.toAbsolutePath());
        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        New_image_playlist_window_context context =  new New_image_playlist_window_context(
                new_path,
                rectangle,
                shutdown_target,
                originator);
        if ( dbg) logger.log(("\nreplace_different_folder\n"+ context.to_string() ));
        new Image_playlist_browser(context.path,context.shutdown_target, context.rectangle, logger);

    }


    //**********************************************************
    public static Path create_new_playlist_file(Window owner, Logger logger)
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(My_I18n.get_I18n_string("New_Image_Playlist_File", owner, logger));
        Look_and_feel_manager.set_dialog_look(dialog, owner, logger);
        dialog.initOwner(owner);
        dialog.setWidth(1000);
        dialog.setTitle(My_I18n.get_I18n_string("New_Image_Playlist_File", owner, logger));
        dialog.setHeaderText(My_I18n.get_I18n_string("Enter_Name_Of_New_Image_Playlist_File", owner, logger));
        dialog.setContentText(My_I18n.get_I18n_string("New_Image_Playlist_File_Name", owner, logger));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();

            for (int i = 0; i < 10; i++) {
                try {
                    String local = new_name;
                    if ( !local.endsWith("." + Playlist_path_list_provider.KLIK_IMAGE_PLAYLIST_EXTENSION)) local += "." + Playlist_path_list_provider.KLIK_IMAGE_PLAYLIST_EXTENSION;
                    String home = System.getProperty(Non_booleans_properties.USER_HOME);
                    Path new_playlist_file = Path.of( home, local);
                    Files.createFile(new_playlist_file); //Files.createDirectory(new_dir);
                    //Virtual_landscape.scroll_position_cache.put(Path.of( Non_booleans_properties.USER_HOME).toAbsolutePath().toString(), new_playlist_file);
                    return new_playlist_file;
                } catch (IOException e) {
                    logger.log("new directory creation FAILED: " + e);
                    new_name += "_";
                }
            }

        }
        return null;
    }


}
