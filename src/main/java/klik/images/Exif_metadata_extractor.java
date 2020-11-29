package klik.images;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Exif_metadata_extractor
{
    public static final boolean dbg = true;
    Path f;
    boolean image_is_damaged;
    List<String> exif_metadata = null;
    private int rotation = 0;
    Logger logger;

    public Exif_metadata_extractor(Path f_, Logger logger_)
    {
        f = f_;
        logger = logger_;
    }

    public int get_rotation()
    {
        if ( exif_metadata == null) read_exif_metadata_from_file(logger);
        return rotation;
    }
    public List<String> get_exif_metadata()
    {
        if ( exif_metadata == null) read_exif_metadata_from_file(logger);
        return exif_metadata;
    }

    private List<String> read_exif_metadata_from_file(Logger logger)
    {

        // top item in the display: the file name and path
        exif_metadata = new ArrayList<String>();
        if ( f == null)
        {
            exif_metadata.add("this image was not created from file");
            return exif_metadata;
        }
        exif_metadata.add("File:->"+f.toAbsolutePath()+"<-");

        // next top item: image date (filesystem)
        BasicFileAttributes attr;
        try
        {
            attr = Files.readAttributes(f, BasicFileAttributes.class);
            exif_metadata.add("creation time: "+attr.creationTime());
            exif_metadata.add("last access: "+attr.lastAccessTime());
            exif_metadata.add("last modified: "+attr.lastModifiedTime());
        }
        catch (IOException e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (1)->"+e+"<- for:"+f.toAbsolutePath()));
            }
            return null;
        }



        // next top item: file size
        long l = 0;
        try
        {
            l = Files.size(f);
        } catch (IOException e)
        {
            logger.log("extract_exif_metadata() Managed exception (2)->"+e+"<- for:"+f.toAbsolutePath());
        }
        String approx = "";
        if ( l > 1000000000)   approx = "File size: "+(new DecimalFormat("#.##").format(l/1000000000.0))+"GB ";
        else if ( l > 1000000)   approx = "File size: "+(new DecimalFormat("#.##").format(l/1000000.0))+"MB ";
        else if ( l > 1000) approx = "File size: "+(new DecimalFormat("#.##").format(l/1000.0))+"KB ";
        else approx = l+"B";
        exif_metadata.add(approx);

        image_is_damaged = false;

        try (FileInputStream fis = new FileInputStream(f.toFile()))
        {
            Metadata metadata = ImageMetadataReader.readMetadata(fis);
            for (Directory directory : metadata.getDirectories())
            {
                for (Tag tag : directory.getTags())
                {
                    //logger.log(tag);
                    exif_metadata.add(tag.toString());
                    if ( tag.toString().contains("Orientation") == true)
                    {
                        if ( tag.toString().contains("Thumbnail") == false)
                        {
                            // Orientation - Right side, top (Rotate 90 CW)
                            if (tag.toString().contains("90") == true)
                            {
                                if (tag.toString().contains("CW") == true)
                                {
                                    // have to rotate +90
                                    rotation = 90;
                                    exif_metadata.add("rotated 90");
                                }
                            }
                            else if (tag.toString().contains("180") == true)
                            {
                                rotation = 180;
                                exif_metadata.add("rotated 180");
                            }
                            else if (tag.toString().contains("270") == true)
                            {
                                rotation = 270 ;
                                exif_metadata.add("rotated 270");
                            }
                            else
                            {
                                rotation = 0;
                                exif_metadata.add("rotated 0");
                            }
                        }
                    }
                }
            }
        }
        catch (ImageProcessingException e)
        {
            if ( e.toString().contains("File format could not be determined"))
            {
                image_is_damaged = true;
            }
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (3)->"+e+"<- for:"+f.toAbsolutePath()));
           }
        }
        catch (IOException e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (4)->"+e+"<- for:"+f.toAbsolutePath()));
           }
        }
        catch (Exception e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (5)->"+e+"<- for:"+f.toAbsolutePath()));
            }
        }

        return exif_metadata;

    }

}
