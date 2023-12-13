package klik.browser.icons;

import klik.actor.Aborter;
import klik.actor.Actor;
import klik.actor.Message;
import klik.util.From_disk;

public class Aspect_ratio_actor implements Actor {
    @Override
    public String run(Message m) {

        Aspect_ratio_message arm = (Aspect_ratio_message) m;

        double d = From_disk.get_aspect_ratio(arm.path, new Aborter(),arm.logger);

        arm.aspect_ratio_cache.put(arm.path.toAbsolutePath().toString(),new Paths_manager.Aspect_ratio(d,true));

        return null;
    }

}
