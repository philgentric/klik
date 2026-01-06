// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.icons.image_properties_cache;

import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;
import klikr.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Image_properties_message implements Message
//**********************************************************
{

    public final Path path;
    public final Logger logger;
    public final Image_properties_RAM_cache image_properties_cache;
    public final Aborter aborter;

    //**********************************************************
    public Image_properties_message(Path path, Image_properties_RAM_cache image_properties_cache, Aborter aborter_, Logger logger)
    //**********************************************************
    {
        this.path = path;
        this.logger = logger;
        this.image_properties_cache = image_properties_cache;
        aborter = aborter_;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" Image_properties_message: ");
        sb.append(" path: ").append(path);
        return sb.toString();
    }}
