package klik.experimental.backup;

import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Message;
import klik.util.log.Logger;

import java.io.File;

//**********************************************************
public class Directory_backup_job_request implements Message
//**********************************************************
{
    public final File source_dir;
    public final File destination_dir;
    public final Aborter aborter;
    public boolean finished = false;

    //**********************************************************
    public Directory_backup_job_request(File source_dir, File destination_dir,
                                        Aborter aborter_,  Logger logger)
    //**********************************************************
    {
        this.source_dir = source_dir;
        this.destination_dir = destination_dir;
        if ( aborter_ == null)
        {
            logger.log_stack_trace("FATAL: aborter must not be null");
        }
        aborter = aborter_;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "Directory_backup_job_request "+source_dir+" => "+destination_dir;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
