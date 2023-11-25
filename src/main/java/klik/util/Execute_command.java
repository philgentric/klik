package klik.util;

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

    //**********************************************************
    public static boolean execute_command_list(List<String> command_tokens, File wd, int max_ms_wait_time, StringBuilder to_be_returned, Logger logger)
    //**********************************************************
    {

        StringBuilder received_line = new StringBuilder();
        for ( String s : command_tokens)
        {
            received_line.append(s).append(" ");
        }
        if ( to_be_returned != null) to_be_returned.append("going TO SHOOT THIS: ->").append(received_line).append("<-\nin working dir:").append(wd.getAbsolutePath()).append("\n");

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
            return false;
        }

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String message;
        try
        {
            while ((message = stdInput.readLine()) != null)
            {
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
                logger.log(e.toString());
            }
            return false;
        }
        try
        {
            p.waitFor(max_ms_wait_time, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            if ( to_be_returned != null)
            {
                to_be_returned.append("could not wait for  process: ").append(e).append("\n");
                logger.log(to_be_returned.toString());
            }
            else
            {
                logger.log(e.toString());
            }
            return false;
        }
        if ( to_be_returned != null)
        {
            to_be_returned.append("Execute command: end of wait OK");
            logger.log(to_be_returned.toString());
        }
        return true;
    }

}
