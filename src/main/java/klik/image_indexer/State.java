package klik.image_indexer;

import klik.util.files_and_paths.Guess_file_type;
import klik.properties.Static_application_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.performance_monitor.Performance_monitor;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

//**********************************************************
class State
//**********************************************************
{
    private final static boolean dbg = false;
    // STATE ! THE CONTAINERS ARE FINAL, NOT THE CONTENT !
    private final Map<Path, Integer> path_to_index;
    private final Map<Integer,Path> index_to_path;
    Logger logger;
    private final Path current_dir;
    static final String target = "*.{"+Guess_file_type.get_supported_image_formats_as_a_comma_separated_string()+"}";
    private final Comparator<? super Path> file_comparator;

    //**********************************************************
    public State(Path current_dir, Comparator<? super Path> fileComparator, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        this.current_dir = current_dir;
        file_comparator = fileComparator;
        path_to_index = new HashMap<>();
        index_to_path = new HashMap<>();
        rescan();
    }
    //**********************************************************
    public synchronized int how_many_images()
    //**********************************************************
    {
        return index_to_path.size();
    }
    //**********************************************************
    public synchronized void rescan()
    //**********************************************************
    {
        long start = System.currentTimeMillis();
        //logger.log(Stack_trace_getter.get_stack_trace("image file source scan"));

        boolean consider_also_hidden_files =  Static_application_properties.get_show_hidden_files(logger);

        List<Path> path_list = new ArrayList<>();
        if (dbg) logger.log(("image file source scan for:"+target));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(current_dir, target))
        //try (DirectoryStream<Path> stream = Files.newDirectoryStream(current_dir, "*.{jpg,JPG,gif,GIF,png,PNG,jpeg,JPEG}"))
        {

            for (Path path : stream)
            {
                if (Files.isDirectory(path)) continue;

                if ( !consider_also_hidden_files) if ( Guess_file_type.should_ignore(path)) continue;

                path_list.add(path);
            }
        } catch (NoSuchFileException e) {
            logger.log("File not found");
            return;
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
            return;
        }

        try{
            path_list.sort(file_comparator);
        }
        catch (IllegalArgumentException e)
        {
            logger.log(""+e);
        }

        int index = 0;
        index_to_path.clear();
        path_to_index.clear();
        for (Path p : path_list) {
            index_to_path.put(index,p);
            path_to_index.put(p,index);
            index++;
        }
        Performance_monitor.register_new_record("image file source rescan", current_dir.toString(), System.currentTimeMillis() - start, logger);
    }

    public Integer index_from_path(Path path) {
        return path_to_index.get(path);
    }

    public Path path_from_index(int i) {
        return index_to_path.get(i);
    }
}
