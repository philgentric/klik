package klikr.util.execute;


import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job;
import klikr.util.log.Logger;
import klikr.util.ui.Text_frame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

//**********************************************************
public class Script_executor
//**********************************************************
{

    private static final String MACOS_PYTHON = "/opt/homebrew/bin/python3.10";
    private static final String LINUX_PYTHON = "/usr/bin/python3";
    private static final String WINDOWS_PYTHON = "python.exe";

    //**********************************************************
    public static void execute(List<String> cmd,
                               Path tmp_folder,
                               //Path venv_path,
                               Window owner, Logger logger)
    //**********************************************************
    {
        Actor_engine.execute(()->execute_internal(cmd,tmp_folder,
                //venv_path,
                owner,logger),"Script_executor "+String.join(" ",cmd),logger);
    }


    //**********************************************************
    private static void execute_internal(List<String> cmds,
                                         Path tmp_folder,
                                         //Path venv_path,
                                         Window owner, Logger logger)
    //**********************************************************
    {
        //StringBuilder output_capture = new StringBuilder();
        AtomicReference<TextArea> text_area_ref = new AtomicReference<>();

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
            return;// "Error: Interrupted";
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
                /*
                if (venv_path != null)
                {
                    // Python case
                    Path activate_script = venv_path.resolve("Scripts").resolve("Activate.ps1");
                    script_content.add("& \"" + activate_script.toAbsolutePath().toString() + "\"");
                    // Join cmd for python
                    String python_cmd = WINDOWS_PYTHON + " " + String.join(" ", cmd);
                    script_content.add(python_cmd);
                }
                else*/
                {
                    // Simple case
                    script_content.append(String.join(" ", cmds));
                }
            }
            else
            {
                // Bash (Mac/Linux)
                script_content.append("#!/bin/bash\n");
                /*if (venv_path != null)
                {
                    // Python case
                    script_content.add("cd \"" + tmp_folder.toAbsolutePath().toString() + "\";\n");
                    Path activate_script = venv_path.resolve("bin").resolve("activate");
                    script_content.add("source \"" + activate_script.toAbsolutePath().toString() + "\";\n");

                    String python_bin =  LINUX_PYTHON;
                    if ( os == Operating_system.MacOS)
                    {
                        python_bin = MACOS_PYTHON;
                    }

                    String python_cmd = python_bin + " " + String.join(" ", cmd)+"\n";
                    script_content.add(python_cmd);
                }
                else*/
                {
                    // Simple case
                    for ( String s : cmds)
                    {
                        script_content.append(s).append("\n");
                    }
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
                queue.add("Executing in WINDOWS (powershell) ");
                pb = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", script_path.toAbsolutePath().toString());
            }
            else
            {
                queue.add("Executing in unix");
                pb = new ProcessBuilder(script_path.toAbsolutePath().toString());
            }

            pb.directory(tmp_folder.toFile());
            queue.add("Executing in folder: "+tmp_folder);
            pb.redirectErrorStream(true); // Merge stderr into stdout
            Path log_path = tmp_folder.resolve(script_name+".log");
            pb.redirectOutput(log_path.toFile());

            Process process = pb.start();

            Runnable r = () -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        //output_capture.append(line).append("\n");
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

        return;// output_capture.toString();
    }


}

