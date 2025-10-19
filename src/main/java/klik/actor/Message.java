package klik.actor;

//**********************************************************
public interface Message
//**********************************************************
{
    String to_string();
    //default String to_string() {return this.getClass().getName();}
    Aborter get_aborter(); // only one instance per Message = job, cannot be null
}
