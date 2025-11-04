// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.image.decoding;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.icons.image_properties_cache.Rotation;
import klik.look.my_i18n.My_I18n;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.Check_remaining_RAM;
import klik.util.files_and_paths.Extensions;
import klik.util.image.Full_image_from_disk;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.experimental.fusk.Fusk_static_core;
import klik.experimental.fusk.Fusk_strings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Exif_metadata_extractor
//**********************************************************
{
    public static final boolean dbg = false;
    Path path;
    boolean image_is_damaged;
    public String title="";
    List<String> exif_metadata = null;
    private double rotation = 0;
    Logger logger;
    Window owner;

    //**********************************************************
    public Exif_metadata_extractor(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        this.owner = owner;
        this.path = path;
        this.logger = logger;
    }

    //**********************************************************
    @Deprecated
    public double get_rotation(boolean report_if_not_found, Aborter aborter)
    //**********************************************************
    {
        if ( exif_metadata != null ) return rotation;
        logger.log(Stack_trace_getter.get_stack_trace("WARNING"));
        rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(path, report_if_not_found, aborter, logger).orElse(0.0);
        return rotation;
    }

    //**********************************************************
    public boolean is_image_damaged()
    //**********************************************************
    {
        return image_is_damaged;
    }
    //**********************************************************
    public List<String> get_exif_metadata(
            double how_many_pixels,
            boolean report_if_not_found,
            Aborter aborter,
            boolean details)
    //**********************************************************
    {
        if ( exif_metadata != null) return exif_metadata;

        exif_metadata =  new ArrayList<>();

        if ( path == null)
        {
            exif_metadata.add("this image was not created from file");
            return exif_metadata;
        }

        String extension = Extensions.get_extension(path.getFileName().toString());
        if ( extension.equalsIgnoreCase(Fusk_static_core.FUSK_EXTENSION))
        {
            if (Feature_cache.get(Feature.Fusk_is_on)) {
                if (Fusk_static_core.is_fusk(path,logger)) {
                    String base = Extensions.get_base_name(path.toAbsolutePath().toString());
                    exif_metadata.add("... which is a fusk of: ->" + Fusk_strings.defusk_string(base, logger) + "<-");
                } else {
                    exif_metadata.add("... which has a fusk extension BUT IS NOT!");
                }
            }
        }



        List<String> list_of_strings = null;
        if ( details)
        {
            list_of_strings = new ArrayList<>();
        }

        Image_properties image_properties = Fast_image_property_from_exif_metadata_extractor.get_image_properties(path,true,aborter,logger).orElse(new Image_properties(0,0, Rotation.normal));
        exif_metadata.add("EXIF width="+image_properties.get_image_width());
        exif_metadata.add("EXIF height="+image_properties.get_image_height());
        exif_metadata.add("EXIF aspect_ratio="+image_properties.get_aspect_ratio());
        double aspect_ratio = Fast_aspect_ratio_from_exif_metadata_extractor.get_aspect_ratio(path,report_if_not_found,aborter,list_of_strings,logger).orElse(1.0);
        exif_metadata.add("Alternative aspect_ratio="+aspect_ratio);


        {
            long l = 0;
            try
            {
                l = Files.size(path);
            } catch (IOException e)
            {
                logger.log("extract_exif_metadata() Managed exception (2)->"+e+"<- for:"+ path.toAbsolutePath());
            }
            double bits_per_pixel = (double)l*8.0/how_many_pixels;
            String s_bits_per_pixel = My_I18n.get_I18n_string("Bits_per_pixel",owner,logger);
            exif_metadata.add(s_bits_per_pixel+": "+bits_per_pixel);
        }

        // next top item: image date (filesystem)
        BasicFileAttributes attr;
        try
        {
            attr = Files.readAttributes(path, BasicFileAttributes.class);
            exif_metadata.add("creation time: "+attr.creationTime());
            exif_metadata.add("last access: "+attr.lastAccessTime());
            exif_metadata.add("last modified: "+attr.lastModifiedTime());
        }
        catch (IOException e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (1)->"+e+"<- for:"+ path.toAbsolutePath()));
            }
            return exif_metadata;
        }

        image_is_damaged = false;

        if (Check_remaining_RAM.RAM_running_low(logger)) {
            logger.log("get_exif_metadata NOT DONE because running low on memory ! ");
            return exif_metadata;
        }

        InputStream is = Full_image_from_disk.get_image_InputStream(path, Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, aborter, logger);
        if ( is == null)
        {
            image_is_damaged = true;
            return exif_metadata;
        }

        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            for (Directory directory : metadata.getDirectories())
            {
                for (Tag tag : directory.getTags())
                {
                    //logger.log(tag);
                    exif_metadata.add(tag.toString());
                    if (tag.toString().contains("Title")) {
                        title = tag.toString();
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
                                    rotation = 90.0;
                                }
                            }
                            else if (tag.toString().contains("180"))
                            {
                                rotation = 180.0;
                            }
                            else if (tag.toString().contains("270"))
                            {
                                rotation = 270.0;
                            }
                            else
                            {
                                rotation = 0.0;
                            }
                        }
                    }
                }
            }
            is.close();
        }
        catch (ImageProcessingException e)
        {
            if ( e.toString().contains("File format could not be determined"))
            {
                image_is_damaged = true;
            }
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (3)->"+e+"<- for:"+ path.toAbsolutePath()));
           }
        }
        catch (IOException e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (4)->"+e+"<- for:"+ path.toAbsolutePath()));
           }
        }
        catch (Exception e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
            }
        }

        exif_metadata.add("apparently (careful here, this may be wrong in rare cases) rotated:"+rotation);

        if ( list_of_strings!=null) exif_metadata.addAll(list_of_strings);

        return exif_metadata;

    }

}
