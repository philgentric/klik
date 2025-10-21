//SOURCES ../../../actor/virtual_threads/Concurency_limiter.java
//SOURCES ./Animated_gif_generation_message.java
package klik.util.animated_gifs;

import klik.actor.Actor;
import klik.actor.Message;
import klik.actor.virtual_threads.Concurency_limiter;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.ui.Popups;

//**********************************************************
public class Animated_gif_generation_actor implements Actor
//**********************************************************
{
    private static Concurency_limiter cl;

    //**********************************************************
    Animated_gif_generation_actor(Logger logger)
    //**********************************************************
    {
        if ( cl == null) cl = new Concurency_limiter("Animated gif generator",1,logger);
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Animated_gif_generation_actor";
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
        boolean ok = Ffmpeg_utils.video_to_gif(
                mm.video_path,
                mm.height,
                mm.fps,
                mm.destination_gif_full_path,
                mm.dur,
                mm.start,
                0,
                mm.get_aborter(),
                mm.originator,
                mm.logger);
        cl.release();
        if ( !ok)
        {
            if (! mm.abort_reported.get())
            {
                mm.abort_reported.set(true);
                Jfx_batch_injector.inject(() -> Popups.popup_warning( "Massive animated gif generation for "+mm.video_path+" was ABORTED!", "Did you change dir ?",false,mm.originator,mm.logger), mm.logger);

            }
        }

        return null;
    }

}
