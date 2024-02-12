package klik.browser.items;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

// rationale: it is not a good idea to call
// Platform.runLater(()) too often e.g. once per icon
// so, we BATCH it here

//**********************************************************
public class Fx_batch_injector
//**********************************************************
{
    LinkedBlockingDeque<Item_image_target> input = new LinkedBlockingDeque<>();
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
                        Item_image_target tmp = input.poll(1,TimeUnit.SECONDS);
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
                Item_image_target item_image_target = input.pollLast(1, TimeUnit.MICROSECONDS);
                if (item_image_target == null )
                {
                    // dont hold the fx thread too long
                    return count;
                }
                item_image_target.target().do_it_in_fx_thread(item_image_target.payload());
                count++;
            }
            catch (InterruptedException e)
            {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
            }
        }

    }
}
