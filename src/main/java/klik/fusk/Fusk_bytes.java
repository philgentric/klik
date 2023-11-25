package klik.fusk;

import klik.actor.Aborter;
import klik.util.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

//**********************************************************
public class Fusk_bytes
//**********************************************************
{
    private static final String signature_text = "Don't_pay_the_FerryWoman_until_she_brings_you_to_the_other_side";
    private static byte[] signature_clear;
    static byte[] signature_fusk;

    private static volatile boolean initialized = false;
    //**********************************************************
    synchronized static void init(Logger logger)
    //**********************************************************
    {
        if ( !initialized)
        {
            //logger.log(Stack_trace_getter.get_stack_trace("fusk signature initialized as:->"+signature_text+"<-"));
            logger.log("fusk signature initialized");
            signature_clear = signature_text.getBytes(StandardCharsets.UTF_8);
            signature_fusk = fusk(signature_clear, new Aborter());
            initialized = true;
        }
    }

    private static final boolean shorter = true; // when true, maybe a bit faster
    private static final int LIMIT = 1000;

    //**********************************************************
    static byte[] fusk(byte[] in, Aborter aborter)
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
    static byte[] obfusk_and_add_signature(byte[] clear, Logger logger)
    //**********************************************************
    {
        init(logger);
        byte[] obfuscated = new byte[clear.length+signature_fusk.length];
        System.arraycopy(signature_fusk,0,obfuscated,0,signature_fusk.length);
        byte[] fusk = Fusk_bytes.fusk(clear, new Aborter());
        System.arraycopy(fusk,0,obfuscated,signature_fusk.length,fusk.length);
        return obfuscated;
    }

    //**********************************************************
    static byte[] defusk_bytes_and_remove_signature(byte[] obfuscated, Aborter aborter, Logger logger)
    //**********************************************************
    {
        init(logger);
        byte[] fusk = new byte[obfuscated.length-signature_fusk.length];
        // skip signature
        System.arraycopy(obfuscated,signature_fusk.length,fusk,0,fusk.length);
        if ( aborter.should_abort()) return null;
        return fusk(fusk, new Aborter());
    }

    //**********************************************************
    public static boolean check_signature(byte[] obfuscated, Logger logger)
    //**********************************************************
    {
        init(logger);
        if (Arrays.mismatch(obfuscated,signature_fusk) == signature_fusk.length) return true;
        return false;
    }
}
