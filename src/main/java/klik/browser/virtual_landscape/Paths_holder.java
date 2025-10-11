//SOURCES ../comparators/Alphabetical_file_name_comparator.java
//SOURCES ../comparators/Alphabetical_file_name_comparator_gif_first.java
//SOURCES ../comparators/

package klik.browser.virtual_landscape;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.util.files_and_paths.Extensions;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

//**********************************************************
public class Paths_holder
//**********************************************************
{
    // holds the paths of a folder, divided in 3 mutually exclusive lists
    public static final boolean dbg = false;
    public static final String OK = "OK";
    private static Logger logger;

    // these lists MUST be mutually exclusive:
    // reason to use ConcurrentSkipListMap: it is sorted and concurrent
    //public ConcurrentSkipListMap<Path,Boolean> folders;
    //public ConcurrentSkipListMap<Path,Boolean> non_iconized;
    //public ConcurrentLinkedQueue<Path> iconized_paths = new ConcurrentLinkedQueue<>();
    public ConcurrentSkipListSet<Path> folders;
    public ConcurrentSkipListSet<Path> non_iconized;
    public ConcurrentSkipListSet<Path> iconized_paths = new ConcurrentSkipListSet<>();


    private static final boolean show_video_as_gif = true;
    //private final AtomicInteger id_gen = new AtomicInteger(0);
    //public final int ID;
    public final Aborter aborter;
    //private final Icon_factory_actor icon_factory_actor;
    private final Image_properties_RAM_cache image_properties_RAM_cache;

    //**********************************************************
    public Paths_holder(Image_properties_RAM_cache image_properties_RAM_cache,
                        Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        this.image_properties_RAM_cache = image_properties_RAM_cache;
        logger = logger_;
        //ID = id_gen.getAndIncrement();
        aborter = aborter_;
    }


    //**********************************************************
    void add_file(Path path, boolean show_icons_instead_of_text, Window stage)
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
                String extension = Extensions.get_extension(path.getFileName().toString());
                if ( extension.equalsIgnoreCase("MKV"))
                {
                    // special dirty case: MKV can be audio OR video ...
                    if ( Guess_file_type.is_this_a_video_or_audio_file(path,stage,logger))
                    {
                        iconized_paths.add(path);
                    }
                    else
                    {
                        //non_iconized.put(path,true);
                        non_iconized.add(path);
                    }
                    return;
                }
                iconized_paths.add(path);
                return;
            }
            //non_iconized.put(path,true);
            non_iconized.add(path);
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
                //non_iconized.put(path,true);
                non_iconized.add(path);
            }
            return;
        }
        if ( aborter.should_abort())
        {
            logger.log("path manager aborting4");
            return;
        }

        if (Guess_file_type.is_this_extension_an_image(path))
        {
            if (show_icons_instead_of_text)
            {
                // calling this will pre-populate the cache
                image_properties_RAM_cache.prefill_cache(path);
                iconized_paths.add(path);
                if (dbg) logger.log("calling image properties cache from path manager do_file()");
                return;
            }
        }
        // non-image, non-directory
        //non_iconized.put(path,true);
        non_iconized.add(path);
    }

    //**********************************************************
    void add_folder(Path path)
    //**********************************************************
    {
        //folders.put(path,true);
        folders.add(path);
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
        //for (Path p : non_iconized.keySet())
        for (Path p : non_iconized)
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
        //for (Path p : folders.keySet())
        for (Path p : folders)
        {
            returned.add(p.toFile());
        }
        return returned;
    }

    //**********************************************************
    public void remove_empty_folders(boolean recursively)
    //**********************************************************
    {
        //for (Path p : folders.keySet())
        for (Path p : folders)
        {
            Static_files_and_paths_utilities.remove_empty_folders(p, recursively, logger);
        }
    }

}
