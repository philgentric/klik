// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.experimental.backup;

import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

//**********************************************************
public class File_comparator_NIO
//**********************************************************
{
    private Aborter aborter;
    private static final boolean debug_flag = false;
    ByteBuffer buf1 = ByteBuffer.allocateDirect(64000);
    ByteBuffer buf2 = ByteBuffer.allocateDirect(64000);


    Logger logger;

    //**********************************************************
    File_comparator_NIO(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }
    //**********************************************************
    public void abort()
    //**********************************************************
    {
        aborter.abort("File comparator NIO");
    }

    //**********************************************************
    private boolean files_are_same_NIO(File file_to_be_copied, File destination_file)
    //**********************************************************
    {
        long ori_s = file_to_be_copied.length();
        long des_s = destination_file.length();
        if (ori_s != des_s) {
            if (debug_flag)
            {
                logger.log("sizes differ " + file_to_be_copied.getName() + " " + ori_s + " vs " + des_s);
            }
            return false;
        }

        // same length, let us check content, block per block
        // at the first sign of a difference, return false
        boolean returned = true;
        FileChannel ch1 = null;
        FileChannel ch2 = null;
        try {
            RandomAccessFile src1 = new RandomAccessFile(file_to_be_copied, "r");
            ch1 = src1.getChannel();
            RandomAccessFile src2 = new RandomAccessFile(destination_file, "r");
            ch2 = src2.getChannel();

            for (; ;) {
                if ( aborter.should_abort()) return false;
                int bytesRead1 = ch1.read(buf1);
                int bytesRead2 = ch2.read(buf2);
                if (bytesRead1 <= 0) {
                    break;
                }

                if (buf1.mismatch(buf2) != -1) {
                    if (debug_flag == true) logger.log("content differ");
                    returned = false;
                    break;
                }
            }
            src1.close();
            ch1.close();
            src2.close();
            ch2.close();

        } catch (Exception ioe) {
            logger.log(ioe.toString());
            returned = false;
        }
        return returned;
    }

}
