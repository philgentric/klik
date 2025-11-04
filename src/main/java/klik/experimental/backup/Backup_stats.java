// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.backup;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Backup_stats {
    public final AtomicInteger target_dir_count = new AtomicInteger(0);
    public final AtomicInteger done_dir_count = new AtomicInteger(0);
    public final AtomicInteger files_checked = new AtomicInteger(0);
    public final AtomicLong bytes_copied = new AtomicLong(0);
    public final AtomicLong files_skipped = new AtomicLong(0);
    public final AtomicLong files_copied = new AtomicLong(0);
    //public final AtomicLong remaining_time_in_milliseconds = new AtomicLong(0);
    public final AtomicLong number_of_bytes_processed = new AtomicLong(0);
    public final AtomicLong number_of_bytes_read = new AtomicLong(0);
    public long source_byte_count;
}
