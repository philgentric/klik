package klik.browser.icons;

import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.browser.Change_type;
import klik.browser.Error_receiver;
import klik.browser.icons.caches.Image_properties_RAM_cache;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.images.decoding.Fast_date_from_OS;
import klik.properties.Static_application_properties;
import klik.util.Hourglass;
import klik.util.Show_running_man_frame;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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

    private List<Path> iconized_sorted = new ArrayList<>();
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

        switch (Static_application_properties.get_sort_files_by(logger))
        {
            case NAME, ASPECT_RATIO, RANDOM_ASPECT_RATIO, IMAGE_HEIGHT, IMAGE_WIDTH:
                other_file_comparator = alphabetical_file_name_comparator;
                break;
            case RANDOM:
                other_file_comparator = new Random_comparator();
                break;
            case DATE:
                other_file_comparator = new Date_comparator();
                break;
            case SIZE:
                other_file_comparator = decreasing_file_size_comparator;
                break;
            case NAME_GIFS_FIRST:
                other_file_comparator = alphabetical_file_name_comparator_gif_first;
                break;
        }
        image_file_comparator = other_file_comparator;

        logger.log("Path_manager image_file_comparator init\n\n"+image_file_comparator.toString());
        // these MUST be mutually exclusive:
        folders = new ConcurrentSkipListMap<>(alphabetical_file_name_comparator);
        non_iconized = new ConcurrentSkipListMap<>(other_file_comparator);
    }

    //long scan_dir_elapsed = 0;
    //**********************************************************
    public void scan_dir_in_a_thread_2(Stage stage, Change_type change_type)
    //**********************************************************
    {
        long start = System.currentTimeMillis();

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

        image_properties_cache.reload_cache_from_disk();
        do_the_hard_work_of_scan_dir_3(folder_path, stage, show_hidden_directories, show_icons_for_folders, show_hidden_files, show_icons_instead_of_text);

        image_properties_cache.all_image_properties_acquired_4(this,refresh_target,change_type,start, running_man);

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
        iconized_sorted_ready.set(false);
        try
        {
            File files[] = folder_path.toFile().listFiles();
            for ( File f : files)
            {
                if ( aborter.should_abort())
                {
                    logger.log("path manager aborting1");
                    hard_part_guard.set(false);
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
            //Error_type denied = Files_and_Paths.explain_error(folder_path,logger);
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
    public final static Comparator<Path> alphabetical_file_name_comparator_gifs_fist = new Comparator<>()
    //**********************************************************
    {
        @Override
        public int compare(Path f1, Path f2)
        {
            int i =  f1.getFileName().toString().compareTo(f2.getFileName().toString());
            if ( i != 0) return i;
            Boolean is_gif1 = Guess_file_type.is_this_extension_a_gif(FilenameUtils.getExtension(f1.getFileName().toString()));
            Boolean is_gif2 = Guess_file_type.is_this_extension_a_gif(FilenameUtils.getExtension(f2.getFileName().toString()));
            return is_gif1.compareTo(is_gif2);
        }
    };

    //**********************************************************
    public final static Comparator<Path> alphabetical_file_name_comparator = new Comparator<>()
    //**********************************************************
    {
        @Override
        public int compare(Path f1, Path f2)
        {
            int diff = f1.getFileName().toString().compareToIgnoreCase(f2.getFileName().toString());
            if (diff != 0) return diff;
            // in case the file names differ by case
            return f1.getFileName().toString().compareTo(f2.getFileName().toString());
        }
    };


    //**********************************************************
    class Date_comparator implements Comparator<Path>
    //**********************************************************
    {

        @Override
        public int compare(Path p1, Path p2) {
            FileTime ldt1 = Fast_date_from_OS.get_date(p1,logger);
            FileTime ldt2 = Fast_date_from_OS.get_date(p2,logger);
            int diff= ldt2.compareTo(ldt1); // most recent first
            if ( diff != 0) return diff;
            return (p1.toString().compareTo(p2.toString()));
        }
    };



    //**********************************************************
    public final static Comparator<Path> alphabetical_file_name_comparator_gif_first = new Comparator<>()
            //**********************************************************
    {
        @Override
        public int compare(Path f1, Path f2)
        {
            Boolean is_gif1 = Guess_file_type.is_this_extension_a_gif(FilenameUtils.getExtension(f1.getFileName().toString()));
            Boolean is_gif2 = Guess_file_type.is_this_extension_a_gif(FilenameUtils.getExtension(f2.getFileName().toString()));
            if ( is_gif1 && is_gif2)
            {
                return f1.getFileName().toString().compareToIgnoreCase(f2.getFileName().toString());
            }
            else if ( is_gif1 )
            {
                return -1;
            }
            else if ( is_gif2 )
            {
                return 1;
            }
            else
            {
                return f1.getFileName().toString().compareToIgnoreCase(f2.getFileName().toString());
            }
        }
    };


    //**********************************************************
    public final static Comparator<Path> decreasing_file_size_comparator = new Comparator<>()
            //**********************************************************
    {
        @Override
        public int compare(Path p1, Path p2)
        {
            int diff = Long.compare(p2.toFile().length(), p1.toFile().length());
            if ( diff != 0) return diff;
            return (p1.toString().compareTo(p2.toString()));
        }
    };




    //**********************************************************
    public final static Comparator<Path> decreasing_file_size_comparator_gifs_fist = new Comparator<>()
            //**********************************************************
    {
        @Override
        public int compare(Path f1, Path f2)
        {
            int i = Long.compare(f2.toFile().length(), f1.toFile().length());
            if ( i != 0) return i;
            Boolean is_gif1 = Guess_file_type.is_this_extension_a_gif(FilenameUtils.getExtension(f1.getFileName().toString()));
            Boolean is_gif2 = Guess_file_type.is_this_extension_a_gif(FilenameUtils.getExtension(f2.getFileName().toString()));
            return is_gif1.compareTo(is_gif2);
        }
    };

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
            Files_and_Paths.remove_empty_folders(p, recursively, logger);
        }

    }

    public AtomicBoolean iconized_sorted_ready = new AtomicBoolean(false);

    //**********************************************************
    public void set_new_iconized_items_comparator(Comparator<Path> local_file_comparator)
    //**********************************************************
    {
        image_file_comparator = local_file_comparator;
        iconized_sorted_ready.set(false);
    }


    //**********************************************************
    public List<Path> get_iconized_sorted(String from)
    //**********************************************************
    {
        if ( !iconized_sorted_ready.get()) redo_iconized_sorted_7(from);
        return iconized_sorted;
    }

    //**********************************************************
    synchronized public void redo_iconized_sorted_7(String from)
    //**********************************************************
    {
        iconized_sorted.clear();
        for ( Path path : iconized_paths)
        {
            iconized_sorted.add(path);
        }
        Collections.sort(iconized_sorted,image_file_comparator);
        iconized_sorted_ready.set(true);
    }
}
