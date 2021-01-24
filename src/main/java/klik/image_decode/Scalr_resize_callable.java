package klik.image_decode;

import java.awt.Cursor;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import klik.scaler.Scalr;

//**********************************************************
public class Scalr_resize_callable implements Callable<BufferedImage>
//**********************************************************
{
	private static final boolean debug_flag = false;
	private final Path path;
	private BufferedImage source;
	private int target_image_width;
	private int target_image_height;
	private Scalr.Method method;

	//**********************************************************
	public Scalr_resize_callable(BufferedImage source_, int w, int h, Scalr.Method method_, Path image_File_to_be_loaded2)
	//**********************************************************
	{
		//the_customer = the_customer_;
		source = source_;
		target_image_width = w;
		target_image_height = h;
		method = method_;
		path = image_File_to_be_loaded2;
	}

	//**********************************************************
	@Override
	public BufferedImage call() throws Exception
	//**********************************************************
	{
		if ( source == null)
		{
			System.out.println("BufferedImage call() fails =  source is null");
			return null;
		}
		if ( debug_flag) System.out.println("BufferedImage call()");
		BufferedImage the_generated_image = null;
		Cursor cc = null;
		try
		{
			// AAA cc = the_customer.getCursor();
			// AAA the_customer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
			if ( debug_flag) System.out.println("--------------------starting call for rescaling-------------"+method);
			// this is the slow call...
			the_generated_image = Scalr_encapsulation.rescale_with_scalr(
					source,
					method,
					target_image_width,
					target_image_height);
			if ( debug_flag) System.out.println("---------------------ending call for rescaling---------------");
		} 
		finally 
		{
			//the_customer.setCursor(Cursor.getDefaultCursor()); this HANGS !!!!
			if ( cc != null) 
			{
				//System.out.println("---------------------restoring cursor---------------");
				// AAA  the_customer.get_ready_to_restore_cursor(cc);
				//System.out.println("---------------------restoring cursor... done---------------");
			}
		}
		
		if ( debug_flag) System.out.println("BufferedImage call() end ");
		return the_generated_image;
	}

}
