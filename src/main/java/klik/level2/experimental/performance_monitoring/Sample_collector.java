package klik.level2.experimental.performance_monitoring;

import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Sample_collector
//**********************************************************
{
    final AtomicLong pixels_produced = new AtomicLong(0);
    final AtomicLong elapsed_ms = new AtomicLong(0);

    //**********************************************************
    public Sample_collector()
    //**********************************************************
    {
    }

    //**********************************************************
    public synchronized void add_sample(long elapsed,long pixels)
    //**********************************************************
    {
        System.out.println("adding "+pixels);
        pixels_produced.addAndGet(pixels);
        elapsed_ms.addAndGet(elapsed);
    }

    //**********************************************************
    public double get_Mpixel_per_second()
    //**********************************************************
    {
        return (double)pixels_produced.get()/elapsed_ms.get()/1000.0;
    }
}
