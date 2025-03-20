package klik.util.execute;

import javafx.stage.Stage;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Message;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class System_open_message implements Message
//**********************************************************
{

    public final Window window;
    public final Path path;
    public final Logger logger;
    public final Aborter aborter;
    public final boolean special;
    //**********************************************************
    public System_open_message(boolean special, Window window, Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.special = special;
        this.window = window;
        this.path = path;
        this.logger = logger;
       this.aborter = aborter;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "System.open for " + path;
    }

    //**********************************************************
    @Override
    public Aborter get_aborter() {
        return aborter;
    }
    //**********************************************************

}
