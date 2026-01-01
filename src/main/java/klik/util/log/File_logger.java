// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Simple_logger.java
package klik.util.log;

import klik.util.execute.Execute_common;
import klik.util.execute.actor.Actor_engine;
import klik.properties.Non_booleans_properties;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;

//*******************************************************
public class File_logger implements Logger
//*******************************************************
{

	private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
	//*******************************************************
	public File_logger(String tag_)
	//*******************************************************
	{
		Path file = Execute_common.get_tmp_file_path_in_trash(tag_,"txt",null,new Simple_logger());

		Runnable r = () -> {
			for(;;)
			{
				try {
					String s = queue.take();
					//System.out.println("File_logger: "+tag+" => "+s);
					FileWriter fw = new FileWriter(file.toFile(), true);
					fw.write(s+"\n");
					fw.flush();
					fw.close();
				}
				catch (InterruptedException e) {
					System.out.println("File_logger: InterruptedException "+e);
                    return;
				}
                catch (IOException e) {
					System.out.println("File_logger: IOException "+e);
                    return;
				}
			}
		};
		Actor_engine.execute(r, "File logger pump",new Simple_logger());
    }
	//*******************************************************
	@Override
	public void log( boolean also_System_out_println, String s)
	//*******************************************************
	{
		if ( also_System_out_println)
		{
			System.out.println(s);
		}
		queue.add(s);
	}
}
