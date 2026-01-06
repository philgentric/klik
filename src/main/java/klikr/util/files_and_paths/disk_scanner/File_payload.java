// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.files_and_paths.disk_scanner;

import java.io.File;
import java.util.concurrent.atomic.LongAdder;

public interface File_payload {
    void process_file(File f, LongAdder file_count_stop_counter);
}
