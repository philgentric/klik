package klik.actor;

import java.util.concurrent.LinkedBlockingQueue;

//**********************************************************
public interface Message
//**********************************************************
{
    default String to_string() {return this.getClass().getName();}

    Aborter get_aborter(); // only one instance per Message = job, cannot be null

    default LinkedBlockingQueue<Boolean>  get_ticket_queue() {return null;}

    default boolean is_high_priority(){return true;};
}
