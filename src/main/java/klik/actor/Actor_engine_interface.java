package klik.actor;

import klik.util.Logger;
import klik.util.execute.Scheduled_thread_pool;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public interface Actor_engine_interface
//**********************************************************
{

    Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger); // message and/or tr may be null
    void cancel_one(Job canceled);
    void stop();
    int how_many_threads_are_in_flight();
    Aborter get_aborter();

    //**********************************************************
    default void cancel_all(ConcurrentLinkedQueue<Job> jobs)
    //**********************************************************
    {
        for ( Job j : jobs)
        {
            cancel_one(j);
        }
    }

    //**********************************************************
    default Job execute_internal(Runnable r,  Logger logger)
    //**********************************************************
    {
        Actor actor = new Actor() {
            @Override
            public String run(Message m) {
                r.run();
                return "DONE";
            }
        };
        Message message = new Message() {
            @Override
            public Aborter get_aborter() {
                return new Aborter("default thread",logger);
            }
        };
        return run(actor,message,null,logger);
    }






    // ticket system

    ConcurrentLinkedDeque<Job> get_jobs();


    //**********************************************************
    default void register_job(Job j, boolean is_high_priority)
    {
        if (is_high_priority) {
            get_jobs().addFirst(j);
        } else {
            get_jobs().addLast(j);
        }
    }
    //**********************************************************
    default void remove_job(Job j)
    {
        if ( !Actor_engine.use_tickets) return;
       // logger.log("removing  for "+j.to_string());
        get_jobs().remove(j);
    }


    static final boolean full_speed = true;
    //**********************************************************
    default void start_injector(Logger logger)
    //**********************************************************
    {
        if ( full_speed)
        {
            Runnable injector = new Runnable() {
                @Override
                public void run() {
                    logger.log("starting ticket injector");
                    for(;;)
                    {
                        int total = 5000;
                        for (Job j : get_jobs()) {
                            if (total == 0)
                            {
                                if ( j.message.get_ticket_queue()!= null) {
                                    if (j.message.get_ticket_queue().peek() == null) {
                                        logger.log("jobs are starving: " + j.to_string());
                                        break;
                                    }
                                }
                            } else {
                                LinkedBlockingQueue<Boolean> ticket_queue = j.message.get_ticket_queue();
                                if (ticket_queue == null) continue;
                                ticket_queue.add(Boolean.valueOf(true));
                                //logger.log("injecting ticket for "+j.to_string());
                                total--;
                            }
                        }
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            logger.log(""+e);
                        }
                    }
                }
            };
            Actor_engine.execute(injector,new Aborter("ticket injector",logger),logger);
        }
        else {
            Runnable injector = new Runnable() {
                @Override
                public void run() {
                    int total = 100;
                    for (Job j : get_jobs()) {
                        if (total == 0) {
                            logger.log("no more  ticket for " + j.to_string());
                        } else {
                            LinkedBlockingQueue<Boolean> ticket_queue = j.message.get_ticket_queue();
                            if (ticket_queue == null) continue;
                            ticket_queue.add(Boolean.valueOf(true));
                            //logger.log("injecting ticket for "+j.to_string());
                            total--;
                        }
                    }
                }
            };
            Scheduled_thread_pool.execute(injector, 1, TimeUnit.MICROSECONDS);
        }
    }

}

