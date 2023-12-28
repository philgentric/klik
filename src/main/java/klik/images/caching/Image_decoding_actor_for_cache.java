package klik.images.caching;

import klik.actor.Actor;
import klik.actor.Message;
import klik.files_and_paths.Guess_file_type;
import klik.images.Image_context;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

//**********************************************************
public class Image_decoding_actor_for_cache implements Actor
//**********************************************************
{
    private static final boolean dbg = false;
    Logger logger;


    //**********************************************************
    public Image_decoding_actor_for_cache(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Image_decode_request_for_cache request = (Image_decode_request_for_cache) m;
        if (dbg) logger.log("decode request:"+request.get_string());

        String key = request.make_key();
        if ( request.cache.get(key) != null)
        {
            if (dbg)
                logger.log("NOT decoding because image found in cache:"+key);
            return"found in cache";
        }

        if ( m.get_aborter().should_abort()) return "aborted";

        // this is the expensive operation:
        Image_context image_context = Image_context.get_Image_context(request.path,request.aborter, logger);
        if (image_context != null)
        {
            // OutOfMemory can manisfest either as an exception, and then we get a null
            // or the image is of size zero (!)
            if ( (image_context.image.getWidth() > 1) && (image_context.image.getHeight() > 1))
            {
                Object o = request.cache.put(request.make_key(), image_context);
                if (dbg) logger.log("image decoded ok is now in cache: " + image_context.path.getFileName() );
            }
            else
            {
                if (!Guess_file_type.ignore(image_context.path))
                {
                    logger.log( Stack_trace_getter.get_stack_trace(image_context.path.getFileName().toString()
                            +" BADBADBAD weird image ?"+image_context.image.getWidth() +"x"+image_context.image.getHeight()));
                }
            }
        }
        else
        {
            logger.log( Stack_trace_getter.get_stack_trace("BADBADBAD get_Image_and_index failed"));
        }
        return "OK, image decoded";
    }

}
