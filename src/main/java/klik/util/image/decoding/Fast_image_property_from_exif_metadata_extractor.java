package klik.util.image.decoding;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import klik.actor.Aborter;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.icons.image_properties_cache.Rotation;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.image.Full_image_from_disk;
import klik.util.image.Icons_from_disk;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

//**********************************************************
public class Fast_image_property_from_exif_metadata_extractor
//**********************************************************
{
    public static final boolean dbg = false; // gets VERY SLOW with dbg
    private record Directory_result(Double w, Double h, Rotation rotation, boolean w_done, boolean h_done, boolean rot_done){}

    //**********************************************************
    public static Image_properties get_image_properties(Path path, boolean report_if_not_found, Aborter aborter, Logger logger)
    //**********************************************************
    {
        //logger.log("\n\n\nget_image_properties "+path);
        InputStream is = Full_image_from_disk.get_image_InputStream(path, Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, aborter, logger);
        if ( is == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Warning: cannot open file "+path));
            return new Image_properties(-1,-1, Rotation.normal);
        }

        Rotation rotation = Rotation.normal;
        double w = -1.0;
        double h = -1.0;
        //Directory_result best = null;
        //Directory_result best_no_rot = null;
        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            if (dbg) logger.log("\nstart loop on EXIF directories");

            for (Directory directory : metadata.getDirectories())
            {
                if ( directory.toString().contains("Canon Makernote"))
                {
                    continue;
                }
                for (Tag tag : directory.getTags())
                {
                    if (tag.toString().contains("Width"))
                    {
                        if (tag.toString().contains("SubIFD"))
                        {
                            if ( dbg) logger.log("width tag contains SubIFD: ignored");
                            continue; 
                        }
                        if (tag.toString().contains("Related"))
                        {
                            if ( dbg) logger.log("width tag contains Related: ignored");
                            continue; // some images contain the tag "Related Image Width", probably the original before edit?
                        }
                        if (tag.toString().contains("Image"))
                        {
                            double w_tmp = Double.valueOf(get_number(tag.toString()));
                            if ( w_tmp > w) w = w_tmp;
                            if (dbg) logger.log("w="+w+" from tag:"+tag);
                        }
                    }
                    if (tag.toString().contains("Height"))
                    {
                        if (tag.toString().contains("SubIFD"))
                        {
                            if ( dbg) logger.log("height tag contains SubIFD: ignored");
                            continue;
                        }
                        if (tag.toString().contains("Related"))
                        {
                            if ( dbg) logger.log("width tag contains Related: ignored");
                            continue;
                        }
                        if (tag.toString().contains("Image"))
                        {
                            double h_tmp = Double.valueOf(get_number(tag.toString()));
                            if ( h_tmp > h) h = h_tmp;

                            if (dbg) logger.log("h="+h+" from tag:"+tag);
                        }
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
                                    rotation = Rotation.rot_90_clockwise;
                                }
                            }
                            else if (tag.toString().contains("180"))
                            {
                                rotation = Rotation.upsidedown;
                            }
                            else if (tag.toString().contains("270"))
                            {
                                rotation = Rotation.rot_90_anticlockwise;
                            }
                            else
                            {
                                rotation = Rotation.normal;
                            }
                        }
                    }
                }
            }
            is.close();
            return new Image_properties(w,h,rotation);
        }
        catch (ImageProcessingException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (3)->"+e+"<- for:"+ path.toAbsolutePath()));
            if ( e.toString().contains("File format could not be determined"))  
            {
                return new Image_properties(-1,-1, Rotation.normal);
            }
        }
        catch (IOException e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (4)->"+e+"<- for:"+ path.toAbsolutePath()));
            }
            return new Image_properties(-1,-1, Rotation.normal);
        }
        catch (Exception e)
        {
            if (dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
            }
            return new Image_properties(-1,-1, Rotation.normal);
        }

        if ( dbg)
        {
            logger.log("should not happen?");
        }
        return new Image_properties(-1,-1, Rotation.normal);
    }


    public static Double get_number(String s)
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
        return Double.valueOf(0);
    }
}
