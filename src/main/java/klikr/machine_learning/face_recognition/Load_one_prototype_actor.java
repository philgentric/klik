// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.face_recognition;

import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;

//**********************************************************
public class Load_one_prototype_actor implements Actor
//**********************************************************
{
    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Load_one_prototype_message lopm = (Load_one_prototype_message)m;
        lopm.service.load_one_prototype(lopm.f);
        return null;
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Load_one_prototype_actor";
    }

}
