package klik.browser.icons;

import klik.actor.Aborter;
import klik.actor.Message;
import klik.util.Logger;

import java.nio.file.Path;
import java.util.Map;

//**********************************************************
public class Aspect_ratio_message implements Message
//**********************************************************
{
    public final Path path;
    public final Logger logger;
    public final Map<String, Aspect_ratio_cache.Aspect_ratio> aspect_ratio_cache;
    public final Aborter aborter;

    //**********************************************************
    public Aspect_ratio_message(Path path, Map<String, Aspect_ratio_cache.Aspect_ratio> aspectRatioCache, Aborter aborter_, Logger logger)
    //**********************************************************
    {
        this.path = path;
        this.logger = logger;
        aspect_ratio_cache = aspectRatioCache;
        aborter = aborter_;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
