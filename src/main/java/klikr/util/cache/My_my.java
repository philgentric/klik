package klikr.util.cache;

import java.nio.file.Path;

public class My_my
{
    public String s;
    public int length;

    My_my(String s, int l)
    {

        this.s = s;
        this.length = l;
    }

    public String to_string()
    {
        return s+" => "+length;
    }

    public static My_my from_string(String s)
    {
        return new My_my( s, s.length());
    }

    public static My_my make(Path p)
    {
        String s = p.toAbsolutePath().toString();
        return new My_my( s,s.length());
    }


}
