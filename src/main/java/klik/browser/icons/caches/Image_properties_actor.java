package klik.browser.icons.caches;

import klik.actor.Actor;
import klik.actor.Message;
import klik.files_and_paths.Guess_file_type;
import klik.images.decoding.Fast_image_property_from_exif_metadata_extractor;
import klik.util.From_disk;
import org.apache.commons.io.FilenameUtils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Image_properties_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;
    //public static final double ISO_A4_aspect_ratio = 1.0/Math.sqrt(2.0);
    //public static final double US_letter_aspect_ratio = 21.6/27.9;
    //public static final double movie_aspect_ratio = 16.0/9.0;
    private AtomicInteger in_flight = new AtomicInteger(0);
    LinkedBlockingQueue<String> end;

    //**********************************************************
    public Image_properties_actor(LinkedBlockingQueue<String> end_ )
    //**********************************************************
    {
        end = end_;
    }


    //**********************************************************
    public int get_in_flight()
    //**********************************************************
    {
        return in_flight.get();
    }

    //**********************************************************
    public void increment_in_flight()
    //**********************************************************
    {
        in_flight.incrementAndGet();
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Image_properties_message image_properties_message = (Image_properties_message) m;
        if (dbg) image_properties_message.logger.log("Aspect_ratio_actor START for"+image_properties_message.path);

        if (image_properties_message.aborter.should_abort())
        {
            //aspect_ratio_message.logger.log("Aspect_ratio_actor aborting 1");
            return "aborted";
        }

        Image_properties ip = Fast_image_property_from_exif_metadata_extractor.get_image_properties(image_properties_message.path,true,image_properties_message.aborter, image_properties_message.logger);
        image_properties_message.image_properties_cache.inject(image_properties_message.path,ip,false);
        int r = in_flight.decrementAndGet();
        if (dbg) image_properties_message.logger.log("Done image propertiesfor: "+image_properties_message.path+" remaining="+r);
        if ( r == 0)
        {
            end.add("END");
            image_properties_message.image_properties_cache.save_whole_cache_to_disk();
        }
        return "ok";
    }

}
