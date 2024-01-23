package klik.search;

import java.nio.file.Path;
import java.util.List;

public record Search_config(Path path, List<String> keywords, boolean look_only_for_images, String extension, boolean also_folders, boolean check_case) {}
