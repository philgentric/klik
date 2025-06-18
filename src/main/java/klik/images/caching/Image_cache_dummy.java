package klik.images.caching;

import javafx.stage.Window;
import klik.images.Image_context;
import klik.images.Image_display_handler;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Image_cache_dummy implements Cache_interface
//**********************************************************
{
    Logger logger;
    private String key;
    private Image_context image_context;

    //**********************************************************
    public Image_cache_dummy( Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }


    //**********************************************************
    @Override
    public Image_context get(String key_)
    //**********************************************************
    {
        logger.log("Image_cache_dummy: key = "+key_);
        if ( image_context == null) logger.log("image context is null");
        return image_context;
    }

    //**********************************************************
    @Override
    public void put(String key_, Image_context value)
    //**********************************************************
    {
        logger.log("writing in dummy image cache:" + value.path.getFileName());
        key = key_;
        image_context = value;
    }


    //**********************************************************
    @Override
    public void preload(Image_display_handler image_display_handler, boolean ultimate, boolean forward, Window owner)
    //**********************************************************
    {
    }

    //**********************************************************
    public void check_decoded_image_cache_size(Image_display_handler image_context_owner, Logger logger)
    //**********************************************************
    {
    }

    //**********************************************************
    @Override // Image_cache_interface
    public void evict(Path path, Window owner)
    //**********************************************************
    {
    }

    @Override
    //**********************************************************
    public void clear_all()
    //**********************************************************
    {
       image_context = null;
    }

    //**********************************************************
    @Override
    public void print()
    //**********************************************************
    {
        logger.log("   cache entry: "+image_context.path);
        long total_pixel = (long) (image_context.image.getHeight()*image_context.image.getWidth());
        logger.log("cache size: "+total_pixel/1_000_000+" Mpixels");
    }

}
