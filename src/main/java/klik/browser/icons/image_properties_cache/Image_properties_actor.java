package klik.browser.icons.image_properties_cache;

import klik.actor.Actor;
import klik.actor.Message;
import klik.images.decoding.Fast_image_property_from_exif_metadata_extractor;

//**********************************************************
public class Image_properties_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Image_properties_message image_properties_message = (Image_properties_message) m;
        if (dbg) image_properties_message.logger.log("Image_properties_actor START for"+image_properties_message.path);

        if (image_properties_message.aborter.should_abort())
        {
            if (dbg) image_properties_message.logger.log("Image_properties_actor aborting "+image_properties_message.path);
            return "aborted";
        }

        Image_properties ip = Fast_image_property_from_exif_metadata_extractor.get_image_properties(image_properties_message.path,true,image_properties_message.aborter, image_properties_message.logger);

        image_properties_message.image_properties_cache.inject(image_properties_message.path,ip,false);
        return "ok";
    }

}
