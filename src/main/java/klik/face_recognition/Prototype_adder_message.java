package klik.face_recognition;

import javafx.scene.image.Image;
import klik.actor.Aborter;
import klik.actor.Message;

public class Prototype_adder_message implements Message {
    private final Aborter aborter;
    public final Image face;
    public final String label;

    public Prototype_adder_message(Image face, String label, Aborter aborter) {
        this.face = face;
        this.label = label;
        this.aborter = aborter;
    }

    @Override
    public String to_string() {
        return "Prototype_adder_message";
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
