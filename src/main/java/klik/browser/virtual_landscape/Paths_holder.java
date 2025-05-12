//SOURCES ../comparators/Alphabetical_file_name_comparator.java
//SOURCES ../comparators/Alphabetical_file_name_comparator_gif_first.java
//SOURCES ../comparators/

package klik.browser.virtual_landscape;

import javafx.scene.text.Text;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Paths_holder
//**********************************************************
{
    public static final boolean dbg = false;
    public static final String OK = "OK";
    private static Logger logger;

    // these MUST be mutually exclusive:
    public ConcurrentSkipListMap<Path,Boolean> folders;
    public ConcurrentSkipListMap<Path,Boolean> non_iconized;

    public ConcurrentLinkedQueue<Path> iconized_paths = new ConcurrentLinkedQueue<>();


    private static final boolean show_video_as_gif = true;
    AtomicInteger ig_gen = new AtomicInteger(0);
    public final int ID;
    public final Aborter aborter;
    //public final Path folder_path;
    //Path_list_provider path_list_provider;
    private final Icon_factory_actor icon_factory_actor;
    private final Image_properties_RAM_cache image_properties_RAM_cache;

    //**********************************************************
    public Paths_holder(Icon_factory_actor icon_factory_actor,
                        Image_properties_RAM_cache image_properties_RAM_cache,
                        //Path displayed_folder_path,
                        // Path_list_provider path_list_provider,
                        Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        this.image_properties_RAM_cache = image_properties_RAM_cache;
        //folder_path = displayed_folder_path;
        //this.path_list_provider = path_list_provider;
        logger = logger_;
        ID = ig_gen.getAndIncrement();
        aborter = aborter_;
        this.icon_factory_actor = icon_factory_actor;
    }


    //**********************************************************
    void do_file(Path path, boolean show_icons_instead_of_text, Window stage)
    //**********************************************************
    {

        if ( aborter.should_abort())
        {
            logger.log("path manager aborting2");
            return;
        }

        if (Guess_file_type.is_this_path_a_video(path))
        {
            if (show_video_as_gif)
            {
                /*
                if (icon_factory_actor.videos_for_which_giffing_failed.contains(path))
                {
                    logger.log("Paths_holder: detected animated icon failure for video:"+path);
                    // if the giffing process failed a video becomes non-iconized
                    non_iconized.put(path,true);
                    return;
                }*/
                String extension = Static_files_and_paths_utilities.get_extension(path.getFileName().toString());
                if ( extension.equalsIgnoreCase("MKV"))
                {
                    // special dirty case: MKV can be audio OR video ...
                    if ( Guess_file_type.is_this_a_video_or_audio_file(stage,path,logger))
                    {
                        iconized_paths.add(path);
                    }
                    else
                    {
                        non_iconized.put(path,true);
                    }
                    return;
                }
                iconized_paths.add(path);
                return;
            }
            non_iconized.put(path,true);
            return;
        }
        if ( aborter.should_abort())
        {
            logger.log("path manager aborting3");
            return;
        }

        if (Guess_file_type.is_this_path_a_pdf(path))
        {
            if (show_icons_instead_of_text)
            {
                iconized_paths.add(path);
            }
            else
            {
                non_iconized.put(path,true);
            }
            return;
        }
        if ( aborter.should_abort())
        {
            logger.log("path manager aborting4");
            return;
        }

        if (Guess_file_type.is_this_path_an_image(path))
        {
            if (show_icons_instead_of_text)
            {
                // calling this will pre-populate the cache
                //the_browser.virtual_landscape.image_properties_RAM_cache.prefill_cache(path);
                image_properties_RAM_cache.prefill_cache(path);
                iconized_paths.add(path);
                if (dbg) logger.log("calling image properties cache from path manager do_file()");
                return;
            }
        }
        // non-image, non-directory
        non_iconized.put(path,true);
    }

    //**********************************************************
    void do_folder(
            //Browser the_browser,
            Path path)
    //**********************************************************
    {
        folders.put(path,true);
        Text t = new Text(path.getFileName().toString());
        //double l = t.getLayoutBounds().getWidth();
        //if (l > the_browser.max_dir_text_length) the_browser.max_dir_text_length = l;
    }





    //**********************************************************
    public List<File> get_file_list()
    //**********************************************************
    {
        List<File> returned = new ArrayList<>();
        for (Path p : iconized_paths)
        {
            returned.add(p.toFile());
        }
        for (Path p : non_iconized.keySet())
        {
            returned.add(p.toFile());
        }
        return returned;
    }


    //**********************************************************
    public List<File> get_folder_list()
    //**********************************************************
    {
        List<File> returned = new ArrayList<>();
        for (Path p : folders.keySet())
        {
            returned.add(p.toFile());
        }
        return returned;
    }

    //**********************************************************
    public void remove_empty_folders(boolean recursively)
    //**********************************************************
    {
        for (Path p : folders.keySet())
        {
            Static_files_and_paths_utilities.remove_empty_folders(p, recursively, logger);
        }
    }

}
