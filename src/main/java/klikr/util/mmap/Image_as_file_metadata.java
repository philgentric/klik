package klikr.util.mmap;

public record Image_as_file_metadata(Piece piece, String tag, long offset, long length) implements Meta{}
