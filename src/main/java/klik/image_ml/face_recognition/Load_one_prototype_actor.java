package klik.image_ml.face_recognition;

import klik.actor.Actor;
import klik.actor.Message;

public class Load_one_prototype_actor implements Actor {
    @Override
    public String run(Message m) {
        Load_one_prototype_message lopm = (Load_one_prototype_message)m;
        lopm.service.load_one_prototype(lopm.f);
        return null;
    }

    @Override
    public String name() {
        return Actor.super.name();
    }
}
