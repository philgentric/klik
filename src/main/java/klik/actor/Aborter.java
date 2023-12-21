package klik.actor;


import java.util.concurrent.atomic.AtomicBoolean;

// a container for aborting background tasks
//**********************************************************
public class Aborter
//**********************************************************
{
    private AtomicBoolean abort = new AtomicBoolean(false);

    //**********************************************************
    public void abort()
    //**********************************************************
    {
        abort.set(true);
    }
    //**********************************************************
    public boolean should_abort()
    //**********************************************************
    {
        return abort.get();
    }
}
