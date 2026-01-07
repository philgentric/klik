package klikr.audio;

import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;

//**********************************************************
public class Duration_message implements Message
//**********************************************************
{
    public final Path path;
    public final LongAdder seconds;
    public final CountDownLatch cdl;
    public final Aborter aborter;
    public final Song_duration_RAM_cache cache;
    //**********************************************************
    public Duration_message(Path path, Song_duration_RAM_cache cache, LongAdder seconds, CountDownLatch cdl, Aborter aborter)
    //**********************************************************
    {
        this.cache = cache;
        this.path = path;
        this.seconds = seconds;
        this.cdl = cdl;
        this.aborter = aborter;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" Duration_message: ");
        sb.append(" path: ").append(path);
        return sb.toString();
    }

    //**********************************************************
    @Override
    public Aborter get_aborter()
    //**********************************************************
    {
        return aborter;
    }
}