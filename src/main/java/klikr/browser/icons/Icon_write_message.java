// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.icons;

import javafx.scene.image.Image;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

import java.nio.file.Path;

//**********************************************************
public record Icon_write_message(
        Image image,
        int icon_size,
        Path absolute_path,
        Aborter aborter) implements Message
//**********************************************************
{

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "Icon_write_message for: " + absolute_path;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }


}
