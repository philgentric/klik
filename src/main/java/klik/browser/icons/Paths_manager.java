package klik.browser.icons;

import javafx.scene.text.Text;
import javafx.stage.Stage;

import klik.actor.Actor_engine;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.properties.File_sorter;
import klik.properties.Properties_manager;
import klik.properties.Static_application_properties;
import klik.util.*;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

//**********************************************************
public class Paths_manager
//**********************************************************
{
    public static final boolean dbg = true;
    public static final String OK = "OK";
    public static final String ASPECT_RATIO_CACHE_FILE = "aspect_ratio_cache_file";
    public double max_dir_text_length;
    private static Logger logger;

    // these MUST be mutually exclusive:
    public List<Path> folders = new ArrayList<>();
    public List<Path> non_iconized = new ArrayList<>();
    private List<Path> iconized = new ArrayList<>();
    public Comparator<? super Path> file_comparator;
    private static final boolean show_video_as_gif = true;
    AtomicInteger ig_gen = new AtomicInteger(0);
    public final int ID;

    public record Aspect_ratio(double value, boolean truth){}

    //static Map<String, Double> aspect_ratio_cache = new HashMap<>();
    static Map<String, Aspect_ratio> aspect_ratio_cache = new ConcurrentHashMap<>();

    /*
    when we scan a dir 3 possible states are possible
    1. the aspect ratios for all (or most) of the files are in the aspect_ratios_cache in RAM
    2. the aspect ratios for all (or most) of the files are in the aspect_ratio_cache on FILE
    3. none (or almost) of the files have their aspect ratio computed yet

    assume that in terms of speed (1) is ideal, (2) is acceptable...
    (3) is NOT acceptable for folders with a lot of images as scan_dir will take a long time and block the UI

    therefore scan dir MUST NOT use the aspect_ratio file comparator YET
    instead it must
    a) use the alphabetical one
    b) start the computing of all the aspect ratios in a thread ...when finished:
    c) switch the file comparator
    d) generate a scene_geometry_changed()

    problem: how do we know we are in state (3)?
    answer: we dont know
    solution: assume we always are in state(3)
    scan the dir, for each path, try the RAM cache
    if null { put (1.0+tmp=true) put in queue for actor}

     */

    private static Refresh_target refresh_target;
    static Aspect_ratio_actor aspect_ratio_actor = new Aspect_ratio_actor();

    static Runnable look_for_end = null;
    //**********************************************************
    static Double get_aspect_ratio(Path p)
    //**********************************************************
    {
        if ( look_for_end == null)
        {
            look_for_end = new Runnable() {
                @Override
                public void run() {

                    for(;;)
                    {
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        if ( aspect_ratio_actor.in_flight.get() == 0)
                        {
                            logger.log("\n\n\n refresh");
                            refresh_target.refresh();
                            return;
                        }
                        else {
                            logger.log("in_flight:"+ aspect_ratio_actor.in_flight.get());

                        }

                    }

                }
            };
            Threads.execute(look_for_end,logger);
        }
        Aspect_ratio d = aspect_ratio_cache.get(p.toAbsolutePath().toString());
        if ( d == null)
        {
            logger.log("not in RAM for: "+p.toAbsolutePath());
            aspect_ratio_cache.put(p.toAbsolutePath().toString(), new Aspect_ratio(1.0,false));
            Actor_engine.run(aspect_ratio_actor, new Aspect_ratio_message(p,aspect_ratio_cache,logger),null,logger);
            return 1.0;
        }
        if ( ! d.truth ) {
            logger.log("RAM is fake for: "+p.toAbsolutePath());

            Actor_engine.run(aspect_ratio_actor, new Aspect_ratio_message(p,aspect_ratio_cache,logger),null,logger);
            return 1.0;
        }
        logger.log(d+" in RAM for: "+p.toAbsolutePath());
        return d.value;
    }

    //**********************************************************
    public static Path get_path_of_aspect_ratio_cache_file()
    //**********************************************************
    {
        Path dir = Files_and_Paths.get_icon_cache_dir(logger);
        return Path.of(dir.toAbsolutePath().toString(), ASPECT_RATIO_CACHE_FILE);
    }

    //**********************************************************
    public static void erase_aspect_ratio_cache_file()
    //**********************************************************
    {
        Properties_manager pm = new Properties_manager(get_path_of_aspect_ratio_cache_file(),logger);
        pm.erase_all_and_save();
        aspect_ratio_cache.clear();
        if (dbg) logger.log("aspect ratio cache file cleared");

    }

    //**********************************************************
    private void reload_aspect_ratio_cache()
    //**********************************************************
    {
        Properties_manager pm = new Properties_manager(get_path_of_aspect_ratio_cache_file(),logger);
        for(String s : pm.get_all_keys())
        {
            String v = pm.get(s);
            if (aspect_ratio_cache.get(s) == null) {
                aspect_ratio_cache.put(s, new Aspect_ratio(Double.valueOf(v),true));
            }
        }
        if (dbg) logger.log("aspect ratio cache reloaded from file");
    }
    //**********************************************************
    private void save_aspect_ratio_cache()
    //**********************************************************
    {
        Path dir = Files_and_Paths.get_icon_cache_dir(logger);
        Properties_manager pm = new Properties_manager(get_path_of_aspect_ratio_cache_file(),logger);

        int saved = 0;
        for(Map.Entry e : aspect_ratio_cache.entrySet())
        {
            Aspect_ratio ar = (Aspect_ratio) e.getValue();
            if (ar.truth) {
                saved++;
                pm.imperative_store((String) e.getKey(), Double.toString(ar.value), false, false);
            }
        }
        pm.store_properties();
        if (dbg) logger.log(saved +"items of aspect ratio cache saved to file");
    }

    //**********************************************************
    public Paths_manager(Refresh_target refreshTarget, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        refresh_target = refreshTarget;
        ID = ig_gen.getAndIncrement();

        switch (Static_application_properties.get_sort_files_by(logger))
        {
            default:
            case NAME:
                file_comparator = alphabetical_file_name_comparator;
                break;

            case SIZE:
                file_comparator = decreasing_file_size_comparator;
                break;

            case ASPECT_RATIO:
                file_comparator = aspect_ratio_comparator;
                break;
        }
    }


    //**********************************************************
    public Error_type scan_dir(Path folder_path, Stage stage)
    //**********************************************************
    {
        if ( Static_application_properties.get_sort_files_by(logger) == File_sorter.ASPECT_RATIO) reload_aspect_ratio_cache();

        //logger.log(Stack_trace_getter.get_stack_trace("scan dir "+folder_path));

        boolean show_icons_instead_of_text = Static_application_properties.get_show_icons(logger);
        boolean show_hidden_files = Static_application_properties.get_show_hidden_files(logger);
        max_dir_text_length = 0;

        boolean show_hidden_directories = Static_application_properties.get_show_hidden_directories(logger);
        boolean show_icons_for_folders = Static_application_properties.get_show_icons_for_folders(logger);

        iconized.clear();
        non_iconized.clear();
        folders.clear();

        try {
            Stream<Path> stream = Files.list(folder_path);
            stream.forEach(path ->{
                if (Files.isDirectory(path))
                {
                    do_folder(path,show_hidden_directories,show_icons_for_folders);
                }
                else
                {
                    do_file(path, show_hidden_files, show_icons_instead_of_text, stage);
                }

            });
        } catch (IOException e) {
            Error_type denied = Files_and_Paths.explain_error(folder_path,logger);
            if (denied != null) return denied;
        }

        if ( Static_application_properties.get_sort_files_by(logger) == File_sorter.ASPECT_RATIO)
        {

        }

        sort_iconized();

        non_iconized.sort(alphabetical_file_name_comparator);
        folders.sort(alphabetical_file_name_comparator);
        return Error_type.OK;
    }



    //**********************************************************
    private void sort_iconized()
    //**********************************************************
    {

        boolean show_gifs_first = Static_application_properties.get_show_gifs_first(logger);
        if ( show_gifs_first)
        {
            List<Path> tmp = new ArrayList<>(iconized);
            iconized.clear();
            List<Path> gif_or_video = new ArrayList<>(iconized);
            List<Path> others = new ArrayList<>(iconized);
            for ( Path p : tmp)
            {
                if ( Guess_file_type.is_this_path_a_gif(p))
                {
                    gif_or_video.add(p);
                    continue;
                }
                if ( Guess_file_type.is_this_path_a_video(p))
                {
                    gif_or_video.add(p);
                    continue;
                }
                others.add(p);
            }
            gif_or_video.sort(file_comparator);
            iconized.addAll(gif_or_video);
            others.sort(file_comparator);
            iconized.addAll(others);
        }
        else
        {
            iconized.sort(file_comparator);
        }
        save_aspect_ratio_cache();
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


        if (Guess_file_type.is_this_path_a_video(path))
        {
            if (show_video_as_gif)
            {
                if (Icon_factory_actor.videos_for_which_giffing_failed.contains(path))
                {
                    logger.log("Paths_manager: detected animated icon failure for video:"+path);
                    // if the giffing process failed a video becomes non-iconized
                    non_iconized.add(path);
                    return;
                }
                //logger.log("Paths_manager: video NOT on videos_for_which_giffing_failed list: "+path);


                String extension = FilenameUtils.getExtension(path.getFileName().toString());
                if ( extension.equalsIgnoreCase("MKV"))
                {
                    // special dirty case: MKV can be audio OR video ...
                    if ( Guess_file_type.is_this_a_video_or_audio_file(stage,path,logger))
                    {
                        iconized.add(path);
                    }
                    else
                    {
                        non_iconized.add(path);
                    }
                    return;
                }
                iconized.add(path);
                return;
            }
            else
            {
                non_iconized.add(path);
                return;
            }
        }

        if (Guess_file_type.is_this_path_a_pdf(path))
        {
            if (show_icons_instead_of_text)
            {
                iconized.add(path);
                return;
            }
            else
            {
                non_iconized.add(path);
                return;
            }
        }

        if (Guess_file_type.is_this_path_an_image(path))
        {
            if (show_icons_instead_of_text)
            {
                iconized.add(path);
                return;
            }
            else
            {
                non_iconized.add(path);
                return;
            }
        }
        // non-image, non-directory
        non_iconized.add(path);
    }

    //**********************************************************
    private void do_folder(Path path, boolean show_hidden_directories, boolean show_icons_for_folders)
    //**********************************************************
    {
        if (!show_hidden_directories)
        {
            if (Guess_file_type.is_this_path_invisible_when_browsing(path)) return; // invisible
        }
        folders.add(path);

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
            return f1.getFileName().toString().compareToIgnoreCase(f2.getFileName().toString());
        }
    };

    //**********************************************************
    public final static Comparator<Path> decreasing_file_size_comparator = new Comparator<>()
            //**********************************************************
    {
        @Override
        public int compare(Path f1, Path f2)
        {
            return Long.compare(f2.toFile().length(), f1.toFile().length());
        }
    };



    //**********************************************************
    public final static Comparator<Path> aspect_ratio_comparator = new Comparator<>()
            //**********************************************************
    {
        @Override
        public int compare(Path f1, Path f2)
        {
            Double a1 = get_aspect_ratio(f1);
            Double a2 = get_aspect_ratio(f2);
            //logger.log(a1+" vs "+ a2+ " for: "+f1.toAbsolutePath()+" vs "+f2.toAbsolutePath());
            return a1.compareTo(a2);
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
        for (Path p : iconized)
        {
            returned.add(p.toFile());
        }
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
        for (Path p : folders)
        {
            returned.add(p.toFile());
        }
        /*for (Path p : folders_without_icon)
        {
            returned.add(p.toFile());
        }
        for (Path p : folders_with_icon)
        {
            returned.add(p.toFile());
        }*/
        return returned;
    }

    //**********************************************************
    public void remove_empty_folders(boolean recursively)
    //**********************************************************
    {
        for (Path p : folders)
        {
            Files_and_Paths.remove_empty_folders(p, recursively, logger);
        }
        /*
        for (Path p : folders_without_icon)
        {
            Files_and_Paths.remove_empty_folders(p, recursively, logger);
        }
        for (Path p : folders_with_icon)
        {
            Files_and_Paths.remove_empty_folders(p, recursively, logger);
        }*/
    }

    public List<Path> get_iconized() {
        return iconized;
    }
}
