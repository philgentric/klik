package klik.browser.icons;

import javafx.scene.text.Text;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.Error_receiver;
import klik.browser.icons.caches.Aspect_ratio_cache;
import klik.browser.icons.caches.Rotation_cache;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.images.decoding.Fast_date_from_OS;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Paths_manager
//**********************************************************
{
    public static final boolean dbg = true;
    public static final String OK = "OK";
    public double max_dir_text_length;
    private static Logger logger;

    // these MUST be mutually exclusive:
    public ConcurrentSkipListMap<Path,Integer> folders;
    public ConcurrentSkipListMap<Path,Integer> non_iconized;
    public ConcurrentSkipListMap<Path, Boolean> iconized;


    public Comparator<? super Path> image_file_comparator;
    public Comparator<? super Path> other_file_comparator;
    private static final boolean show_video_as_gif = true;
    AtomicInteger ig_gen = new AtomicInteger(0);
    public final int ID;
    public final Aborter aborter;
    public final Path folder_path;
    final Aspect_ratio_cache aspect_ratio_cache;
    final  Rotation_cache rotation_cache;
    private final Icon_factory_actor icon_factory_actor;

    private Refresh_target refresh_target;

    //**********************************************************
    public Paths_manager(Aspect_ratio_cache aspect_ratio_cache, Rotation_cache rotation_cache, Icon_factory_actor icon_factory_actor, Path displayed_folder_path, Refresh_target refresh_target_, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        this.aspect_ratio_cache = aspect_ratio_cache;
        this.rotation_cache = rotation_cache;
        folder_path = displayed_folder_path;
        logger = logger_;
        refresh_target = refresh_target_;
        ID = ig_gen.getAndIncrement();
        aborter = aborter_;
        this.icon_factory_actor = icon_factory_actor;

        //boolean gif_first = Static_application_properties.get_show_gifs_first(logger);
        switch (Static_application_properties.get_sort_files_by(logger))
        {
            case NAME, ASPECT_RATIO, RANDOM_ASPECT_RATIO:
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

        // these MUST be mutually exclusive:
        folders = new ConcurrentSkipListMap<>(other_file_comparator);
        non_iconized = new ConcurrentSkipListMap<>(other_file_comparator);
        iconized = new ConcurrentSkipListMap<>(image_file_comparator);

    }


    //**********************************************************
    public boolean do_we_still_have(Path p)
    //**********************************************************
    {
        if ( iconized.containsKey(p)) return true;
        if ( non_iconized.containsKey(p)) return true;
        if ( folders.containsKey(p)) return true;
        return false;
    }


    //long scan_dir_elapsed = 0;
    //**********************************************************
    public Error_type scan_dir(Stage stage, String from)
    //**********************************************************
    {
        //long start = System.currentTimeMillis();
        logger.log((from+" scan dir "+folder_path));

        boolean show_icons_instead_of_text = Static_application_properties.get_show_icons(logger);
        boolean show_hidden_files = Static_application_properties.get_show_hidden_files(logger);
        max_dir_text_length = 0;

        boolean show_hidden_directories = Static_application_properties.get_show_hidden_directories(logger);
        boolean show_icons_for_folders = Static_application_properties.get_show_icons_for_folders(logger);

        iconized.clear();
        non_iconized.clear();
        folders.clear();

        aspect_ratio_cache.reload_cache_from_disk();
        // start a thread that will refresh and switch the file_comparator
        aspect_ratio_cache.look_for_end(this, refresh_target,stage);
        rotation_cache.reload_cache_from_disk();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                do_the_hard_work_of_scan_dir_in_a_thread(folder_path, stage, show_hidden_directories, show_icons_for_folders, show_hidden_files, show_icons_instead_of_text);
                refresh_target.refresh_no_scan_dir();
            }
        };
        Actor_engine.execute(r,aborter,logger);

        //scan_dir_elapsed += (System.currentTimeMillis()-start);
        //logger.log("scan_dir_elapsed: "+scan_dir_elapsed);
        return Error_type.OK;
    }

    private AtomicBoolean hard_part_ongoing = new AtomicBoolean(false);

    //**********************************************************
    synchronized private void do_the_hard_work_of_scan_dir_in_a_thread(Path folder_path, Stage stage, boolean show_hidden_directories, boolean show_icons_for_folders, boolean show_hidden_files, boolean show_icons_instead_of_text)
    //**********************************************************
    {
        if ( hard_part_ongoing.get()) return;

        hard_part_ongoing.set(true);
        try
        {
            File files[] = folder_path.toFile().listFiles();
            for ( File f : files)
            {
                if ( aborter.should_abort())
                {
                    logger.log("path manager aborting1");
                    hard_part_ongoing.set(false);
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
            hard_part_ongoing.set(false);
            ((Error_receiver)refresh_target).receive_error(Error_type.DENIED);
            return;
        }
        //logger.log("hard part in a thread done!");
        aspect_ratio_cache.save_whole_cache_to_disk();
        hard_part_ongoing.set(false);
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
                    non_iconized.put(path,1);
                    return;
                }
                String extension = FilenameUtils.getExtension(path.getFileName().toString());
                if ( extension.equalsIgnoreCase("MKV"))
                {
                    // special dirty case: MKV can be audio OR video ...
                    if ( Guess_file_type.is_this_a_video_or_audio_file(stage,path,logger))
                    {
                        iconized.put(path,true);//movie_aspect_ratio);
                    }
                    else
                    {
                        non_iconized.put(path,1);
                    }
                    return;
                }
                iconized.put(path,true);//movie_aspect_ratio);
                return;
            }
            non_iconized.put(path,1);
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
                iconized.put(path,true);//Aspect_ratio_actor.ISO_A4_aspect_ratio);
                return;
            }
            else
            {
                non_iconized.put(path,1);
                return;
            }
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
                iconized.put(path,true);//1.0);
                return;
            }
            else
            {
                non_iconized.put(path,1);
                return;
            }
        }
        // non-image, non-directory
        non_iconized.put(path,1);
    }

    //**********************************************************
    private void do_folder(Path path, boolean show_hidden_directories, boolean show_icons_for_folders)
    //**********************************************************
    {
        if (!show_hidden_directories)
        {
            if (Guess_file_type.is_this_path_invisible_when_browsing(path)) return; // invisible
        }
        folders.put(path,1);

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
            System.out.println("alphabetical_file_name_comparator_gifs_fist ");
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
            int diff= ldt1.compareTo(ldt2);
            if ( diff != 0) return diff;
            return (p1.toString().compareTo(p2.toString()));
        }
    };

    //**********************************************************
    class Random_comparator implements Comparator<Path>
            //**********************************************************
    {

        long seed;
        public Random_comparator()
        {
            Random r = new Random();
            seed = r.nextLong();
        }
        @Override
        public int compare(Path p1, Path p2) {

            // same aspect ratio so the order must be pseudo random... but consistent for each comparator instance
            long s1 = UUID.nameUUIDFromBytes(p1.getFileName().toString().getBytes()).getMostSignificantBits();
            Long l1 = new Random(seed*s1).nextLong();
            long s2 = UUID.nameUUIDFromBytes(p2.getFileName().toString().getBytes()).getMostSignificantBits();
            Long l2 = new Random(seed*s2).nextLong();
            return l1.compareTo(l2);

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
        for (Path p : iconized.keySet())
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

    //**********************************************************
    public ConcurrentSkipListMap<Path, Boolean> get_iconized()
    //**********************************************************
    {
        return iconized;
    }
}
