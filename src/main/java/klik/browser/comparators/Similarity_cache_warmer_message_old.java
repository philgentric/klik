package klik.browser.comparators;

import klik.actor.Aborter;
import klik.actor.Message;

//**********************************************************
public class Similarity_cache_warmer_message_old implements Message
//**********************************************************
{

    private final Aborter aborter;
    final int i1;

    //**********************************************************
    public Similarity_cache_warmer_message_old(Aborter aborter, int i1)
    //**********************************************************
    {

        this.aborter = aborter;
        this.i1 = i1;
    }
    @Override
    public String to_string() {
        return "Similarity_cache_warmer_message "+i1;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
