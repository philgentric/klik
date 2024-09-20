package klik.util.Performance_monitor;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.log.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Performance_monitor
//**********************************************************
{
    private static Performance_monitor instance;

    private final LinkedBlockingQueue<End_record> input = new LinkedBlockingQueue<>();
    public final Logger logger;
    public final Aborter aborter;
    Map<String, Performance_monitor_span> monitored = new HashMap<>();
    List<End_record> statistics_of_ended_spans = new ArrayList<>();

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
        Actor_engine.execute(this::monitor_in_a_thread,logger);
        Actor_engine.execute(this::record_ends_in_a_thread,logger);
    }

    //**********************************************************
    private void record_ends_in_a_thread()
    //**********************************************************
    {
        for(;;)
        {
            End_record end_record;
            try {
                end_record = input.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.log(""+e);
                return;
            }
            if ( aborter.should_abort()) return;
            if ( end_record != null)
            {
                monitored.remove(end_record.uuid());
                statistics_of_ended_spans.add(end_record);
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
            if( ! check_all_items()) return;
            long sleep = 300 - (System.currentTimeMillis() - start);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //**********************************************************
    private boolean check_all_items()
    //**********************************************************
    {
        if ( aborter.should_abort()) return false;
        for ( Map.Entry<String,Performance_monitor_span> e : monitored.entrySet())
        {
            Performance_monitor_span performance_monitor_span = e.getValue();
            performance_monitor_span.report_possible_issue();
        }

        return true;
    }

    //**********************************************************
    private Monitoring_ends_reporter register_new_span_internal(String name, long limit_ms)
    //**********************************************************
    {

        Performance_monitor_span performance_monitor_span = new Performance_monitor_span(name,limit_ms, logger);
        monitored.put(performance_monitor_span.uuid,performance_monitor_span);

        return () -> {
            End_record i = new End_record(name,performance_monitor_span.duration(),performance_monitor_span.uuid);
            input.add(i);
        };
    }


    //**********************************************************
    public static Monitoring_ends_reporter register_new_span(String task_name, long limit_ms, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            logger.log("Performance_monitor, WARNING : you must create an instance first");
            return null;
        }
        return instance.register_new_span_internal(task_name, limit_ms);
    }

    //**********************************************************
    public static void report(Logger logger)
    //**********************************************************
    {
        if (instance == null) {
            logger.log("Performance_monitor, WARNING : you must create an instance first");
            return;
        }
        instance.report_internal();
    }

    //**********************************************************
    private void report_internal()
    //**********************************************************
    {
        for ( End_record end_record : instance.statistics_of_ended_spans)
        {
            logger.log(end_record.to_string());
        }
    }

}
