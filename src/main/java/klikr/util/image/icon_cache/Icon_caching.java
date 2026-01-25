// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.image.icon_cache;

import javafx.stage.Window;
import klikr.util.cache.Cache_folder;
import klikr.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

//**********************************************************
public class Icon_caching
//**********************************************************
{
    private static final boolean dbg_names = false;

    private static Path icon_cache_dir = null;
    public static final String png_extension = "png";
    public static final String gif_extension = "gif";


    //**********************************************************
    public static Path path_for_icon_caching(
            Path original_image_file,
            String tag,
            String extension,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        if ( original_image_file == null) return null;

        if (icon_cache_dir == null) icon_cache_dir = Cache_folder.get_cache_dir(Cache_folder.icon_cache,owner,logger);
        //int icon_size = Non_booleans_properties.get_icon_size(owner);
        //String tag = String.valueOf(icon_size);
        return icon_cache_dir.resolve(make_cache_name(original_image_file.toAbsolutePath().toString(), tag, extension));
    }

    //**********************************************************
    private static File file_for_icon_caching(
            Path original_image_file,
            String tag,
            String extension,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        if ( original_image_file == null) return null;

        if (icon_cache_dir == null) icon_cache_dir = Cache_folder.get_cache_dir(Cache_folder.icon_cache,owner,logger);
        //int icon_size = Non_booleans_properties.get_icon_size(owner);
        //String tag = String.valueOf(icon_size);
        return new File(icon_cache_dir.toFile(), make_cache_name(original_image_file.toAbsolutePath().toString(), tag, extension));
    }






    //**********************************************************
    private static File file_for_icon_caching2( Path original_image_file, String tag, String extension)
    //**********************************************************
    {
        if ( original_image_file == null) return null;
        return new File(icon_cache_dir.toFile(), make_cache_name(original_image_file.toAbsolutePath().toString(), tag, extension));
    }


    //**********************************************************
    public static String make_cache_name(String tag, String icon_size_tag, String extension)
    //**********************************************************
    {
        if ( tag == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(make_cache_name_raw(tag));
        sb.append("_");
        sb.append(icon_size_tag);
        sb.append(".");
        sb.append(extension);
        return sb.toString();
//		return clean_name(full_name) + "_"+tag + "."+extension;
    }
    //**********************************************************
    public static String make_cache_name_raw(String tag)
    //**********************************************************
    {
        if ( tag == null) return null;

        StringBuilder sb = new StringBuilder();
        if ( dbg_names)
        {
            sb.append(clean_name(tag));
        }
        else
        {
            sb.append(UUID.nameUUIDFromBytes(tag.getBytes())); // the name is always the same length and is obfuscated
        }
        return sb.toString();
//		return clean_name(full_name) + "_"+tag + "."+extension;
    }



    //**********************************************************
    private static String clean_name(String s)
    //**********************************************************
    {
        s = s.replace("/", "_");
        s = s.replace(".", "_");
        s = s.replace("\\[", "_");
        s = s.replace("]", "_");
        //s = s.replace(" ", "_"); this is a bug: files named "xxx_yyy" and "xxx yyy" get the same icon!, sometimes no icon e.g. pdf
        return s;
    }

}
