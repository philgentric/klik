package klik.util;

import klik.actor.Aborter;
import klik.change.history.History_auto_clean;

public class Monitor {

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
        disk_usage_monitor = new Disk_usage_monitor(aborter, logger);
        cache_auto_clean = new Cache_auto_clean(aborter, logger);
        history_auto_clean = new History_auto_clean(aborter, logger);
    }

    //**********************************************************
    public void start()
    //**********************************************************
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    if ( aborter.should_abort()) return;

                    if ( !disk_usage_monitor.monitor()) break;
                    if ( !cache_auto_clean.monitor()) break;
                    if ( !history_auto_clean.monitor()) break;

                    try {
                        Thread.sleep(10*60*1000);
                    } catch (InterruptedException e) {
                        logger.log(""+e);
                    }
                }
            }
        };
        Threads.execute(r,logger);

    }
}
