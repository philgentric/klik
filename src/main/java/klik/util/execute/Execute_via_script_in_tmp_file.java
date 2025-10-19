package klik.util.execute;

import javafx.application.Platform;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Popups;
import klik.util.ui.Text_frame;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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

    private static final boolean dbg = true;
    public static final String END = "__END_OF_PROCESS_OUTPUT__";

    //**********************************************************
    public static void execute(String the_command,  boolean show_window, Window owner, Logger logger)
    //**********************************************************
    {
        LinkedBlockingQueue<String> output_queue;
        if ( show_window)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Showing output window for command: "+the_command));
            output_queue = new LinkedBlockingQueue<>();
            Platform.runLater(()-> Text_frame.show(the_command, output_queue,logger));
        }
        else
        {
            output_queue = null;
        }
        Runnable r = () -> execute_internal(the_command,show_window,output_queue,owner,logger);
        Actor_engine.execute(r,"Execute script",logger);

    }
    //**********************************************************
    private static void execute_internal(String the_command, boolean show_window, BlockingQueue<String> output_queue, Window owner, Logger logger)
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
            if ( dbg) logger.log("Going to execute:->"+the_command+"<-");
            Process process = pb.start();

            // Read output in a separate thread to prevent blocking
            // this is a bit of a too-careful, the output is redirected
            // so normally nothing will come out of here
            Runnable r =() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())))
                {
                    //logger.log("going to read Process output");
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        logger.log("Process output: " + line);
                        if ( output_queue != null) output_queue.add(line);
                    }
                    //logger.log("End of Process output");

                } catch (IOException e) {
                    logger.log("Error reading process output: " + e);
                }
            };
            Actor_engine.execute(r,"Monitor execution process",logger);

            Aborter aborter = new Aborter("Execute_via_script_in_tmp_file tailer", logger);

            // use a Tail-er to read the log file as it is written
            TailerListener listener = new TailerListener()
            {
                Tailer tailer;
                long last = -1;
                @Override
                public void init(Tailer tailer)
                {
                    this.tailer = tailer;
                    Runnable suicidor = () ->
                    {
                        for(;;)
                        {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                logger.log("Tailer monitor interrupted: " + e);
                                return;
                            }
                            if ( last < 0) continue;
                            if ( aborter.should_abort())
                            {
                                if ( dbg) logger.log("TailerListener: abort detected, stopping tailer");
                                tailer.stop();
                                if ( output_queue != null) output_queue.add(END);
                                if ( show_window) Platform.runLater(() ->Popups.info_popup(the_command+" finished",null,owner,logger));
                                return;
                            }
                            if ( System.currentTimeMillis()-last> 600_000)
                            {
                                if ( dbg) logger.log("TailerListener: stopping tailer");
                                tailer.stop();
                                if ( output_queue != null) output_queue.add(END);
                                aborter.abort("timeout");
                                return;
                            }
                        }
                    };
                    Actor_engine.execute(suicidor,"Monitor Tailer for end of script",logger);

                }

                @Override
                public void fileNotFound() {
                    if ( dbg) logger.log("Log file not found: " + log_file_name);
                }

                @Override
                public void fileRotated() {
                }

                @Override
                public void handle(String line) {
                    if ( output_queue != null) output_queue.add(line);
                    last = System.currentTimeMillis();
                }

                @Override
                public void handle(Exception ex) {
                    logger.log("Error tailing log file: " + ex);
                }
            };

            Tailer tailer = new Tailer(Path.of(log_file_name).toFile(),listener);

            Actor_engine.execute(tailer,"Run tailer for execution log",logger);

            try {
                if (process.waitFor(10, TimeUnit.MINUTES))
                {
                    int exitValue = process.exitValue();
                    if ( dbg) logger.log("Process exited with value: " + exitValue);
                    aborter.abort("process ended");
                }

            } catch (InterruptedException e) {
                logger.log("Process wait interrupted: " + e);
            }
        } catch (IOException e) {
            logger.log(""+e);
        }
    }

}
