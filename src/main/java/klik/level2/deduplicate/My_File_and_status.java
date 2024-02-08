package klik.level2.deduplicate;

import klik.files_and_paths.My_File;

//**********************************************************
public class My_File_and_status
//**********************************************************
{
    public final My_File my_file;
    boolean to_be_deleted;

    //**********************************************************
    public My_File_and_status(My_File f_)
    //**********************************************************
    {
        my_file =f_;
        to_be_deleted=false;
    }
}
