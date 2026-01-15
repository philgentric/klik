package klikr.util.log;

import javafx.stage.Window;
import klikr.properties.Non_booleans_properties;
import klikr.util.execute.Application_jar;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

//**********************************************************
public class Tmp_file_in_trash
//**********************************************************
{
    //**********************************************************
    public static Path create_copy_in_trash(String cmd,Window owner, Logger logger)
    //**********************************************************
    {
        Path tmp_path = get_path_in_trash(cmd,owner, logger);
        if ( tmp_path == null) return null;
        if (Files.exists(tmp_path)) return tmp_path;

        String name = "/scripts/"+cmd;
        InputStream input_stream =  Application_jar.get_jar_InputStream_by_name(name);
        if ( input_stream == null)
        {
            logger.log("❌ Fatal, can open jar stream for: ->"+name+"<-");
            return null;
        }
        // create a temporary copy of this file:

        try
        {
            FileUtils.copyInputStreamToFile(input_stream, tmp_path.toFile());
            return tmp_path;
        }
        catch (IOException e)
        {
            logger.log("" + e);
            return null;
        }
    }

    //**********************************************************
    public static Path get_tmp_file_path_in_trash(String prefix, String extension,Window owner, Logger logger)
    //**********************************************************
    {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String uuid = LocalDateTime.now().format(dtf)+"_"+ UUID.randomUUID();
        return get_path_in_trash(prefix+"_"+uuid+"."+extension,owner,logger);
    }

    //**********************************************************
    public static Path get_path_in_trash(String file_name, Window owner, Logger logger)
    //**********************************************************
    {
        Path klik_trash = Non_booleans_properties.get_trash_dir(Path.of("").toAbsolutePath(),owner,logger);
        if ( klik_trash == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ Fatal "));
            return null;
        }
        return klik_trash.resolve(file_name);
    }

}
