// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute;

import javafx.application.Platform;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;
import klikr.util.ui.Text_frame;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


//**********************************************************
public class Nix_execute_via_script_in_tmp_file
//**********************************************************
{
    // this is a workaround for problems when using ProcessBuilder
    // to execute commands that require a specific shell environment,
    // such as python in virtual environments or other complex commands.
    // THIS creates a temporary script file in the klikr trash directory,
    // writes the command to it, and then executes that script.
    // Logs are also created in the klikr trash directory.

    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    public static final String END = "__END_OF_PROCESS_OUTPUT__";

    //**********************************************************
    public static void execute(Path folder, String the_command, boolean chmod_it,  List<String> args, boolean show_window, boolean in_a_thread, Window owner, Logger logger)
    //**********************************************************
    {
        LinkedBlockingQueue<String> output_queue;
        if ( show_window)
        {
            if ( dbg)
                logger.log(("Showing output window for command: "+the_command));
            output_queue = new LinkedBlockingQueue<>();
            Platform.runLater(()-> Text_frame.show(the_command, output_queue,logger));
        }
        else
        {
            output_queue = null;
        }

        Runnable r = () -> execute_internal(folder,the_command,chmod_it, args,show_window,output_queue,owner,logger);
        if (in_a_thread)
        {
            Actor_engine.execute(r, "Execute_via_script_in_tmp_file in a thread", logger);
        }
        else
        {
            r.run();
        }

    }

    //**********************************************************
    private static void execute_internal(Path folder,String command, boolean chmod_it, List<String> args, boolean show_window, BlockingQueue<String> output_queue, Window owner, Logger logger)
    //**********************************************************
    {
        execute_internal_nix(folder,command,chmod_it, args,show_window,output_queue,owner,logger);
    }

    //**********************************************************
    private static void execute_internal_nix(Path folder, String command, boolean chmod_it, List<String> args, boolean show_window, BlockingQueue<String> output_queue, Window owner, Logger logger)
    //**********************************************************
    {
        Path p_for_log = Execute_common.get_tmp_file_path_in_trash("log","txt",owner, logger);
        if ( p_for_log == null) return;
        String log_file_name = p_for_log.toString();

        StringBuilder args_string = new StringBuilder();
        for ( String s : args) args_string.append(" ").append(s);
        String script_content = "#!/usr/bin/env bash\n";
        script_content += "cd "+folder+"\n";
        if ( chmod_it) script_content += "chmod +x "+command+"\n";
        script_content += "./"+command + args_string+ " > "+log_file_name+" 2>&1";
        logger.log("Going to execute (Nix) ->" + command+ args_string+"<-\nvia script in tmp file, logs in : "+log_file_name);

        // Write script to temporary file
        Path script_path;
        try {
            script_path = Execute_common.get_tmp_file_path_in_trash("cmd_","sh",owner, logger);
            //if ( Files.exists(script_path)) Files.delete(script_path);
            Files.write(script_path, script_content.getBytes());
            Files.setPosixFilePermissions(script_path, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
        catch (UnsupportedOperationException e) {
            logger.log("FATAL ! don't use NIX command on WINDOWS: " + e);
            return;
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
            logger.log("Working directory: " + pb.directory()+"\nCommand: " + String.join(" ", pb.command()));

            // Add environment variables debugging
            /*Map<String, String> env = pb.environment();
            logger.log("Environment variables:");
            env.forEach((k, v) -> logger.log(k + "=" + v));
            */
        }
        pb.inheritIO();
        try {
            if ( dbg) logger.log("Executing:->"+command+"<-");
            Process the_command_process = pb.start();

            // Read output in a separate thread to prevent blocking
            // this is a bit of a too-careful, the output is redirected
            // so normally nothing will come out of here
            Runnable r =() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(the_command_process.getInputStream())))
                {
                    //logger.log("going to read Process output");
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        logger.log("Process output: " + line);
                        if ( output_queue != null) output_queue.add(line);
                    }
                    //logger.log("End of Process output");

                }
                catch (IOException e)
                {
                    logger.log("Error reading process output: " + e);
                }
            };
            Actor_engine.execute(r,"Monitor execution process",logger);

            Aborter aborter_local = new Aborter("Execute_via_script_in_tmp_file tailer", logger);

            // use a Tail-er to read the log file as it is written
            TailerListener listener = new TailerListener()
            {
                Tailer tailer;
                long last = -1;
                @Override
                public void init(Tailer tailer)
                {
                    if (ultra_dbg) logger.log("TailerListener INIT "+tailer.toString());
                    this.tailer = tailer;
                    Runnable suicidor = () ->
                    {
                        for(;;)
                        {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                if (dbg) logger.log("TailerListener monitor interrupted: " + e);
                                return;
                            }
                            if ( last < 0) continue;
                            if ( aborter_local.should_abort())
                            {
                                if ( dbg) logger.log("TailerListener: abort detected, stopping tailer");
                                tailer.stop();
                                if ( output_queue != null) output_queue.add(END);
                                if ( show_window) Platform.runLater(() ->Popups.info_popup("'"+command+"' ➡\uFE0F done",null,owner,logger));
                                return;
                            }
                            if ( System.currentTimeMillis()-last> 600_000)
                            {
                                if ( dbg) logger.log("TailerListener: stopping tailer on timeout");
                                tailer.stop();
                                if ( output_queue != null) output_queue.add(END);
                                aborter_local.abort("TailerListener timeout");
                                return;
                            }
                        }
                    };
                    Actor_engine.execute(suicidor,"Monitor Tailer for end of script",logger);

                }

                @Override
                public void fileNotFound() {
                    logger.log("❗Warning: TailerListener Log file not found: " + log_file_name);
                }

                @Override
                public void fileRotated() {
                    if ( dbg) logger.log("❗Warning: TailerListener Log file ROTATED: " + log_file_name);
                }

                @Override
                public void handle(String line) {
                    if (ultra_dbg) logger.log("TailerListener, adding output :"+line);
                    if ( output_queue != null) output_queue.add(line);
                    last = System.currentTimeMillis();
                }

                @Override
                public void handle(Exception ex) {
                    logger.log("❌ WARNING: TailerListener, Error tailing log file: " + ex);
                }
            };

            Tailer tailer = new Tailer(Path.of(log_file_name).toFile(),listener);

            Actor_engine.execute(tailer,"Run tailer for execution log",logger);

            try {
                if (the_command_process.waitFor(10, TimeUnit.MINUTES))
                {
                    int exitValue = the_command_process.exitValue();

                    if ( exitValue != 0)
                    {
                        String msg = "❗Warning: Process ->"+command+"<- exited with value: " + exitValue;
                        logger.log(msg);
                        if ( output_queue != null) output_queue.add(msg+"\n");

                    }
                    else if ( ultra_dbg)
                    {
                        logger.log("Process exited with value: " + exitValue);
                    }
                    // wait a bit before aborting the tailer or we might miss the output

                    Actor_engine.execute(()->{
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            logger.log(""+e);
                        }
                        aborter_local.abort("process ended");
                    },"wait a bit before aborting tailer",logger);

                }

            } catch (InterruptedException e) {
                logger.log("Process wait interrupted: " + e);
            }
        } catch (IOException e) {
            logger.log(""+e);
        }
    }

}
