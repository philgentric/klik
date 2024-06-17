package klik.util;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.actor.Actor_engine;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// rationale: it is not a good idea to call
// Platform.runLater(()) too often
// so, we BATCH it here

//**********************************************************
public class Fx_batch_injector
//**********************************************************
{
    private static final boolean enable = true;
    private static final boolean dbg = false;
    private final LinkedBlockingQueue<Runnable> input = new LinkedBlockingQueue<>();
    private final ConcurrentLinkedQueue<Runnable> batch = new ConcurrentLinkedQueue<>();
    private final Logger logger;
    private static Fx_batch_injector instance;
    private final Aborter aborter;

    //**********************************************************
    public static void inject(Runnable r, Logger logger)
    //**********************************************************
    {
        if (instance == null) instance = new Fx_batch_injector(logger);
        instance.inject(r);
    }

    //**********************************************************
    public static void now(Runnable r)
    //**********************************************************
    {
        Platform.runLater(r);
    }

    //**********************************************************
    private void inject(Runnable r)
    //**********************************************************
    {
        if ( enable)
        {
            input.add(r);
        }
        else
        {
            Platform.runLater(r);
        }
    }

    //**********************************************************
    private Fx_batch_injector( Logger logger)
    //**********************************************************
    {
        aborter = new Aborter("Fx_batch_injector",logger);
        this.logger = logger;

        // batch building pump
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Long start =  System.nanoTime();
                for(;;)
                {
                    try {
                        Runnable tmp = input.poll(10,TimeUnit.MILLISECONDS);
                        if ( tmp == null)
                        {
                            start = System.nanoTime();
                            run_batch();
                            continue;
                        }
                        batch.add(tmp);
                        long now = System.nanoTime();
                        if (now - start > 10_000_000)
                        {
                            start = now;
                            run_batch();
                        }
                    }
                    catch (InterruptedException e) {
                        logger.log(Stack_trace_getter.get_stack_trace("" + e));
                    }
                }
            }
        };
        Actor_engine.execute(r,logger);
    }

    //**********************************************************
    private void run_batch()
    //**********************************************************
    {
        Platform.runLater(()-> run_the_batch_in_fx_thread());
    }

    //**********************************************************
    private void run_the_batch_in_fx_thread()
    //**********************************************************
    {
        int count = 0;
        for (;;)
        {
            Runnable to_be_injected = batch.poll();
            if (to_be_injected == null )
            {
                if ( dbg) if ( count > 0) logger.log("FX injector EMPTY batch, after "+count+" done ");
                return;
            }
            to_be_injected.run();
            count++;
        }
    }
}
