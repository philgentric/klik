package klik.level2.backup;

import klik.actor.Aborter;
import klik.util.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

/*
this is NOT re-entrant : you need one instance per thread
 */
//**********************************************************
public class File_comparator
//**********************************************************
{
    private static final int BUFFER_SIZE = 1_000_000;//8192;//4096;//1024;

    private Aborter aborter;
    private static final boolean debug_flag = false;
    private final byte[] hashsrc = new byte[BUFFER_SIZE];
    private final byte[] hashdest = new byte[BUFFER_SIZE];

    Logger logger;

    //**********************************************************
    File_comparator(Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        if ( aborter_ == null)
        {
            logger.log_stack_trace("FATAL aborter must not be null");
        }
        aborter = aborter_;
        logger = logger_;
    }
    //**********************************************************
    public void abort()
    //**********************************************************
    {
        aborter.abort();
    }
    //**********************************************************
    public  Similarity_result files_are_same(File file_to_be_copied, File destination_file, long[] bytes_read)
    //**********************************************************
    {
       long ori_s = file_to_be_copied.length();
        long des_s = destination_file.length();
        if (ori_s != des_s) {
            if (debug_flag) {
                logger.log("sizes differ " + file_to_be_copied.getName() + " " + ori_s + " vs " + des_s);
            }
            return Similarity_result.not_same;
        }

        // same size, let us check MD5, block per block
        // at the first sign of a difference, return false
        try (
                BufferedInputStream bis1 = new BufferedInputStream(new FileInputStream(file_to_be_copied));
                BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(destination_file))
        )
        {
            boolean quit = false;
            for (;;)
            {
                if (aborter.should_abort())
                {
                    logger.log("abort from files_are_same()");
                    return Similarity_result.aborted;
                }
                int available_src = bis1.available();
                int available_dest = bis2.available();
                if (available_src != available_dest)
                {
                    //if ( debug_flag == true)
                    {
                        logger.log("read sizes differ");
                    }
                    return Similarity_result.not_same;
                }
                if (available_src < BUFFER_SIZE)
                {
                    if ( bis1.read(hashsrc, 0, available_src) != available_src)
                    {
                        logger.log("WARNING reading from file "+file_to_be_copied+"failed ");
                        return Similarity_result.not_same;
                    }
                    if ( bis2.read(hashdest, 0, available_dest) != available_dest)
                    {
                        logger.log("WARNING reading from file "+destination_file+"failed ");
                        return Similarity_result.not_same;
                    }
                    bytes_read[0]+= 2*available_src;
                    quit = true;
                } else {
                    if ( bis1.read(hashsrc, 0, BUFFER_SIZE) != BUFFER_SIZE)
                    {
                        logger.log("WARNING reading from file "+file_to_be_copied+"failed ");
                        return Similarity_result.not_same;
                    }
                    if ( bis2.read(hashdest, 0, BUFFER_SIZE) != BUFFER_SIZE)
                    {
                        logger.log("WARNING reading from file "+destination_file+"failed ");
                        return Similarity_result.not_same;
                    }
                    bytes_read[0]+= 2*BUFFER_SIZE;
                }

                //if (!Arrays.equals(hashsrc, hashdest))
                if (Arrays.mismatch(hashsrc, hashdest) != -1)
                {
                    //if ( debug_flag == true)
                    logger.log("content differ");
                    return Similarity_result.not_same;
                }
                if (quit) break; // last block
            }
            return Similarity_result.same;
        } // end try
        catch (Exception ioe) {
            logger.log(ioe.toString());
            return Similarity_result.aborted;
        }

    }

}
