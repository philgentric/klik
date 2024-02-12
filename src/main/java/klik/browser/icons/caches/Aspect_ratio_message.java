package klik.browser.icons.caches;

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
    public final Cache_for_doubles aspect_ratio_cache;
    public final Aborter aborter;

    //**********************************************************
    public Aspect_ratio_message(Path path, Cache_for_doubles aspect_ratio_cache_, Aborter aborter_, Logger logger)
    //**********************************************************
    {
        this.path = path;
        this.logger = logger;
        aspect_ratio_cache = aspect_ratio_cache_;
        aborter = aborter_;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }

}
