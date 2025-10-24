package klik.experimental.fusk;

import klik.util.execute.actor.Aborter;
import klik.Shared_services;
import klik.util.files_and_paths.Extensions;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

//**********************************************************
public class Fusk_static_core
//**********************************************************
{

    public static final String FUSK_EXTENSION = "fusk";
    public static final String FUSK_EXTENSION_WITH_DOT = "."+FUSK_EXTENSION;

    //**********************************************************
    public static boolean is_fusk(Path in,
                                  Logger logger)
    //**********************************************************
    {
        if ( !Fusk_bytes.is_initialized() )
        {
            Fusk_bytes.initialize(logger);
            return false;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(in.toFile());
            byte[] buf = new byte[Fusk_bytes.signature_fusk.length];
            if ( fis.read(buf,0,Fusk_bytes.signature_fusk.length) != Fusk_bytes.signature_fusk.length)
            {
                logger.log("WARNING: file read failed "+in);
                return false;
            }
            if ( Arrays.mismatch(buf,Fusk_bytes.signature_fusk) == -1 ) return true;
            else return false;
        } catch (FileNotFoundException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            return false;
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            return false;
        }
        finally {
            try {
                fis.close();
            } catch (IOException e) {
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                return false;            }
        }

    }

    //**********************************************************
    public static byte[] defusk_file_to_bytes(Path in, Aborter aborter, Logger logger)
    //**********************************************************
    {
        try {
            byte[] obfuscated = Files.readAllBytes(in); // this may time out on "internet" network drives like googleDrive and MS OneDrive
            // TODO: replace with a block based reader
            if (aborter.should_abort()) return null;
            if ( obfuscated == null)
            {
                logger.log("WARNING: readAllBytes failed for "+in.toAbsolutePath());
                return null;
            }
            if ( !Fusk_bytes.check_signature(obfuscated,logger))
            {
                // happens a lot logger.log("WARNING: "+in.toAbsolutePath()+" is NOT fusked!");
                return new byte[0]; // empty array to signal not fusked
            }
            return Fusk_bytes.defusk_bytes_and_remove_signature(obfuscated, aborter, logger);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("For Path: "+in+"\n"+e));
        }
        return null;
    }

    //**********************************************************
    public static boolean fusk_file(Path in, Path destination_folder,Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( !Fusk_bytes.is_initialized())
        {
            if (!Fusk_bytes.initialize(logger))
            {
                logger.log("❌ FATAL: fusk_file, Fusk_bytes not initialized " + in.toAbsolutePath());
                return false;
            }
        }
        Path out = get_fusk_path(in, destination_folder,logger);
        try {
            byte[] clear = Files.readAllBytes(in);
            byte[] obfuskated = Fusk_bytes.obfusk_and_add_signature(clear, aborter, logger);

            Files.write(out,obfuskated);

        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            return false;
        }
        return true;
    }

    //**********************************************************
    public static boolean defusk_file(Path in, Path destination_folder, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path out = get_defusk_path(in, destination_folder,logger);
        int k = 0;
        while ( out.toFile().exists())
        {
            logger.log(" "+out.toAbsolutePath()+" exists");
            String parent = out.getParent().toAbsolutePath().toString();
            String ext = Extensions.get_extension(out.getFileName().toString());
            String base = Extensions.get_base_name(out.getFileName().toString());
            if ( ext.isEmpty())
            {
                out = Paths.get(parent, base + "_" + k );
            }
            else
            {
                out = Paths.get(parent, base + "_" + k + "." + ext);
            }
            k++;
            logger.log("trying "+out.toAbsolutePath());
        }
        try {
            byte[] fusked = Files.readAllBytes(in);
            byte[] clear = Fusk_bytes.defusk_bytes_and_remove_signature(fusked, aborter, logger);

            Files.write(out,clear);

        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
            return false;
        }
        return true;
    }

    //**********************************************************
    private static Path get_fusk_path(Path in, Path destination_folder, Logger logger)
    //**********************************************************
    {
        return Paths.get(destination_folder.toString(),Fusk_strings.fusk_string(in.getFileName().toString(), logger)+FUSK_EXTENSION_WITH_DOT);
    }

    //**********************************************************
    private static Path get_defusk_path(Path in, Path destination_folder, Logger logger)
    //**********************************************************
    {
        String s = Extensions.get_base_name(in.getFileName().toString());
        return Paths.get(destination_folder.toString(),Fusk_strings.defusk_string(s, logger));
    }

    //**********************************************************
    public static void main(String[] args)
    //**********************************************************
    {

        Logger logger = Shared_services.logger();
        Aborter aborter = Shared_services.aborter();


        Fusk_bytes.initialize( logger);
        {
            String test = "The quick brown fox jumps over the lazy dog 372";
            String f = Fusk_strings.fusk_string(test,logger);
            logger.log(test+"=>"+f);
            String f2 = Fusk_strings.defusk_string(f,logger);
            logger.log(f+"=>"+f2);
            if ( !test.equals(f2))
            {
                logger.log("❌ FATAL "+test+"!="+f2);
            }
        }
        {
            String test = "dfshefr3guy4652324234243s53@$#@#!%@$@#@()(*&^%$_+";
            String f = Fusk_strings.fusk_string(test,logger);
            logger.log(test+"=>"+f);
            String f2 = Fusk_strings.defusk_string(f,logger);
            logger.log(f+"=>"+f2);
            if ( !test.equals(f2))
            {
                logger.log("❌ FATAL "+test+"!="+f2);
            }
        }
        {
            Random r = new Random();

            int size =58649966;
            byte[] clear = new byte[size];
            r.nextBytes(clear);

            byte[] fusk = Fusk_bytes.fusk(clear);

            byte[] check = Fusk_bytes.fusk(fusk);

            boolean ok = true;
            for ( int i = 0; i< size ;i++)
            {
                if ( clear[i] != check[i])
                {
                    ok = false;
                    logger.log("❌ fatal error!");
                    break;
                }
            }
            if ( ok) logger.log("byte fusk OK");

        }
        {
            Random r = new Random();
            int size =58649966;
            byte[] clear = new byte[size];
            r.nextBytes(clear);

            logger.log("clear size"+clear.length);
            byte[] fusk = Fusk_bytes.obfusk_and_add_signature(clear,aborter, logger);
            logger.log("fusk size"+fusk.length);
            logger.log("difference "+(fusk.length-clear.length) +" = "+ Fusk_bytes.signature_fusk.length);

            byte[] check = Fusk_bytes.defusk_bytes_and_remove_signature(fusk, aborter, logger);

            boolean ok = true;
            for ( int i = 0; i< size ;i++)
            {
                if ( clear[i] != check[i])
                {
                    ok = false;
                    logger.log("❌ fatal error! "+i);
                    break;
                }
            }
            if ( ok) logger.log("byte fusk with signature OK");

        }

        {
            Random r = new Random();
            int size =455;
            byte[] clear = new byte[size];
            r.nextBytes(clear);
            if ( !Fusk_bytes.check_signature(clear,logger))
            {
                logger.log("check signature OK1");
            }
            else
            {
                logger.log("OHO ? no signature found ????"); // well, that coukd happen but proba is VERY low !-)
            }
            byte[] fusk = Fusk_bytes.obfusk_and_add_signature(clear,aborter,logger);

            if ( Fusk_bytes.check_signature(fusk,logger))
            {
                logger.log("check signature OK2");
            }
            else
            {
                logger.log("❌ FATAL signature not found");
            }
        }

            {
                Random r = new Random();
                long start = System.currentTimeMillis();
                for ( int k = 0; k < 20; k++)
                {
                    int size = 5_649_966;
                    byte[] clear = new byte[size];
                    r.nextBytes(clear);

                    byte[] fusk = Fusk_bytes.fusk(clear);

                    byte[] check = Fusk_bytes.fusk(fusk);

                    boolean ok = true;
                    for ( int i = 0; i< size ;i++)
                    {
                        if ( clear[i] != check[i])
                        {
                            ok = false;
                            break;
                        }
                    }
                    if ( ok) logger.log("✅ byte fusk OK "+k);
                }
                long end = System.currentTimeMillis();
                logger.log("elapsed = "+(end-start));

            }
    }
}
