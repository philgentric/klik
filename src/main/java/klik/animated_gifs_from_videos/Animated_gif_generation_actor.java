package klik.animated_gifs_from_videos;

import klik.actor.Actor;
import klik.actor.Message;
import klik.actor.virtual_threads.Concurency_limiter;
import klik.util.Logger;

//**********************************************************
public class Animated_gif_generation_actor implements Actor
//**********************************************************
{
    private static Concurency_limiter cl;
    Animated_gif_generation_actor(Logger logger)
    {
        if ( cl == null) cl = new Concurency_limiter("Animated gif generator",1,logger);

    }
    @Override
    //**********************************************************
    public String run(Message m)
    //**********************************************************
    {
        try {
            cl.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Animated_gif_generation_message mm = (Animated_gif_generation_message) m;
        Animated_gif_generator.video_to_gif(
                mm.owner,
                mm.video_path,
                mm.destination_gif_full_path,
                mm.dur,
                mm.start,
                mm.get_aborter(),
                mm.logger);
        cl.release();

        return null;
    }

}
