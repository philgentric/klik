package klik.images.decoding;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import klik.actor.Aborter;
import klik.files_and_paths.Files_and_Paths;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.From_disk;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.level3.fusk.Fusk_static_core;
import klik.level3.fusk.Fusk_strings;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//**********************************************************
public class Exif_metadata_extractor
//**********************************************************
{
    public static final boolean dbg = false;
    Path path;
    boolean image_is_damaged;
    public String title="";
    List<String> exif_metadata = null;
    private double rotation = 0;
    Logger logger;

    //**********************************************************
    public Exif_metadata_extractor(Path f_, Logger logger_)
    //**********************************************************
    {
        path = f_;
        logger = logger_;
    }

    //**********************************************************
    @Deprecated
    public double get_rotation(boolean report_if_not_found, Aborter aborter)
    //**********************************************************
    {
        if ( exif_metadata != null ) return rotation;
        logger.log(Stack_trace_getter.get_stack_trace("WARNING"));
        rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(path, report_if_not_found, aborter, logger);
        return rotation;
    }

    //**********************************************************
    public boolean is_image_damaged()
    //**********************************************************
    {
        return image_is_damaged;
    }
    //**********************************************************
    public List<String> get_exif_metadata(double how_many_pixels, boolean report_if_not_found, Aborter aborter, boolean details)
    //**********************************************************
    {
        if ( exif_metadata != null) return exif_metadata;

        exif_metadata =  new ArrayList<>();

        if ( path == null)
        {
            exif_metadata.add("this image was not created from file");
            return exif_metadata;
        }

        //String file = I18n.get_I18n_string("File",logger);

        exif_metadata.add("File on disk is ->"+ path.toAbsolutePath()+"<-");

        String extension = FilenameUtils.getExtension(path.getFileName().toString());
        if ( extension.equalsIgnoreCase(Fusk_static_core.FUSK_EXTENSION))
        {
            if ( Static_application_properties.get_enable_fusk(logger)) {
                if (Fusk_static_core.is_fusk(path, aborter,logger)) {
                    String base = FilenameUtils.getBaseName(path.toAbsolutePath().toString());
                    exif_metadata.add("... which is a fusk of: ->" + Fusk_strings.defusk_string(base, logger) + "<-");
                } else {
                    exif_metadata.add("... which has a fusk extension BUT IS NOT!");
                }
            }
        }

        String file_size = Files_and_Paths.get_1_line_string_with_size(path.toAbsolutePath(),logger);
        //file_size = file_size.replace("\n","  -  ");
        exif_metadata.add(file_size);

        List<String> list_of_strings = null;
        if ( details)
        {
            list_of_strings = new ArrayList<>();
        }
        double aspect_ratio = Fast_aspect_ratio_from_exif_metadata_extractor.get_aspect_ratio(path,report_if_not_found,aborter,list_of_strings,logger);
        exif_metadata.add("aspect_ratio="+aspect_ratio);

        {
            long l = 0;
            try
            {
                l = Files.size(path);
            } catch (IOException e)
            {
                logger.log("extract_exif_metadata() Managed exception (2)->"+e+"<- for:"+ path.toAbsolutePath());
            }
            double bits_per_pixel = (double)l*8.0/how_many_pixels;
            String s_bits_per_pixel = I18n.get_I18n_string("Bits_per_pixel",logger);
            exif_metadata.add(s_bits_per_pixel+": "+bits_per_pixel);
        }

        // next top item: image date (filesystem)
        BasicFileAttributes attr;
        try
        {
            attr = Files.readAttributes(path, BasicFileAttributes.class);
            exif_metadata.add("creation time: "+attr.creationTime());
            exif_metadata.add("last access: "+attr.lastAccessTime());
            exif_metadata.add("last modified: "+attr.lastModifiedTime());
        }
        catch (IOException e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (1)->"+e+"<- for:"+ path.toAbsolutePath()));
            }
            return exif_metadata;
        }

        image_is_damaged = false;

        boolean enable_fusk = Static_application_properties.get_enable_fusk(logger);
        InputStream is = From_disk.get_image_InputStream(path, enable_fusk, report_if_not_found, aborter, logger);
        if ( is == null)
        {
            image_is_damaged = true;
            return exif_metadata;
        }

        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            //Metadata metadata = JpegMetadataReader.readMetadata(is);
            for (Directory directory : metadata.getDirectories())
            {
                for (Tag tag : directory.getTags())
                {
                    //logger.log(tag);
                    exif_metadata.add(tag.toString());
                    if (tag.toString().contains("Title")) {
                        title = tag.toString();
                    }
                    if (tag.toString().contains("Orientation"))
                    {
                        if (!tag.toString().contains("Thumbnail"))
                        {
                            // Orientation - Right side, top (Rotate 90 CW)
                            if (tag.toString().contains("90"))
                            {
                                if (tag.toString().contains("CW"))
                                {
                                    // have to rotate +90
                                    rotation = 90.0;
                                }
                            }
                            else if (tag.toString().contains("180"))
                            {
                                rotation = 180.0;
                            }
                            else if (tag.toString().contains("270"))
                            {
                                rotation = 270.0;
                            }
                            else
                            {
                                rotation = 0.0;
                            }
                        }
                    }
                }
            }
            is.close();
        }
        catch (ImageProcessingException e)
        {
            if ( e.toString().contains("File format could not be determined"))
            {
                image_is_damaged = true;
            }
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (3)->"+e+"<- for:"+ path.toAbsolutePath()));
           }
        }
        catch (IOException e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (4)->"+e+"<- for:"+ path.toAbsolutePath()));
           }
        }
        catch (Exception e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
            }
        }

        exif_metadata.add("apparently (careful here, this may be wrong in rare cases) rotated:"+rotation);

        if ( list_of_strings!=null) exif_metadata.addAll(list_of_strings);

        return exif_metadata;

    }

}
