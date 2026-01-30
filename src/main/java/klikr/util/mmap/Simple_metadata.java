package klikr.util.mmap;

public record Simple_metadata(Piece piece, String tag, long offset, long length) implements Meta{}
