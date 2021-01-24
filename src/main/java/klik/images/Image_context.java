package klik.images;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.Static_image_utilities;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

//**********************************************************
public class Image_context
//**********************************************************
{
    public static final boolean dbg = false;

    public final Path path;
    public final Image image;
    public final ImageView imageView;
    private List<String> exifs_tags_list = null;
    private int rotation = 0;
    Logger logger;
    //private String date;
    boolean pix_for_pix;
    double zoom_factor;
    double scroll_x;
    double scroll_y;
    public boolean image_is_damaged;
    //boolean loaded_from_cache = false;

    public final int previous_index; // this may be totally invalid!

    //**********************************************************
    public static Image_context get_Image_context(Path f_, Logger logger_)
    //**********************************************************
    {
        if ( Files.exists(f_) == false) return null;
        Image local_image = From_disk.load_image_fx_from_disk(f_, logger_);
        if ( local_image == null) return null;
        if ( local_image.isError())
        {
            Image broken = Look_and_feel_manager.get_broken_icon(300);

            new Image_context(f_,broken,false,-1,logger_);
        }

        return new Image_context(f_,local_image,true,-1,logger_);
    }

    //**********************************************************
    public static Image_context get_Image_context2(Path f_, int size, Logger logger_)
    //**********************************************************
    {
        if ( Files.exists(f_) == false) return null;
        Image local_image = From_disk.load_image_fx_from_disk(f_, logger_);
        if ( local_image == null) return null;
        if ( local_image.isError())
        {
            Image broken = Look_and_feel_manager.get_broken_icon(300);

            new Image_context(f_,broken,false,-1,logger_);
        }

        Image resized_image = Static_image_utilities.transform_2(local_image,size,true,logger_);
        return new Image_context(f_,resized_image,true,-1,logger_);
    }

    //**********************************************************
    public Image_context(Path f_, Image im_, boolean get_rotation, int previous_index_, Logger logger_)
    //**********************************************************
    {
        path = f_;
        logger = logger_;
        previous_index = previous_index_;
        image = im_;
        imageView = new ImageView(image);
        if ( get_rotation) get_rotation();
        if ( dbg)
        {
            if ( path ==null)
            {
                logger.log("NULL file, image loaded:"+image.getWidth()+"x"+image.getHeight());
            }
            else
            {
                logger.log("image loaded:"+ path.getFileName()+" "+image.getWidth()+"x"+image.getHeight());
            }

        }
    }

    //**********************************************************
    public static String get_key(Path f)
    //**********************************************************
    {
        return f.toAbsolutePath().toString();
    }


/*
    //**********************************************************
    public static Image_context from_Image(WritableImage destination, Logger logger_)
    //**********************************************************
    {

        return new Image_context(null,destination,logger_);
    }
*/

    public void load_image(Logger logger)
    {
    }

    public void make_ImageView(Logger logger)
    {

    }



    public long how_many_pixels()
    {
        return (long)(image.getWidth()*image.getHeight());
    }

    public int get_index(Image_file_source ifs)
    {
        return ifs.get_index_of(path);
    }


    public void set_pix_for_pix(boolean b)
    {
        pix_for_pix = b;
    }

    public double get_zoom_factor()
    {
        return zoom_factor;
    }

    public boolean get_pix_for_pix()
    {
        return pix_for_pix;
    }

    public double get_scroll_x()
    {
        return scroll_x;
    }
    public double get_scroll_y()
    {
        return scroll_y;
    }

    public void set_quality(String quality)
    {
    }

    /*public boolean get_loaded_from_cache()
    {
        return loaded_from_cache;
    }*/


    public List<String> get_exif_metadata()
    {
        load_exif();
        return exifs_tags_list;
    }
    public int get_rotation()
    {
        load_exif();
        return rotation;
    }

    private void load_exif()
    {
        if (exifs_tags_list != null) return;
        image_is_damaged = false;
        try
        {
            Exif_metadata_extractor extractor = new Exif_metadata_extractor(path,logger);
            exifs_tags_list = extractor.get_exif_metadata();
            rotation = extractor.get_rotation();
            image_is_damaged = extractor.image_is_damaged;
        }
        catch (OutOfMemoryError e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
    }


}
