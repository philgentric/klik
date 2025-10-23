package klik.util.execute.actor;

/*
    An actor is a kind of "factory"
    which code will work asynchronously
    with data passed in a Message
*/

//**********************************************************
public interface Actor
//**********************************************************
{
    String run(Message m); // the return string can be used to report a simple result
    String name();
}
