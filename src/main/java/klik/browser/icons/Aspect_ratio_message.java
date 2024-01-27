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

    public static final double ISO_A4_aspect_ratio = 1.0/Math.sqrt(2.0);
    //public static final double US_letter_aspect_ratio = 21.6/27.9;

    public final Path path;
    public final Logger logger;
    public final Map<String, Aspect_ratio> aspect_ratio_cache;
    public final Aborter aborter;

    //**********************************************************
    public Aspect_ratio_message(Path path, Map<String, Aspect_ratio> aspectRatioCache, Aborter aborter_, Logger logger)
    //**********************************************************
    {
        this.path = path;
        this.logger = logger;
        aspect_ratio_cache = aspectRatioCache;
        aborter = aborter_;
    }

    public static Aspect_ratio get_best(Aspect_ratio a1, Aspect_ratio a2, String tag, Logger logger)
    {
        if ( a1.truth() && !a2.truth()) return a1;
        if ( !a1.truth() && a2.truth()) return a2;
        if ( a1.truth() && a2.truth())
        {
            if ( a1.value() == a2.value()) return a1;
            logger.log(tag+ " WARNING: aspect ratio conflict "+a1 +" vs "+a2);
            return a1;
        }
        if ( !a1.truth() && !a2.truth())
        {
            if ( a1.value() == a2.value()) return a1;
            logger.log(tag+" WARNING: aspect ratio conflict "+a1 +" vs "+a2);
            return a1;
        }
        return a1;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
