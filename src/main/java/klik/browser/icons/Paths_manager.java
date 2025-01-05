//SOURCES ../comparators/Alphabetical_file_name_comparator.java
//SOURCES ../comparators/Alphabetical_file_name_comparator_gif_first.java
//SOURCES ../comparators/

package klik.browser.icons;

import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.browser.Error_receiver;
import klik.browser.comparators.*;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.properties.File_sort_by;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.files_and_paths.Guess_file_type;
import klik.properties.Static_application_properties;
import klik.util.ui.Hourglass;
import klik.util.ui.Show_running_man_frame;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Paths_manager
//**********************************************************
{
    public static final boolean dbg = false;
    public static final String OK = "OK";
    public double max_dir_text_length;
    private static Logger logger;

    // these MUST be mutually exclusive:
    public ConcurrentSkipListMap<Path,Boolean> folders;
    public ConcurrentSkipListMap<Path,Boolean> non_iconized;

    public ConcurrentLinkedQueue<List<Path>> iconized_sorted_queue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Path> iconized_paths = new ConcurrentLinkedQueue<>();

    public Comparator<? super Path> image_file_comparator = null;
    public Comparator<? super Path> other_file_comparator;
    private static final boolean show_video_as_gif = true;
    AtomicInteger ig_gen = new AtomicInteger(0);
    public final int ID;
    public final Aborter aborter;
    public final Path folder_path;
    final Image_properties_RAM_cache image_properties_cache;
    private final Icon_factory_actor icon_factory_actor;

    private Refresh_target refresh_target;

    //**********************************************************
    public Paths_manager(Image_properties_RAM_cache image_properties_cache, Icon_factory_actor icon_factory_actor, Path displayed_folder_path, Refresh_target refresh_target_, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        this.image_properties_cache = image_properties_cache;
        folder_path = displayed_folder_path;
        logger = logger_;
        refresh_target = refresh_target_;
        ID = ig_gen.getAndIncrement();
        aborter = aborter_;
        this.icon_factory_actor = icon_factory_actor;

        Alphabetical_file_name_comparator alphabetical_file_name_comparator = new Alphabetical_file_name_comparator();
        switch (File_sort_by.get_sort_files_by(logger))
        {
            //case SIMILARITY:
            //    other_file_comparator = new Similarity_comparator(logger);
            //    break;
            case NAME, ASPECT_RATIO, RANDOM_ASPECT_RATIO, IMAGE_HEIGHT, IMAGE_WIDTH:
                other_file_comparator = alphabetical_file_name_comparator;
                break;
            case RANDOM:
                other_file_comparator = new Random_comparator();
                break;
            case DATE:
                other_file_comparator = new Date_comparator(logger);
                break;
            case SIZE:
                other_file_comparator = new Decreasing_file_size_comparator();
                break;
            case NAME_GIFS_FIRST:
                other_file_comparator = new Alphabetical_file_name_comparator_gif_first();
                break;
        }
        image_file_comparator = other_file_comparator;

        // these MUST be mutually exclusive:
        folders = new ConcurrentSkipListMap<>(alphabetical_file_name_comparator);
        non_iconized = new ConcurrentSkipListMap<>(other_file_comparator);
    }

    //long scan_dir_elapsed = 0;
    //**********************************************************
    public void scan_dir_in_a_thread_2(Stage stage, long start)
    //**********************************************************
    {

        Hourglass running_man = null;
        if (Browser.show_running_man)
        {
            running_man = Show_running_man_frame.show_running_man("Scanning folder", 20*60,  aborter, logger);
        }

        boolean show_icons_instead_of_text = Static_application_properties.get_show_icons(logger);
        boolean show_hidden_files = Static_application_properties.get_show_hidden_files(logger);
        max_dir_text_length = 0;
        boolean show_hidden_directories = Static_application_properties.get_show_hidden_directories(logger);
        boolean show_icons_for_folders = Static_application_properties.get_show_icons_for_folders(logger);
        iconized_paths.clear();
        non_iconized.clear();
        folders.clear();
        iconized_sorted_queue.clear();



        image_properties_cache.reload_cache_from_disk();
        do_the_hard_work_of_scan_dir_3(folder_path, stage, show_hidden_directories, show_icons_for_folders, show_hidden_files, show_icons_instead_of_text);

        image_properties_cache.all_image_properties_acquired_4(this,refresh_target,start, running_man);

    }



    private final AtomicBoolean hard_part_guard = new AtomicBoolean(false);

    //**********************************************************
    private void do_the_hard_work_of_scan_dir_3(Path folder_path, Stage stage, boolean show_hidden_directories, boolean show_icons_for_folders, boolean show_hidden_files, boolean show_icons_instead_of_text)
    //**********************************************************
    {
        if (Platform.isFxApplicationThread())
        {
            logger.log(Stack_trace_getter.get_stack_trace("PANIC"));
        }
        if ( hard_part_guard.get()) return;

        hard_part_guard.set(true);
        iconized_sorted_queue.clear();
        try
        {
            File files[] = folder_path.toFile().listFiles();
            for ( File f : files)
            {
                if ( aborter.should_abort())
                {
                    logger.log("path manager aborting1");
                    hard_part_guard.set(false);
                    aborter.on_abort();
                    return;
                }

                Path path  = f.toPath();
                if ( f.isDirectory())
                {
                    do_folder(path, show_hidden_directories, show_icons_for_folders);
                }
                else
                {
                    do_file(path, show_hidden_files, show_icons_instead_of_text, stage);
                }
            }
        }
        catch (Exception e)
        {
            logger.log(""+e);
            //Error_type denied = Static_files_and_paths_utilities.explain_error(folder_path,logger);
            hard_part_guard.set(false);
            ((Error_receiver)refresh_target).receive_error(Error_type.DENIED);
            return;
        }
        //logger.log("hard part in a thread done!");
        hard_part_guard.set(false);
    }


    //**********************************************************
    private void do_file(Path path, boolean show_hidden_files, boolean show_icons_instead_of_text, Stage stage)
    //**********************************************************
    {
        if (!show_hidden_files)
        {
            if (Guess_file_type.is_this_path_invisible_when_browsing(path))
            {
                return; // invisible
            }
        }
        if ( aborter.should_abort())
        {
            logger.log("path manager aborting2");
            return;
        }

        if (Guess_file_type.is_this_path_a_video(path))
        {
            if (show_video_as_gif)
            {
                if (icon_factory_actor.videos_for_which_giffing_failed.contains(path))
                {
                    logger.log("Paths_manager: detected animated icon failure for video:"+path);
                    // if the giffing process failed a video becomes non-iconized
                    non_iconized.put(path,true);
                    return;
                }
                String extension = FilenameUtils.getExtension(path.getFileName().toString());
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
                iconized_paths.add(path);
                {
                    if (dbg) logger.log("calling image properties cache from path manager do_file()");
                    // calling this will pre-populate the cache
                    image_properties_cache.get_from_cache(path,null, false);
                }
                return;
            }
        }
        // non-image, non-directory
        non_iconized.put(path,true);
    }

    //**********************************************************
    private void do_folder(Path path, boolean show_hidden_directories, boolean show_icons_for_folders)
    //**********************************************************
    {
        if (!show_hidden_directories)
        {
            if (Guess_file_type.is_this_path_invisible_when_browsing(path)) return; // invisible
        }
        folders.put(path,true);

        Text t = new Text(path.getFileName().toString());
        double l = t.getLayoutBounds().getWidth();
        if (l > max_dir_text_length) max_dir_text_length = l;
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


    //**********************************************************
    public void set_new_iconized_items_comparator(Comparator<Path> local_file_comparator)
    //**********************************************************
    {
        image_file_comparator = local_file_comparator;
    }


    //**********************************************************
    public List<Path> get_iconized_sorted(String from)
    //**********************************************************
    {
        List<Path> returned = iconized_sorted_queue.poll();
        if ( returned != null) return returned;
        redo_iconized_sorted_7(from);
        return iconized_sorted_queue.poll();
    }

    //**********************************************************
    synchronized public void redo_iconized_sorted_7(String from)
    //**********************************************************
    {
        logger.log("making & sorting iconized_sorted with comparator:"+image_file_comparator);
        List<Path> local_iconized_sorted = new ArrayList<>(iconized_paths);
        local_iconized_sorted.sort(image_file_comparator);
        iconized_sorted_queue.add(local_iconized_sorted);
    }
}
