package klik.backup;

import klik.actor.Aborter;
import klik.actor.Message;

import java.io.File;

//**********************************************************
public class File_backup_job_request implements Message
//**********************************************************
{
    public final File destination_dir;
    public final File file_to_be_copied;
    public final Per_folder_mini_console mini_console;
    public final boolean enable_check_for_same_file_different_name;
    public final Aborter aborter;

    //**********************************************************
    public File_backup_job_request(
            File destination_dir,
            File file_to_be_copied,
            Per_folder_mini_console mini_console,
            boolean enable_check_for_same_file_different_name, Aborter aborter_)
    //**********************************************************
    {
        this.destination_dir = destination_dir;
        this.file_to_be_copied = file_to_be_copied;
        this.mini_console = mini_console;
        this.enable_check_for_same_file_different_name = enable_check_for_same_file_different_name;
        this.aborter = aborter_;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
