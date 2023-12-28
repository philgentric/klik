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
        boolean invert_width_and_height = false;
        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(is);

            boolean w_done = false;
            boolean h_done = false;
            boolean rot_done = false;
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
                            if (dbg)logger.log("w="+w);
                            w_done = true;
                            if (w_done && h_done && rot_done) break;
                        }
                    }
                    if (tag.toString().contains("Height"))
                    {
                        //logger.log("==>"+ tag);
                        if (tag.toString().contains("Image"))
                        {
                            h = Double.valueOf(get_number(tag.toString()));
                            if (dbg)logger.log("h="+h);
                            h_done = true;
                            if (w_done && h_done && rot_done) break;
                        }
                    }
                    if (tag.toString().contains("Orientation"))
                    {
                        if (!tag.toString().contains("Thumbnail"))
                        {
                            // Orientation - Right side, top (Rotate 90 CW clockwise)
                            if (tag.toString().contains("90"))
                            {
                                if (tag.toString().contains("CW"))
                                {
                                    if (dbg)logger.log("90");

                                    invert_width_and_height = true;
                                    rot_done = true;
                                    if (w_done && h_done && rot_done) break;
                                }
                            }
                            else if (tag.toString().contains("180"))
                            {
                                if (dbg)logger.log("180");
                                rot_done = true;
                                if (w_done && h_done && rot_done) break;
                            }
                            else if (tag.toString().contains("270"))
                            {
                                if (dbg)logger.log("270");

                                invert_width_and_height = true;
                                rot_done = true;
                                if (w_done && h_done && rot_done) break;
                            }
                            else
                            {
                                if (dbg)logger.log("0");
                                rot_done = true;
                                if (w_done && h_done && rot_done) break;
                            }
                        }
                    }
                }
                if (w_done && h_done && rot_done) break;
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
            if ( dbg)
                logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
            return 1.0;
        }

        if (( w != null) && ( h != null))
        {
            if (invert_width_and_height)
            {
                if (dbg)logger.log(path.getFileName().toString()+" INVERTED aspect ratio: "+h/w);
                logger.log("from exif ->"+h/w+"<-");

                return h/w;
            }
            if (dbg)logger.log(path.getFileName().toString()+" aspect ratio: "+w/h);
            logger.log("from exif ->"+w/h+"<-");
            return w/h;
        }
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
