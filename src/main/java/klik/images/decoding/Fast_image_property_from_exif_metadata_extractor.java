package klik.images.decoding;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import klik.actor.Aborter;
import klik.browser.icons.caches.Image_properties;
import klik.browser.icons.caches.Rotation;
import klik.properties.Static_application_properties;
import klik.util.From_disk;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Fast_image_property_from_exif_metadata_extractor
//**********************************************************
{
    public static final boolean dbg = false;
    private record Directory_result(Double w, Double h, Rotation rotation, boolean w_done, boolean h_done, boolean rot_done){}

    //**********************************************************
    public static Image_properties get_image_properties(Path path, boolean report_if_not_found, Aborter aborter, Logger logger)
    //**********************************************************
    {

        boolean enable_fusk = Static_application_properties.get_enable_fusk(logger);
        InputStream is = From_disk.get_image_InputStream(path, enable_fusk, report_if_not_found, aborter, logger);
        if ( is == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Warning: cannot open file "+path));
            return new Image_properties(-1,-1, Rotation.normal);
        }
        List<String> sb = null;
        if ( dbg) sb = new ArrayList<>();
        Directory_result best = null;
        Directory_result best_no_rot = null;
        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            if (sb != null)
            {
                sb.add("\nstart loop on EXIF directories");
                //logger.log(Stack_trace_getter.get_stack_trace("WTF"));
            }
            for (Directory directory : metadata.getDirectories())
            {
                if ( aborter.should_abort())
                {
                    //logger.log("Fast_aspect_ratio_from_exif_metadata_extractor aborting ");
                    return new Image_properties(-1,-1, Rotation.normal);
                }

                if ( directory.toString().contains("Canon Makernote"))
                {
                    if (sb != null)sb.add("skipping directory="+directory);

                    continue;
                }
                if (sb != null)sb.add("directory="+directory);
                Directory_result local = do_one_dir(directory,sb);

                if (local.w_done && local.h_done && local.rot_done)
                {
                    best = local;
                    if (sb != null)sb.add("rotation found");
                    break;
                }
                if (local.w_done && local.h_done)
                {
                    if (sb != null)sb.add("rotation not found");
                    best_no_rot = local;
                }
            }
            is.close();
        }
        catch (ImageProcessingException e)
        {
            if ( sb != null) sb.add(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (3)->"+e+"<- for:"+ path.toAbsolutePath()));
            if ( e.toString().contains("File format could not be determined"))  
            {
                return new Image_properties(-1,-1, Rotation.normal);
            }
        }
        catch (IOException e)
        {
            if ( sb != null) sb.add(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (4)->"+e+"<- for:"+ path.toAbsolutePath()));
            return new Image_properties(-1,-1, Rotation.normal);
        }
        catch (Exception e)
        {
            if ( sb != null)
                sb.add(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
            return new Image_properties(-1,-1, Rotation.normal);
        }

        Directory_result result = best;
        if ( result == null)
        {
            if (sb != null)sb.add("finally ... rotation not found");
            result = best_no_rot;
        }
        else
        {
            if (sb != null)sb.add("finally ... rotation found");
        }

        if ((best != null) && (best_no_rot != null))
        {
            double h = 0;
            double w = 0;
            if (best.h != best_no_rot.h) h = best_no_rot.h;
            if (best.w != best_no_rot.w) w = best_no_rot.w;
            if (sb != null) sb.add(" w= "+w+" h= "+h+" rot: "+best.rotation);
            return new Image_properties(w, h, best.rotation);
        }

        if ( result == null)
        {
            if ( sb != null)
            {
                sb.add("NO EXIF data?");
                logger.log(sb.toString());
            }
            return new Image_properties(-1,-1, Rotation.normal);
        }
        if (( result.w != null) && ( result.h != null))
        {
            if (sb != null) sb.add(" w= "+result.w+" h= "+result.h+" rot: "+result.rotation);
            return new Image_properties(result.w,result.h,result.rotation);
        }
        if ( sb != null)
        {
            sb.add("should not happen?");
            logger.log(sb.toString());
        }
        return new Image_properties(-1,-1, Rotation.normal);
    }


    //**********************************************************
    private static Directory_result do_one_dir(Directory directory, List<String> sb)
    //**********************************************************
    {
        Double w = null;
        Double h = null;
        Rotation rotation = null;
        boolean w_done = false;
        boolean rot_done = false;
        boolean h_done = false;


        for (Tag tag : directory.getTags())
        {
            if (tag.toString().contains("Thumbnail")) continue;
            if (dbg) sb.add("tag->"+tag+"<-");
            if (tag.toString().contains("Width"))
            {
                if (tag.toString().contains("SubIFD"))
                {
                    if ( sb!=null) sb.add("width tag contains Related: ignored");
                    continue; // some images contain the tag "Related Image Width", probably the original before edit?
                }
                if (tag.toString().contains("Related"))
                {
                    if ( sb!=null) sb.add("width tag contains Related: ignored");
                    continue; // some images contain the tag "Related Image Width", probably the original before edit?
                }
                if (tag.toString().contains("Image"))
                {
                    w = Double.valueOf(get_number(tag.toString()));
                    if (sb!=null) sb.add("w="+w+" from tag:"+tag);
                    w_done = true;
                    if (w_done && h_done && rot_done) break;
                }
            }
            if (tag.toString().contains("Height"))
            {
                if (tag.toString().contains("SubIFD"))
                {
                    if ( sb!=null) sb.add("height tag contains Related: ignored");
                    continue; // some images contain the tag "Related Image Width", probably the original before edit?
                }
                if (tag.toString().contains("Related"))
                {
                    if ( sb!=null) sb.add("width tag contains Related: ignored");
                    continue; // some images contain the tag "Related Image Width", probably the original before edit?
                }
                if (tag.toString().contains("Image"))
                {
                    h = Double.valueOf(get_number(tag.toString()));
                    if (sb!=null) sb.add("h="+h+" from tag:"+tag);
                    h_done = true;
                    if (w_done && h_done && rot_done) break;
                }
            }
            if (tag.toString().contains("Orientation"))
            {
                if (tag.toString().contains("Thumbnail")) continue;

                {
                    // Orientation - Right side, top (Rotate 90 CW clockwise)
                    if (tag.toString().contains("90"))
                    {
                        if (tag.toString().contains("CW"))
                        {
                            if (sb!=null)sb.add("rotation=90");
                            rotation = Rotation.rot_90_clockwise;
                            rot_done = true;
                            if (w_done && h_done && rot_done) break;
                        }
                    }
                    else if (tag.toString().contains("180"))
                    {
                        if (sb!=null)sb.add("rotation=180");
                        rot_done = true;
                        if (w_done && h_done && rot_done) break;
                    }
                    else if (tag.toString().contains("270"))
                    {
                        if (sb!=null)sb.add("rotation=270");
                        rotation = Rotation.rot_90_anticlockwise;
                        rot_done = true;
                        if (w_done && h_done && rot_done) break;
                    }
                    else
                    {
                        if (sb!=null)sb.add("rotation=0");
                        rot_done = true;
                        if (w_done && h_done && rot_done) break;
                    }
                }
            }
        }
        return new Directory_result(w,h, rotation,w_done,h_done,rot_done);
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
