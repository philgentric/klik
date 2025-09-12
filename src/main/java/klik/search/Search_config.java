package klik.search;

import java.nio.file.Path;
import java.util.List;

public record Search_config(Path path,
                            List<String> keywords,
                            boolean look_only_for_images,
                            String extension,
                            boolean search_folders,
                            boolean search_files,
                            boolean ignore_hidden,
                            boolean check_case) {}
