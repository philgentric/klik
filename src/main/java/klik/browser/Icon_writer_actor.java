package klik.browser;

//import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import klik.util.Logger;
import klik.util.Tool_box;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

/*
 * this actor aceepts icons and writes them to the disk cache
 */

//**********************************************************
public class Icon_writer_actor implements Runnable
//**********************************************************
{
	private static final boolean dbg = false;
	Path cache_dir;
	LinkedBlockingQueue<Icon_write_message> queue = new LinkedBlockingQueue<>();
	Logger logger;
	//**********************************************************
	public Icon_writer_actor(Path cache_dir_,
							 Logger l)
	//**********************************************************
	{
		logger = l;
		if ( dbg) logger.log("Icon_writer_actor created");
		cache_dir = cache_dir_;
	}

	//**********************************************************
	public void push(Icon_write_message ii)
	//**********************************************************
	{
		queue.add(ii);
	}

	//**********************************************************
	@Override
	public void run() 
	//**********************************************************
	{
		//logger.log("Icon_writer starts!");
		for(;;)
		{
			try 
			{
				//logger.log("Icon_writer take or block");
				Icon_write_message iwm = queue.take();
				//logger.log("Icon_writer request received!");

				if ( iwm.image == null)
				{
					logger.log("WTF??? "+iwm.original_path+" image is null ??");
					return;
				}

				write1(iwm);
				//logger.log("Icon_writer request finished!");
			}
			catch (InterruptedException e) 
			{
				logger.log("Icon_writer exception (2) ="+e);
				return;
			}
		}
	}

	private void write1(Icon_write_message iwm)
	{
		try
		{
//			BufferedImage bim = SwingFXUtils.fromFXImage(iwm.image, null);
			BufferedImage bim = JavaFX_to_Swing.fromFXImage(iwm.image, null, logger);
			if ( bim == null)
			{
				logger.log("WTF??? "+iwm.original_path+" image is null ??");
				return;
			}
			boolean status = ImageIO.write(bim, "png", new File(
					cache_dir.toFile(),
					Tool_box.MAKE_CACHE_NAME(iwm.original_path,(int)iwm.icon_size))
			);

			if ( status == false)
			{
				logger.log("Icon_writer: ImageIO.write returns false for: "+iwm.original_path);
			}
		}
		catch(Exception e)
		{
			logger.log("Icon_writer exception (1) ="+e+" path="+iwm.original_path);
		}
		//logger.log("Icon_writer OK for path="+iwm.original_path+" png done");
	}

	/*
	this writes a raw pixel dump ... which would require the symetric reader
	that we do not have?
	 */
	private void write2(Icon_write_message iwm)
	{
		Image img = iwm.image;
		int width = (int) img.getWidth();
		int height = (int) img.getHeight();
		PixelReader reader = img.getPixelReader();
		byte[] buffer = new byte[width * height * 4];
		WritablePixelFormat<ByteBuffer> format = PixelFormat.getByteBgraInstance();
		reader.getPixels(0, 0, width, height, format, buffer, 0, width * 4);
		try {
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(
							new File(
					cache_dir.toFile(),
					Tool_box.MAKE_CACHE_NAME(iwm.original_path,(int)iwm.icon_size))
					)
			);
			for(int count = 0; count < buffer.length; count += 4) {
				out.write(buffer[count + 2]);
				out.write(buffer[count + 1]);
				out.write(buffer[count]);
				out.write(buffer[count + 3]);
			}
			out.flush();
			out.close();
		}
		catch(IOException e)
		{
			logger.log("Icon_writer exception (2) ="+e+" path="+iwm.original_path);
		}
	}


	//**********************************************************
	public static Icon_writer_actor launch_icon_writer(Path cache_dir,  Logger logger)
	//**********************************************************
	{
		Icon_writer_actor icon_writer = new Icon_writer_actor(cache_dir,logger );
		try
		{
			if ( dbg) logger.log("the_Executor_service execute before icon writer");
			Tool_box.execute(icon_writer,logger);
			if ( dbg) logger.log("the_Executor_service execute after icon writer");
		}
		catch (RejectedExecutionException e)
		{
			logger.log("Icon_source icon writer execute failed "+e);
		}
		catch (Exception e)
		{
			logger.log("Icon_source icon writer execute failed 2 "+e);
		}
		return icon_writer;
	}

    public void die() {
		push(new Icon_write_message(null,0, null));
	}
}
