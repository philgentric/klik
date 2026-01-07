package klikr.audio;

import klikr.util.animated_gifs.Ffmpeg_utils;
import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;
import klikr.util.execute.actor.virtual_threads.Concurrency_limiter;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;

//**********************************************************
public class Song_duration_actor implements Actor
//**********************************************************
{
    private static final Concurrency_limiter cl =  new Concurrency_limiter("audio duration",0.5);
    private final Logger logger;

    //**********************************************************
    public Song_duration_actor(Logger logger)
    //**********************************************************
    {
        this.logger = logger;
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        try {
            cl.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Duration_message dm = (Duration_message) m;
        Double duration = get_media_duration(dm.path, dm.seconds, dm.cdl,logger);
        if ( dm.cache != null) dm.cache.inject(dm.path,duration,false);
        cl.release();
        return "OK";    }

    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Song_duration_actor";
    }

    //**********************************************************
    private Double get_media_duration(Path path, LongAdder seconds, CountDownLatch cdl, Logger logger)
    //**********************************************************
    {
        Double dur = Ffmpeg_utils.get_media_duration(path, null, logger);
        if ( dur != null)
        {
            if ( seconds != null) seconds.add((long) (double)dur);
        }
        if ( cdl != null) cdl.countDown();
        return dur;
    }


}
