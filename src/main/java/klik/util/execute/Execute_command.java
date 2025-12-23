// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.execute;

import klik.util.log.Logger;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Execute_command
//**********************************************************
{
    public static final String WORKING_DIR_DOES_NOT_EXIST = "WORKING DIR DOES NOT EXIST";
    public static final String GOING_TO_SHOOT_THIS = "going TO SHOOT THIS: ->";
    public static final String EXECUTE_COMMAND_END_OF_WAIT_OK = "Execute command: end of wait OK";
    public static final String IN_WORKING_DIR = "in working dir";

    //**********************************************************
    public static Execute_result execute_command_list(List<String> command_tokens, File wd, int max_ms_wait_time, StringBuilder string_builder, Logger logger)
    //**********************************************************
    {
        StringBuilder received_line = new StringBuilder();
        for ( String s : command_tokens)
        {
            received_line.append(s).append(" ");
        }
        if ( string_builder != null) string_builder.append(GOING_TO_SHOOT_THIS).append(received_line).append("<-\n" + IN_WORKING_DIR + ":").append(wd.getAbsolutePath()).append("\n");

        String output = "";
        ProcessBuilder process_builder = new ProcessBuilder(command_tokens);
        process_builder.directory(wd);
        process_builder.redirectErrorStream(true);
        Process p;
        try
        {
            p = process_builder.start();
        }
        catch (IOException e1)
        {
            if ( e1.toString().contains("No such file or directory"))
            {
                if ( !wd.exists())
                {
                    return new Execute_result(false,WORKING_DIR_DOES_NOT_EXIST);
                }
            }
            return new Execute_result(false,"process_builder.start() failed"+ e1);
        }
        catch (Exception e1)
        {
            if ( string_builder != null)
            {
                string_builder.append("EXEC error: ").append(e1).append("\n");
                logger.log(string_builder.toString());
            }
            else
            {
                logger.log("EXEC error: " + e1 + "\n");
            }
            return new Execute_result(false,"process_builder.start() failed"+ e1);
        }

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String message;
        try
        {
            while ((message = stdInput.readLine()) != null)
            {
                output += message + "\n";
                if ( string_builder != null)
                {
                    string_builder.append(message);
                    string_builder.append("\n");
                }
            }
        }
        catch (IOException e)
        {
            if ( string_builder != null)
            {
                string_builder.append("could not read from process: ").append(e).append("\n");
                logger.log(string_builder.toString());
            }
            else
            {
                logger.log_stack_trace(e.toString());
            }
            return new Execute_result(false,"process output stream filaed: "+ e);
        }
        //System.out.println("going to wait");
        try
        {
            p.waitFor(max_ms_wait_time, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            if ( string_builder != null)
            {
                string_builder.append("could not wait for  process: ").append(e).append("\n");
                logger.log_stack_trace(string_builder.toString());
            }
            else
            {
                logger.log_stack_trace(e.toString());
            }
            return new Execute_result(false,"process interrupted: "+ e);
        }
        if ( string_builder != null)
        {
            string_builder.append(EXECUTE_COMMAND_END_OF_WAIT_OK);
        }

        return new Execute_result(true,output);
    }


    //**********************************************************
    public static Execute_result execute_command_list_no_wait(List<String> command_tokens, File wd, Logger logger)
    //**********************************************************
    {
        StringBuilder received_line = new StringBuilder();
        for ( String s : command_tokens)
        {
            received_line.append(s).append(" ");
        }
        if ( logger != null) logger.log(GOING_TO_SHOOT_THIS+" "+received_line+" "+ IN_WORKING_DIR + ":"+" "+wd.getAbsolutePath());

        ProcessBuilder process_builder = new ProcessBuilder(command_tokens);
        process_builder.directory(wd);
        process_builder.redirectErrorStream(true);
        process_builder.inheritIO(); // so that when the new process prints, it goes to the caller console
        Process p;
        try
        {
            p = process_builder.start();
        }
        catch (IOException e1)
        {
            if ( e1.toString().contains("No such file or directory"))
            {
                if ( !wd.exists())
                {
                    return new Execute_result(false,WORKING_DIR_DOES_NOT_EXIST);
                }
            }
            return new Execute_result(false,"process_builder.start() failed"+ e1);
        }
        catch (Exception e1)
        {
            if ( logger!= null) logger.log("EXEC error: " + e1);
            return new Execute_result(false,"process_builder.start() failed"+ e1);
        }

        if ( logger!= null) logger.log("execute_command_list_no_wait done!");

        return new Execute_result(true,"execute_command_list_no_wait done");
    }

}
