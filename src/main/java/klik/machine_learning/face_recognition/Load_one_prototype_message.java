// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning.face_recognition;

import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Message;

import java.io.File;

public class Load_one_prototype_message implements Message {
    final File f;
    final Aborter aborter;
    final Face_recognition_service service;


    public Load_one_prototype_message(File f, Face_recognition_service service, Aborter aborter) {
        this.f = f;
        this.service = service;
        this.aborter = aborter;
    }

    @Override
    public String to_string() {
        return "Load_one_prototype_message";
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
