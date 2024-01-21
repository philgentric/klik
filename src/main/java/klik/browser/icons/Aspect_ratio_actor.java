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
    AtomicInteger in_flight;

    public Aspect_ratio_actor(AtomicInteger in_flight_) {
        in_flight = in_flight_;
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Aspect_ratio_message arm = (Aspect_ratio_message) m;
        if (arm.aborter.should_abort())
        {
            //arm.logger.log("Aspect_ratio_actor aborting 1");
            return "aborted";
        }

        double aspect_ratio;
        if (Guess_file_type.is_this_extension_a_pdf(FilenameUtils.getExtension(arm.path.getFileName().toString())))
        {
            aspect_ratio = 1.0;
        }
        else
        {
            aspect_ratio = From_disk.get_aspect_ratio(arm.path, arm.aborter,arm.logger);
        }

        arm.aspect_ratio_cache.put(Aspect_ratio_cache.key_from_path(arm.path),new Aspect_ratio_cache.Aspect_ratio(aspect_ratio,true));
        int r = in_flight.decrementAndGet();
        //arm.logger.log(d+" is aspect ratio for: "+arm.path+" remaining="+r);
        return "ok";
    }

}
