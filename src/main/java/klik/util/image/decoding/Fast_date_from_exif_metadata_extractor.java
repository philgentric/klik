// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.image.decoding;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import klik.util.execute.actor.Aborter;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.Check_remaining_RAM;
import klik.util.image.Full_image_from_disk;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

//**********************************************************
public class Fast_date_from_exif_metadata_extractor
//**********************************************************
{
    public static final boolean dbg = false;

    //**********************************************************
    public static LocalDateTime get_date(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (Check_remaining_RAM.RAM_running_low(logger)) {
            logger.log("get_date NOT DONE because running low on memory ! ");
            return LocalDateTime.now();
        }

        InputStream is = Full_image_from_disk.get_image_InputStream(path, Feature_cache.get(Feature.Fusk_is_on), true, aborter, logger);
        if ( is == null)
        {
            return LocalDateTime.now();
        }

        try
        {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            for (Directory directory : metadata.getDirectories())
            {
                for (Tag tag : directory.getTags())
                {
                    if (tag.toString().contains("Date/Time"))
                    {
                        String s = tag.toString().substring(tag.toString().indexOf("-")+1);
                        s = s.trim();
                        try {
                            LocalDateTime x = LocalDateTime.parse(s);
                            logger.log(path+" date is :"+x.toString());
                            return x;
                        }
                        catch (DateTimeParseException e)
                        {
                            logger.log("WARNING cannot parse this date string? ->"+s+"<-");

                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd hh:mm:ss");
                            LocalDateTime dateTime = LocalDateTime.parse(s, formatter);
                            return dateTime;
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
                return LocalDateTime.now();
            }
        }
        catch (IOException e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (4)->"+e+"<- for:"+ path.toAbsolutePath()));
            return LocalDateTime.now();
        }
        catch (Exception e)
        {
            if ( dbg) logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (5)->"+e+"<- for:"+ path.toAbsolutePath()));
            return LocalDateTime.now();
        }

        BasicFileAttributes attr;
        try
        {
            attr = Files.readAttributes(path, BasicFileAttributes.class);
            String s = attr.creationTime().toString();
            try {
                LocalDateTime x = LocalDateTime.parse(s);
            }
            catch(DateTimeParseException e)
            {
                logger.log("WARNING cannot parse this date string? ->"+s+"<-");
            }
        }
        catch (IOException e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (1)->"+e+"<- for:"+ path.toAbsolutePath()));
            }
        }
        return LocalDateTime.now();
    }

}
