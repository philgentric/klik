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
import java.util.Optional;

//**********************************************************
public class Fast_rotation_from_exif_metadata_extractor
//**********************************************************
{
    public static final boolean dbg = false;

    //**********************************************************
    public static Optional<Double> get_rotation(Path path, boolean report_if_not_found, Aborter aborter, Logger logger)
    //**********************************************************
    {

        if (Check_remaining_RAM.RAM_running_low(logger)) {
            logger.log("get_rotation NOT DONE because running low on memory ! ");
            return Optional.empty();
        }

        InputStream is = Full_image_from_disk.get_image_InputStream(path, Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, aborter, logger);
        if ( is == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Warning: cannot open file "+path));
            return Optional.empty();
        }

        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            for (Directory directory : metadata.getDirectories())
            {
                for (Tag tag : directory.getTags())
                {
                    if (tag.toString().contains("Orientation"))
                    {
                        if (!tag.toString().contains("Thumbnail"))
                        {
                            // Orientation - Right side, top (Rotate 90 CW)
                            if (tag.toString().contains("90"))
                            {
                                if (tag.toString().contains("CW"))
                                {
                                    return Optional.of(90.0);
                                }
                            }
                            else if (tag.toString().contains("180"))
                            {
                                return Optional.of(180.0);
                            }
                            else if (tag.toString().contains("270"))
                            {
                                return Optional.of(270.0);
                            }
                            else
                            {
                                return Optional.of(0.0);
                            }
                        }
                    }
                }
            }
            is.close();
        }
        catch (ImageProcessingException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (3)->"+e+"<- for:"+ path.toAbsolutePath()));
            if ( e.toString().contains("File format could not be determined"))
            {
                logger.log("Warning:"+e);
                return Optional.empty();
            }
        }
        catch (IOException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (4)->"+e+"<- for:"+ path.toAbsolutePath()));
        }
        catch (Exception e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
        }

        return Optional.empty();
    }

}
