package klikr.util.execute;


import javafx.application.Platform;
import javafx.stage.Window;
import klikr.Klikr_application;
import klikr.properties.Non_booleans_properties;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Text_frame;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/* can execute commands (as a list of Strings)
for both windows and unix (macOS/Linux)
the trick is to make a temporary file 'script' (.ps1 in windows, .sh otherwise)
and execute it with ProcessBuilder
why a script ? because it creates a new 'shell' a.k.a. execution env
that does not have the limitations of the JVM env

the commands MUST be taylored for Windows or MacOS

the advantage is that the popup window, the stream capture,
the ProcessBuilder specifics are ONCE here
 */
//**********************************************************
public class Script_executor
//**********************************************************
{


    //**********************************************************
    public static void execute(List<String> cmd,
                               Path tmp_folder,
                               Window owner, Logger logger)
    //**********************************************************
    {
        Actor_engine.execute(()->execute_internal(cmd,tmp_folder, owner,logger),"Script_executor "+String.join(" ",cmd),logger);
    }


    //**********************************************************
    private static void execute_internal(List<String> cmds,
                                         Path tmp_folder,
                                         Window owner, Logger logger)
    //**********************************************************
    {

        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        CountDownLatch ui_latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Text_frame.show("Script_executor", queue, logger);
                logger.log("Script_executor window open");
            } finally {
                ui_latch.countDown();
            }
        });

        try {
            ui_latch.await();
        } catch (InterruptedException e) {
            logger.log("Interrupted while waiting for UI: " + e.getMessage());
            return;
        }


        Path script_path = null;
        try {
            Operating_system os = Guess_OS.guess(owner, logger);

            String script_name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "_" + UUID.randomUUID();
            if ( os == Operating_system.Windows)
            {
                script_name += ".ps1";
            }
            else
            {
                script_name += ".sh";
            }
            script_path = tmp_folder.resolve(script_name);

            StringBuilder script_content = new StringBuilder();

            if (os == Operating_system.Windows)
            {
                // PowerShell
                script_content.append(String.join(" ", cmds));
            }
            else
            {
                // Bash (Mac/Linux)
                script_content.append("#!/bin/bash\n");
                for ( String s : cmds)
                {
                    script_content.append(s).append("\n");
                }
            }

            Files.write(script_path, List.of(script_content.toString()));

            if (os != Operating_system.Windows)
            {
                try {
                    Set<PosixFilePermission> perms = new HashSet<>();
                    perms.add(PosixFilePermission.OWNER_READ);
                    perms.add(PosixFilePermission.OWNER_WRITE);
                    perms.add(PosixFilePermission.OWNER_EXECUTE);
                    Files.setPosixFilePermissions(script_path, perms);
                } catch (Exception e) {
                    // ignore if FS doesn't support POSIX
                    queue.add("Warning: Could not set executable permissions: " + e.getMessage());
                }
            }

            queue.add("Generated script: " + script_path);
            queue.add("Content:");
            for ( String s : cmds) queue.add(s);


            ProcessBuilder pb;
            if (os == Operating_system.Windows)
            {
                List<String> command = List.of(
                        "powershell.exe",
                        "-ExecutionPolicy", "Bypass",
                        "-File", script_path.toAbsolutePath().toString()
                );
                queue.add("Executing Windows script: "+String.join(" ",command));
                pb = new ProcessBuilder(command);
            }
            else
            {
                queue.add("Executing 'nix script: "+script_path.toAbsolutePath().toString());
                pb = new ProcessBuilder(script_path.toAbsolutePath().toString());
            }

            pb.directory(tmp_folder.toFile());
            queue.add("Executing in folder: "+tmp_folder);
            pb.redirectErrorStream(true); // Merge stderr into stdout
            Path log_path = tmp_folder.resolve(script_name+".log");
            pb.redirectOutput(log_path.toFile()); // this is super essential, without this, some commands may fail because they cannot stream out

            Process process = pb.start();

            Runnable r = () -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        queue.add(line + "\n");
                    }
                }
                catch (IOException e)
                {
                    queue.add("Error reading output: " + e.getMessage());
                }
            };

            Job job = Actor_engine.execute(r, "Script_executor stream capture", logger);

            int exit_code = process.waitFor();

            queue.add("Process exited with code: " + exit_code);
            job.cancel();

        } catch (Exception e)
        {
            String error_msg = "Exception: " + e.toString();
            queue.add(error_msg);
            for (StackTraceElement ste : e.getStackTrace()) {
                queue.add(ste.toString());
            }
        }
    }


    //**********************************************************
    static Path get_scripts_folder(Logger logger)
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

