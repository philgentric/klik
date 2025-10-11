package klik.images;

import java.util.List;

public record Exif_read_result(String title, List<String> exif_items, double rotation, boolean image_is_damaged) {
}
