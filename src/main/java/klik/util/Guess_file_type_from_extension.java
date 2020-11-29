package klik.util;

import java.io.File;
import java.nio.file.Path;

public class Guess_file_type_from_extension {



    // extension is just an indication...
    //**********************************************************
    public static boolean is_gif_extension(Path p)
    //**********************************************************
    {
        if (p.getFileName().toString().toUpperCase().endsWith(".GIF")) return true;
        return false;
    }


    //**********************************************************
    public static boolean is_file_a_image(File f)
    //**********************************************************
    {
        return is_this_path_an_image(f.toPath());
    }


    //**********************************************************
    public static boolean is_this_path_an_image(Path path)
    //**********************************************************
    {
        if (

                        (path.getFileName().toString().toUpperCase().endsWith(".JPG")) ||
                        (path.getFileName().toString().toUpperCase().endsWith(".JPEG")) ||
                        (path.getFileName().toString().toUpperCase().endsWith(".PNG")) ||
                        (path.getFileName().toString().toUpperCase().endsWith(".GIF"))
        ) {
            return true;
        }

        return false;
    }

    //**********************************************************
    public static boolean is_this_path_a_video(Path path)
    //**********************************************************
    {
        if (

                path.getFileName().toString().toUpperCase().endsWith(".MP4")
                        || path.getFileName().toString().toUpperCase().endsWith(".WEBM")
                        || path.getFileName().toString().toUpperCase().endsWith(".MOV")
                        || path.getFileName().toString().toUpperCase().endsWith(".M4V")
                        || path.getFileName().toString().toUpperCase().endsWith(".MPG")
                        || path.getFileName().toString().toUpperCase().endsWith(".MKV")
                        || path.getFileName().toString().toUpperCase().endsWith(".AVI")
        )
        {
            return true;
        }

        return false;
    }




}
