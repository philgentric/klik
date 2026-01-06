// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute;

import javafx.application.Platform;
import javafx.stage.Window;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;
import klikr.util.ui.Text_frame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
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
    public static void execute(List<String> the_commands,  boolean show_window, boolean in_a_thread, Window owner, Logger logger)
    //**********************************************************
    {
        LinkedBlockingQueue<String> output_queue;
        if ( show_window)
        {
            if ( dbg)
                logger.log(("Showing output window for command: "+the_commands));
            output_queue = new LinkedBlockingQueue<>();
            StringBuilder the_command = new StringBuilder();
            for ( String s : the_commands) the_command.append(s).append("\n");
            Platform.runLater(()-> Text_frame.show(the_command.toString(), output_queue,logger));
        }
        else
        {
            output_queue = null;
        }

        Runnable r = () -> execute_internal(the_commands,show_window,output_queue,owner,logger);
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
    private static void execute_internal(List<String> commands, boolean show_window, BlockingQueue<String> output_queue, Window owner, Logger logger)
    //**********************************************************
    {

        logger.log("Requesting elevated execution: " + commands);

        /* ------------------------------------------------------------------ */
        /* 1️⃣  Build a *PowerShell* one‑liner that starts an elevated CMD   */
        /* ------------------------------------------------------------------ */

        // The whole string that PowerShell sees:
        //   Start-Process cmd.exe -Verb RunAs -ArgumentList '/c "choco.exe …"' -Wait
        // We wrap the whole argument list in single quotes so that inner quotes
        // survive the PowerShell parser.  Inside the quoted argument list we
        // double‑quote the *actual* command because cmd.exe expects a single
        // quoted argument when /c is used.

        // 1. Build the PowerShell script as a single string.
        StringBuilder ps = new StringBuilder();

        ps.append("foreach ($cmd in $commands) { ");
        ps.append("    Start-Process -FilePath 'cmd.exe' ");
        ps.append("    -ArgumentList '/c', $cmd ");
        ps.append("    -Verb RunAs ");
        ps.append("    -Wait ");
        ps.append("}");

        // 2. Prepare the array of commands (you already have this logic).
        StringBuilder commandsArr = new StringBuilder("@(");
        for (int i = 0; i < commands.size(); i++) {
            // Escape single quotes inside each command.
            commandsArr.append('\'')
                    .append(commands.get(i).replace("'", "''"))
                    .append('\'');
            if (i < commands.size() - 1) {
                commandsArr.append(", ");
            }
        }
        commandsArr.append(')');

        // 3. Combine everything into the final script.
        String script = commandsArr.toString() + "\n" + ps.toString();

        // 4. Build the ProcessBuilder arguments.
        List<String> psArgs = List.of(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-Command", script
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
                    Popups.info_popup("✅  '" + commands + "' finished", null, owner, logger));
        }
    }

}