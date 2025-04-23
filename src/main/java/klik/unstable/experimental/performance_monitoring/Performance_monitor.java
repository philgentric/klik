
//SOURCES Record.java
//SOURCES Performance_stage.java





package klik.unstable.experimental.performance_monitoring;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.Browser;
import klik.util.log.Logger;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Performance_monitor
//**********************************************************
{
    private static Performance_monitor instance;

    private final LinkedBlockingQueue<Record> input = new LinkedBlockingQueue<>();
    public final Logger logger;
    public final Aborter aborter;
    private final Comparator<? super Record> comparator;

    Map<String,List<Record>> all_records = new HashMap<>();

    //**********************************************************
    public static Performance_monitor create_performance_monitor(Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            instance = new Performance_monitor(aborter, logger);
        }
        return instance;
    }

    //**********************************************************
    private Performance_monitor(Aborter aborter_, Logger logger_)
    //**********************************************************
    {
        aborter = aborter_;
        logger = logger_;

        comparator = (Comparator<Record>) (o1, o2) -> {
            Long l1 = o1.dur_ms();
            Long l2 = o2.dur_ms();
            return l2.compareTo(l1);
        };

        //Actor_engine.execute(this::monitor_in_a_thread,logger);
        Actor_engine.execute(this::collect_records,logger);
    }

    //**********************************************************
    public static void show(Logger logger)
    //**********************************************************
    {
        create_performance_monitor(Browser.monitoring_aborter,logger);
        Performance_stage.show_stage(logger);
    }

    //**********************************************************
    private void collect_records()
    //**********************************************************
    {
        for(;;)
        {
            Record record;
            try {
                record = input.poll(300, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.log(""+e);
                return;
            }
            if ( aborter.should_abort()) return;
            if ( record == null)
            {
                refresh();
            }
            else
            {
                List<Record> l = all_records.get(record.type());
                if ( l == null)
                {
                    l = new ArrayList<>();
                    all_records.put(record.type(),l);
                }
                l.add(record);
            }

       }
    }

    //**********************************************************
    private void monitor_in_a_thread()
    //**********************************************************
    {
        for(;;)
        {
            long start = System.currentTimeMillis();
            if( ! refresh()) return;
            long sleep = 300 - (System.currentTimeMillis() - start);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //**********************************************************
    private boolean refresh()
    //**********************************************************
    {

        //if ( aborter.should_abort()) return false;
        Performance_stage.clear();

        for (String type : all_records.keySet())
        {
            List<Record> local = all_records.get(type);
            Collections.sort(local,comparator);
            Performance_stage.add(type+":");

            {
                Record r = local.get(0);
                Performance_stage.add("    worse:" + r.dur_ms()+ "ms "+r.tag());
            }
            {
                double ave = 0;
                int count = 0;
                for ( Record r : local)
                {
                    ave += r.dur_ms();
                    count++;
                }
                ave = ave/(double)count;
                Performance_stage.add("  average:" + ave+ "ms ");
            }
            {
                Record r = local.get(local.size()-1);
                Performance_stage.add("     best:" + r.dur_ms()+ "ms "+r.tag());
            }

        }

        return true;
    }

    //**********************************************************
    private void register_new_record_internal(String type, String tag, long dur_ms)
    //**********************************************************
    {
        Record i = new Record(type,tag,dur_ms, UUID.randomUUID().toString());
        input.add(i);
    }


    //**********************************************************
    public static void register_new_record(String type, String tag, long dur_ms, Logger logger)
    //**********************************************************
    {
        if ( instance != null) instance.register_new_record_internal(type,tag, dur_ms);
    }


}
