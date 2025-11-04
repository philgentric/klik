// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.backup;

import klik.util.log.Logger;

import java.io.*;
import java.nio.channels.FileChannel;

//**********************************************************
public class File_copier
//**********************************************************
{
    //**********************************************************
    public static long copy_file_NIO(File destfile, File srcfile) throws IOException
    //**********************************************************
    {
        long returned_bytes_copied =0;
        FileChannel inChannel = new FileInputStream(srcfile).getChannel();
        FileChannel outChannel = new FileOutputStream(destfile).getChannel();
        try
        {
            returned_bytes_copied = inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        catch (IOException e)
        {
            throw e;
        }
        finally
        {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
        return returned_bytes_copied;
    }
    //**********************************************************
    @Deprecated
    public static long copy_file(File destfile, File srcfile, Logger logger) throws IOException
    //**********************************************************
    {
        long returned_bytes_copied =0;
        byte[] bytearr = new byte[8192];
        int len = 0;
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(srcfile));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destfile));
        try
        {
            while ((len = bis.read(bytearr)) != -1)
            {
                bos.write(bytearr, 0, len);
                returned_bytes_copied += len;
            }
        }
        catch (Exception exc)
        {
            logger.log_stack_trace(exc.toString());
        }
        finally
        {
            bis.close();
            bos.close();
        }
        return returned_bytes_copied;
    }
}
