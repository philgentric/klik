package klik.actor;

//**********************************************************
public interface Actor
//**********************************************************
{
    String run(Message m);// the return string can be used to report a simple result

    default String name() {
        return this.getClass().getName();
    }

}
