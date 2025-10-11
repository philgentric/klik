package klik.images.decoding;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import klik.actor.Aborter;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.files_and_paths.From_disk;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

//**********************************************************
public class Fast_width_from_exif_metadata_extractor
//**********************************************************
{
    public static final boolean dbg = false;

    //**********************************************************
    public static double get_width(Path path, boolean report_if_not_found, List<String> sb, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if( sb != null)
        {
            sb.add(path.toString());
        }
        InputStream is = From_disk.get_image_InputStream(path, Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, aborter, logger);
        if ( is == null)
        {
            if ( sb != null)
            {
                sb.add(" get_aspect_ratio failed cannot open input stream");
                logger.log(sb.toString());
            }
            return -1.0;
        }

        Double w = null;
        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            if (sb != null)
            {
                if ( dbg) sb.add("\nstart loop on EXIF directories");
            }
            for (Directory directory : metadata.getDirectories())
            {
                if ( aborter.should_abort())
                {
                    //logger.log("Fast_aspect_ratio_from_exif_metadata_extractor aborting ");
                    return -1;
                }

                if ( directory.toString().contains("Canon Makernote"))
                {
                    if (sb != null)sb.add("skipping directory="+directory);

                    continue;
                }
                if (sb != null)sb.add("directory="+directory);
                Double local = do_one_dir(directory,sb);

                if (local != null)
                {
                    if (sb != null)sb.add("width found");
                    w = local;
                    break;
                }
            }
            is.close();
        }
        catch (ImageProcessingException e)
        {
            if ( sb != null) sb.add(Stack_trace_getter.get_stack_trace("get_width() Managed exception (3)->"+e+"<- for:"+ path.toAbsolutePath()));
            if ( e.toString().contains("File format could not be determined"))  return -1.0;
        }
        catch (IOException e)
        {
            if ( sb != null) sb.add(Stack_trace_getter.get_stack_trace("get_width() Managed exception (4)->"+e+"<- for:"+ path.toAbsolutePath()));
            return -1.0;
        }
        catch (Exception e)
        {
            if ( sb != null)
                sb.add(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
            return -1.0;
        }
        return w;
    }


    //**********************************************************
    private static Double do_one_dir(Directory directory, List<String> sb)
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

                            invert_width_and_height = true;
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

                        invert_width_and_height = true;
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
        if ( invert_width_and_height) return h;
        return w;
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
