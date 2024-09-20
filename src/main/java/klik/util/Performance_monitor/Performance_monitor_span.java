package klik.util.Performance_monitor;

import klik.util.log.Logger;

import java.time.LocalDateTime;
import java.util.UUID;

//**********************************************************
public class Performance_monitor_span
//**********************************************************
{
    Logger logger;
    public final long start;
    public final long limit;
    public final LocalDateTime local_date_time;
    public final String task_name;
    public final String uuid;
    public long last = -1L;
    //**********************************************************
    public Performance_monitor_span(String task_name_, long limit_, Logger logger_)
    //**********************************************************
    {
        limit = limit_;
        task_name = task_name_;
        logger = logger_;
        start = System.currentTimeMillis();
        local_date_time = LocalDateTime.now();
        uuid = UUID.randomUUID().toString();
    }

    //**********************************************************
    void report_possible_issue()
    //**********************************************************
    {
        long now = System.currentTimeMillis();
        if ( now-start > limit)
        {
            if (( last < 0) || ( now-last > 300))
            {
                logger.log(task_name+" takes an unexpectedly long time: "+(now-start)+" ms > "+limit);
            }
        }
        last = now;
    }

    //**********************************************************
    public long duration()
    //**********************************************************
    {
        return last-start;
    }
}
