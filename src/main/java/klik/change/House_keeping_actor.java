package klik.change;

import klik.util.execute.actor.Actor;
import klik.util.execute.actor.Message;

import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public class House_keeping_actor implements Actor
//**********************************************************
{
    ConcurrentLinkedQueue<Change_receiver> change_gang_receivers;

    //**********************************************************
    public House_keeping_actor(ConcurrentLinkedQueue<Change_receiver> change_gang_receivers)
    //**********************************************************
    {
        this.change_gang_receivers = change_gang_receivers;
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "House_keeping_actor";
    }
    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        House_keeping_message hkm = (House_keeping_message) m;
        switch (hkm.type) {
            case register -> change_gang_receivers.add(hkm.originator);
            case deregister -> change_gang_receivers.remove(hkm.originator);
        }
        return null;
    }


}
