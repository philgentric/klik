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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
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

        boolean enable_fusk = Static_application_properties.get_enable_fusk(logger);
        InputStream is = From_disk.get_image_InputStream(path, enable_fusk, true, aborter, logger);
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
