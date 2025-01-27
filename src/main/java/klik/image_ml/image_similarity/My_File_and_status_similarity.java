package klik.image_ml.image_similarity;

import klik.util.files_and_paths.File_with_a_few_bytes;

//**********************************************************
public class My_File_and_status_similarity
//**********************************************************
{
    public final File_with_a_few_bytes my_file;
    boolean to_be_deleted;

    //**********************************************************
    public My_File_and_status_similarity(File_with_a_few_bytes f_)
    //**********************************************************
    {
        my_file =f_;
        to_be_deleted=false;
    }
}
