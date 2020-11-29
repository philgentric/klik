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
    public static String execute_command_list(List<String> cmds, File wd, int max_ms_wait_time, Logger logger)
    //**********************************************************
    {
        StringBuilder to_be_returned = new StringBuilder(100);

        String received_line = "";
        for ( String s : cmds)
        {
            received_line += s +" ";
        }
        logger.log("going TO SHOOT THIS: ->"+ received_line+"<-");

        ProcessBuilder process_builder = new ProcessBuilder(cmds);
        process_builder.directory(wd);
        process_builder.redirectErrorStream(true);
        Process p;
        try
        {
            p = process_builder.start();
        }
        catch (Exception e1)
        {
            String err = "EXEC error: "+e1;
            logger.log(err);
            to_be_returned.append(err);
            return to_be_returned.toString();
        }

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String message = "";
        try
        {
            while ((message = stdInput.readLine()) != null)
            {
                logger.log(message);
                to_be_returned.append(message);
                to_be_returned.append("\n");
            }
        }
        catch (IOException e)
        {
            logger.log("could not read from process: "+e);
        }
        try
        {
            p.waitFor(max_ms_wait_time, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            logger.log("could not WAIT for process: "+e);
        }
        logger.log("end of wait ");
        return to_be_returned.toString();
    }

}
