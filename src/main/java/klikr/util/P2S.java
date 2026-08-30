package klikr.util;

import java.nio.file.Path;

public class P2S
{
    public static String p2s(Path p)
    {
        return p.toAbsolutePath().normalize().toString();
    }
}
