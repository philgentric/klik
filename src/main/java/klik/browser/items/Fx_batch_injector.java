package klik.browser.items;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

// rationale: it is not a good idea to call
// Platform.runLater(()) too often
// so, we BATCH it here

//**********************************************************
public class Fx_batch_injector
//**********************************************************
{
    public LinkedBlockingDeque<Runnable> input = new LinkedBlockingDeque<>();
   // LinkedBlockingDeque<Item_image_target> input = new LinkedBlockingDeque<>();
    Logger logger;
    //**********************************************************
    public Fx_batch_injector(Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Long start =  System.nanoTime();
                for(;;)
                {
                    if ( aborter.should_abort()) return;
                    try {
                        Runnable tmp = input.poll(1,TimeUnit.SECONDS);
                        if ( tmp != null) input.addFirst(tmp);
                        long now = System.nanoTime();
                        if (now-start >  10_000_000) // 10 milliseconds
                        {
                            send_batch(aborter);
                            start = now;
                        }
                    } catch (InterruptedException e) {
                        logger.log(Stack_trace_getter.get_stack_trace(""+e));
                    }
                }
            }
        };

        Actor_engine.execute(r,aborter,logger);

    }

    //**********************************************************
    private void send_batch(Aborter aborter)
    //**********************************************************
    {
        if ( input.size() == 0) return;
        Platform.runLater(()->{
            logger.log("batch size was: "+do_it_in_fx_thread(aborter));
        });
    }

    //**********************************************************
    private int do_it_in_fx_thread(Aborter aborter)
    //**********************************************************
    {
        //logger.log("batch size = "+input.size());
        int count = 0;
        for ( ;;)
        {
            if ( aborter.should_abort()) return count;
            try
            {
                Runnable r = input.pollLast(1, TimeUnit.MICROSECONDS);
                if (r == null )
                {
                    // dont hold the fx thread too long
                    return count;
                }
                r.run();
                count++;
            }
            catch (InterruptedException e)
            {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
            }
        }

    }
}
