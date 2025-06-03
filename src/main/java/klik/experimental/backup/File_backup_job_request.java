package klik.experimental.backup;

import klik.actor.Aborter;
import klik.actor.Message;
import klik.util.log.Logger;

import java.io.File;

//**********************************************************
public class File_backup_job_request implements Message
//**********************************************************
{
    public final File destination_dir;
    public final File file_to_be_copied;
    public final Per_folder_mini_console mini_console;
    public final boolean check_for_same_file_different_name;
    public final Aborter aborter;
    public final boolean deep_byte_check;

    //**********************************************************
    public File_backup_job_request(
            File destination_dir,
            File file_to_be_copied,
            Per_folder_mini_console mini_console,
            boolean check_for_same_file_different_name,
            boolean deep_byte_check,
            Aborter aborter_,
            Logger logger)
    //**********************************************************
    {
        this.destination_dir = destination_dir;
        this.file_to_be_copied = file_to_be_copied;
        this.mini_console = mini_console;
        this.check_for_same_file_different_name = check_for_same_file_different_name;
        this.deep_byte_check = deep_byte_check;
        if ( aborter_ == null)
        {
            logger.log_stack_trace("FATAL: aborter must not be null");
        }
        this.aborter = aborter_;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
