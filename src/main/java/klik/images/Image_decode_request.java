package klik.images;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class Image_decode_request {
    public int index;
    public Image_file_source image_file_source;
    public final boolean high_quality;
    public final int target_width;

    //public LinkedBlockingQueue<Image_decode_request> to_be_removed;
    public ConcurrentHashMap<String, Image_and_index> cache;

    public Image_decode_request(int index_, boolean high_quality_, int target_width_, Image_file_source image_file_source_,
                                ConcurrentHashMap<String, Image_and_index> preloaded_) {
        index = index_;
        high_quality = high_quality_;
        target_width = target_width_;
        image_file_source = image_file_source_;
        cache = preloaded_;
        //to_be_removed = to_be_removed_;
    }

    public static String get_key(Image_file_source image_file_source_, int index_)
    {
        Path p = image_file_source_.get_path(index_);
        if ( p == null) return null;
        return p.toAbsolutePath().toString();

    }

    public String make_key() {
        return get_key(image_file_source, index);
    }



    public String get_string() {
        return index + " file:" + image_file_source.get_path(index).toAbsolutePath();
    }
}
