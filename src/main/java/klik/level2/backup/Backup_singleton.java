package klik.level2.backup;

import javafx.stage.Stage;
import klik.util.Logger;
import klik.util.Popups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//**********************************************************
public class Backup_singleton
//**********************************************************
{
    private static Backup_singleton instance;
    Logger logger;
    Path source;
    Path sink;
    private final List<Backup_engine> engines = new ArrayList<>();

    //**********************************************************
    public static void set_source(Path source_, Logger logger_)
    //**********************************************************
    {
        Backup_singleton b = get_instance(logger_);
        b.source = source_;
    }
    //**********************************************************
    public static void set_sink(Path sink_, Logger logger_)
    //**********************************************************
    {
        Backup_singleton b = get_instance(logger_);
        b.sink = sink_;
    }
    //**********************************************************
    public static boolean start_the_backup(Stage owner)
    //**********************************************************
    {
        if ( instance == null) return false;
        instance.start(owner);
        return true;
    }

    //**********************************************************
    private void start(Stage owner)
    //**********************************************************
    {
        Iterator<Backup_engine> it = engines.iterator();
        while ( it.hasNext())
        {
            Backup_engine e = it.next();
            if ( e.is_finished()) it.remove();
            else
            {
                if (e.source.equals(source))
                {
                    if (e.sink.equals(sink)) {
                        Popups.popup_warning(owner,"A backup like this is already running", "Sorry: denied",true,logger);
                        return;
                    }
                }
            }
        }

// Get a CONFIRMATION
        String header = "Copy Confirmation Required";
        String content = "This will copy all the files down from directory:\n" + source.toAbsolutePath() + "\n"
                + "Into the directory:\n" + sink.toAbsolutePath() + "\n"
                + "(this is safe because files with same names, if different, will be renamed)\n"
                + "Are you sure you want to do that ?";

        if (!Popups.popup_ask_for_confirmation(owner,header, content, logger)) return;

        Backup_engine b = new Backup_engine(source, sink, logger);
        b.go();
        engines.add(b);
    }

    //**********************************************************
    public static void abort()
    //**********************************************************
    {
        if ( instance==null) return;
        instance.abort_now();
    }

    //**********************************************************
    private void abort_now()
    //**********************************************************
    {
        for ( Backup_engine e : engines)
        {
            logger.log("CANCEL for "+e.to_string());
            e.abort();
        }
    }



    //**********************************************************
    private static Backup_singleton get_instance(Logger logger_)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new Backup_singleton(logger_);
        }
        return instance;
    }
    //**********************************************************
    private Backup_singleton(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }


}