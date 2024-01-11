package klik.browser;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Message;
import klik.util.Logger;

import java.nio.file.Path;

//**********************************************************
public class System_open_message implements Message
//**********************************************************
{

    public final Stage the_Stage;
    public final Path path;
    public final Logger logger;
    public final Aborter aborter;

    //**********************************************************
    public System_open_message(Stage owner, Path path, Logger logger)
    //**********************************************************
    {
        the_Stage = owner;
        this.path = path;
        this.logger = logger;
        aborter = new Aborter();
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
