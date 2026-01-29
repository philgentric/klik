// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.icons;

import javafx.stage.Window;
import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Message;
import klikr.util.image.Static_image_utilities;
import klikr.util.image.icon_cache.Icon_caching;
import klikr.util.log.Logger;
import klikr.util.mmap.Mmap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/*
 * this actor accepts icons and writes them to the disk cache
 */

//**********************************************************
public class Icon_writer_actor implements Actor
//**********************************************************
{
	private static final boolean dbg = false;
	public static final boolean use_mmap = true;
	// dbg_names is super useful to debug this feature BUT it has a major caveat:
	// folders with [whatever] in the name will not have an animated icon

	public static Mmap mmap;
	Path cache_dir;
	private final Logger logger;
    private final Window owner;
	//**********************************************************
	public Icon_writer_actor(Path cache_dir_, Window owner, Logger logger)
	//**********************************************************
	{
		this.logger = logger;
        this.owner = owner;
		if ( dbg) logger.log("Icon_writer_actor created");
		cache_dir = cache_dir_;
		if ( use_mmap)
		{
			mmap = Mmap.get_instance(100, owner,logger);
		}
	}


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Icon_writer_actor";
    }


    //**********************************************************
	public void push(Icon_write_message ii)
	//**********************************************************
	{
		Actor_engine.run(this, ii, null,logger);
	}


	//**********************************************************
	@Override
	public String run(Message m)
	//**********************************************************
	{
		Icon_write_message mm = (Icon_write_message) m;
		write_icon_to_cache_on_disk(mm);
		return "icon written";
	}

    //**********************************************************
    public void write_icon_to_cache_on_disk(Icon_write_message iwm)
    //**********************************************************
    {
		Path out_path = Icon_caching.path_for_icon_caching(iwm.absolute_path(), String.valueOf(iwm.icon_size()), Icon_caching.png_extension, owner, logger);
		if ( use_mmap)
		{
			Runnable on_end = ()->
			{
				try
				{
					Files.delete(out_path);
				}
				catch (IOException e)
				{
					logger.log(""+e);
				}
			};
			mmap.write_image_as_pixels(out_path.toAbsolutePath().toString(),iwm.image(),true, on_end);
		}
		else
		{
			Static_image_utilities.write_png_to_disk(iwm.image(), out_path, logger);
		}
	}

}
