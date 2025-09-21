package klik.util.files_and_paths;

import java.io.File;

//**********************************************************
public record File_pair(File f1, File f2)
//**********************************************************
{
    //**********************************************************
    public boolean both_file_exist()
    //**********************************************************
    {
        boolean returned = true;
        if (!f1.exists()) returned = false;
        if (!f2.exists()) returned = false;
        return returned;
    }
}
