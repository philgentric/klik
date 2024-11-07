package klik.browser.icons.image_properties_cache;

import klik.actor.Aborter;
import klik.actor.Message;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Image_properties_message implements Message
//**********************************************************
{

    public final Path path;
    public final Logger logger;
    public final Image_properties_RAM_cache image_properties_cache;
    public final Aborter aborter;

    //**********************************************************
    public Image_properties_message(Path path, Image_properties_RAM_cache image_properties_cache, Aborter aborter_, Logger logger)
    //**********************************************************
    {
        this.path = path;
        this.logger = logger;
        this.image_properties_cache = image_properties_cache;
        aborter = aborter_;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }

}
