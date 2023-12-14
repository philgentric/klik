package klik.browser.icons;

import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Message;
import klik.util.From_disk;

import java.util.concurrent.atomic.AtomicInteger;

public class Aspect_ratio_actor implements Actor {

    public  AtomicInteger in_flight = new AtomicInteger(0);

    @Override
    public String run(Message m) {

        Aspect_ratio_message arm = (Aspect_ratio_message) m;

        double d = From_disk.get_aspect_ratio(arm.path, new Aborter(),arm.logger);

        arm.aspect_ratio_cache.put(arm.path.toAbsolutePath().toString(),new Aspect_ratio_cache.Aspect_ratio(d,true));
        in_flight.decrementAndGet();
        return null;
    }

}
