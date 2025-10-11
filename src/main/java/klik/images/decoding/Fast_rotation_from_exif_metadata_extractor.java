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

//**********************************************************
public class Fast_rotation_from_exif_metadata_extractor
//**********************************************************
{
    public static final boolean dbg = false;

    //**********************************************************
    public static double get_rotation(Path path, boolean report_if_not_found, Aborter aborter, Logger logger)
    //**********************************************************
    {

        InputStream is = From_disk.get_image_InputStream(path, Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, aborter, logger);
        if ( is == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Warning: cannot open file "+path));
            return 0.0;
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
                                    return 90.0;
                                }
                            }
                            else if (tag.toString().contains("180"))
                            {
                                return 180.0;
                            }
                            else if (tag.toString().contains("270"))
                            {
                                return 270.0;
                            }
                            else
                            {
                                return 0.0;
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
                return 0.0;
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

        return 0.0;
    }

}
