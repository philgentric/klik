package klik.image_decode;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import klik.scaler.Scalr;



/*
 * an image factory that uses Scalr as resizer
 */

//**********************************************************
public class Scalr_image_factory
//**********************************************************
{

	private static final boolean debug_flag = true;
	protected static Future<BufferedImage> promise = null;
	protected static ExecutorService threadpool = Executors.newFixedThreadPool(10);

	//**********************************************************
	public static void queue_for_resize(BufferedImage the_current_image, int width, int height, Scalr.Method method, Path path)
	//**********************************************************
	{
			reset_promise();

			promise = threadpool.submit(
					new Scalr_resize_callable(
							the_current_image,
							width,
							height,
							method,
							path));
			if ( debug_flag) System.out.println("SLOW SCALR RESCALE deferred to separate thread ->"+method+"<-");

	}


	public static Future<BufferedImage> get_promise()
	{
		return promise;
	}

	public static void reset_promise()
	{
		if ( promise != null)
		{
			System.out.println("CANCELLING previous rescale thread");
			promise.cancel(true);
		}
		promise = null;
	}





}
