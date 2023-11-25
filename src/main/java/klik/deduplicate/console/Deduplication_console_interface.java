package klik.deduplicate.console;

import klik.util.Logger;

import java.util.concurrent.LinkedBlockingDeque;

//**********************************************************
public class Deduplication_console_interface
//**********************************************************
{
    private LinkedBlockingDeque<Thing_to_do> things_to_do = new LinkedBlockingDeque<Thing_to_do>();
    Logger logger;
    Deduplication_console_window owner;

    //**********************************************************
    public Deduplication_console_interface(Deduplication_console_window owner_, Logger logger_)
    //**********************************************************
    {
        owner = owner_;
        logger = logger_;
    }

    //**********************************************************
    public LinkedBlockingDeque<Thing_to_do> get_queue()
    //**********************************************************
    {
        return things_to_do;
    }

    //**********************************************************
    public void increment_deleted()
    //**********************************************************
    {
        Thing_to_do thing_to_do = Thing_to_do.get_increment_deleted_thing_to_do();
        things_to_do.add(thing_to_do);
    }
    //**********************************************************
    public void increment_to_be_deleted()
    //**********************************************************
    {
        Thing_to_do thing_to_do = Thing_to_do.get_increment_to_be_deleted_thing_to_do();
        things_to_do.add(thing_to_do);
    }
    //**********************************************************
    public void increment_examined()
    //**********************************************************
    {
        Thing_to_do thing_to_do = Thing_to_do.get_increment_examined_thing_to_do();
        things_to_do.add(thing_to_do);
    }


    //**********************************************************
    public void set_status_text(String text)
    //**********************************************************
    {
        Thing_to_do thing_to_do = Thing_to_do.get_status_thing_to_do(text);
        things_to_do.add(thing_to_do);
    }

    //**********************************************************
    public void add(Thing_to_do thing_to_do)
    //**********************************************************
    {
        things_to_do.add(thing_to_do);
    }

    //**********************************************************
    public boolean increment_directory_examined()
    //**********************************************************
    {
        Thing_to_do thing_to_do = Thing_to_do.get_increment_directory_examined_thing_to_do();
        things_to_do.add(thing_to_do);
        return true;
    }

    //**********************************************************
    public void set_total_files_to_be_examined(int size)
    //**********************************************************
    {
        Thing_to_do thing_to_do = Thing_to_do.get_total_to_be_examined_thing_to_do(size);
        things_to_do.add(thing_to_do);
    }
}
