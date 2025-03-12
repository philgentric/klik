package klik.util.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface Session {
    void on_client_connection(DataInputStream dis, DataOutputStream dos);
    String name();
}
