package klik.machine_learning.face_recognition;

import javafx.scene.image.Image;
import klik.actor.Aborter;
import klik.actor.Message;
import klik.machine_learning.feature_vector.Feature_vector;

public class Prototype_adder_message implements Message {
    private final Aborter aborter;
    public final Image face;
    public final String label;
    public final Feature_vector feature_vector;

    public Prototype_adder_message(String label, Image face, Feature_vector feature_vector, Aborter aborter) {
        this.label = label;
        this.face = face;
        this.feature_vector = feature_vector;
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
