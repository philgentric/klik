// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core.icons;

import javafx.scene.image.Image;
import klikr.browser_core.Image_and_properties;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

import java.nio.file.Path;

//**********************************************************
public record Icon_write_message(
        Image_and_properties iap,
        int icon_size,
        Path absolute_path,
        Aborter aborter) implements Message
//**********************************************************
{

    //**********************************************************
    @Override
    public String thread_name()
    //**********************************************************
    {
        return "Writing icon to disk cache for: " + absolute_path;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }


}
