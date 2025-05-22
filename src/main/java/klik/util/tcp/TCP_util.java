package klik.util.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

//**********************************************************
public class TCP_util
//**********************************************************
{
    //**********************************************************
    public static String read_string(DataInputStream dis) throws IOException
    //**********************************************************
    {
        int size = dis.readInt();
        byte buffer[] = new byte[size];
        dis.read(buffer);
        return new String(buffer, StandardCharsets.UTF_8);
    }

    //**********************************************************
    public static void write_string(String s, DataOutputStream dos) throws IOException
    //**********************************************************
    {
        byte[] buffer = s.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(buffer.length);
        dos.write(buffer);
    }
}
