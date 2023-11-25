package klik.change;

import klik.files_and_paths.Old_and_new_Path;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

//**********************************************************
public class Undo_item
//**********************************************************
{

    public static Comparator<? super Undo_item> comparator_by_date = (Comparator<Undo_item>) (o1, o2) -> o2.time_stamp.compareTo(o1.time_stamp);

    // most recent first
    public final List<Old_and_new_Path> oans;
    public final LocalDateTime time_stamp;
    public final UUID index;

    //**********************************************************
    public Undo_item(List<Old_and_new_Path> oans_, LocalDateTime time_stamp_, UUID index_)
    //**********************************************************
    {
        oans = oans_;
        time_stamp = time_stamp_;
        index = index_;
    }


    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        StringBuilder returned = new StringBuilder("Undo_item ->" + index + " " + time_stamp);
        for ( Old_and_new_Path oan : oans)
        {
            returned.append(" ").append(oan.old_Path.toAbsolutePath());
            if ( oan.new_Path != null) returned.append("=>").append(oan.new_Path.toAbsolutePath());
        }
        return returned.toString();
    }
}
