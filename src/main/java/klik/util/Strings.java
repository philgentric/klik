package klik.util;

//**********************************************************
public class Strings
//**********************************************************
{
    //**********************************************************
    public static String create_nice_remaining_time_string(long time_in_milliseconds)
    //**********************************************************
    {
        String returned;
        if (time_in_milliseconds < 1000) {
            returned = time_in_milliseconds + " milliseconds";
            return returned;
        }
        int time_in_seconds = (int) (time_in_milliseconds / 1000);
        if (time_in_seconds < 60) {
            returned = time_in_seconds + " seconds";
        } else {
            int remaining_time_in_minutes = time_in_seconds / 60;
            if (remaining_time_in_minutes < 60) {
                int remaining_time_in_seconds2 = time_in_seconds - remaining_time_in_minutes * 60;
                returned = remaining_time_in_minutes + " minute(s) " + remaining_time_in_seconds2 + " seconds";
            } else {
                int remaining_time_in_hours = remaining_time_in_minutes / 60;
                if (remaining_time_in_hours < 24) {
                    int remaining_time_in_minutes2 = remaining_time_in_minutes - remaining_time_in_hours * 60;

                    returned = remaining_time_in_hours + " hour(s) " + remaining_time_in_minutes2 + " minute(s)";
                } else {
                    int remaining_time_in_days = remaining_time_in_hours / 24;
                    int remaining_time_in_hours2 = remaining_time_in_hours - remaining_time_in_days * 24;
                    returned = remaining_time_in_days + " day(s) " + remaining_time_in_hours2 + " hour(s)";
                }
            }
        }
        return returned;
    }

    //**********************************************************
    public static String create_nice_bytes_per_second_string(long speed)
    //**********************************************************
    {
        if ( speed > 1_000_000_000)
        {
            return String.format("%.2f",(double)speed/1_000_000_000.0)+" GB/s";
        }
        if ( speed > 1_000_000)
        {
            return String.format("%.2f",(double)speed/1_000_000.0)+" MB/s";
        }
        if ( speed > 1_000)
        {
            return String.format("%.2f",(double)speed/1_000.0)+" kB/s";
        }

        return String.format("%.2f",(double)speed)+" B/s";
    }

    //**********************************************************
    public static String create_nice_bytes_string(long byte_count)
    //**********************************************************
    {
        if ( byte_count > 1_000_000_000)
        {
            return String.format("%.3f",(double)byte_count/1_000_000_000.0)+" GB";
        }
        if ( byte_count > 1_000_000)
        {
            return String.format("%.3f",(double)byte_count/1_000_000.0)+" MB";
        }
        if ( byte_count > 1_000)
        {
            return String.format("%.3f",(double)byte_count/1_000.0)+" kB";
        }

        return String.format("%.3f",(double)byte_count)+" B";
    }
}
