package klik.browser.icons;

import javafx.stage.Window;
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
    public final static int max_retry = 3;
    public final Window originator;

    //**********************************************************
    public Icon_factory_request(Icon_destination destination, int icon_size,Window originator,Aborter aborter)
    //**********************************************************
    {
        this.icon_size = icon_size;
        this.destination = destination;
        this.aborter = aborter;
        this.originator = originator;
    }

    @Override
    public String to_string() {
        return "Icon_factory_request for: "+destination.get_string();
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }


}
