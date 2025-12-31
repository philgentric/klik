// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.execute;

import javafx.application.Platform;
import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.ui.Text_frame;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


//**********************************************************
public class Execute_windows_command
//**********************************************************
{

    private static final boolean dbg = true;
    public static final String END = "__END_OF_PROCESS_OUTPUT__";

    //**********************************************************
    public static void execute(String the_command,  boolean show_window, boolean in_a_thread, Window owner, Logger logger)
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

        Runnable r = () -> execute_internal(the_command,show_window,output_queue,owner,logger);
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
    private static void execute_internal(String command, boolean show_window, BlockingQueue<String> output_queue, Window owner, Logger logger)
    //**********************************************************
    {

        logger.log("Requesting elevated execution: " + command);

        /* ------------------------------------------------------------------ */
        /* 1️⃣  Build a *PowerShell* one‑liner that starts an elevated CMD   */
        /* ------------------------------------------------------------------ */

        // The whole string that PowerShell sees:
        //   Start-Process cmd.exe -Verb RunAs -ArgumentList '/c "choco.exe …"' -Wait
        // We wrap the whole argument list in single quotes so that inner quotes
        // survive the PowerShell parser.  Inside the quoted argument list we
        // double‑quote the *actual* command because cmd.exe expects a single
        // quoted argument when /c is used.
        String psCmd = String.format(
                "Start-Process cmd.exe -Verb RunAs -ArgumentList '/c \"%s\"' -Wait",
                command);

        List<String> psArgs = List.of(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-Command", psCmd
        );

        ProcessBuilder pb = new ProcessBuilder(psArgs);
        /* ------------------------------------------------------------------ */
        /* 2️⃣  Merge stdout+stderr so we don’t dead‑lock                      */
        /* ------------------------------------------------------------------ */
        pb.redirectErrorStream(true);

        /* ------------------------------------------------------------------ */
        /* 3️⃣  Add a *known* path element so that chocolatey is found       */
        /* ------------------------------------------------------------------ */
        Map<String, String> env = pb.environment();
        String programData = System.getenv("ProgramData");
        if (programData != null) {
            // On Win10+ chocolatey is installed in %ProgramData%\chocolatey\bin
            String chocoBin = Paths.get(programData, "chocolatey", "bin")
                    .toAbsolutePath()
                    .toString();
            // Prepend it – elevated shells inherit a *minimal* PATH
            String oldPath = env.getOrDefault("PATH", "");
            env.put("PATH", chocoBin + ";" + oldPath);
        }

        /* ------------------------------------------------------------------ */
        /* 4️⃣  Start the process                                               */
        /* ------------------------------------------------------------------ */
        Process proc;
        try {
            proc = pb.start();
        } catch (IOException e) {
            logger.log("Could not start elevated process: " + e);
            return;
        }

        /* ------------------------------------------------------------------ */
        /* 5️⃣  Capture output asynchronously                                 */
        /* ------------------------------------------------------------------ */

        Runnable r = () -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.log("[ELEVATED] " + line);
                    if ( output_queue != null) output_queue.add(line);
                }
            } catch (IOException ex) {
                logger.log("Error reading elevated process output: " + ex);
            }
        };
        Actor_engine.execute(r,"Windows command",logger);

        /* ------------------------------------------------------------------ */
        /* 6️⃣  Wait for completion (10min max)                              */
        /* ------------------------------------------------------------------ */
        int exitCode;
        try {
            boolean finished = proc.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                logger.log("Elevated command timed out – killing it");
                proc.destroyForcibly();
                return;
            }
            exitCode = proc.exitValue();
        } catch (InterruptedException e) {
            logger.log("Interrupted while waiting for elevated process");
            Thread.currentThread().interrupt();
            return;
        }

        /* ------------------------------------------------------------------ */
        /* 7️⃣  Handle special exit codes                                    */
        /* ------------------------------------------------------------------ */
        if (exitCode == 0) {
            logger.log("Elevated command succeeded (exit 0)");
        } else if (exitCode == 1223) {   // ERROR_CANCELLED
            logger.log("User cancelled the UAC prompt (exit 1223)");
            return;
        } else {
            logger.log("Elevated command failed (exit " + exitCode + ")");
        }

        /* ------------------------------------------------------------------ */
        /* 8️⃣  Optionally show a “done” popup                               */
        /* ------------------------------------------------------------------ */
        if (show_window) {
            Platform.runLater(() ->
                    Popups.info_popup("✅  '" + command + "' finished", null, owner, logger));
        }
    }

}