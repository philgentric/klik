// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.browser.icons.image_properties_cache;

import javafx.scene.image.Image;
import klik.util.execute.actor.Actor;
import klik.util.execute.actor.Message;
import klik.util.image.Full_image_from_disk;
import klik.util.image.decoding.Fast_image_property_from_exif_metadata_extractor;

import java.util.Optional;

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

        Optional<Image_properties> ip = Fast_image_property_from_exif_metadata_extractor.get_image_properties(image_properties_message.path,true,image_properties_message.aborter, image_properties_message.logger);
        if (ip.isPresent()) {
            image_properties_message.image_properties_cache.inject(image_properties_message.path, ip.get(), false);
        }
        else
        {
            image_properties_message.logger.log("EXIF failed to return Image properties for"+image_properties_message.path);
            // try to load the image
            Optional<Image> op = Full_image_from_disk.load_native_resolution_image_from_disk(image_properties_message.path, true, null, image_properties_message.aborter,image_properties_message.logger);
            if ( op.isPresent())
            {
                Image image = op.get();
                image_properties_message.image_properties_cache.inject(
                        image_properties_message.path,
                        new Image_properties(image.getWidth(), image.getHeight(), Rotation.normal),
                        false);
            }
        }
        return "ok";
    }

    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Image_properties_actor";
    }
}
