package klikr.util.execute.actor;


// this allows to NOT CATCH exception in the runnables
// e.g. launched by the 'execute' framework
// and then the exception CAN be catched in the framework
// so that you get WHERE the thread/actor was started from
// unless of getting a stack trace that ends in the
// java thread source code ....
public interface My_Runnable
{
    public void run() throws Exception;
}
