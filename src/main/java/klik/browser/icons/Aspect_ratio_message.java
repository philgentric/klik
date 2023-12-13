package klik.browser.icons;

import klik.actor.Aborter;
import klik.actor.Message;
import klik.util.Logger;

import java.nio.file.Path;
import java.util.Map;

public class Aspect_ratio_message implements Message {
    
    public final Path path;
    public final Logger logger;
    public final Map<String, Paths_manager.Aspect_ratio> aspect_ratio_cache;

    public Aspect_ratio_message(Path path, Map<String, Paths_manager.Aspect_ratio> aspectRatioCache, Logger logger) {
        this.path = path;
        this.logger = logger;
        aspect_ratio_cache = aspectRatioCache;
    }

    @Override
    public Aborter get_aborter() {
        return new Aborter();
    }
}
