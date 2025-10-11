package klik.images.decoding;

import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

//**********************************************************
public class Fast_date_from_OS
//**********************************************************
{
    public static final boolean dbg = false;

    //**********************************************************
    public static FileTime get_date(Path path, Logger logger)
    //**********************************************************
    {

        BasicFileAttributes attr;
        try
        {
            attr = Files.readAttributes(path, BasicFileAttributes.class);
            return attr.creationTime();
        }
        catch (IOException e)
        {
            if ( dbg)
            {
                logger.log(Stack_trace_getter.get_stack_trace("extract_exif_metadata() Managed exception (1)->"+e+"<- for:"+ path.toAbsolutePath()));
            }
        }
        return FileTime.from(Instant.now());

    }

}
