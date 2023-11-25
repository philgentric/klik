package klik.browser.icons;

import klik.actor.Aborter;
import klik.actor.Message;

//**********************************************************
public class Icon_factory_request implements Message
//**********************************************************
{
    public final int icon_size;
    public final Icon_destination destination;
    public final Aborter aborter;

    //**********************************************************
    public Icon_factory_request(Icon_destination destination_, int icon_size_)
    //**********************************************************
    {
        icon_size = icon_size_;
        destination = destination_;
        aborter = new Aborter();
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
