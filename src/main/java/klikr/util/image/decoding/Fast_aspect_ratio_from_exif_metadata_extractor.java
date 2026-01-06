// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.image.decoding;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import klikr.util.execute.actor.Aborter;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.Check_remaining_RAM;
import klikr.util.image.Full_image_from_disk;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

//**********************************************************
public class Fast_aspect_ratio_from_exif_metadata_extractor
//**********************************************************
{
    public static final boolean dbg = false;

    private record Directory_result(Double w, Double h, boolean invert_width_and_height, boolean w_done, boolean h_done, boolean rot_done){}

    //**********************************************************
    public static Optional<Double> get_aspect_ratio(Path path, boolean report_if_not_found, Aborter aborter, List<String> sb, Logger logger)
    //**********************************************************
    {
        if( sb != null)
        {
            sb.add(path.toString());
        }
        if (Check_remaining_RAM.RAM_running_low(logger)) {
            logger.log("get_aspect_ratio NOT DONE because running low on memory ! ");
            return Optional.empty();
        }
        InputStream is = Full_image_from_disk.get_image_InputStream(path, Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, aborter, logger);
        if ( is == null)
        {
            if ( sb != null)
            {
                sb.add(" get_aspect_ratio failed cannot open input stream");
                logger.log(sb.toString());
            }
            return Optional.empty();
        }

        Directory_result best = null;
        Directory_result best_no_rot = null;
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
                    return Optional.empty();
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
                return Optional.empty();
            }
        }
        catch (IOException e)
        {
            if ( sb != null) sb.add(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (4)->"+e+"<- for:"+ path.toAbsolutePath()));
            return Optional.empty();
        }
        catch (Exception e)
        {
            if ( sb != null)
                sb.add(Stack_trace_getter.get_stack_trace("get_aspect_ratio() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
            return Optional.empty();
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
            if (best.invert_width_and_height)
            {
                if (sb != null) sb.add(" INVERTED aspect ratio h/w: "+h+"/"+w+"="+h/w);
                return Optional.of(h/w);
            }
            else
            {
                if (sb != null) sb.add(" aspect ratio w/h: "+w+"/"+h+"="+w/h);
                return Optional.of(w/h);
            }
        }

        if ( result == null)
        {
            if ( sb != null)
            {
                sb.add("NO EXIF data?");
                logger.log(sb.toString());
            }
            return Optional.empty();
        }
        if (( result.w != null) && ( result.h != null))
        {
            if (result.invert_width_and_height)
            {
                if (sb != null)
                {
                    sb.add(" INVERTED aspect ratio h/w: "+result.h+"/"+result.w+"="+result.h/result.w);
                    logger.log(sb.toString());
                }
                return Optional.of(result.h/result.w);
            }
            if (sb != null)
            {
                sb.add(" aspect ratio w/h: "+result.w+"/"+result.h+"="+result.w/result.h);
                logger.log(sb.toString());

            }
            return Optional.of(result.w/result.h);
        }
        if ( sb != null)
        {
            sb.add("should not happen?");
            logger.log(sb.toString());
        }
        return Optional.empty();
    }


    //**********************************************************
    private static Directory_result do_one_dir(Directory directory, List<String> sb)
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
        return new Directory_result(w,h,invert_width_and_height,w_done,h_done,rot_done);
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
