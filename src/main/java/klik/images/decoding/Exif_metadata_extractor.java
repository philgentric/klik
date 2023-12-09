package klik.images.decoding;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import klik.actor.Aborter;
import klik.files_and_paths.Files_and_Paths;
import klik.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.From_disk;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

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
    public double get_rotation(double how_many_pixels, Aborter aborter)
    //**********************************************************
    {
        if ( exif_metadata != null ) return rotation;
        get_exif_metadata(how_many_pixels, aborter);
        return rotation;
    }

    //**********************************************************
    public boolean is_image_damaged()
    //**********************************************************
    {
        return image_is_damaged;
    }
    //**********************************************************
    public List<String> get_exif_metadata(double how_many_pixels, Aborter aborter)
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


        String file_size = Files_and_Paths.get_2_line_string_with_size(path.toAbsolutePath(),logger);
        file_size = file_size.replace("\n","  -  ");
        exif_metadata.add(file_size);

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
            return null;
        }

        image_is_damaged = false;

        InputStream is = From_disk.get_image_InputStream(path, aborter, logger);
        if ( is == null)
        {
            image_is_damaged = true;
            return null;
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
                                    exif_metadata.add("rotated 90");
                                }
                            }
                            else if (tag.toString().contains("180"))
                            {
                                rotation = 180.0;
                                exif_metadata.add("rotated 180");
                            }
                            else if (tag.toString().contains("270"))
                            {
                                rotation = 270.0;
                                exif_metadata.add("rotated 270");
                            }
                            else
                            {
                                rotation = 0.0;
                                exif_metadata.add("rotated 0");
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

        return exif_metadata;

    }

}
