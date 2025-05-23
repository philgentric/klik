package klik.util.execute;

import klik.util.log.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Execute_command
//**********************************************************
{

    public static final String GOING_TO_SHOOT_THIS = "going TO SHOOT THIS: ->";
    public static final String EXECUTE_COMMAND_END_OF_WAIT_OK = "Execute command: end of wait OK";
    public static final String IN_WORKING_DIR = "in working dir";

    //**********************************************************
    public static String execute_command_list(List<String> command_tokens, File wd, int max_ms_wait_time, StringBuilder to_be_returned, Logger logger)
    //**********************************************************
    {
        StringBuilder received_line = new StringBuilder();
        for ( String s : command_tokens)
        {
            received_line.append(s).append(" ");
        }
        if ( to_be_returned != null) to_be_returned.append(GOING_TO_SHOOT_THIS).append(received_line).append("<-\n" + IN_WORKING_DIR + ":").append(wd.getAbsolutePath()).append("\n");

        String output = "";
        ProcessBuilder process_builder = new ProcessBuilder(command_tokens);
        process_builder.directory(wd);
        process_builder.redirectErrorStream(true);
        Process p;
        try
        {
            p = process_builder.start();
        }
        catch (Exception e1)
        {
            if ( to_be_returned != null)
            {
                to_be_returned.append("EXEC error: ").append(e1).append("\n");
                logger.log(to_be_returned.toString());
            }
            else
            {
                logger.log("EXEC error: " + e1 + "\n");
            }
            return null;
        }

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String message;
        try
        {
            while ((message = stdInput.readLine()) != null)
            {
                output += message + "\n";
                if ( to_be_returned != null)
                {
                    to_be_returned.append(message);
                    to_be_returned.append("\n");
                }
            }
        }
        catch (IOException e)
        {
            if ( to_be_returned != null)
            {
                to_be_returned.append("could not read from process: ").append(e).append("\n");
                logger.log(to_be_returned.toString());
            }
            else
            {
                logger.log_stack_trace(e.toString());
            }
            return null;
        }
        //System.out.println("going to wait");
        try
        {
            p.waitFor(max_ms_wait_time, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            if ( to_be_returned != null)
            {
                to_be_returned.append("could not wait for  process: ").append(e).append("\n");
                logger.log_stack_trace(to_be_returned.toString());
            }
            else
            {
                logger.log_stack_trace(e.toString());
            }
            return null;
        }
        if ( to_be_returned != null)
        {
            to_be_returned.append(EXECUTE_COMMAND_END_OF_WAIT_OK);
        }

        return output;
    }


    //**********************************************************
    public static boolean execute_command_list_no_wait(List<String> command_tokens, File wd, int max_ms_wait_time, StringBuilder to_be_returned, Logger logger)
    //**********************************************************
    {
        StringBuilder received_line = new StringBuilder();
        for ( String s : command_tokens)
        {
            received_line.append(s).append(" ");
        }
        if ( to_be_returned != null) to_be_returned.append(GOING_TO_SHOOT_THIS).append(received_line).append("<-\n" + IN_WORKING_DIR + ":").append(wd.getAbsolutePath()).append("\n");

        ProcessBuilder process_builder = new ProcessBuilder(command_tokens);
        process_builder.directory(wd);
        process_builder.redirectErrorStream(true);
        process_builder.inheritIO(); // so that when the new process prints, it goes to the caller console
        Process p;
        try
        {
            p = process_builder.start();
        }
        catch (Exception e1)
        {
            if ( to_be_returned != null)
            {
                to_be_returned.append("EXEC error: ").append(e1).append("\n");
                logger.log(to_be_returned.toString());
            }
            else
            {
                logger.log("EXEC error: " + e1 + "\n");
            }
            return false;
        }

        System.out.println("execute_command_list_no_wait done!");

        return true;
    }

}
