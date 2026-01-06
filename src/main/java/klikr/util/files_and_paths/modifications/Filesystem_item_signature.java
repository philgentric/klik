// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.files_and_paths.modifications;

import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;

// stores the signature of a folder or a file
//**********************************************************
public class Filesystem_item_signature
//**********************************************************
{
    byte[] file_signature_array; // MD5 hash of file content, null if file is a folder
    String[] folder_signature_array; // list of files/folders, null if file is a file
    final static int internal_hash_computation_buffer_size_in_bytes = 1024;
    final Logger logger;

    //**********************************************************
    public Filesystem_item_signature(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }


    //**********************************************************
    public File_status init(Path path)
    //**********************************************************
    {
        if ( path.toFile().isDirectory())
        {
            file_signature_array = null;
            folder_signature_array = path.toFile().list();

            if ( folder_signature_array ==null)
            {
                logger.log("❌ FATAL: Filesystem_item_signature scanning failed for "+path);
                return File_status.FOLDER_NOT_FOUND;
            }
            // File.list() returns an OS dependent order, which is also user choice dependant
            // so the same folder may show a different order at different times
            // we must sort it to have a reproducible signature
            Arrays.sort(folder_signature_array);
        }
        else
        {
            folder_signature_array = null;
            Hash_and_status hash_and_status = get_file_hash(path, logger);
            if ( hash_and_status.status() != File_status.OK)
            {
                return hash_and_status.status();
            }
            file_signature_array = hash_and_status.hash();
            if ( file_signature_array ==null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("❌ SHOULD NOT HAPPEN: Filesystem_item_signature file_signature_array == null for "+path));
                return File_status.EXCEPTION;
            }
            if ( file_signature_array.length == 0)
            {
                logger.log(Stack_trace_getter.get_stack_trace("❌ SHOULD NOT HAPPEN: Filesystem_item_signature file_signature_array is empty for "+path));
                return File_status.EXCEPTION;
            }
        }
        return File_status.OK;
    }


    //**********************************************************
    public static Hash_and_status get_file_hash(Path path, Logger logger)
    //**********************************************************
    {
        byte[] hash = null;
        try {
            FileInputStream fis = new FileInputStream(path.toFile());
            byte[] buffer = new byte[internal_hash_computation_buffer_size_in_bytes];

            MessageDigest sha = MessageDigest.getInstance("MD5");
            //sha.reset();
            for (;;)
            {
                int available = fis.read(buffer, 0, internal_hash_computation_buffer_size_in_bytes);
                if ( available == -1) break;
                sha.update(buffer, 0, available);
            }
            fis.close();
            hash = sha.digest();
            //logger.log("\n the MD5 hash of " + path.toAbsolutePath() + " is " + new String(hash) + " " + sha.getDigestLength());

        } catch (FileNotFoundException e) {
            logger.log("Filesystem_item_signature get_file_hash() fails because of: " + e);
            return new Hash_and_status(new byte[0],File_status.FILE_NOT_FOUND);
        } catch (Exception e) {
            logger.log("Filesystem_item_signature get_file_hash() fails because of: " + e);
            return new Hash_and_status(new byte[0],File_status.EXCEPTION);
        }
        return new Hash_and_status(hash,File_status.OK);
    }


    //**********************************************************
    public boolean is_same(Filesystem_item_signature other)
    //**********************************************************
    {
        if ( file_signature_array == null)
        {
            // we are comparing folders
            if ( folder_signature_array == null)
            {
                logger.log("❌ FATAL, Filesystem_item_signature file_signature_array = null ");
                return true;
            }
            if ( folder_signature_array.length != other.folder_signature_array.length) return false;

            if ( Arrays.mismatch(folder_signature_array,other.folder_signature_array) != -1) return false;

            return true;
        }

        if (file_signature_array.length == 0) return false;
        if (other.file_signature_array.length != this.file_signature_array.length) return false;
        if ( Arrays.mismatch(file_signature_array,other.file_signature_array) != -1) return false;
        return true;
    }
}
