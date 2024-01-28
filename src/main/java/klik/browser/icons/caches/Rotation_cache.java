package klik.browser.icons.caches;

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
public class Rotation_cache extends Cache_for_doubles
//**********************************************************
{
    private static final boolean dbg = true;
    Properties_manager pm = new Properties_manager(cache_file_path,logger);

    //**********************************************************
    public Rotation_cache(Path folder_path, Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        super(folder_path, "rotation_cache.properties",aborter_,logger_);
    }


    //**********************************************************
    public Double get_rotation(Path path)
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio"));
        Double rotation = get_from_cache(path);
        if ( rotation != null) return rotation;

        if(dbg) logger.log("not in RAM for: "+path.toAbsolutePath());
        if (Guess_file_type.is_this_extension_a_pdf(FilenameUtils.getExtension(path.getFileName().toString())))
        {
            if(dbg) logger.log("not in RAM for: "+path.toAbsolutePath()+" setting 0 ");
            rotation = 0.0;
            put_in_cache(path,rotation);
            return rotation;
        }
        Path path_for_display_icon = get_path_for_display_icon(path);
        if ( path_for_display_icon == null)
        {
            return 0.0;
        }
        rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(path_for_display_icon, false, aborter, logger);
        put_in_cache(path,rotation);
        save_one_item_to_disk(path,rotation);// since reading EXIF is expensive
        return rotation;
    }


    //**********************************************************
    private Path get_path_for_display_icon(Path path)
    //**********************************************************
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
        // pick first image
        return images_in_folder.get(0).toPath();

    }


}
