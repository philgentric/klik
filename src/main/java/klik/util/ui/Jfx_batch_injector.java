package klik.util.ui;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.log.Stack_trace_getter;
import klik.util.log.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// crazy idea:
// rationale: it is not a good idea to call Platform.runLater(()) too often
// here, we BATCH the runnables
// verdict: does not work well enough
// especially it creates mysterious/spurious bugs where the browser does not
// always display the full content !!!

//**********************************************************
public class Jfx_batch_injector
//**********************************************************
{
    private static final boolean enable = false;
    private static final boolean dbg = false;
    private final LinkedBlockingQueue<Runnable> input = new LinkedBlockingQueue<>();

    // if something arrives while the batch is being executed, it will get in the batch
    private final ConcurrentLinkedQueue<Runnable> batch = new ConcurrentLinkedQueue<>();
    private final Logger logger;
    private static Jfx_batch_injector instance;
    private final Aborter aborter;

    //**********************************************************
    public static void inject(Runnable r, Logger logger)
    //**********************************************************
    {
        if ( enable) {
            if (instance == null) instance = new Jfx_batch_injector(logger);
            instance.inject(r);
        }
        else {
            Platform.runLater(r);
        }
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
        input.add(r);
    }

    //**********************************************************
    private Jfx_batch_injector(Logger logger)
    //**********************************************************
    {
        aborter = new Aborter("Jfx_batch_injector",logger);
        this.logger = logger;

        // batch building pump
        Runnable r = () -> {
            Long start =  System.nanoTime();
            for(;;)
            {
                try {
                    Runnable tmp = input.poll(10,TimeUnit.MILLISECONDS);
                    if ( tmp == null)
                    {
                        // this is a timeout, let us do a batch!
                        // this is the maximum time the user will wait for a batch to be executed
                        start = System.nanoTime();
                        run_batch();
                        continue;
                    }
                    // new item to put in the batch
                    batch.add(tmp);
                    long now = System.nanoTime();
                    if (now - start > 10_000_000) // also 10 ms
                    {
                        start = now;
                        run_batch();
                    }
                }
                catch (InterruptedException e) {
                    logger.log(Stack_trace_getter.get_stack_trace("" + e));
                }
            }
        };
        Actor_engine.execute(r,"JFX batch injector pump (experimental)",logger);
    }

    //**********************************************************
    private void run_batch()
    //**********************************************************
    {
        Platform.runLater(()-> run_the_batch_on_Jfx_thread());
    }

    //**********************************************************
    private void run_the_batch_on_Jfx_thread()
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
