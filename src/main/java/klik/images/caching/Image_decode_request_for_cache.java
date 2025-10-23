package klik.images.caching;

import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Message;
import klik.images.Image_window;

import java.nio.file.Path;
import java.util.Objects;

//**********************************************************
public class Image_decode_request_for_cache implements Message
//**********************************************************
{
    public final Path path;
    public final Image_cache_interface cache;
    public final Aborter aborter;
    public final Image_window image_window;

    //**********************************************************
    public Image_decode_request_for_cache(Path path_,
                                          Image_cache_interface preloaded_,
                                          Image_window image_window,Aborter aborter)
    //**********************************************************
    {
        path = Objects.requireNonNull(path_);
        cache = preloaded_;
        this.image_window = image_window;
        this.aborter = aborter;
    }

    //**********************************************************
    public static String get_key(Path path)
    //**********************************************************
    {
        return path.toAbsolutePath().toString();
    }

    //**********************************************************
    public String make_key()
    //**********************************************************
    {
        return get_key(path);
    }



    //**********************************************************
    public String get_string()
    //**********************************************************
    {
        if ( path == null)         return "path:null";

        return " path:" + path.toAbsolutePath();
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "image decoding request for: "+path;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
