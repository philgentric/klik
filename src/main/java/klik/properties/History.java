package klik.properties;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class History
//**********************************************************
{
    private final String key_base; // name of this history in properties file
    public List<History_item> history = new ArrayList<>();
    Properties_manager pm;

    //**********************************************************
    public History(String key_base_)
    //**********************************************************
    {
        key_base = key_base_;
        pm = Properties.get_properties_manager();
        history = pm.get_history_of(key_base);
    }

    //**********************************************************
    public void add(Path p)
    //**********************************************************
    {
        pm.save_multiple(key_base,p.toAbsolutePath().toString());
    }

    //**********************************************************
    public void clear()
    //**********************************************************
    {
        pm = Properties.get_properties_manager();
        pm.clear(key_base);
    }
}
