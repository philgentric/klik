package klik.browser.icons.caches;

import klik.actor.Actor;
import klik.actor.Message;
import klik.files_and_paths.Guess_file_type;
import klik.util.From_disk;
import org.apache.commons.io.FilenameUtils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Aspect_ratio_actor implements Actor
//**********************************************************
{
    public static final boolean dbg = false;
    public static final double ISO_A4_aspect_ratio = 1.0/Math.sqrt(2.0);
    //public static final double US_letter_aspect_ratio = 21.6/27.9;
    public static final double movie_aspect_ratio = 16.0/9.0;
    AtomicInteger in_flight;
    LinkedBlockingQueue<String> end;

    public Aspect_ratio_actor(AtomicInteger in_flight_,LinkedBlockingQueue<String> end_ ) {
        in_flight = in_flight_;
        end = end_;
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Aspect_ratio_message aspect_ratio_message = (Aspect_ratio_message) m;
        if (dbg) aspect_ratio_message.logger.log("Aspect_ratio_actor START for"+aspect_ratio_message.path);

        if (aspect_ratio_message.aborter.should_abort())
        {
            //aspect_ratio_message.logger.log("Aspect_ratio_actor aborting 1");
            return "aborted";
        }

        if (Guess_file_type.is_this_extension_a_pdf(FilenameUtils.getExtension(aspect_ratio_message.path.getFileName().toString())))
        {
            double aspect_ratio = ISO_A4_aspect_ratio;
            //double aspect_ratio = Aspect_ratio_message.US_letter_aspect_ratio;
            if ( dbg) aspect_ratio_message.logger.log("PDF => aspect_ratio "+aspect_ratio);
            aspect_ratio_message.aspect_ratio_cache.put_in_cache(aspect_ratio_message.path,aspect_ratio);
        }
        else
        {
            double aspect_ratio = From_disk.determine_aspect_ratio(aspect_ratio_message.path, dbg, aspect_ratio_message.aborter,aspect_ratio_message.logger);
            aspect_ratio_message.aspect_ratio_cache.put_in_cache(aspect_ratio_message.path,aspect_ratio);
        }

        int r = in_flight.decrementAndGet();
        if (dbg) aspect_ratio_message.logger.log("Done aspect ratio for: "+aspect_ratio_message.path+" remaining="+r);
        if ( r == 0)
        {
            end.add("END");
        }
        return "ok";
    }

}
