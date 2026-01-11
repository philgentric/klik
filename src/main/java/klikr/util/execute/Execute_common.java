package klikr.util.execute;

import javafx.stage.Window;
import klikr.Klikr_application;
import klikr.properties.Non_booleans_properties;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.log.Tmp_file_in_trash;
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

@Deprecated
public class Execute_common
//**********************************************************
{
    //**********************************************************
    @Deprecated
    public static Execution_context get_context(String cmd, Window owner, Logger logger)
    //**********************************************************
    {
        Path folder = Script_executor.get_scripts_folder(logger);
        if ( folder != null) {
            return new Execution_context(false,folder, cmd);
        }

        // oho, we are executing from an installer
        // we need to read the target script FROM THE JAR
        // and create a tmp file

        Path copy = Tmp_file_in_trash.create_copy_in_trash(cmd,owner,logger);
        if ( copy == null) return null;

        logger.log("for:"+cmd+", extracted content from jar and created copy in trash");
        //Files.setPosixFilePermissions(tmp_path, PosixFilePermissions.fromString("rwxr-xr-x"));

        return new Execution_context(true,copy.getParent(),copy.getFileName().toString());

    }




}
