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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


//**********************************************************
public class Execute_Windows_ps1_script
//**********************************************************
{
    // this is Windows specific 'powershell' way to execute Windows scripts

    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    public static final String END = "__END_OF_PROCESS_OUTPUT__";

    //**********************************************************
    public static void execute(String description,Path ps1_script,  List<String> ports, boolean show_window, boolean in_a_thread, Window owner, Logger logger)
    //**********************************************************
    {
        LinkedBlockingQueue<String> output_queue;
        if ( show_window)
        {
            if ( dbg)
                logger.log(("Showing output window for command: "+description));
            output_queue = new LinkedBlockingQueue<>();
            Platform.runLater(()-> Text_frame.show(description, output_queue,logger));
        }
        else
        {
            output_queue = null;
        }

        Runnable r = () -> execute_internal(description,ps1_script,ports,show_window,output_queue,owner,logger);
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
    private static void execute_internal(
            String description,
            Path ps1_script,
            List<String> ports,
            boolean show_window,
            BlockingQueue<String> output_queue,
            Window owner,
            Logger logger)
    //**********************************************************
    {

        logger.log("Requesting elevated execution: " + description);



        List<String> cmd = new ArrayList<>();
        cmd.add("powershell.exe");
        cmd.add("-ExecutionPolicy");
        cmd.add("Bypass");
        cmd.add("-File");
        cmd.add(ps1_script.toAbsolutePath().toString());
        for (String p : ports)
        {
            cmd.add(p);
        }


        ProcessBuilder pb = new ProcessBuilder(cmd);
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
        Actor_engine.execute(r,"Windows ps1 script",logger);

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
                    Popups.info_popup("✅  '" + description + "' finished", null, owner, logger));
        }
    }

}
