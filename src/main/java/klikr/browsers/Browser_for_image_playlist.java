// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers;

import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.Window_builder;
import klikr.browser_core.Abstract_browser;
import klikr.browser_core.Window_manager;
import klikr.path_lists.Path_list_provider;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.util.execute.actor.Aborter;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Browser_for_image_playlist extends Abstract_browser
//**********************************************************
{
    private static AtomicInteger id_generator   = new AtomicInteger(0);
    private int ID;
    public final Path_list_provider_for_playlist path_list_provider;

    //**********************************************************
    public Browser_for_image_playlist(Window_builder window_builder, Logger logger)
    //**********************************************************
    {
        super(Color.BLUE, logger);
        ID = id_generator.getAndIncrement();

        path_list_provider = (Path_list_provider_for_playlist) window_builder.path_list_provider;
        aborter = new Aborter("Abstract_browser for: " + get_name(), logger);

        init_abstract_browser(window_builder,this,"playlist",aborter);


        logger.log("\n\n\n\n\n\n\n\n\n\n\nNEW IMAGE PLAYLIST "+path_list_provider.get_key());

    }




    //**********************************************************
    @Override
    protected String get_path_for_history()
    //**********************************************************
    {

        return path_list_provider.the_playlist_file_path.toAbsolutePath().toString();
    }

    //**********************************************************
    @Override
    protected String get_name()
    //**********************************************************
    {
        return "Browser_for_image_playlist "+path_list_provider.the_playlist_file_path.toAbsolutePath().toString();
    }

    //**********************************************************
    @Override
    public Path_list_provider get_Path_list_provider()
    //**********************************************************
    {
        logger.log("Browser_for_image_playlist.get_Path_list_provider() ID="+ID);

        return path_list_provider;
    }

    //**********************************************************
    @Override
    public String signature()
    //**********************************************************
    {
        logger.log("Browser_for_image_playlist.signature() ID="+ID);

        return path_list_provider.get_key();
    }

    //**********************************************************
    @Override
    public void monitor_current_path_list_source()
    //**********************************************************
    {
        logger.log("Browser_for_image_playlist.monitor() ID="+ID);

    }

    //**********************************************************
    @Override
    public void set_title()
    //**********************************************************
    {
        logger.log("Browser_for_image_playlist.set_title() ID="+ID);

        my_Stage.the_Stage.setTitle("Image PLAYLIST "+ path_list_provider.the_playlist_file_path.getFileName().toString()+"(this is NOT a folder!)");
    }

    //**********************************************************
    @Override
    public void go_full_screen()
    //**********************************************************
    {
        logger.log("Browser_for_image_playlist.go_full_screen() ID="+ID);

    }

    //**********************************************************
    @Override
    public void stop_full_screen()
    //**********************************************************
    {
        logger.log("Browser_for_image_playlist.stop_full_screen() ID="+ID);

    }

    //**********************************************************
    @Override
    public void shutdown()
    //**********************************************************
    {
        logger.log("Browser_for_image_playlist.shutdown() ID="+ID);
        my_Stage.the_Stage.close();
        Window_manager.unregister(ID,logger);
    }

    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Window owner, Logger logger)
    //**********************************************************
    {
        logger.log("Browser_for_image_playlist.you_receive_this_because_a_file_event_occurred_somewhere() ID=" + ID);
        for (Old_and_new_Path oanp : l)
        {
            if (oanp.new_Path != null)
            {
                String s = path_list_provider.the_playlist_file_path.toAbsolutePath().toString();
                if( oanp.new_Path.toAbsolutePath().toString().equals(s))
                {
                    logger.log("Change_broadcaster Gang says : playlist changed !!");
                    path_list_provider.reload("image playlist changed, according to Change_broadcaster Gang",aborter);
                    virtual_landscape.redraw_fx(true,"change gang for dir: " + path_list_provider.the_playlist_file_path, true);
                }
            }
        }
    }

    //**********************************************************
    @Override
    public String get_Change_receiver_string()
    //**********************************************************
    {
        return "Browser_for_image_playlist ID="+ID;
    }

    //*******************************************************
    @Override
    public Comparator<? super Path> get_file_comparator()
    //*******************************************************
    {
        return virtual_landscape.other_file_comparator;
    }
    //**********************************************************
    @Override
    public void set_unique_selected_item(Path path)
    //**********************************************************
    {
        logger.log("Browser_for_image_playlist.replace_current_item() not implemented");
    }
}
