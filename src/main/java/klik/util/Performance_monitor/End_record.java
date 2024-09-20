package klik.util.Performance_monitor;

//**********************************************************
public record End_record(String name, long dur_ms, String uuid)
//**********************************************************
{
    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        return name + ": task executed for "+dur_ms+ " uuid:"+uuid;
    }
}
