package klik.change;

import klik.actor.Aborter;
import klik.actor.Message;

//**********************************************************
public class House_keeping_message implements Message
//**********************************************************
{
    public final Change_receiver originator;
    public final House_keeping_message_type type;
    public final Aborter aborter;

    //**********************************************************
    public House_keeping_message(Change_receiver amh, House_keeping_message_type type_)
    //**********************************************************
    {
        originator = amh;
        type = type_;
        aborter = new Aborter();

    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "House_keeping_message "+originator.get_string();
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
