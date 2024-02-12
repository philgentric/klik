package klik.browser.icons;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Message;

import java.util.concurrent.LinkedBlockingQueue;

//**********************************************************
public class Icon_factory_request implements Message
//**********************************************************
{
    public final int icon_size;
    public final Icon_destination destination;
    public final Aborter aborter;
    public int retry_count = 0;
    public final static int max_retry = 42;
    private final boolean is_high_priority;
    LinkedBlockingQueue<Boolean> ticket_queue;

    //**********************************************************
    public Icon_factory_request(Icon_destination destination_, int icon_size_, boolean is_high_priority,Aborter aborter)
    //**********************************************************
    {
        icon_size = icon_size_;
        destination = destination_;
        this.aborter = aborter;
        this.is_high_priority = is_high_priority;
        if (Actor_engine.use_tickets) ticket_queue = new LinkedBlockingQueue();
    }

    @Override
    public String to_string() {
        return "Icon_factory_request for: "+destination.get_string();
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }





    @Override
    public LinkedBlockingQueue<Boolean> get_ticket_queue()
    {
        return ticket_queue;
    }

    @Override
    public boolean is_high_priority()
    {
        return is_high_priority;
    }
}
