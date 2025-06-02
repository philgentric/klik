package klik.browser.comparators;

import klik.actor.Aborter;
import klik.actor.Message;

import java.nio.file.Path;

//**********************************************************
public class Similarity_cache_warmer_message implements Message
//**********************************************************
{

    private final Aborter browser_aborter;
    final Path p1;

    //**********************************************************
    public Similarity_cache_warmer_message(Aborter browser_aborter, Path p1)
    //**********************************************************
    {

        this.browser_aborter = browser_aborter;
        this.p1 = p1;
    }
    @Override
    public String to_string() {
        return "Similarity_cache_warmer_message "+p1;
    }

    @Override
    public Aborter get_aborter() {
        return browser_aborter;
    }
}
