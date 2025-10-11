package klik.util.files_and_paths;

import java.util.concurrent.ConcurrentLinkedQueue;

public record Sizes(long bytes, int folders, long files, long images
        //, ConcurrentLinkedQueue<String> warnings
){}
