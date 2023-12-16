package klik.browser.icons;

import javafx.scene.text.Text;
import javafx.stage.Stage;

import klik.actor.Aborter;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.images.decoding.Fast_date_from_exif_metadata_extractor;
import klik.properties.File_sorter;
import klik.properties.Static_application_properties;
import klik.util.*;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

//**********************************************************
public class Paths_manager
//**********************************************************
{
    public static final boolean dbg = true;
    public static final String OK = "OK";
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
    public final Aborter aborter;
    Aspect_ratio_cache aspect_ratio_cache = null;
    private Refresh_target refresh_target;

    //**********************************************************
    public Paths_manager(Refresh_target refresh_target_, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        refresh_target = refresh_target_;
        ID = ig_gen.getAndIncrement();
        aborter = aborter_;

        //boolean gif_first = Static_application_properties.get_show_gifs_first(logger);
        switch (Static_application_properties.get_sort_files_by(logger))
        {
            case NAME, ASPECT_RATIO, RANDOM_ASPECT_RATIO:
                file_comparator = alphabetical_file_name_comparator;
                break;
            case DATE:
                file_comparator = new Date_comparator();
                break;
            case SIZE:
                file_comparator = decreasing_file_size_comparator;
                break;
            case RANDOM:
                file_comparator = null;
                break;
            case NAME_GIFS_FIRST:
                file_comparator = alphabetical_file_name_comparator_gif_first;
                break;
       }
    }


    //**********************************************************
    public boolean do_have_still_have(Path p)
    //**********************************************************
    {
        if ( iconized.contains(p)) return true;
        if ( non_iconized.contains(p)) return true;
        if ( folders.contains(p)) return true;
        return false;
    }


    //**********************************************************
    public Error_type scan_dir(Path folder_path, Stage stage)
    //**********************************************************
    {
        boolean use_aspect_ratio = false;
        if (
                (Static_application_properties.get_sort_files_by(logger) == File_sorter.ASPECT_RATIO) ||
                (Static_application_properties.get_sort_files_by(logger) == File_sorter.RANDOM_ASPECT_RATIO ))
        {
            use_aspect_ratio = true;
            if (aspect_ratio_cache == null) aspect_ratio_cache = new Aspect_ratio_cache(folder_path,aborter,logger);
            aspect_ratio_cache.reload_aspect_ratio_cache();
            aspect_ratio_cache.look_for_end(this, refresh_target,aborter);
        }

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

        if ( aborter.should_abort())
        {
            if ( use_aspect_ratio) aspect_ratio_cache.save_aspect_ratio_cache();
            return Error_type.OK;
        }

            if ( Static_application_properties.get_sort_files_by(logger) == File_sorter.RANDOM)
        {
            Collections.shuffle(iconized);
        }
        else
        {
            iconized.sort(file_comparator);
        }
        if ( use_aspect_ratio) aspect_ratio_cache.save_aspect_ratio_cache();

        non_iconized.sort(alphabetical_file_name_comparator);
        folders.sort(alphabetical_file_name_comparator);
        return Error_type.OK;
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
        if ( aborter.should_abort()) return;

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
        if ( aborter.should_abort()) return;

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
        if ( aborter.should_abort()) return;

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
    class Date_comparator implements Comparator<Path>
            //**********************************************************
    {

        @Override
        public int compare(Path p1, Path p2) {
            LocalDateTime ldt1 = Fast_date_from_exif_metadata_extractor.get_date(p1,aborter,logger);
            LocalDateTime ldt2 = Fast_date_from_exif_metadata_extractor.get_date(p2,aborter,logger);
            return ldt1.compareTo(ldt2);
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
        public int compare(Path f1, Path f2)
        {
            return Long.compare(f2.toFile().length(), f1.toFile().length());
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
