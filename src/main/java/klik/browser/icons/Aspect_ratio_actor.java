package klik.browser.icons;

import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Message;
import klik.files_and_paths.Guess_file_type;
import klik.util.From_disk;
import klik.util.Logger;
import org.apache.commons.io.FilenameUtils;

import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Aspect_ratio_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;
    AtomicInteger in_flight;

    public Aspect_ratio_actor(AtomicInteger in_flight_) {
        in_flight = in_flight_;
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Aspect_ratio_message aspect_ratio_message = (Aspect_ratio_message) m;
        if (aspect_ratio_message.aborter.should_abort())
        {
            //arm.logger.log("Aspect_ratio_actor aborting 1");
            return "aborted";
        }

        if (Guess_file_type.is_this_extension_a_pdf(FilenameUtils.getExtension(aspect_ratio_message.path.getFileName().toString())))
        {
            double aspect_ratio = Aspect_ratio_message.ISO_A4_aspect_ratio;
            //double aspect_ratio = Aspect_ratio_message.US_letter_aspect_ratio;
            if ( dbg) aspect_ratio_message.logger.log("PDF => aspect_ratio "+aspect_ratio);
            aspect_ratio_message.aspect_ratio_cache.put(Aspect_ratio_cache.key_from_path(aspect_ratio_message.path),new Aspect_ratio(aspect_ratio,false));
        }
        else
        {
            double aspect_ratio = From_disk.get_aspect_ratio(aspect_ratio_message.path, dbg, aspect_ratio_message.aborter,aspect_ratio_message.logger);
            aspect_ratio_message.aspect_ratio_cache.put(Aspect_ratio_cache.key_from_path(aspect_ratio_message.path),new Aspect_ratio(aspect_ratio,true));
        }

        int r = in_flight.decrementAndGet();
        //arm.logger.log(d+" is aspect ratio for: "+arm.path+" remaining="+r);
        return "ok";
    }

}
