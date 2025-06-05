package klik.change.history;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

//**********************************************************
public class History_item
//**********************************************************
{

    public static Comparator<? super History_item> comparator_by_date = (Comparator<History_item>) (o1, o2) -> o2.time_stamp.compareTo(o1.time_stamp);

    public final String value; // typically a pah
    public final LocalDateTime time_stamp;
    public final UUID uuid;
    private boolean available;

    // when reloading recorded history from file:
    //**********************************************************
    public History_item(String value_, String time_stamp_, UUID uuid_)
    //**********************************************************
    {
        Objects.requireNonNull(value_,"History_item constructor, path cannot be null");
        Objects.requireNonNull(time_stamp_,"History_item constructor, time_stamp cannot be null");
        Objects.requireNonNull(uuid_,"History_item constructor, uuid cannot be null");
        value = value_;
        time_stamp = LocalDateTime.parse(time_stamp_);
        uuid = uuid_;
    }
    // when adding new stuff "live"
    //**********************************************************
    public History_item(String value_, LocalDateTime time_stamp_)
    //**********************************************************
    {
        value = value_;
        time_stamp = time_stamp_;
        uuid = UUID.randomUUID();
    }

    //**********************************************************
    String get_timestamp_as_string()
    //**********************************************************
    {
        return time_stamp.toString();
    }


    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        return value +" "+time_stamp.toString()+" "+uuid;
    }

    public void set_available(boolean b)
    {
        available = b;
    }

    public boolean get_available()
    {
        return available;
    }
}
