// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr;

//SOURCES ./util/tcp/TCP_util.java

import javafx.application.Platform;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.properties.Non_booleans_properties;
import klikr.util.http.Klikr_communicator;
import klikr.util.log.Logger;

import java.util.function.Consumer;

//**********************************************************
public interface UI_change_target
//**********************************************************
{
    String UI_CHANGED = "UI_CHANGED";

    void define_UI();

}
