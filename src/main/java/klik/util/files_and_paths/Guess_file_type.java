// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.files_and_paths;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.experimental.fusk.Fusk_static_core;
//import klik.path_lists.Path_list_provider_for_playlist;
import klik.util.image.decoding.Exif_metadata_extractor;
import klik.properties.boolean_features.Booleans;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;

//import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// static utilities to guess the file type
// 1. from its extension, for speed
// 2. from the metadata

//**********************************************************
public class Guess_file_type
//**********************************************************
{
    private static final String GIF = "GIF";
    private static final String PNG = "PNG";
    public static final String PDF = "PDF";
    private static final String[] JAVAFX_supported_image_formats = {
            "BMP","GIF","JPEG","JPG","PNG"};
    //private static final String[] supported_image_formats = ImageIO.getReaderFormatNames();

    // GraphicsMagick supported formats as per 'gm convert -list format'
    // commented out formats are already supported by JavaFX
    // OR are causing collisions (like C, CACHE, FILE, MAP, HTML etc)
    private static final String[] graphicsMagick_supported_image_formats = {
            "3FR","8BIM","8BIMTEXT","8BIMWTEXT","AAI","APP1","APP1JPEG","ART", "ARW","AVIF","AVS","B","BIGTIFF",
            //"BMP",
            "BMP2","BMP3","BRF",
            //"C",
            //"CACHE",
            "CALS","CAPTION","CIN","CMYK","CMYKA","CR2","CRW","CUR",
            "CUT","DCM","DCR","DCX","DNG","DPX","EPDF","EPI","EPS","EPS2",
            "EPS3","EPSF","EPSI","EPT","EPT2","EPT3","ERF","EXIF","FAX",
            //"FILE", (?)
            "FITS",
            "FRACTAL","FTP","G",
            //"GIF",
            "GIF87","GRADIENT","GRAY",
            "GRAYA","HEIC","HEIF","HISTOGRAM","HRZ",
            //"HTML","HTTP",
            "ICB",
            //"ICC",
            "ICM","ICO","ICON","IDENTITY","IMAGE","INFO","IPTC",
            "IPTCTEXT","IPTCWTEXT","ISOBRL","ISOBRL6","J2C","JNG","JNX",
            "JP2","JPC",
            //"JPEG","JPG",
            "JXL","K","K25","KDC","LABEL","M", "M2V","MAC",
            //"MAP",
            "MAT","MATTE","MEF","MIFF","MNG","MONO",
            "MPC",
            //"MPEG","MPG",
            "MRW","MSL","MTV","MVG","NEF","NULL",
            //"O",
            "ORF","OTB","P7","PAL","PALM","PAM","PBM","PCD","PCDS","PCL",
            "PCT","PCX","PDB",
            //"PDF", supported separately
            "PEF","PFA","PFB","PGM","PGX","PICON",
            "PICT","PIX","PLASMA",
            //"PNG",
            "PNG00","PNG24","PNG32","PNG48", "PNG64","PNG8","PNM","PPM","PREVIEW","PS","PS2","PS3","PTIF",
            "PWP","R","RAF","RAS","RGB","RGBA","RLA","RLE","SCT","SFW", "SGI","SHTML","SR2","SRF","STEGANO","SUN",
            "SVG", "SVGZ",
            //"TEXT", "TXT", (?)
            "TGA",
            "TIF","TIFF",
            "TILE","TIM","TOPOL", "UBRL","UBRL6", "UIL","UYVY","VDA","VICAR","VID","VIFF","VST",
            "WBMP",
            "WEBP",
            "WPG","X3F","XBM","XC",
            "XCF",
            "XMP","XPM","XV","Y","YUV"};
    public static final boolean use_nasa_fits_java_lib = false;
    private static final String[] fits = {"FITS"};

    //private static final String[] VIPS_supported_image_formats = {"TIFF","WEBP","JPEG2000","HEIC","AVIF","FITS","MATLAB","OPENEXR","SVG","HDR","PPM","PGM","PFM","CSV","ANALYZE","NIFTI","DEEPZOOM","OPENSLIDE"};
    private static final String[] supported_text_formats = {"TXT","NFO","RTF","MD","PY","C","C++","CPP","JAVA","JS","HTML"};
    public static final String[] supported_video_extensions = {"MP4","WEBM","MOV","M4V","MPG","MKV","AVI","FLV","WMV"};
    public static final String[] supported_audio_extensions = {"WAV","AAC","MP3","PCM","AVC","VP6","M4A"};//,"MKV"};
    public static final String KLIK_AUDIO_PLAYLIST_EXTENSION = "klik_audio_playlist";
    static String[] supported_non_gif_non_png_image_formats = null; // initialized the first time

    // portability notes:
    // "._" when a Mac writes a file into an external drive (or NAS via AFP or SMB)
    // or if the file system of the disk does not support Extended Attributes
    // typically FAT32 and extFAT
    // a file like this is created for EACH file in the folder
    // e.g. 'file_name.xxx' creates a file ._filename.xxx
    // this is an issue because when "show hidden file" is true
    // since the ._zzzz file has the extension of the original file
    // it may be processed, say as an image ... causing all sorts of trouble
    //
    // .DS_Store is similar: this is meta data for the MAC Finder (one file per folder)
    //
    // .color is klik specific: when present, it is the color tag for a folder
    private static final String[] invisible_if_starts_with = {".","._",".DS_Store",".color"};
    private static final String[] invisible_if_ends_with = {".properties",".prototype"};

/*
    static {
        System.out.println("\n\n");
        for( String s : ImageIO.getReaderFormatNames())
        System.out.println(s);
    }

 */
    //**********************************************************
    public static boolean is_this_file_an_image(File f,Logger logger)
    //**********************************************************
    {
        return is_this_path_an_image(f.toPath(),logger);
    }
/*
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
*/


    //**********************************************************
    public static boolean is_this_path_a_text(Path path,Logger logger)
    //**********************************************************
    {
        if (should_ignore(path,logger)) return false;
        String extension = Extensions.get_extension(path.getFileName().toString());
        return is_this_extension_a_text(extension);
    }
    //**********************************************************
    public static boolean is_this_path_an_image(Path path,Logger logger)
    //**********************************************************
    {
        if (should_ignore(path,logger)) return false;
        String extension = Extensions.get_extension(path.getFileName().toString());
        return is_this_extension_an_image(extension);
    }

    //**********************************************************
    public static boolean should_ignore(Path path,Logger logger)
    //**********************************************************
    {
        if ( path.getFileName() == null)
        {
            logger.log("Warning ignoring path with no name ->"+path);
            return true;
        }
        for ( String i : invisible_if_starts_with)
        {
            if (path.getFileName().toString().startsWith(i)) return true;
        }
        for ( String i : invisible_if_ends_with)
        {
            if (path.getFileName().toString().endsWith(i)) return true;
        }
        return false;
    }



    //**********************************************************
    public static boolean is_this_path_a_music(Path path,Logger logger)
    //**********************************************************
    {
        if ( should_ignore(path,logger)) return false;
        String extension = Extensions.get_extension(path.getFileName().toString());
        return is_this_extension_an_audio(extension);
    }

    //**********************************************************
    public static boolean is_this_path_an_audio_playlist(Path path,Logger logger)
    //**********************************************************
    {
        if ( should_ignore(path,logger)) return false;
        String extension = Extensions.get_extension(path.getFileName().toString());
        return is_this_extension_an_audio_playlist(extension);
    }
    //**********************************************************
    public static boolean is_this_path_an_image_playlist(Path path,Logger logger)
    //**********************************************************
    {
        if ( should_ignore(path,logger)) return false;
        String extension = Extensions.get_extension(path.getFileName().toString());
        return is_this_extension_an_image_playlist(extension);
    }
    //**********************************************************
    public static boolean is_this_path_a_pdf(Path path,Logger logger)
    //**********************************************************
    {
        if ( should_ignore(path,logger)) return false;
        String extension = Extensions.get_extension(path.getFileName().toString());
        return is_this_extension_a_pdf(extension);
    }
    //**********************************************************
    public static boolean is_this_path_a_gif(Path path,Logger logger)
    //**********************************************************
    {
        if ( should_ignore(path,logger)) return false;
        String extension = Extensions.get_extension(path.getFileName().toString());
        return is_this_extension_a_gif(extension);
    }

    //**********************************************************
    public static boolean is_this_path_a_video(Path path,Logger logger)
    //**********************************************************
    {
        if ( should_ignore(path,logger)) return false;
        String extension = Extensions.get_extension(path.getFileName().toString());
        return is_this_extension_a_video(extension);
    }

    //**********************************************************
    public static boolean does_this_file_contain_a_video_track(
            Path path,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        List<String> list = get_ffprobe_cmd(path);
        StringBuilder sb = new StringBuilder();
        File wd = path.getParent().toFile();
        if (Execute_command.execute_command_list(list, wd, 2000, sb, logger) == null)
        {
            Booleans.manage_show_ffmpeg_install_warning(owner,logger);
        }
        logger.log("ffprobe result:->"+sb+"<-");

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
    private static List<String> get_ffprobe_cmd(Path path)
    //**********************************************************
    {
        List<String> list = new ArrayList<>();
        list.add("ffprobe");
        list.add("-v");
        list.add("error");
        list.add("-show_entries");
        list.add("stream=codec_type");
        list.add("-of");
        list.add("default=nw=1:nk=1");
        list.add(path.getFileName().toString());
        return list;
    }

    //**********************************************************
    public static boolean does_this_file_contain_an_audio_track(
            Path path,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        List<String> list = get_ffprobe_cmd( path);
        StringBuilder sb = new StringBuilder();
        File wd = path.getParent().toFile();
        if (Execute_command.execute_command_list(list, wd, 2000, sb, logger) == null)
        {
            Booleans.manage_show_ffmpeg_install_warning(owner,logger);
        }
        logger.log("ffprobe result:->"+sb+"<-");

        String[] x = sb.toString().split("\\R");
        for (String l : x) {
            if (l.contains("audio"))
            {
                return true;
            }
        }
        return false;
    }

    //**********************************************************
    public static boolean is_this_path_a_animated_gif(Path path, Window owner,Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (! Guess_file_type.is_this_path_a_gif(path,logger)) return false;

        Exif_metadata_extractor e = new Exif_metadata_extractor(path,owner,logger);
        List<String> l = e.get_exif_metadata(42,true,aborter,false);

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
    public static boolean is_this_path_invisible_when_browsing(Path path,Logger logger)
    //**********************************************************
    {
        return (should_ignore(path,logger));
    }

    //**********************************************************
    public static boolean is_this_extension_an_image(String extension)
    //**********************************************************
    {
        for (String e : JAVAFX_supported_image_formats)
        {
            if (extension.toUpperCase().equals(e) )return true;
        }
        for (String e : graphicsMagick_supported_image_formats)
        {
            if (extension.toUpperCase().equals(e) )
            {
                //System.out.println("\n=============== SUPPORTED by GraphicsMagick: "+extension);
                return true;
            }
        }
        if ( use_nasa_fits_java_lib)
        {
            for (String e : fits) {
                if (extension.toUpperCase().equals(e)) return true;
            }
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
    public static boolean is_this_extension_an_audio_playlist(String extension)
    //**********************************************************
    {
        if (extension.equals(KLIK_AUDIO_PLAYLIST_EXTENSION))  return true;
        return false;
    }


    //**********************************************************
    public static boolean is_this_extension_an_image_playlist(String extension)
    //**********************************************************
    {
        //if (extension.equals(Path_list_provider_for_playlist.KLIK_IMAGE_PLAYLIST_EXTENSION))  return true;
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
    public static boolean is_this_extension_a_non_javafx_type(String extension)
    //**********************************************************
    {
        for ( String s : graphicsMagick_supported_image_formats)
        {
            if ( extension.equalsIgnoreCase(s)) return true;
        }
        return false;
    }
    //**********************************************************
    public static boolean is_this_extension_a_fits(String extension)
    //**********************************************************
    {
        for ( String s : fits)
        {
            if ( extension.equalsIgnoreCase(s)) return true;
        }
        return false;
    }
    //**********************************************************
    public static boolean is_this_extension_an_image_not_gif_not_png(String extension)
    //**********************************************************
    {

        if ( supported_non_gif_non_png_image_formats == null)
        {
            int size = 0;
            for ( String s : JAVAFX_supported_image_formats)
            {
                if ( ! s.equalsIgnoreCase(GIF)) size++;
                if ( ! s.equalsIgnoreCase(PNG)) size++;
            }
            for ( String s : graphicsMagick_supported_image_formats)
            {
                if ( ! s.equalsIgnoreCase(GIF)) size++;
                if ( ! s.equalsIgnoreCase(PNG)) size++;
            }
            size += fits.length;
            size++; // for fusk we need 1 more slot (just safety since we skip GIF and PNG)
            supported_non_gif_non_png_image_formats = new String[size];
            int i = 0;
            for ( String s : JAVAFX_supported_image_formats)
            {
                if ( ! s.equalsIgnoreCase(GIF)) supported_non_gif_non_png_image_formats[i++] = s.toUpperCase();
                if ( ! s.equalsIgnoreCase(PNG)) supported_non_gif_non_png_image_formats[i++] = s.toUpperCase();
            }
            for ( String s : graphicsMagick_supported_image_formats)
            {
                if ( ! s.equalsIgnoreCase(GIF)) supported_non_gif_non_png_image_formats[i++] = s.toUpperCase();
                if ( ! s.equalsIgnoreCase(PNG)) supported_non_gif_non_png_image_formats[i++] = s.toUpperCase();
            }
            supported_non_gif_non_png_image_formats[i] = Fusk_static_core.FUSK_EXTENSION.toUpperCase();
        }
        for (String supportedNonGifImageFormat : supported_non_gif_non_png_image_formats)
        {
            if (extension.toUpperCase().equals(supportedNonGifImageFormat))
            {
                //System.out.println("SUPPORTED: "+extension);
                return true;
            }
        }
        //System.out.println("NOT supported: "+extension);
        return false;
    }
}
