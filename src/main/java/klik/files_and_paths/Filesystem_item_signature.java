package klik.files_and_paths;

import klik.util.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;

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
                logger.log("WARNING: no files in "+path);
                return false;
            }
            if ( folder_signature_array.length == 0)
            {
                logger.log("WARNING: no files in "+path);
                return false;
            }
            Arrays.sort(folder_signature_array); // just in case
        }
        else
        {
            folder_signature_array = null;
            file_signature_array = get_file_hash(path, logger);
            if ( file_signature_array ==null)
            {
                logger.log("WARNIN: file_signature_array == null for "+path);
                return false;
            }
            if ( file_signature_array.length == 0)
            {
                logger.log("WARNIN: file_signature_array is empty ??? for "+path);
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
            byte[] b = new byte[internal_hash_computation_buffer_size_in_bytes];

            MessageDigest sha = MessageDigest.getInstance("MD5");
            //sha.reset();
            for (; ; ) {
                int available = fis.available();
                if (available < internal_hash_computation_buffer_size_in_bytes) {
                    if ( fis.read(b, 0, available) != available)
                    {
                        logger.log("file read failed for: "+path);
                        return null;
                    }
                    //System.out.println("byte " + new String(b));
                    sha.update(b, 0, available);
                    break;
                } else {
                    if (fis.read(b, 0, internal_hash_computation_buffer_size_in_bytes) != internal_hash_computation_buffer_size_in_bytes)
                    {
                        logger.log("file read failed for: "+path);
                        return null;
                    }
                    //System.out.println("byte " + new String(b));
                    sha.update(b, 0, internal_hash_computation_buffer_size_in_bytes);
                }
            }
            fis.close();
            hash = sha.digest();
            //System.out.println("the MD5 hash of " + srcfile + " is " + new String(hash) + " " + sha.getDigestLength());

        } catch (FileNotFoundException e) {
            logger.log("get_file_hash() fails because of: " + e);
            return new byte[0];
        } catch (Exception ioe) {
            logger.log("get_file_hash() fails because of: " + ioe);
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
                logger.log("FATAL, file_signature_array = null ");
            }
            if ( folder_signature_array.length != other.folder_signature_array.length) return false;

            if ( Arrays.mismatch(folder_signature_array,other.folder_signature_array) != -1) return false;

            //for (int i = 0; i < folder_signature_array.length ; i++ )
            //{
            //    if ( ! (folder_signature_array[i].equals(other.folder_signature_array[i]) )) return false;
            // }
            return true;
        }

        if (file_signature_array.length == 0) return false;
        if (other.file_signature_array.length != this.file_signature_array.length) return false;
        //no if ( Arrays.compare(previously.signature,presently.signature) != 0) return false;
        //better stop asap:
        if ( Arrays.mismatch(file_signature_array,other.file_signature_array) != -1) return false;

        //for (int i = 0; i < other.file_signature_array.length; i++)
        //{
        //    if (other.file_signature_array[i] != this.file_signature_array[i]) return false;
        //}
        return true;
    }
}
