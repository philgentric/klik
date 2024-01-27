package klik.browser.icons;

import klik.actor.Aborter;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.images.decoding.Fast_rotation_from_exif_metadata_extractor;
import klik.properties.Properties_manager;
import klik.util.Logger;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class Rotation_cache
//**********************************************************
{
    private static final boolean dbg = true;
    Map<String, Double> rotation_cache = new ConcurrentHashMap<>();
    //private Aspect_ratio_actor aspect_ratio_actor;

    public final Logger logger;
    public final Aborter aborter;
    private final String cache_file_name;
    private final Path path_of_rotation_cache_file;

    //**********************************************************
    public Rotation_cache(Path folder_path, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        String s = "rotation_cache.properties";//folder_path.toAbsolutePath().toString();
        cache_file_name = UUID.nameUUIDFromBytes(s.getBytes()).toString();
        {
            Path dir = Files_and_Paths.get_aspect_ratio_and_rotation_caches_dir(logger);
            path_of_rotation_cache_file= Path.of(dir.toAbsolutePath().toString(), cache_file_name);
        }
        logger.log("Aspect_ratio_cache CONSTRUCTOR:  using "+s+" => "+path_of_rotation_cache_file);
        aborter = aborter_;
        //aspect_ratio_actor = new Aspect_ratio_actor(in_flight);
    }


    //**********************************************************
    static String key_from_path(Path p)
    //**********************************************************
    {
        return p.toAbsolutePath().toString();
    }

    //**********************************************************
    Double get_rotation(Path path)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio"));
        Double rotation = rotation_cache.get(key_from_path(path));
        if ( rotation != null) return rotation;

        if(dbg) logger.log("not in RAM for: "+path.toAbsolutePath());
        if (Guess_file_type.is_this_extension_a_pdf(FilenameUtils.getExtension(path.getFileName().toString())))
        {
            if(dbg) logger.log("not in RAM for: "+path.toAbsolutePath()+" setting 0 ");
            rotation = 0.0;
            rotation_cache.put(key_from_path(path),rotation);
            return rotation;
        }
        Path path_for_display_icon = get_path_for_display_icon(path);
        if ( path_for_display_icon == null)
        {
            return 0.0;
        }
        rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(path_for_display_icon, false, aborter, logger);
        rotation_cache.put(key_from_path(path),rotation);
        return rotation;
    }

    private Path get_path_for_display_icon(Path path)
    {
        if ( !Files.isDirectory(path))
        {
            return path;
        }
        File dir = path.toFile();
        File[] files = dir.listFiles();
        if ( files == null)
        {
            if ( dbg) logger.log("WARNING: dir is access denied: "+path);
            return null;
        }
        if ( files.length == 0)
        {
            if ( dbg) logger.log("dir is empty: "+path);
            return null;
        }
        Arrays.sort(files);
        List<File> images_in_folder = new ArrayList<>();

        for ( File f : files)
        {
            if (f.isDirectory()) continue; // ignore folders
            if (!Guess_file_type.is_file_an_image(f)) continue; // ignore non images
            if (Guess_file_type.is_this_path_a_gif(f.toPath()))
            {
                if (Guess_file_type.is_this_path_a_animated_gif(f.toPath(), aborter, logger))
                {
                    return f.toPath();
                }
                continue; // ignore not animated gifs
            }
            images_in_folder.add(f);
        }
        if ( images_in_folder.isEmpty())
        {
            for (File folder : files) {
                if (folder.isDirectory()) {
                    File[] files2 = folder.listFiles();
                    if (files2 == null) return null;
                    Arrays.sort(files2);
                    for (File f2 : files2) {
                        if (f2.isDirectory()) continue; // ignore folders
                        if (!Guess_file_type.is_file_an_image(f2)) continue; // ignore non images
                        if (Guess_file_type.is_this_path_a_gif(f2.toPath())) {
                            if (Guess_file_type.is_this_path_a_animated_gif(f2.toPath(), aborter, logger)) {
                                return f2.toPath();
                            }
                            continue; // ignore not animated gifs
                        }
                        return f2.toPath();
                    }
                }
            }
            return null;
        }

        return images_in_folder.get(0).toPath();

    }

    //**********************************************************
    public void clear_rotation_RAM_cache()
    //**********************************************************
    {
        rotation_cache.clear();
        if (dbg) logger.log("aspect ratio cache file cleared");
    }

    //**********************************************************
    synchronized void reload_rotation_cache()
    //**********************************************************
    {
        Properties_manager pm = new Properties_manager(path_of_rotation_cache_file,logger);
        int reloaded = 0;
        int already_in_RAM = 0;
        List<String> cleanup = new ArrayList<>();

        for(String key : pm.get_all_keys())
        {
            if ( dbg) logger.log("reloading : "+key);

            String value = pm.get(key);
            if (rotation_cache.get(key) == null)
            {
                try
                {
                    double d = Double.valueOf(value);
                    rotation_cache.put(key, d);
                    if ( dbg) logger.log("reloading : "+key+" => "+ d);
                    reloaded++;
                }
                catch(NumberFormatException x)
                {
                    // this entry in the file cache is wrong
                    cleanup.add(key);
                }
            }
            else
            {
                already_in_RAM++;
                if ( dbg) logger.log("already in RAM : "+key+" => "+ rotation_cache.get(key));
            }
        }
        for ( String key:cleanup)
        {
            pm.remove(key);
        }
        if ( !cleanup.isEmpty()) pm.store_properties();
        if (dbg) logger.log("rotation cache: "+already_in_RAM+" already in RAM, "+reloaded+" items reloaded from file");

        if ( dbg)
        {
            logger.log("\n\n\n*********************ROTATION CACHE************************");
            for (String s  : rotation_cache.keySet())
            {
                logger.log("rotation cache: "+s+" "+rotation_cache.get(s));
            }
            logger.log("****************************************************************\n\n\n");
        }
    }
    //**********************************************************
    void save_rotation_cache()
    //**********************************************************
    {
        Properties_manager pm = new Properties_manager(path_of_rotation_cache_file,logger);

        int saved = 0;
        for(Map.Entry<String, Double> e : rotation_cache.entrySet())
        {
            Double rot  = e.getValue();
            saved++;
            pm.imperative_store(e.getKey(), Double.toString(rot), false, false);
        }
        pm.store_properties();
        if (dbg) logger.log(saved +" items of rotation cache saved to file");
    }


}
