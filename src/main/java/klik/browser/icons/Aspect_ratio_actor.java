package klik.browser.icons;

import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Message;
import klik.util.From_disk;
import klik.util.Logger;

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
        if (arm.aborter.should_abort()) return "aborted";
        double d = From_disk.get_aspect_ratio(arm.path, arm.aborter,arm.logger);
        arm.aspect_ratio_cache.put(Aspect_ratio_cache.key_from_path(arm.path),new Aspect_ratio_cache.Aspect_ratio(d,true));
        int r = in_flight.decrementAndGet();
        //arm.logger.log(d+" is aspect ratio for: "+arm.path+" remaining="+r);
        return "ok";
    }

}
