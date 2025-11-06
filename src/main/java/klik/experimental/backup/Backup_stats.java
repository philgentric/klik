// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.backup;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;

public class Backup_stats {
    public final LongAdder target_dir_count = new LongAdder();
    public final LongAdder done_dir_count = new LongAdder();
    public final LongAdder files_checked = new LongAdder();
    public final LongAdder bytes_copied = new LongAdder();
    public final LongAdder files_skipped = new LongAdder();
    public final LongAdder files_copied = new LongAdder();
    public final LongAdder number_of_bytes_processed = new LongAdder();
    public final LongAdder number_of_bytes_read = new LongAdder();
    public long source_byte_count;
}
