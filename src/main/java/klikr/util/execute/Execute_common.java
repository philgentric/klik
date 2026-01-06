package klikr.util.execute;

import javafx.stage.Window;
import klikr.Klikr_application;
import klikr.properties.Non_booleans_properties;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

//**********************************************************
public class Execute_common
//**********************************************************
{
    /**
     *  several different cases must be considered
     *  1. from source : used to return src/main/resources/scripts
     *  2. from source, after compiling, will now return:
     *  <wherever is the repo>/build/resources/main/scripts
     *  3. from the jar after a DMG installer installed the app on macOS
     *  4. from the jar after a MSI installer installed the app on Windows
     */

    //**********************************************************
    public static Execution_context get_context(String cmd, Window owner, Logger logger)
    //**********************************************************
    {
        Path folder = get_scripts_folder(logger);
        if ( folder != null) {
            return new Execution_context(false,folder, cmd);
        }

        // oho, we are executing from an installer
        // we need to read the target script FROM THE JAR
        // and create a tmp file

        Path copy = create_copy_in_trash(cmd,owner,logger);
        if ( copy == null) return null;

        logger.log("for:"+cmd+", extracted content from jar and created copy in trash");
        //Files.setPosixFilePermissions(tmp_path, PosixFilePermissions.fromString("rwxr-xr-x"));

        return new Execution_context(true,copy.getParent(),copy.getFileName().toString());

    }

    //**********************************************************
    public static Path create_copy_in_trash(String cmd,Window owner, Logger logger)
    //**********************************************************
    {
        String name = "/scripts/"+cmd;
        InputStream input_stream =  Application_jar.get_jar_InputStream_by_name(name);
        if ( input_stream == null)
        {
            logger.log("❌ Fatal, can open jar stream for: ->"+name+"<-");
            return null;
        }
        // create a temporary copy of this file:
        Path tmp_path = get_path_in_trash(cmd,owner, logger);
        if ( tmp_path == null) return null;

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
    private static Path get_scripts_folder(Logger logger)
    //**********************************************************
    {
        // For JAR launched with `java -jar`, the location is the JAR itself
        // For native executables (macOS `.app`, Windows `.exe`, Linux binary),
        // this points to the folder that contains the launcher.

        Class<Klikr_application> klas = Klikr_application.class;
        logger.log("class is:"+klas);
        URL url = klas.getClassLoader().getResource("scripts/");

        if ( url == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("PANIC "));
            return null;
        }
        logger.log("Execute_common, url is:"+url);

        String protocol = url.getProtocol();
        logger.log("Execute_common, protocol is:"+protocol);

        if (protocol.equals("jar"))
        {
            // we are executing from an installer
            return null;
        }

        boolean from_source = false;
        if (protocol.equals("file"))
        {
            // we are executing from SOURCE
            from_source = true;
        }
        logger.log("Execute_common, executing from source");


        URI uri = null;
        try
        {
            uri = url.toURI();
        }
        catch( URISyntaxException e)
        {
            logger.log(""+e);
            return null;
        }
        logger.log("uri is:"+uri);

        Path path = Paths.get(uri);
        logger.log("path is:"+path);

        if ( from_source) return path;

        // If the path is a directory (e.g. when launched from an IDE),
        // just use it as is.  If it is a file (JAR or executable),
        // use its parent directory.
        if (Files.isRegularFile(path))
        {
            return path.getParent();
        }
        else
        {
            return path;
        }
    }

    //**********************************************************
    public static Path get_tmp_file_path_in_trash(String prefix, String extension,Window owner, Logger logger)
    //**********************************************************
    {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String uuid = LocalDateTime.now().format(dtf)+"_"+UUID.randomUUID();
        return get_path_in_trash(prefix+"_"+uuid+"."+extension,owner,logger);
    }

    //**********************************************************
    public static Path get_path_in_trash(String file_name,Window owner, Logger logger)
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


    //**********************************************************
    public static boolean make_executable(Path p, Logger logger)
    //**********************************************************
    {
        try {
            Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwxr-xr-x"));
            return true;
        } catch (IOException e) {
            logger.log("make_executable FAILED: "+e);
            return false;
        }
    }
}
