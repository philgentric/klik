package klik.images;

import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.Tool_box;

import java.util.concurrent.LinkedBlockingQueue;

public class Decoded_image_engine
{
    private static final boolean ultradbg = false;
    public static final int FORWARD_PRELOAD_SIZE = 10;
    public static final int CACHE_SIZE = 2*FORWARD_PRELOAD_SIZE;
    Logger logger;
    private LinkedBlockingQueue<Image_decode_request> request = new LinkedBlockingQueue<Image_decode_request>();


    public Decoded_image_engine(Logger logger_)
    {
        logger = logger_;
        int n_thread = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < n_thread ; i++)
        {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    for(;;)
                    {
                        try {
                            Image_decode_request r = request.take();
                            if ( ultradbg) logger.log("decode request:"+r.get_string());

                            String key = r.make_key();
                            if ( r.cache.get(key) != null)
                            {
                                logger.log("Already in cache:"+key);
                                continue;
                            }

                            // this is the expensive operation:
                            Image_and_index local = r.image_file_source.get_Image_and_index(r.index, r.high_quality, r.target_width);
                            if (local != null)
                            {
                                // OutOfMemory can manisfest either as an exception, and then we get a null
                                // or the image is of size zero (!)
                                if ( (local.ic.image.getWidth() > 1) && (local.ic.image.getHeight() > 1))
                                {
                                    Object o = r.cache.put(r.make_key(), local);
                                    /*
                                    if ( o == null)
                                    {
                                        r.to_be_removed.put(r);
                                    }
                                    else
                                    {
                                        // if was already in cache no need to try to delete twice (or more!)
                                    }*/
                                    logger.log(r.cache.size()+" in cache,new:" + r.get_string() );
                                }
                                else
                                {
                                    if ( local.ic.image_is_damaged == false) {
                                        clear_image_cache(r, "bad image size while warming the cache:" + local.ic.path.getFileName().toString() + local.ic.image.getWidth() + "x" + local.ic.image.getHeight());
                                    }
                                }
                            }
                            else
                            {
                                clear_image_cache(r,"null image size while warming the cache:");
                            }

                        } catch (InterruptedException e) {
                            logger.log("DECODER DEAD");
                            logger.log(Stack_trace_getter.get_stack_trace(""+e));
                            return;
                        }
                    }
                }
            };
            Tool_box.execute(r,logger);
        }
    }



    public void inject(Image_decode_request r)
    {
        try {
            request.put(r);
        } catch (InterruptedException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
    }
    private void clear_image_cache(Image_decode_request r, String error)
    {
        logger.log("oops "+error+", outOfMemory suspected, clearing image cache");
        r.cache.clear();
    }


}
