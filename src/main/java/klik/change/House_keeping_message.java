// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./House_keeping_message_type.java
package klik.change;

import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Message;

//**********************************************************
public class House_keeping_message implements Message
//**********************************************************
{
    public final Change_receiver originator;
    public final House_keeping_message_type type;
    public final Aborter aborter;

    //**********************************************************
    public House_keeping_message(Change_receiver amh, House_keeping_message_type type_, Aborter aborter)
    //**********************************************************
    {
        originator = amh;
        type = type_;
        this.aborter = aborter;

    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "House_keeping_message "+originator.get_Change_receiver_string();
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
