package klikr.machine_learning;

//**********************************************************
public record ML_server(int port, String uuid, String name, String sub_type)
//**********************************************************
{
    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        StringBuilder sb = new StringBuilder();
        sb.append("port: ").append(port).append("\n");
        sb.append("uuid: ").append(uuid).append("\n");
        sb.append("name: ").append(name).append("\n");
        sb.append("sub-type: ").append(sub_type).append("\n");
        return sb.toString();
    }
}
