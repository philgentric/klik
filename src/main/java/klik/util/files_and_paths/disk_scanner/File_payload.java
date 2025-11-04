// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.files_and_paths.disk_scanner;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public interface File_payload {
    void process_file(File f, AtomicLong file_count_stop_counter);
}
