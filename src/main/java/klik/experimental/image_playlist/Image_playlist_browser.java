package klik.experimental.image_playlist;

import javafx.geometry.Rectangle2D;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import klik.Context_type;
import klik.browser.Abstract_browser;
import klik.path_lists.Path_list_provider;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.path_lists.Path_list_provider_for_playlist;
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


        init_abstract_browser(Context_type.Image_playlist_2D,shutdown_target, rectangle,this,"playlist");

        logger.log("\n\n\n\n\n\n\n\n\n\n\nNEW IMAGE PLAY LIST "+path_list_provider.get_name());

    }


    //**********************************************************
    @Override
    public void set_background_color(Pane pane)
    //**********************************************************
    {
        logger.log("Image_playlist_browser.set_background_color() ID="+ID);
        Look_and_feel i = Look_and_feel_manager.get_instance(get_owner(),logger);
        pane.setBackground(new Background(i.get_image_playlist_fill()));

    }


    //**********************************************************
    @Override
    protected String get_name()
    //**********************************************************
    {
        logger.log("Image_playlist_browser.get_name() ID="+ID);

        return path_list_provider.get_name();
    }

    //**********************************************************
    @Override
    public Path_list_provider get_Path_list_provider()
    //**********************************************************
    {
        logger.log("Image_playlist_browser.get_Path_list_provider() ID="+ID);

        return path_list_provider;
    }

    //**********************************************************
    @Override
    public String signature()
    //**********************************************************
    {
        logger.log("Image_playlist_browser.signature() ID="+ID);

        return path_list_provider.get_name();
    }

    //**********************************************************
    @Override
    public void monitor()
    //**********************************************************
    {
        logger.log("Image_playlist_browser.monitor() ID="+ID);

    }

    //**********************************************************
    @Override
    public void set_title()
    //**********************************************************
    {
        logger.log("Image_playlist_browser.set_title() ID="+ID);

        my_Stage.the_Stage.setTitle("Image PLAY LIST (Not a folder): "+ path_list_provider.get_name());
    }

    //**********************************************************
    @Override
    public void go_full_screen()
    //**********************************************************
    {
        logger.log("Image_playlist_browser.go_full_screen() ID="+ID);

    }

    //**********************************************************
    @Override
    public void stop_full_screen()
    //**********************************************************
    {
        logger.log("Image_playlist_browser.stop_full_screen() ID="+ID);

    }

    //**********************************************************
    @Override
    public void shutdown()
    //**********************************************************
    {
        logger.log("Image_playlist_browser.shutdown() ID="+ID);
        my_Stage.the_Stage.close();

    }

    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner, Logger logger)
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
                    logger.log("Change Gang says : playlist changed !!");
                    path_list_provider.reload();
                    virtual_landscape.redraw_fx("change gang for dir: " + path_list_provider.the_playlist_file_path);
                }
            }
        }
    }

    //**********************************************************
    @Override
    public String get_Change_receiver_string()
    //**********************************************************
    {
        return "Image_playlist_browser ID="+ID;
    }
}
