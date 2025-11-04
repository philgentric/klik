// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.images;

import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Message;
import klik.util.log.Logger;

import java.util.Objects;

//**********************************************************
public class Change_image_message implements Message
//**********************************************************
{
    public final Logger logger;
    public final int delta;
    public final Image_window image_window;
    public final boolean ultimate;
    public final Image_context input_image_context;
    public final Image_context[] output_image_context;
    public final Aborter aborter;

    //**********************************************************
    public Change_image_message(int delta, Image_context image_context, Image_window image_window, boolean ultimate, Image_context[] returned, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.delta = delta;
        this.input_image_context = Objects.requireNonNull(image_context);
        this.image_window = image_window;
        this.ultimate = ultimate;
        this.output_image_context = returned;
        this.aborter = aborter;
    }

    //**********************************************************
    @Override
    public Aborter get_aborter()
    //**********************************************************
    {
        return aborter;
    }


    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" Change_image_message: ");
        sb.append(" path: ").append(input_image_context.path);
        return sb.toString();
    }
}
