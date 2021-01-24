package klik.images;

import javafx.scene.image.Image;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.Tool_box;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public class From_disk
{
    public static final boolean dbg = false;
    public static final int MIN_REMAINING_FREE_MEMORY_10MB = 10000000;

    // this call loads the image in the native size
    //**********************************************************
    public static Image load_image_fx_from_disk(Path original_image_file, Logger logger)
    //**********************************************************
    {

        if (Tool_box.get_remaining_memory() < MIN_REMAINING_FREE_MEMORY_10MB) {
            logger.log("load_image_fx WARNING! running low on memory ! ");
            return null;
        }

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
    public static Image load_icon_fx_from_disk(Path original_image_file, double icon_size, Logger logger)
    //**********************************************************
    {


      try (FileInputStream input_stream = new FileInputStream(original_image_file.toFile())) {


            Image image = new Image(input_stream, icon_size, icon_size, true, true);

            if ( image.isError())
            {
                //return null;
                logger.log("From_disk WARNING: an error occurred when reading: "+original_image_file.toAbsolutePath().toString());


                return null; //Look_and_feel_manager.get_broken_icon(icon_size);
            }
            return image;
        } catch (FileNotFoundException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }


        return null;
    }

    //**********************************************************
    public static Image load_icon_from_disk_cache_fx(
            Path original_image_file,
            Path cache_dir,
            double icon_size,
            Logger logger)
    //**********************************************************
    {

        if (Tool_box.get_remaining_memory() < MIN_REMAINING_FREE_MEMORY_10MB)
        {
            logger.log("load_icon_from_cache_fx WARNING: running low on memory ! loading default icon");
            return Look_and_feel_manager.get_default_icon(icon_size);
        }

        try (FileInputStream input_stream = new FileInputStream(new File(cache_dir.toFile(), Tool_box.MAKE_CACHE_NAME(original_image_file, (int) icon_size)))) {
            Image image = new Image(input_stream);
            return image;
        } catch (FileNotFoundException e) {
            // this happens the first time one visits a directory...
            // or when the icon cache dir content has been erased etc
            // so quite a lot, so it is logged only in debug
            if (dbg) logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return null;
    }

}
