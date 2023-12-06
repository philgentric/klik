package klik.images.decoding;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import klik.actor.Aborter;
import klik.properties.Static_application_properties;
import klik.util.From_disk;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

//**********************************************************
public class Fast_aspect_ratio_from_exif_metadata_extractor
//**********************************************************
{
    public static final boolean dbg = false;

    //**********************************************************
    public static double get_aspect_ratio(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {

        boolean enable_fusk = Static_application_properties.get_enable_fusk(logger);
        InputStream is = From_disk.get_image_InputStream(path, enable_fusk, aborter, logger);
        if ( is == null)
        {
            //logger.log(" get_aspect_ratio failed1");
            return 1.0;
        }
        Double w = null;
        Double h = null;

        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(is);

            boolean done = false;
            for (Directory directory : metadata.getDirectories())
            {
                for (Tag tag : directory.getTags())
                {
                    if (tag.toString().contains("Width"))
                    {
                        //logger.log("==>"+ tag);
                        if (tag.toString().contains("Image"))
                        {
                            w = Double.valueOf(get_number(tag.toString()));
                            if ( h !=null)
                            {
                                done = true;
                                break;
                            }
                        }
                    }
                    if (tag.toString().contains("Height"))
                    {
                        //logger.log("==>"+ tag);
                        if (tag.toString().contains("Image"))
                        {
                            h = Double.valueOf(get_number(tag.toString()));
                            if ( w !=null)
                            {
                                done = true;
                                break;
                            }
                        }
                    }
                }
                if ( done) break;
            }
            is.close();
        }
        catch (ImageProcessingException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (3)->"+e+"<- for:"+ path.toAbsolutePath()));
            //logger.log(" get_aspect_ratio failed2");
            if ( e.toString().contains("File format could not be determined"))  return 1.0;
        }
        catch (IOException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (4)->"+e+"<- for:"+ path.toAbsolutePath()));
            //logger.log(" get_aspect_ratio failed3");
            return 1.0;
        }
        catch (Exception e)
        {
            //if ( dbg)
                logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
            //logger.log(" get_aspect_ratio failed4");
            return 1.0;
        }

        if (( w != null) && ( h != null))
        {
            //logger.log("aspect ratio: "+w/h);
            return w/h;
        }
        //logger.log(" get_aspect_ratio failed5");
        return 1.0;
    }

    public static double get_number(String s)
    {
        String[] pieces = s.split(" ");
        for ( int i = pieces.length-1; i>0;i--)
        {
            String p = pieces[i];
            try
            {
                Double x = Double.valueOf(p);
                return x;
            }
            catch(IllegalArgumentException e)
            {
                continue;
            }
        }
        return 0;
    }

}
