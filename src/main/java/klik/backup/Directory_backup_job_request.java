package klik.backup;

import klik.actor.Aborter;
import klik.actor.Message;

import java.io.File;

//**********************************************************
public class Directory_backup_job_request implements Message
//**********************************************************
{
    public final File source_dir;
    public final File destination_dir;
    public final Aborter aborter;
    public boolean finished = false;
    public final boolean has_files;
    //**********************************************************
    public Directory_backup_job_request(File source_dir, File destination_dir,
                                        Aborter aborter_, boolean has_files_)
    //**********************************************************
    {
        this.source_dir = source_dir;
        this.destination_dir = destination_dir;
        aborter = aborter_;
        has_files = has_files_;
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
