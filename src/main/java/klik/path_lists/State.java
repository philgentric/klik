package klik.path_lists;

import klik.util.execute.actor.Aborter;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.*;

//**********************************************************
class State
//**********************************************************
{
    private final static boolean dbg = false;
    private final Map<Path, Integer> path_to_index;
    private final Map<Integer,Path> index_to_path;
    Logger logger;
    private final Aborter aborter;
    private final Path_list_provider path_list_provider;
    private final Comparator<? super Path> file_comparator;
    private final Type type;
    //**********************************************************
    public State(Type type, Path_list_provider path_list_provider, Comparator<? super Path> fileComparator, Aborter aborter,Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.aborter = aborter;
        this.path_list_provider = path_list_provider;
        this.type = type;
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
        //long start = System.currentTimeMillis();
        //logger.log(Stack_trace_getter.get_stack_trace("image file source scan"));

        List<Path> path_list = null;
        switch (type)
        {
            case images -> path_list = path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files));
            case songs -> path_list = path_list_provider.only_song_paths(Feature_cache.get(Feature.Show_hidden_files));
            case all_files -> path_list = path_list_provider.only_file_paths(Feature_cache.get(Feature.Show_hidden_files));
        }
        if (path_list == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("rescan failed"));
            return;
        }
        /*
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
        }*/

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
    }

    public Integer index_from_path(Path path) {
        return path_to_index.get(path);
    }

    public Path path_from_index(int i) {
        return index_to_path.get(i);
    }
}
