package klik;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Window;
import klik.browser.classic.Browser;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class New_window_context
//**********************************************************
{
    private static final boolean dbg = false;
    public final Path target_path; // this can be an absolute folder path or a image play list FILE  path
    public final Rectangle2D rectangle;
    public final Shutdown_target shutdown_target; // if null, there is no previous guy to shutdown
    public final Window originator;

    //**********************************************************
    private New_window_context(
            Path target,
            Rectangle2D rectangle,
            Shutdown_target shutdown_target,
            Window originator)
    //**********************************************************
    {
        //this.wtf_port = port;
        this.rectangle = rectangle;
        this.shutdown_target = shutdown_target;
        this.target_path = target;
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
        New_window_context context = new New_window_context(
                new_path,
                null,
                null,
                originator);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        return new Browser(context, logger);
    }

    //**********************************************************
    public static void additional_same_folder(
            //int port,
            Path new_and_old_path,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        // make sure the new window is scrolled at the same position
        Browsing_caches.scroll_position_cache_write(new_and_old_path,top_left);

        Rectangle2D rectangle = new Rectangle2D(originator.getX()+100,originator.getY()+100,originator.getWidth()-100,originator.getHeight()-100);

        New_window_context context =  new New_window_context(
                //port,
                new_and_old_path,
                rectangle,
                null,
                originator);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ context.to_string() ));
        new Browser(context, logger);
    }


    //**********************************************************
    public static void additional_same_folder_fat_tall(
            //int port,
            Path new_and_old_path,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(
                //port,
                new_and_old_path,5,top_left,originator ,logger);

    }
    //**********************************************************
    public static void additional_same_folder_twin(
            //int port,
            Path new_and_old_path,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(
                //port,
                new_and_old_path,2,top_left,originator,logger);
    }
    //**********************************************************
    public static void additional_same_folder_ratio(
            //int port,
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

        New_window_context context = new New_window_context(
                //port,
                new_and_old_path,
                rectangle,
                null,
                originator);
        if (dbg) logger.log(("\nadditional_same_folder\n" + context.to_string()));
        new Browser(context, logger);
    }

/*
    //**********************************************************
    public static void additional_different_folder(
            Path new_path,
            Window parent_window,
            Path old_path,
            Path top_left,
            Logger logger)
    //**********************************************************
    {
        Virtual_landscape.scroll_position_cache.put(old_path.toAbsolutePath().toString(),top_left);


        Rectangle2D rectangle = new Rectangle2D(parent_window.getX()+100,parent_window.getY()+100,parent_window.getWidth()-100,parent_window.getHeight()-100);
        New_window_context context =  new New_window_context(
                new_path,
                rectangle,
                null);
        if ( dbg) logger.log(("\nadditional_different_folder\n"+ context.to_string() ));
        new Browser(context, logger);
    }
    */


    //**********************************************************
    public static void replace_same_folder(
            //int port,
            Shutdown_target shutdown_target,
            Path old_and_new_path,
            Path top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        Browsing_caches.scroll_position_cache_write(old_and_new_path,top_left);

        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        New_window_context context =  new New_window_context(
                //port,
                old_and_new_path,
                rectangle,
                shutdown_target,
                originator);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ context.to_string() ));
        new Browser(context, logger);
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
        New_window_context context =  new New_window_context(
                //port,
                new_path,
                rectangle,
                shutdown_target,
                originator);
        if ( dbg) logger.log(("\nreplace_different_folder\n"+ context.to_string() ));
        new Browser(context, logger);

    }

/*
    // experimental: image playlists

    //**********************************************************
    public static void open_new_image_playlist(
            Path new_path,
            Window owner,
            Path old_path,
            Path topLeft,
            Logger logger)
    //**********************************************************
    {
        Virtual_landscape.scroll_position_cache.put(old_path.toAbsolutePath().toString(),topLeft);
        Rectangle2D rectangle = new Rectangle2D(owner.getX()+100,owner.getY()+100,owner.getWidth()-100,owner.getHeight()-100);


        New_window_context context = new New_window_context(new_path, rectangle, null);
        new Image_playlist(context, logger);
    }

    //**********************************************************
    public static void replace_image_playlist(
            Shutdown_target shutdown_target,
            Path new_path,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        Rectangle2D rectangle = new Rectangle2D(owner.getX()+100,owner.getY()+100,owner.getWidth()-100,owner.getHeight()-100);

        New_window_context context = new New_window_context(new_path, rectangle, shutdown_target);
        new Image_playlist(context, logger);
    }

    //**********************************************************
    public static void create_new_image_playlist(Window owner, Logger logger)
    //**********************************************************
    {
        Rectangle2D rectangle = new Rectangle2D(owner.getX()+100,owner.getY()+100,owner.getWidth()-100,owner.getHeight()-100);

        Path path = create_new_playlist_file(owner, logger);
        New_window_context context = new New_window_context(path, rectangle, null);
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

 */
}
