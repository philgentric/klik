package klik.actor;

import klik.util.Logger;
import klik.util.System_out_logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//**********************************************************
public class Test
//**********************************************************
{
    //**********************************************************
    public static void main(String args[])
    //**********************************************************
    {
        Logger logger = new System_out_logger();

        class Message1 implements Message
        {
            String s;
            Aborter aborter = new Aborter();
            public Message1(String s_) {
                s = s_;
            }

            @Override
            public String to_string() {
                return s;
            }

            @Override
            public Aborter get_aborter() {
                return aborter;
            }
        }
        class Actor1 implements Actor
        {
            String actor_name;
            public Actor1(String actor_name_) {
                actor_name = actor_name_;
            }

            @Override
            public String run(Message m) {
                Message1 m1 = (Message1) m;
                System.out.println("Actor "+actor_name+" runs message: "+m1.s);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Actor "+actor_name+" DONE message: "+m1.s);

                return actor_name+" ended: "+m1.s;
            }



        }
        logger.log("actor test");
        Actor a = new Actor1("actor_A");
        Actor b = new Actor1("actor_B");
        Random random = new Random();

        Job_termination_reporter cr = new Job_termination_reporter() {
            @Override
            public void has_ended(String s, Job job) {
                logger.log("has ended:"+s);
            }
        };
        for(int i = 0; i < 1000; i++)
        {
            Message1 m = new Message1("message_"+i);
            Actor ac = null;
            if ( random.nextBoolean()) ac = a;
            else ac = b;

            Job am = Actor_engine.run(ac,m, cr,logger);

        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        Actor_engine.get(logger).stop();
    }
}
