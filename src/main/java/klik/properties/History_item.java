package klik.properties;

import java.time.LocalDateTime;
import java.util.Comparator;

//**********************************************************
public class History_item
//**********************************************************
{
    public static Comparator<? super History_item> comparator_by_date = new Comparator<History_item>() {
        @Override
        public int compare(History_item o1, History_item o2)
        {
            return o2.time_stamp.compareTo(o1.time_stamp);
        }
    };

    public final String path;
    public final LocalDateTime time_stamp;

    // when reading from file:
    //**********************************************************
    public History_item(String path_, String time_stamp_)
    //**********************************************************
    {
        path = path_;
        time_stamp = LocalDateTime.parse(time_stamp_);

    }
    // when recording live
    //**********************************************************
    public History_item(String path_)
    //**********************************************************
    {
        path = path_;
        time_stamp = LocalDateTime.now();

    }
    //**********************************************************
    String get_timestamp_as_string()
    //**********************************************************
    {
        return time_stamp.toString();
    }
}
