package klik.util;

import klik.actor.Aborter;
import klik.browser.icons.Icon_writer_actor;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.fusk.Fusk_static_core;

import javafx.scene.image.Image;
import java.io.*;
import java.nio.file.Path;

/*
static utilities for loading images and icons from the disk
 */
//**********************************************************
public class From_disk
//**********************************************************
{

    public static final boolean dbg = false;
    public static final int MIN_REMAINING_FREE_MEMORY_10MB = 10000000;


    //**********************************************************
    public static InputStream get_image_InputStream(Path original_image_file, boolean try_fusked, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (try_fusked)
        {
            byte[] buf= Fusk_static_core.defusk_file_to_bytes(original_image_file, aborter, logger);
            if ( buf == null)
            {
                // error
                return null;
            }
            else if ( buf.length == 0)
            {
                // not fusked, fall back to standard
            }
            else
            {
                logger.log("fusked image detected "+original_image_file);
                // was fusked !
                return new ByteArrayInputStream(buf);
            }
        }
        // "standard"
        try
        {
            return new FileInputStream(original_image_file.toFile());
        }
        catch(FileNotFoundException e)
        {
            //logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            logger.log((e.toString()));
            return null;
        }

    }

    //**********************************************************
    public static Image load_image_from_disk(Path original_image_file, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (get_remaining_memory() < MIN_REMAINING_FREE_MEMORY_10MB) {
            logger.log("load_image_fx NOT DONE because running low on memory ! ");
            return null;
        }
        boolean enable_fusk = Static_application_properties.get_enable_fusk(logger);
        InputStream input_stream = get_image_InputStream(original_image_file, enable_fusk, aborter, logger);
        if ( input_stream == null) return null;
        Image image = new Image(input_stream);
        try {
            input_stream.close();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));

            e.printStackTrace();
        }
        if ( image.isError())
        {
            if( image.getException().toString().contains("OutOfMemoryError"))
            {
                logger.log("IMAGE decode Panic :"+image.getException());
                Popups.popup_Exception(image.getException(),100,"Kill the instance and restart one!",logger);
            }
            else
            {
                logger.log("IMAGE ERROR :"+original_image_file.toAbsolutePath()+" : "+image.getException());
            }
        }
        return image;

    }
    // this call RESIZES to the target icon size
    //**********************************************************
    public static Image read_original_image_from_disk_and_return_icon(Path original_image_file, double icon_size, Aborter aborter, boolean dbg, Logger logger)
    //**********************************************************
    {
        boolean enable_fusk = Static_application_properties.get_enable_fusk(logger);
        InputStream input_stream = get_image_InputStream(original_image_file, enable_fusk, aborter,logger);
        if (input_stream == null) return null;
        if ( aborter.should_abort()) return null;
        Image image = new Image(input_stream, icon_size, icon_size, true, true);
        try {
            input_stream.close();
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));

            e.printStackTrace();
        }
        if ( image.isError())
        {
            if ( dbg) logger.log("From_disk WARNING: an error occurred when reading: "+original_image_file.toAbsolutePath());
            return null;
        }
        return image;

    }

    //**********************************************************
    public static long get_remaining_memory()
    //**********************************************************
    {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        long remaining = max - used;
        return remaining;
    }


    
    //**********************************************************
    public static File file_for_icon_cache(Path cache_dir, Path original_image_file, String tag, String extension)
    //**********************************************************
    {
        if ( original_image_file == null) return null;
        return new File(cache_dir.toFile(), Icon_writer_actor.make_cache_name(original_image_file, tag, extension));
    }

    //**********************************************************
    public static Image load_icon_from_disk_cache(
            Path original_image_file, // this NOT the icon path
            Path cache_dir,
            int icon_size,
            String tag,
            String extension,
            boolean dbg_local,
            Logger logger)
    //**********************************************************
    {

        if (get_remaining_memory() < MIN_REMAINING_FREE_MEMORY_10MB)
        {
            logger.log("load_icon_from_cache_fx WARNING: running low on memory ! loading default icon");
            return Look_and_feel_manager.get_default_icon(icon_size);
        }

        File f = file_for_icon_cache(cache_dir,original_image_file,tag, extension);

        if (dbg) logger.log("load_icon_from_disk file is:"+f.getAbsolutePath()+" for "+original_image_file);

        try (FileInputStream input_stream = new FileInputStream(f))
        {
            Image image = new Image(input_stream);
            return image;
        } catch (FileNotFoundException e) {
            // this happens the first time one visits a directory...
            // or when the icon cache dir content has been erased etc.
            // so quite a lot, so it is logged only in debug
            if (dbg_local) logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return null;
    }

    /*
        // this call loads the image in the native size
    //**********************************************************
    public static Image load_image_fx_from_disk_old(Path original_image_file, Logger logger)
    //**********************************************************
    {

        if (get_remaining_memory() < MIN_REMAINING_FREE_MEMORY_10MB) {
            logger.log("load_image_fx WARNING! running low on memory ! ");
            return null;
        }

        // SPOT33
        if (encrypt_images)
        {

            byte[] buf= Fusk.defusk_file_to_bytes(original_image_file,logger);
            if ( buf != null) {
                // was fusked !

                try (ByteArrayInputStream input_stream = new ByteArrayInputStream(buf)) {
                    Image image = new Image(input_stream);
                    return image;
                } catch (FileNotFoundException e) {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                } catch (IOException e) {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                } catch (OutOfMemoryError e) {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                } catch (Exception e) {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }
                return null;
            }

        }
        // not fusked
        try (FileInputStream input_stream = new FileInputStream(original_image_file.toFile())) {
            Image image = new Image(input_stream);
            return image;
        } catch (FileNotFoundException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (OutOfMemoryError e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (Exception e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }

        return null;
    }

    // this call RESIZES to the target icon size
    //**********************************************************
    public static Image read_original_image_from_disk_and_return_icon_old(Path original_image_file, double icon_size, boolean dbg, Logger logger)
    //**********************************************************
    {
        // SPOT33
        try (FileInputStream input_stream = new FileInputStream(original_image_file.toFile())) {
            Image image = new Image(input_stream, icon_size, icon_size, true, true);
            if ( image.isError())
            {
                if ( dbg) logger.log("From_disk WARNING: an error occurred when reading: "+original_image_file.toAbsolutePath());
                return null;
            }
            return image;
        } catch (FileNotFoundException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }


        return null;
    }


     */
}