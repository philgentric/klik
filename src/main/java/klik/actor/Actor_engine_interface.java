package klik.actor;

import javafx.application.Platform;
import klik.util.log.Logger;
import klik.util.ui.Text_frame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

//**********************************************************
public interface Actor_engine_interface
//**********************************************************
{
    Job run(Actor actor, Message message, Job_termination_reporter tr, Logger logger); // error_message and/or tr may be null
    void cancel_job(Job canceled);
    void stop();

    LinkedBlockingQueue<Job> get_job_queue();

    //**********************************************************
    default void list_jobs(Logger logger)
    //**********************************************************
    {
        List<String> job_list = new ArrayList<>();
        for ( Job job : get_job_queue())
        {
            logger.log("Job: "+job.to_string());
            job_list.add(job.to_string());
        }
        Platform.runLater(()->Text_frame.show(job_list, logger));
    }

    //**********************************************************
    default void cancel_all(ConcurrentLinkedQueue<Job> jobs)
    //**********************************************************
    {
        for ( Job j : jobs)  cancel_job(j);
    }

    //**********************************************************
    default Job execute_internal(Runnable r,  String tracker, Logger logger)
    //**********************************************************
    {
        Actor actor = new Actor() {
            //**********************************************************
            @Override
            public String run(Message m)
            //**********************************************************
            {
                r.run();
                return "DONE";
            }

            //**********************************************************
            @Override
            public String name()
            //**********************************************************
            {
                return "(Pseudo) actor for: "+tracker;
            }

        };
        Message message = new Message() {
            @Override
            public String to_string() {
                return ""; //this is a dummy
            }

            @Override
            public Aborter get_aborter() {
                return new Aborter("default thread",logger);
            }
        };


        return run(actor,message,null,logger);
    }





}

