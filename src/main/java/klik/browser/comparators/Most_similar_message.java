package klik.browser.comparators;

import klik.actor.Aborter;
import klik.actor.Message;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

//**********************************************************
public class Most_similar_message implements Message
//**********************************************************
{
    public final Aborter aborter;
    public final Path p1;
    public final List<Path> images;

    //**********************************************************
    public Most_similar_message(Path p1, List<Path> images_copy, Aborter aborter)
    //**********************************************************
    {
        this.aborter = aborter;
        this.p1 = p1;
        this.images = images_copy;
    }

    @Override
    public String to_string() {
        return "Most_similar_message";
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
