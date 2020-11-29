package klik.images;

import klik.util.Logger;
import klik.util.Tool_box;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Image_cache
{
    private static final boolean ultradbg = false;
    Logger logger;
    public LinkedBlockingQueue<String> requested;
    private ConcurrentHashMap<String, Image_and_index> cache;


    public Image_cache(Logger logger_)
    {
        requested = new LinkedBlockingQueue<String>();
        cache = new ConcurrentHashMap<String, Image_and_index>();
        logger =logger_;
    }
    public Image_and_index get(String key)
    {
        return cache.get(key);
    }

    public void clear() {
        cache.clear();
    }

    public void preload(int k, boolean ultimate, boolean forward, Image_file_source image_file_source)
    {
        int increment = -1;
        if ( forward) increment = 1;
        int how_many_preload_to_request = Decoded_image_engine.FORWARD_PRELOAD_SIZE;

        if ( ultradbg) logger.log("preloading target: "+how_many_preload_to_request);
        final int[] kk = new int[how_many_preload_to_request];
        kk[0] = image_file_source.check_index(k + increment, ultimate);

        for (int i = 1; i < kk.length; i++)
        {
            kk[i] = image_file_source.check_index(kk[i - 1] + increment, ultimate);
        }
        for (int i : kk)
        {
            Image_decode_request idr = new Image_decode_request(i, image_file_source, cache);
            if ( ultradbg) logger.log("preloading request: "+idr.get_string());
            String skey = idr.make_key();
            if ( requested.contains(skey)) continue;
            try {
                requested.put(skey);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Tool_box.inject_image_decode_request(idr, logger);

        }
        if ( ultradbg) logger.log("preloading request END ");

        // perform some clean up

        check_decoded_image_cache_size(k,image_file_source,logger);


    }

    public void check_decoded_image_cache_size(int current_index, Image_file_source image_file_source,Logger logger)
    {
        if (cache.size() <= Decoded_image_engine.CACHE_SIZE)
        {
            logger.log("No need to evict because cache size is now:"+ cache.size());
            return;
        }

        for ( Map.Entry e : cache.entrySet())
        {
            String key = (String) e.getKey();
            Image_and_index iai = (Image_and_index) e.getValue();
            int possible_deletion_target = iai.index;
            int distance = distance(possible_deletion_target,current_index,image_file_source);
            if ( distance > Decoded_image_engine.CACHE_SIZE )
            {
                Object r = cache.remove(key);
                if ( r != null)
                {
                    logger.log("       Evicted:"+possible_deletion_target+", distance ="+distance+ " from this:"+ current_index+ " "+iai.ic.path.toAbsolutePath());
                    if ( cache.size() <= Decoded_image_engine.CACHE_SIZE) return;
                }
                else
                {
                    logger.log("WTF? wrong key :"+possible_deletion_target+", distance ="+distance+ " from this: "+ current_index+ " "+iai.ic.path.toAbsolutePath());
                    return;
                }
            }
            else
            {
                if ( ultradbg) logger.log("       NOT evicted:"+possible_deletion_target+", distance ="+distance+ " from this: "+ current_index+ " "+iai.ic.path.toAbsolutePath());

            }
        }

    }

                /*
            max = 5000
            current = 4990
            possible_deletion_target = 3
            max - current = 10
            distance is 12, return false
             */

    private int distance(int possible_deletion_target, int current, Image_file_source image_file_source) {
        int distance1 = current - possible_deletion_target;
        if ( distance1 < 0) distance1 = -distance1;

        int max = image_file_source.how_many_images();
        int distance2 =  max-current+ possible_deletion_target;
        if ( distance1 < distance2) return distance1;
        else return distance2;

    }

}
