package klikr.properties;

import klikr.Shared_services;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Store_engine
//**********************************************************
{
    private static final boolean dbg = false;
    private static Store_engine instance;
    private static BlockingQueue<Save_job> disk_store_request_queue;

    //**********************************************************
    public static BlockingQueue<Save_job> get_queue(Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = new Store_engine(logger);
        return disk_store_request_queue;
    }
    //**********************************************************
    private Store_engine(Logger logger)
    //**********************************************************
    {
        disk_store_request_queue = new LinkedBlockingQueue<>();

        Runnable r = () -> {
            for(;;)
            {

                try {
                    Save_job save_job = disk_store_request_queue.poll(20, TimeUnit.SECONDS);
                    if ( save_job == null)
                    {
                        // this is a time out (20 seconds), nothing to save
                        if (Shared_services.aborter().should_abort())
                        {
                            if ( dbg) logger.log("exiting Properties store engine due to abort ");
                            return;
                        }
                        continue;
                    }
                    if (save_job.pm().aborter.should_abort())
                    {
                        if ( dbg) logger.log("Properties store engine not saving: " + save_job.pm().purpose + " " + save_job.pm().the_properties_path);
                        continue;
                    }

                    //logger.log("\n\n\n"+save_job.reload_before_save()+" Properties store engine : " + save_job.pm().purpose + " " + save_job.pm().the_properties_path);

                    save_job.pm().save_everything_to_disk(save_job.reload_before_save());
                }
                catch (InterruptedException e)
                {
                    Shared_services.logger().log("INTERRUPTED Properties store engine");
                    return;
                }
            }
        };
        Actor_engine.execute(r, "Properties_manager store engine",logger);
    }

}
