package klik.search;

import java.nio.file.Path;
import java.util.List;

public record Search_result(Path path, List<String> matched_keywords){}
