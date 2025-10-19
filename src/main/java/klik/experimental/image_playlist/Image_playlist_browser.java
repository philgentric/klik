package klik.experimental.image_playlist;

import javafx.geometry.Rectangle2D;
import javafx.scene.layout.Background;
import javafx.stage.Window;
import klik.browser.Abstract_browser;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Image_playlist_browser extends Abstract_browser
//**********************************************************
{
    private static AtomicInteger id_generator   = new AtomicInteger(0);
    private int ID;
    public final Path_list_provider_for_playlist path_list_provider;

    //**********************************************************
    public Image_playlist_browser(Path target_path, Shutdown_target shutdown_target, Rectangle2D rectangle, Logger logger)
    //**********************************************************
    {
        super(logger);
        ID = id_generator.getAndIncrement();
        path_list_provider = new Path_list_provider_for_playlist(target_path, logger);
        init_abstract_browser(shutdown_target, rectangle,this,"playlist");
        set_pink_background(logger);

        logger.log("\n\n\n\n\n\n\n\n\n\n\nNEW IMAGE PLAY LIST "+path_list_provider.get_name());

    }

    //**********************************************************
    private void set_pink_background(Logger logger)
    //**********************************************************
    {
        Look_and_feel i = Look_and_feel_manager.get_instance(get_owner(),logger);
        virtual_landscape.the_Pane.setBackground(new Background(i.get_image_playlist_fill()));
    }

    //**********************************************************
    @Override
    protected String get_name()
    //**********************************************************
    {
        return path_list_provider.get_name();
    }

    //**********************************************************
    @Override
    public Path_list_provider get_Path_list_provider()
    //**********************************************************
    {
        return path_list_provider;
    }

    //**********************************************************
    @Override
    public String signature()
    //**********************************************************
    {
        return path_list_provider.get_name();
    }

    //**********************************************************
    @Override
    public void monitor()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override
    public void set_title()
    //**********************************************************
    {
        my_Stage.the_Stage.setTitle("Image PLAY LIST (Not a folder): "+ path_list_provider.get_name());
    }

    //**********************************************************
    @Override
    public void go_full_screen()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override
    public void stop_full_screen()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override
    public void shutdown()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner, Logger logger2)
    //**********************************************************
    {
        logger.log("Image_playlist_browser.you_receive_this_because_a_file_event_occurred_somewhere() ID=" + ID);
        for (Old_and_new_Path oanp : l)
        {
            if (oanp.new_Path != null)
            {
                String s = path_list_provider.the_playlist_file_path.toAbsolutePath().toString();
                if( oanp.new_Path.toAbsolutePath().toString().equals(s))
                {
                    path_list_provider.reload();
                    virtual_landscape.redraw_fx("change gang for dir: " + path_list_provider.the_playlist_file_path);
                    set_pink_background(logger);
                }
            }
        }
    }

    //**********************************************************
    @Override
    public String get_Change_receiver_string()
    //**********************************************************
    {
        return "";
    }
}
