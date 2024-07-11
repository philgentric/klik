package klik.files_and_paths;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.images.decoding.Exif_metadata_extractor;
import klik.properties.Static_application_properties;
import klik.util.execute.Execute_command;
import klik.util.Logger;
import klik.level3.fusk.Fusk_static_core;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static klik.audio.Audio_player.PLAYLIST_EXTENSION;

/*
static utilities to guess the file type from its extension
 */
//**********************************************************
public class Guess_file_type
//**********************************************************
{
    private static final String GIF = "GIF";
    private static final String PNG = "PNG";
    public static final String PDF = "PDF";
    private static final String[] supported_image_formats = ImageIO.getReaderFormatNames();
    private static final String[] supported_text_formats = {"TXT","NFO","RTF","MD","PY","C","C++","CPP","JAVA","JS","HTML"};
    public static final String[] supported_video_extensions = {"MP4","WEBM","MOV","M4V","MPG","MKV","AVI","FLV","WMV"};
    public static final String[] supported_audio_extensions = {"WAV","AAC","MP3","PCM","AVC","VP6","M4A"};//,"MKV"};
    public static final String[] ignored_prefixes = {"._",".DS_Store",".color"};
    static String[] supported_non_gif_non_png_image_formats = null;

    //**********************************************************
    public static boolean is_file_an_image(File f)
    //**********************************************************
    {
        return is_this_path_an_image(f.toPath());
    }

    //**********************************************************
    public static String get_supported_image_formats_as_a_comma_separated_string()
    //**********************************************************
    {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < supported_image_formats.length; i++)
        {
            sb.append(supported_image_formats[i]);
            if ( i != supported_image_formats.length-1) sb.append(",");
        }
        sb.append(",");
        sb.append(Fusk_static_core.FUSK_EXTENSION);
        sb.append(",");
        sb.append(Fusk_static_core.FUSK_EXTENSION.toUpperCase());
        return sb.toString();
    }



    //**********************************************************
    public static boolean is_this_path_a_text(Path path)
    //**********************************************************
    {
        if (should_ignore(path)) return false;
        String extension = FilenameUtils.getExtension(path.getFileName().toString());
        return is_this_extension_a_text(extension);
    }
    //**********************************************************
    public static boolean is_this_path_an_image(Path path)
    //**********************************************************
    {
        if (should_ignore(path)) return false;
        String extension = FilenameUtils.getExtension(path.getFileName().toString());
        return is_this_extension_an_image(extension);
    }

    //**********************************************************
    public static boolean should_ignore(Path path)
    //**********************************************************
    {
        for ( String i : ignored_prefixes)
        {
            if (path.getFileName().toString().startsWith(i)) return true;
        }
        return false;
    }



    //**********************************************************
    public static boolean is_this_path_a_music(Path path)
    //**********************************************************
    {
        if (path.getFileName().toString().startsWith("._")) return false;
        String extension = FilenameUtils.getExtension(path.getFileName().toString());
        return is_this_extension_an_audio(extension);
    }

    //**********************************************************
    public static boolean is_this_path_a_playlist(Path path)
    //**********************************************************
    {
        if (path.getFileName().toString().startsWith("._")) return false;
        String extension = FilenameUtils.getExtension(path.getFileName().toString());
        return is_this_extension_a_playlist(extension);
    }
    //**********************************************************
    public static boolean is_this_path_a_pdf(Path path)
    //**********************************************************
    {
        if (path.getFileName().toString().startsWith("._")) return false;
        String extension = FilenameUtils.getExtension(path.getFileName().toString());
        return is_this_extension_a_pdf(extension);
    }
    //**********************************************************
    public static boolean is_this_path_a_gif(Path path)
    //**********************************************************
    {
        if (path.getFileName().toString().startsWith("._")) return false;
        String extension = FilenameUtils.getExtension(path.getFileName().toString());
        return is_this_extension_a_gif(extension);
    }

    //**********************************************************
    public static boolean is_this_path_a_video(Path path)
    //**********************************************************
    {
        if (path.getFileName().toString().startsWith("._")) return false;
        String extension = FilenameUtils.getExtension(path.getFileName().toString());
        return is_this_extension_a_video(extension);
    }

    //**********************************************************
    public static boolean is_this_a_video_or_audio_file(
            Stage owner,
            Path path,
            Logger logger)
    //**********************************************************
    {
        List<String> list = new ArrayList<>();
        list.add("ffprobe");
        list.add("-loglevel");
        list.add("error");
        list.add("-show_entries");
        list.add("-stream=codec_type");
        list.add("-of");
        list.add("default=nw=1=nk=1");
        list.add(path.getFileName().toString());
        StringBuilder sb = new StringBuilder();
        File wd = path.getParent().toFile();
        if (!Execute_command.execute_command_list(list, wd, 2000, sb, logger))
        {
            Static_application_properties.manage_show_ffmpeg_install_warning(owner,logger);
        }
        logger.log("->"+sb+"<-");

        String[] x = sb.toString().split("\\R");
        for (String l : x) {
            if (l.contains("video"))
            {
                return true;
            }
        }
        return false;
    }

    //**********************************************************
    public static boolean is_this_path_a_animated_gif(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (! Guess_file_type.is_this_path_a_gif(path)) return false;

        Exif_metadata_extractor e = new Exif_metadata_extractor(path,logger);
        List<String> l = e.get_exif_metadata(42,true, aborter,false);

        if ( l == null) return false;
        if ( l.isEmpty()) return false;
        int count = 0;
        for ( String line : l)
        {
            //logger.log("EXIF: "+line);
            if ( line.startsWith("[GIF Image]"))
            {
                if ( line.contains("Width"))
                {
                    count++;
                    if ( count > 3)
                    {
                        // assume it is an animated gif
                        return true;
                    }
                }
            }
        }
        return false;
    }


    //**********************************************************
    public static boolean is_this_path_invisible_when_browsing(Path path)
    //**********************************************************
    {
        if (path.getFileName().toString().startsWith("."))
        {
            return true;
        }
        if (path.getFileName().toString().toLowerCase().endsWith(".properties"))
        {
            return true;
        }
        return (should_ignore(path));

    }

    //**********************************************************
    public static boolean is_this_extension_an_image(String extension)
    //**********************************************************
    {
        for (String e : supported_image_formats)
        {
            if (extension.toUpperCase().equals(e) )return true;
        }
        if (extension.equalsIgnoreCase(Fusk_static_core.FUSK_EXTENSION))
        {
            return true;
        }
        return false;
    }


    //**********************************************************
    public static boolean is_this_extension_a_text(String extension)
    //**********************************************************
    {
        for (String e : supported_text_formats)
        {
            if (extension.toUpperCase().equals(e) )return true;
        }
        return false;
    }



    //**********************************************************
    public static boolean is_this_extension_a_video(String extension)
    //**********************************************************
    {
        for (String e : supported_video_extensions)
        {
            if (extension.toUpperCase().equals(e))  return true;
        }
        return false;
    }

    //**********************************************************
    public static boolean is_this_extension_an_audio(String extension)
    //**********************************************************
    {
        for (String e : supported_audio_extensions)
        {
            if (extension.toUpperCase().equals(e))  return true;
        }
        return false;
    }

    //**********************************************************
    public static boolean is_this_extension_a_playlist(String extension)
    //**********************************************************
    {
        if (extension.equals(PLAYLIST_EXTENSION))  return true;
        return false;
    }

    //**********************************************************
    public static boolean is_this_extension_a_pdf(String extension)
    //**********************************************************
    {
        if ( extension.equalsIgnoreCase(PDF)) return true;
        return false;
    }

    //**********************************************************
    public static boolean is_this_extension_a_gif(String extension)
    //**********************************************************
    {
        if (extension.equalsIgnoreCase(GIF) )return true;
        return false;
    }

    //**********************************************************
    public static boolean is_this_extension_a_png(String extension)
    //**********************************************************
    {
        if (extension.equalsIgnoreCase(PNG) )return true;
        return false;
    }


    //**********************************************************
    public static boolean is_this_extension_an_image_not_gif_not_png(String extension)
    //**********************************************************
    {

        if ( supported_non_gif_non_png_image_formats == null)
        {
            int size = 0;
            for ( String s : supported_image_formats)
            {
                if ( ! s.equalsIgnoreCase(GIF)) size++;
                if ( ! s.equalsIgnoreCase(PNG)) size++;
            }
            size++; // for fusk
            supported_non_gif_non_png_image_formats = new String[size];
            int i = 0;
            for ( String s : supported_image_formats)
            {
                if ( ! s.equalsIgnoreCase(GIF)) supported_non_gif_non_png_image_formats[i++] = s.toUpperCase();
                if ( ! s.equalsIgnoreCase(PNG)) supported_non_gif_non_png_image_formats[i++] = s.toUpperCase();
            }
            supported_non_gif_non_png_image_formats[i] = Fusk_static_core.FUSK_EXTENSION.toUpperCase();
        }
        for (String supportedNonGifImageFormat : supported_non_gif_non_png_image_formats) {
            if (extension.toUpperCase().equals(supportedNonGifImageFormat)) return true;
        }
        return false;
    }
}
