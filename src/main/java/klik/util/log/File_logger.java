//SOURCES ./Simple_logger.java
package klik.util.log;

import klik.actor.Actor_engine;
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
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd:HHmmss");
		String tag = tag_+"_"+now.format(dtf);
		Path folder = Non_booleans_properties.get_trash_dir(Path.of("."), null,new Simple_logger());
		Path file = folder.resolve(tag+".txt");


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
					System.out.println("File_logger: ERROR writing to file: "+e);
				}catch (IOException e) {
					System.out.println("File_logger: ERROR writing to file: "+e);
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
