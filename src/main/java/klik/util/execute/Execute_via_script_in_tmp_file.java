package klik.util.execute;

import javafx.stage.Window;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


//**********************************************************
public class Execute_via_script_in_tmp_file
//**********************************************************
{
    // this is a workaround for problems when using ProcessBuilder
    // to execute commands that require a specific shell environment,
    // such as python in virtual environments or other complex commands.
    // It creates a temporary script file in the klik trash directory,
    // writes the command to it, and then executes that script.
    // Logs are also created in the klik trash directory.

    private static final boolean dbg = false;

    //**********************************************************
    public static void execute(String the_command, Window owner, Logger logger)
    //**********************************************************
    {

        String uuid = UUID.randomUUID().toString();
        Path klik_trash = Non_booleans_properties.get_trash_dir(Path.of("").toAbsolutePath(),owner,logger);
        String log_file_name = klik_trash.resolve("log_"+uuid+".log").toString();
        logger.log("Going to execute ->" + the_command+"<-\nvia script in tmp file, logs in : "+log_file_name);
        String script_content = "#!/bin/bash\n"
                + the_command + " > "+log_file_name+" 2>&1";

        // Write script to temporary file
        Path script_path;
        try {
            script_path = klik_trash.resolve("cmd_"+ uuid+".sh");
            //if ( Files.exists(script_path)) Files.delete(script_path);
            Files.write(script_path, script_content.getBytes());
            Files.setPosixFilePermissions(script_path, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
        catch (IOException e) {
            logger.log("Error with script file: " + e);
            return;
        }

        // execute the script
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", script_path.toString());
        pb.redirectErrorStream(true); // Merge stderr into stdout
        pb.redirectErrorStream(true); // Merge stderr into stdout

        if ( dbg)
        {
            logger.log("Working directory: " + pb.directory());
            logger.log("Command: " + String.join(" ", pb.command()));

            // Add environment variables debugging
            Map<String, String> env = pb.environment();
            logger.log("Environment variables:");
            env.forEach((k, v) -> logger.log(k + "=" + v));

        }
        pb.inheritIO();
        try {
            if ( dbg) logger.log("Going to execute:->"+pb.command()+"<-");
            Process process = pb.start();

            // Read output in a separate thread to prevent blocking
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.log("Process output: " + line);
                    }
                } catch (IOException e) {
                    logger.log("Error reading process output: " + e);
                }
            }).start();

            // Wait a bit to catch immediate failures
            try {
                if (process.waitFor(5, TimeUnit.SECONDS)) {
                    int exitValue = process.exitValue();
                    if ( dbg) logger.log("Process exited with value: " + exitValue);
                }
            } catch (InterruptedException e) {
                logger.log("Process wait interrupted: " + e);
            }
        } catch (IOException e) {
            logger.log(""+e);
        }
    }

}
