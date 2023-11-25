package klik.browser.icons;

import javafx.scene.text.Text;
import javafx.stage.Stage;

import klik.browser.items.Item_folder_with_icon;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

//**********************************************************
public class Paths_manager
//**********************************************************
{
    public static final boolean dbg = false;
    public static final String OK = "OK";
    public double max_dir_text_length;
    private final Logger logger;

    // these MUST be mutually exclusive:
    public List<Path> folders = new ArrayList<>();
    //public List<Path> folders_without_icon = new ArrayList<>();
    //public List<Path> folders_with_icon = new ArrayList<>();
    public List<Path> non_iconized = new ArrayList<>();
    public List<Path> iconized = new ArrayList<>();

    public Comparator<? super Path> file_comparator;

    private static final boolean show_video_as_gif = true;


   //**********************************************************
    public Paths_manager(Logger logger_)
    //**********************************************************
    {
        logger = logger_;

        if (Static_application_properties.get_sort_files_by_name(logger))
        {
            file_comparator = alphabetical_file_name_comparator;
        }
        else
        {
            file_comparator = decreasing_file_size_comparator;
        }
    }


    //**********************************************************
    public Error_type scan_dir(Path dir, Stage stage)
    //**********************************************************
    {

        boolean show_icons_instead_of_text = Static_application_properties.get_show_icons(logger);
        boolean show_hidden_files = Static_application_properties.get_show_hidden_files(logger);
        max_dir_text_length = 0;

        boolean show_hidden_directories = Static_application_properties.get_show_hidden_directories(logger);
        boolean show_icons_for_folders = Static_application_properties.get_show_icons_for_folders(logger);

        iconized.clear();
        non_iconized.clear();
        folders.clear();
        //folders_with_icon.clear();
        //folders_without_icon.clear();
        
        {
            /*
            File[] files = dir.toFile().listFiles();
            if ( files == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("files[] == null"));
                try
                {
                    BasicFileAttributes x = Files.readAttributes(dir, BasicFileAttributes.class);
                    logger.log(dir.toAbsolutePath()+": "+x.);
                }
                catch (AccessDeniedException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace("ACCESS DENIED EXCEPTION" + e));
                    return Error_type.denied;
                }
                catch (NoSuchFileException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace("NoSuchFileException" + e));
                    // the DIR is gone !!
                    return Error_type.not_found;
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return Error_type.error;
                }
                return Error_type.error;
            }
            for (File f : files)
            {
                if (f.isDirectory() )
                {
                    do_folder(f.toPath(),show_hidden_directories,show_icons_for_folders);
                }
                else
                {
                    do_file(f.toPath(), show_hidden_files, show_icons_instead_of_text, stage);
                }
            }
            */
            try {
                Stream<Path> stream = Files.list(dir);
                stream.forEach(path ->{
                    if (Files.isDirectory(path))
                    {
                        do_folder(path,show_hidden_directories,show_icons_for_folders);
                    }
                    else {
                        do_file(path, show_hidden_files, show_icons_instead_of_text, stage);
                    }

                });
            } catch (IOException e) {
                Error_type denied = Files_and_Paths.explain_error(dir,logger);
                if (denied != null) return denied;
            }

        }


        iconized.sort(file_comparator);
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
        non_iconized.sort(file_comparator);
        folders.sort(file_comparator);
        //folders_with_icon.sort(file_comparator);
        //folders_without_icon.sort(file_comparator);
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

}
