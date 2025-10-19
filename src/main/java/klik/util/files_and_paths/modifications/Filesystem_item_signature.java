package klik.util.files_and_paths.modifications;

import klik.util.log.Logger;

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
    byte[] file_signature_array; // MD5 hash of file content
    String[] folder_signature_array; // list of files/folders
    final static int internal_hash_computation_buffer_size_in_bytes = 1024;
    final Logger logger;

    //**********************************************************
    public Filesystem_item_signature(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }


    //**********************************************************
    public boolean init(Path path)
    //**********************************************************
    {
        if ( path.toFile().isDirectory())
        {
            file_signature_array = null;
            folder_signature_array = path.toFile().list();

            if ( folder_signature_array ==null)
            {
                logger.log("FATAL: Filesystem_item_signature scanning failed for "+path);
                return false;
            }
            // File.list() returns an OS dependent order, which is also user choice dependant
            // so the same folder may show a different order at different times
            // we must sort it to have a reproducible signature
            Arrays.sort(folder_signature_array);
        }
        else
        {
            folder_signature_array = null;
            file_signature_array = get_file_hash(path, logger);
            if ( file_signature_array ==null)
            {
                logger.log("FATAL: Filesystem_item_signature file_signature_array == null for "+path);
                return false;
            }
            if ( file_signature_array.length == 0)
            {
                logger.log("WARNING: Filesystem_item_signature file_signature_array is empty ??? for "+path);
                return false;
            }
        }
        return true;
    }


    //**********************************************************
    public static byte[] get_file_hash(Path path, Logger logger)
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
            return new byte[0];
        } catch (Exception e) {
            logger.log("Filesystem_item_signature get_file_hash() fails because of: " + e);
            return new byte[0];
        }
        return hash;
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
                logger.log("FATAL, Filesystem_item_signature file_signature_array = null ");
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
