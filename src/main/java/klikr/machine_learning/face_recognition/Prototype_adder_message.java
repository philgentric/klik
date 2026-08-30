// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.face_recognition;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;
import klikr.machine_learning.feature_vector.Feature_vector;

public class Prototype_adder_message implements Message {
    private final Aborter aborter;
    public final Image face;
    public final String label;
    public final Feature_vector feature_vector;
    public final Window owner;

    public Prototype_adder_message(String label, Image face, Feature_vector feature_vector, Aborter aborter, Window owner) {
        this.label = label;
        this.face = face;
        this.feature_vector = feature_vector;
        this.aborter = aborter;
        this.owner = owner;
    }

    @Override

    public String thread_name() {
        return "Adding a prototype";
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
