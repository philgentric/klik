// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface Session {
    boolean on_client_connection(DataInputStream dis, DataOutputStream dos);
    String name();
}
