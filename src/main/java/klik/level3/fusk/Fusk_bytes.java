//SOURCES ./Pin_code_client.java
//SOURCES ./Pin_code_getter_stage.java
package klik.level3.fusk;

import klik.actor.Aborter;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Fusk_bytes implements Pin_code_client
//**********************************************************
{
    private static Fusk_bytes instance = null;
    private static final String signature_text = "Don't_pay_the_FerryWoman_until_she_brings_you_to_the_other_side";
    private static byte[] signature_clear;
    static byte[] signature_fusk;
    private static AtomicBoolean initialized = new AtomicBoolean(false);
    private static String pin_code = null;
    public final Logger logger;
    public final Aborter aborter;

    //**********************************************************
    private Fusk_bytes(Aborter aborter,Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        logger = logger_;
    }

    //**********************************************************
    public static boolean is_initialized()
    //**********************************************************
    {
        return initialized.get();
    }


    /*
    initialization is super complicated:
    0. someone needs to fusk or defusk something, but there is no instance
    1. creates an instance, and ask the user for the pin
    ... while the user is entering the pin (which can take forever) ... still not usable
    2. user is finished entering the pin, set_code_pin is called asynchronously, we have the pin!
    3. someone tries again, init() is called and -this time- succeeds

    to get a new instance one must call RESET
     */
    //**********************************************************
    synchronized public static boolean initialize(Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( is_initialized())
        {
            logger.log("already initialized");
            return true;
        }
        if ( instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("creating instance"));
            instance = new Fusk_bytes(aborter,logger);
        }
        return instance.init(aborter, logger);
    }

    //**********************************************************
    public static void reset(Logger logger)
    //**********************************************************
    {
        initialized.set(false);
        instance = null;
        pin_code = null;
        logger.log("fusk reset done");

    }


    //**********************************************************
    @Override
    public void set_pin_code(String new_pin_code)
    //**********************************************************
    {
        pin_code = new_pin_code;
        logger.log("set_pin_code->"+new_pin_code+"<-");
        init(aborter,logger);
    }
    //**********************************************************
    private synchronized boolean init(Aborter aborter,Logger logger)
    //**********************************************************
    {

        if ( is_initialized())
        {
            logger.log("already initialized");
            return true;
        }
        if ( pin_code == null)
        {
            logger.log("getting pin code from user");

            Pin_code_getter_stage pin_code_getter_stage = new Pin_code_getter_stage(aborter,logger);
            pin_code_getter_stage.ask_pin_code_in_a_thread(this,logger);
            return false; // not ready yet
        }

        //logger.log(Stack_trace_getter.get_stack_trace("fusk signature initialized as:->"+signature_text+"<-"));
        String local = pin_code+signature_text;
        signature_clear = local.getBytes(StandardCharsets.UTF_8);
        signature_fusk = fusk(signature_clear);
        initialized.set(true);
        logger.log("fusk signature initialized: "+signature_clear.length+" bytes string ="+local);
        return true;
    }



    private static final boolean shorter = true; // when true, maybe a bit faster
    private static final int LIMIT = 200;

    //**********************************************************
    static byte[] fusk(byte[] in)
    //**********************************************************
    {

        byte[] out = new byte[in.length];
         if ( shorter & in.length>LIMIT )
         {
             int j = 0;
             for (int i = 0; i < LIMIT; i++) {
                 out[i] = (byte) (in[i] ^ signature_clear[j]);
                 j++;
                 if (j >= signature_clear.length) j = 0;
             }
             System.arraycopy(in,LIMIT,out,LIMIT,in.length-LIMIT);
         }
         else
         {
             int j = 0;
             for (int i = 0; i < in.length; i++) {
                 out[i] = (byte) (in[i] ^ signature_clear[j]);
                 j++;
                 if (j >= signature_clear.length) j = 0;
             }
         }
         return out;
    }

    //**********************************************************
    static byte[] obfusk_and_add_signature(byte[] clear, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (check(aborter,logger)) return null;

        byte[] obfuscated = new byte[clear.length+signature_fusk.length];
        System.arraycopy(signature_fusk,0,obfuscated,0,signature_fusk.length);
        byte[] fusk = Fusk_bytes.fusk(clear);
        System.arraycopy(fusk,0,obfuscated,signature_fusk.length,fusk.length);
        return obfuscated;
    }


    //**********************************************************
    static byte[] defusk_bytes_and_remove_signature(byte[] obfuscated, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (check(aborter,logger)) return null;

        byte[] fusk = new byte[obfuscated.length-signature_fusk.length];
        // skip signature
        System.arraycopy(obfuscated,signature_fusk.length,fusk,0,fusk.length);
        if ( aborter.should_abort()) return null;
        return fusk(fusk);
    }

    //**********************************************************
    public static boolean check_signature(byte[] obfuscated, Aborter aborter,Logger logger)
    //**********************************************************
    {
        if (check(aborter,logger)) return false;

        if (Arrays.mismatch(obfuscated,signature_fusk) == signature_fusk.length) return true;
        return false;
    }

    //**********************************************************
    private static boolean check(Aborter aborter,Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            initialize(aborter,logger);
            return true;
        }
        if( !is_initialized()) return true;
        return false;
    }

}
