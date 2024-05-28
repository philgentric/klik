package klik.util;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.change.history.History_auto_clean;

//**********************************************************
public class Monitor
//**********************************************************
{

    public final Aborter aborter;
    public final Logger logger;
    private final Disk_usage_monitor disk_usage_monitor;
    private final Cache_auto_clean cache_auto_clean;
    private final History_auto_clean history_auto_clean;

    //**********************************************************
    public Monitor(Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.aborter = aborter;
        this.logger = logger;
        disk_usage_monitor = new Disk_usage_monitor(null, aborter, logger);
        cache_auto_clean = new Cache_auto_clean(null, aborter, logger);
        history_auto_clean = new History_auto_clean(aborter, logger);
    }

    //**********************************************************
    public void start()
    //**********************************************************
    {
        Runnable r = () -> {
            for(;;)
            {
                if ( aborter.should_abort())
                {
                    logger.log("All 3 Monitors aborted");
                    return;
                }

                try {
                    Thread.sleep(10*60*1000);
                } catch (InterruptedException e) {
                    logger.log(""+e);
                }

                if ( !disk_usage_monitor.monitor()) break;
                if ( !cache_auto_clean.monitor()) break;
                if ( !history_auto_clean.monitor()) break;


            }
        };
        Actor_engine.execute(r,logger);

    }
}
