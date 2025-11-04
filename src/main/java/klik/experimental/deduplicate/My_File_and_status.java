// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.deduplicate;

import klik.util.files_and_paths.File_with_a_few_bytes;

//**********************************************************
public class My_File_and_status
//**********************************************************
{
    public final File_with_a_few_bytes my_file;
    boolean to_be_deleted;

    //**********************************************************
    public My_File_and_status(File_with_a_few_bytes f_)
    //**********************************************************
    {
        my_file =f_;
        to_be_deleted=false;
    }
}
