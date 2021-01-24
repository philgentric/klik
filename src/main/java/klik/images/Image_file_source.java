package klik.images;

import klik.util.Constants;
import klik.util.Guess_file_type_from_extension;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Image_file_source
//**********************************************************
{

    private final List<Path> path_list;
    private final Map<Path, Integer> indexes;
    public final Path dir;
    private final Logger logger;

    //Image_cache image_cache = null;
    //private static final boolean use_RAM_caching = false;

    //**********************************************************
    public synchronized static Image_file_source get_Image_file_source(Path dir_, Logger logger_)
    //**********************************************************
    {
        if (dir_ == null) {
            logger_.log("PANIC dir_ == null ");
            return null;
        }
        Image_file_source returned = new Image_file_source(dir_, logger_);
        return returned;
    }





    //**********************************************************
    public synchronized int how_many_images()
    //**********************************************************
    {
        return indexes.size();
    }

    //**********************************************************
    public synchronized int get_index_of(Path from)
    //**********************************************************
    {
        //logger.log( "Image_file_source.get_rank_of()"+from.getAbsolutePath());
        //logger.log("Image_file_source.get_rank_of()"+from.getAbsolutePath());

        if (from == null) {
            logger.log("PANIC null from in Image_file_source");
            return 0;
        }
        if (indexes == null) {
            logger.log("PANIC null indexes in Image_file_source");
            return 0;
        }
        Integer ii = indexes.get(from);
        if (ii == null) {
            logger.log(Stack_trace_getter.get_stack_trace("PANIC file NOT in Image_file_source:" + from.toAbsolutePath()));
            return -1;
        }
        return ii;
    }


    //**********************************************************
    private synchronized Image_and_index get_image_for_index(int index, boolean ultimate)
    //**********************************************************
    {
        if (path_list.size() == 0) return null;
        int index2 = check_index(index, ultimate);
        return get_Image_and_index(index2);
    }


    //**********************************************************
    public synchronized int check_index(int index, boolean ultimate)
    //**********************************************************
    {

        //logger.log( "Image_file_source.get_image_for_index()"+i);

        if (index < 0)
        {
            logger.log("This is before start: i < 0");
            return path_list.size() - 1;
        }

        Path path = null;
        for (;;)
        {
            if (index >= path_list.size())
            {
                logger.log("This is the beyond the end: i=" + index + " >= path_list.size()=" + path_list.size());
                return 0;
            }
            path = path_list.get(index);

            if (Files.exists(path))
            {
                if ( ultimate)
                {
                    if (path.getFileName().toString().contains(Constants.ULTIM))
                    {
                        break;
                    }
                    else
                    {
                        index++;
                        continue;
                    }
                }
                break;
            }
            else
                {
                    // file does not exist anymore !!!
                scan();
            }
            index++;
        }
        return index;
    }

    public Image_and_index get_Image_and_index(int index)
    {

        if ( path_list.size() <= index) return null;

        Image_context local_ic = Image_context.get_Image_context(path_list.get(index), logger);
        if (local_ic == null) {
            logger.log(Stack_trace_getter.get_stack_trace("Image_file_source PANIC: cannot load image " + path_list.get(index).toAbsolutePath()));
            return null;
        }
        Image_and_index returned = new Image_and_index(local_ic, index);
        //if (use_RAM_caching) image_cache.add_to_the_cache(key, local_ic);

        return returned;

    }

    public Image_and_index get_Image_and_index2(int index, int size)
    {

        if ( path_list.size() <= index) return null;

        Image_context local_ic = Image_context.get_Image_context2(path_list.get(index), size, logger);
        if (local_ic == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Image_file_source PANIC: cannot load image " + path_list.get(index).toAbsolutePath()));
            return null;
        }
        Image_and_index returned = new Image_and_index(local_ic, index);
        //if (use_RAM_caching) image_cache.add_to_the_cache(key, local_ic);

        return returned;

    }

    //**********************************************************
    public synchronized Image_and_index get_image_for_path(Path newf)
    //**********************************************************
    {
        logger.log("Image_file_source.get_image_for_file()" + newf.toAbsolutePath());
        Integer index = indexes.get(newf);
        if (index == null)
        {
            // maybe this is a new file or a rename...
            scan();
            index = indexes.get(newf);
            if (index == null)
            {
                // no, that file is just NOT here
                logger.log("WARNING (file moved or renamed?) Image_file_source index == null for file:" + newf.toAbsolutePath());
                return null;
            }
        }
        Path f = path_list.get(index);
        if (f == null) {
            logger.log("PANIC Image_file_source f == null");
            return null;
        }

        Image_context ic = Image_context.get_Image_context(f, logger);
        if (ic == null) {
            logger.log(Stack_trace_getter.get_stack_trace("Image_file_source PANIC(2): cannot load image " + f.toAbsolutePath()));
            return null;
        }

        return new Image_and_index(ic, index);
    }

    //**********************************************************
    public Path get_dir()
    //**********************************************************
    {
        return dir;
    }


    //**********************************************************
    private Image_file_source(Path dir_, Logger l)
    //**********************************************************
    {
        logger = l;
        dir = dir_;
        indexes = new HashMap<>();
        path_list = new LinkedList<>();
        scan();
    }

    //**********************************************************
    public synchronized void scan()
    //**********************************************************
    {
        indexes.clear();
        path_list.clear();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{jpg,JPG,gif,GIF,png,PNG,jpeg,JPEG}")) {

            for (Path entry : stream) {
                path_list.add(entry);
            }
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }

        Collections.sort(path_list);
        int index = 0;
        for (Path p : path_list) {
            indexes.put(p, index);
            index++;

        }
    }

    public Path get_path(int index)
    {
        if ( path_list.size() == 0 ) return null;
        if ( index < 0 ) return path_list.get(0);
        if ( index >= path_list.size()) return path_list.get(path_list.size()-1);
        return path_list.get(index);
    }

    public Image_and_index get_Video_and_index(int index)
    {
        for(;;)
        {
            if (path_list.size() <= index) return null;

            Path p = path_list.get(index);
            if (Guess_file_type_from_extension.is_this_path_a_video(p) == false)
            {
                index++;
                continue;
            }

            Image_context local_ic = Image_context.get_Image_context(p, logger);
            if (local_ic == null) {
                logger.log(Stack_trace_getter.get_stack_trace("Image_file_source PANIC: cannot load image " + path_list.get(index).toAbsolutePath()));
                return null;
            }
            Image_and_index returned = new Image_and_index(local_ic, index);
            //if (use_RAM_caching) image_cache.add_to_the_cache(key, local_ic);

            return returned;
        }

    }
}
