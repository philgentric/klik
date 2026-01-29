// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change.undo;

import klikr.util.files_and_paths.old_and_new.Old_and_new_Path;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

//**********************************************************
public class Undo_item
//**********************************************************
{

    public static final String THIS_ITEM_CANNOT_BE_UNDONE_THE_FILE_HAS_BEEN_DELETED_FOREVER = "\nTHIS ITEM CANNOT BE UNDONE: the file has been deleted forever";
    public static final String THIS_ITEM_CANNOT_BE_UNDONE = "\nTHIS ITEM CANNOT BE UNDONE: ";
    public static final String FILES_HAVE_BEEN_DELETED_FOREVER = " file(s) have been deleted forever";
    public static Comparator<? super Undo_item> comparator_by_date = (Comparator<Undo_item>) (o1, o2) -> o2.time_stamp.compareTo(o1.time_stamp);

    // most recent first
    public final List<Old_and_new_Path> oans;
    public final LocalDateTime time_stamp;
    public final UUID index;
    public final Logger logger;

    //**********************************************************
    public Undo_item(List<Old_and_new_Path> oans_, LocalDateTime time_stamp_, UUID index_, Logger logger_)
    //**********************************************************
    {
        oans = oans_;
        time_stamp = time_stamp_;
        index = index_;
        logger = logger_;
    }

    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        StringBuilder returned = new StringBuilder();
        returned.append("Undo_item ->");
        returned.append(index);
        returned.append(" ");
        returned.append(time_stamp);
        for ( Old_and_new_Path oan : oans)
        {
            returned.append(" ").append(oan.old_Path.toAbsolutePath());
            if ( oan.new_Path != null) returned.append("\n=>\n").append(oan.new_Path.toAbsolutePath());
        }
        return returned.toString();
    }

    //**********************************************************
    public String signature()
    //**********************************************************
    {
        if ( !oans.isEmpty())
        {
            String ideal = detect_moving_several_files_from_one_folder_to_another();
            if ( ideal != null) return ideal;
        }
        StringBuilder sb = new StringBuilder();
        boolean at_least_one_can_be_undone = false;
        int count = 0;
        for ( Old_and_new_Path oan : oans)
        {
            sb.append("\n").append(oan.old_Path.toAbsolutePath());
            if ( oan.new_Path != null)
            {
                at_least_one_can_be_undone = true;
                sb.append("\n=>\n").append(oan.new_Path.toAbsolutePath());
            }
            else
            {
                sb.append(THIS_ITEM_CANNOT_BE_UNDONE_THE_FILE_HAS_BEEN_DELETED_FOREVER);
                count++;
            }
        }
        if ( at_least_one_can_be_undone) return sb.toString();
        else return THIS_ITEM_CANNOT_BE_UNDONE+count+FILES_HAVE_BEEN_DELETED_FOREVER;
    }

    //**********************************************************
    private String detect_moving_several_files_from_one_folder_to_another()
    //**********************************************************
    {
        // if the list is about changes in the same folder pair ...
        // i.e. several files from folder A moved to folder B
        Path old_folder = null;
        Path new_folder = null;
        //logger.log("ideal oans length "+oans.length());

        for ( Old_and_new_Path oan : oans)
        {
            if (oan.old_Path == null)
            {
                logger.log("WARNING:  oan.old_Path == null "+oan.to_string());
                return null;
            }
            if (oan.new_Path == null)
            {
                logger.log("WARNING:  oan.new_Path == null "+oan.to_string());
                return null;
            }
            //logger.log("ideal oan = "+oan.get_string());

            if ( !oan.new_Path.getFileName().toString().equals(oan.old_Path.getFileName().toString()))
            {
                // the file names are different
                return null;
            }
            if (old_folder == null)
            {
                old_folder = oan.old_Path.getParent();
                //logger.log("old_folder = "+old_folder);
            }
            else
            {
                if ( !old_folder.toAbsolutePath().toString().equals(oan.old_Path.getParent().toAbsolutePath().toString()))
                {
                    // different old_folders are present ! (weird!?)
                    return null;
                }
            }
            if (new_folder == null)
            {
                new_folder = oan.new_Path.getParent();
            }
            else
            {
                if ( !new_folder.toAbsolutePath().toString().equals(oan.new_Path.getParent().toAbsolutePath().toString()))
                {
                    // different old_folders are present ! (weird!?)
                    return null;
                }
            }
        }
        if ( old_folder == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN1"));

            return null;
        }
        if ( new_folder == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN2"));
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(old_folder.toAbsolutePath().toString());
        sb.append("\n==> ");
        sb.append(new_folder.toAbsolutePath().toString());
        sb.append("\n");
        for ( Old_and_new_Path oan : oans)
        {
            sb.append(oan.old_Path.getFileName().toString());
            sb.append(" ");
        }
        return sb.toString();
    }
}
