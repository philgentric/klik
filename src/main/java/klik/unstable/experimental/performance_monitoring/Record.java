package klik.unstable.experimental.performance_monitoring;

//**********************************************************
public record Record(String type, String tag, long dur_ms, String uuid)
//**********************************************************
{
    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        return type + ": task executed for "+dur_ms+ " tag:"+tag+" uuid:"+uuid;
    }
}
