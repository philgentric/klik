// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Disk_usage_monitor.java
//SOURCES ./Cache_auto_clean.java
package klik.util.cache_auto_clean;

import klik.Window_provider;
import klik.util.execute.actor.Actor_engine;
import klik.Shared_services;
import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;
import klik.util.log.Logger;

//**********************************************************
public class Disk_usage_and_caches_monitor
//**********************************************************
{
    public final Logger logger;
    private final Disk_usage_monitor disk_usage_monitor;
    private final Cache_auto_clean cache_auto_clean;

    //**********************************************************
    public Disk_usage_and_caches_monitor(Window_provider window_provider, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        disk_usage_monitor = new Disk_usage_monitor(window_provider.get_owner(), logger);
        if (Booleans.get_boolean(Feature.Enable_auto_purge_disk_caches.name())) cache_auto_clean = new Cache_auto_clean(window_provider.get_owner(), logger);
        else cache_auto_clean = null;
        //history_auto_clean = new History_auto_clean(logger);
    }

    //**********************************************************
    public void start()
    //**********************************************************
    {
        Runnable r = () -> {
            for(;;)
            {
                if ( Shared_services.aborter().should_abort())
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
                if (cache_auto_clean!= null)
                {
                    if ( !cache_auto_clean.monitor()) break;
                }
                //if ( !history_auto_clean.monitor()) break;


            }
        };
        Actor_engine.execute(r,"Cache auto clean",logger);

    }
}
