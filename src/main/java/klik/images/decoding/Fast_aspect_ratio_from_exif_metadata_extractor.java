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
    public static final boolean dbg = true;

    private record Directory_result(Double w, Double h, boolean invert_width_and_height, boolean w_done, boolean h_done, boolean rot_done){}

    //**********************************************************
    public static double get_aspect_ratio(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        StringBuilder sb;
        if( dbg)
        {
            sb = new StringBuilder();
            sb.append("\n\n").append(path);
        }
        boolean enable_fusk = Static_application_properties.get_enable_fusk(logger);
        InputStream is = From_disk.get_image_InputStream(path, enable_fusk, aborter, logger);
        if ( is == null)
        {
            if ( dbg)
            {
                sb.append(" get_aspect_ratio failed cannot open input stream");
                logger.log(sb.toString());
            }
            return 1.0;
        }
        if (dbg)sb.append("\n\n").append(path);

        Directory_result best = null;
        Directory_result best_no_rot = null;
        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(is);


            if (dbg)sb.append("\nstart loop on EXIF directories\n");
            for (Directory directory : metadata.getDirectories())
            {
                if (dbg)sb.append("directory=").append(directory).append("\n");
                Directory_result result = do_one_dir(directory,sb);

                if (result.w_done && result.h_done && result.rot_done)
                {
                    best = result;
                    break;
                }
                if (result.w_done && result.h_done)
                {
                    best_no_rot = result;
                }
            }
            is.close();
        }
        catch (ImageProcessingException e)
        {
            if ( dbg) sb.append(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (3)->"+e+"<- for:"+ path.toAbsolutePath()));
            //sb.append(" get_aspect_ratio failed2");
            if ( e.toString().contains("File format could not be determined"))  return 1.0;
        }
        catch (IOException e)
        {
            if ( dbg) sb.append(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (4)->"+e+"<- for:"+ path.toAbsolutePath()));
            //sb.append(" get_aspect_ratio failed3");
            return 1.0;
        }
        catch (Exception e)
        {
            if ( dbg)
                sb.append(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
            return 1.0;
        }

        Directory_result result = best;
        if ( result == null) result = best_no_rot;
        if ( result == null)
        {
            if ( dbg)
            {
                sb.append("NO EXIF data?");
                logger.log(sb.toString());
            }
            return 1.0;
        }
        if (( result.w != null) && ( result.h != null))
        {
            if (result.invert_width_and_height)
            {
                if (dbg)
                {
                    sb.append(" INVERTED aspect ratio: ").append(result.h).append("/").append(result.w).append("=").append(result.h/result.w).append("\n");
                    logger.log(sb.toString());
                }
                return result.h/result.w;
            }
            if (dbg)
            {
                sb.append(" STRAIGHT aspect ratio: ").append(result.w).append("/").append(result.h).append("=").append(result.w/result.h).append("\n");
                logger.log(sb.toString());

            }
            return result.w/result.h;
        }
        if ( dbg)
        {
            sb.append("should not happen?");
            logger.log(sb.toString());
        }
        return 1.0;
    }


    //**********************************************************
    private static Directory_result do_one_dir(Directory directory, StringBuilder sb)
    //**********************************************************
    {
        Double w = null;
        Double h = null;
        boolean w_done = false;
        boolean rot_done = false;
        boolean h_done = false;
        boolean invert_width_and_height = false;

        
        for (Tag tag : directory.getTags())
        {
            if (dbg) sb.append("tag=").append(tag).append("\n");
            if (tag.toString().contains("Width"))
            {
                //sb.append("==>"+ tag);
                if (tag.toString().contains("Image"))
                {
                    w = Double.valueOf(get_number(tag.toString()));
                    if (dbg) sb.append("w=").append(w).append("\n");
                    w_done = true;
                    if (w_done && h_done && rot_done) break;
                }
            }
            if (tag.toString().contains("Height"))
            {
                //sb.append("==>"+ tag);
                if (tag.toString().contains("Image"))
                {
                    h = Double.valueOf(get_number(tag.toString()));
                    if (dbg)sb.append("h=").append(h).append("\n");
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
                            if (dbg)sb.append("90").append("\n");

                            invert_width_and_height = true;
                            rot_done = true;
                            if (w_done && h_done && rot_done) break;
                        }
                    }
                    else if (tag.toString().contains("180"))
                    {
                        if (dbg)sb.append("180").append("\n");
                        rot_done = true;
                        if (w_done && h_done && rot_done) break;
                    }
                    else if (tag.toString().contains("270"))
                    {
                        if (dbg)sb.append("270").append("\n");

                        invert_width_and_height = true;
                        rot_done = true;
                        if (w_done && h_done && rot_done) break;
                    }
                    else
                    {
                        if (dbg)sb.append("0").append("\n");
                        rot_done = true;
                        if (w_done && h_done && rot_done) break;
                    }
                }
            }
        }
        return new Directory_result(w,h,invert_width_and_height,w_done,h_done,rot_done);
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
