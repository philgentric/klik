package klikr.util.mmap;

public record Image_as_pixel_metadata(Piece piece, long offset, int width, int height) implements Meta {}
