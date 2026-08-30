// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change;

import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;

import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public class Change_tracking_actor implements Actor
//**********************************************************
{
    ConcurrentLinkedQueue<Change_receiver> change_gang_receivers;

    //**********************************************************
    public Change_tracking_actor(ConcurrentLinkedQueue<Change_receiver> change_gang_receivers)
    //**********************************************************
    {
        this.change_gang_receivers = change_gang_receivers;
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Change_tracking_actor";
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
