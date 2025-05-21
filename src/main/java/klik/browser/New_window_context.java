package klik.browser;

import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Window;
import klik.Window_provider;
import klik.browser.classic.Browser;
import klik.browser.virtual_landscape.Browsing_caches;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class New_window_context
//**********************************************************
{
    private static final boolean dbg = true;
    public final Path target_path; // this can be an absolute folder path or a image play list FILE  path
    public final Rectangle2D rectangle;
    public final Shutdown_target shutdown_target; // if null, there is no previous guy to shutdown
    //**********************************************************
    private New_window_context(
            Path target,
            Rectangle2D rectangle,
            Shutdown_target shutdown_target)
    //**********************************************************
    {
        this.rectangle = rectangle;
        this.shutdown_target = shutdown_target;
        this.target_path = target;
    }

    //**********************************************************
    private String to_string()
    //**********************************************************
    {
        String returned = "shutdown_target="+shutdown_target;
        return returned;
    }


    //**********************************************************
    public static Window_provider additional_no_past(Path new_path, Logger logger)
    //**********************************************************
    {
        New_window_context context = new New_window_context(
                new_path,
                null,
                null);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ context.to_string() ));
        return new Browser(context, logger);
    }

    //**********************************************************
    public static void additional_same_folder(
            Path new_and_old_path,
            Window owner,
            Path top_left,
            Logger logger)
    //**********************************************************
    {
        // make sure the new window is scrolled at the same position
        Browsing_caches.scroll_position_cache.put(new_and_old_path.toAbsolutePath().toString(),top_left);

        Rectangle2D rectangle = new Rectangle2D(owner.getX()+100,owner.getY()+100,owner.getWidth()-100,owner.getHeight()-100);

        New_window_context context =  new New_window_context(
                new_and_old_path,
                rectangle,
                null);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ context.to_string() ));
        new Browser(context, logger);
    }


    //**********************************************************
    public static void additional_same_folder_fat_tall(
            Path new_and_old_path,
            Window parent_window,
            Path top_left,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(new_and_old_path,parent_window ,5,top_left,logger);

    }
    //**********************************************************
    public static void additional_same_folder_twin(
            Path new_and_old_path,
            Window parent_window,
            Path top_left,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(new_and_old_path,parent_window,2,top_left,logger);
    }
    //**********************************************************
    public static void additional_same_folder_ratio(
            Path new_and_old_path,
            Window parent_window,
            int ratio,
            Path top_left,
            Logger logger)
    //**********************************************************
    {
        Browsing_caches.scroll_position_cache.put(new_and_old_path.toAbsolutePath().toString(),top_left);

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

        New_window_context context = new New_window_context(
                new_and_old_path,
                rectangle,
                null);
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
            Shutdown_target shutdown_target,
            Path old_and_new_path,
            Window parent_window,
            Path top_left,
            Logger logger)
    //**********************************************************
    {
        Browsing_caches.scroll_position_cache.put(old_and_new_path.toAbsolutePath().toString(),top_left);

        Rectangle2D rectangle = new Rectangle2D(parent_window.getX(),parent_window.getY(),parent_window.getWidth(),parent_window.getHeight());
        New_window_context context =  new New_window_context(
                old_and_new_path,
                rectangle,
                shutdown_target);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ context.to_string() ));
        new Browser(context, logger);
    }

    //**********************************************************
    public static void replace_different_folder(
            Shutdown_target shutdown_target,
            Path new_path,
            Window parent_window,
            Path old_path,
            Path top_left,
            Logger logger)
    //**********************************************************
    {
        logger.log("replace_different_folder new path: " + new_path.toAbsolutePath());
        if( top_left == null)
        {
            logger.log("SHOULD NOT HAPPEN  top left is null for old path: " + old_path.toAbsolutePath());
        }
        else
        {
            Browsing_caches.scroll_position_cache.put(old_path.toAbsolutePath().toString(),top_left);
            logger.log("yop, recorded that " + old_path.toAbsolutePath() + " has top left =" + top_left.toString());
        }
        Rectangle2D rectangle = new Rectangle2D(parent_window.getX(),parent_window.getY(),parent_window.getWidth(),parent_window.getHeight());
        New_window_context context =  new New_window_context(
                new_path,
                rectangle,
                shutdown_target);
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
