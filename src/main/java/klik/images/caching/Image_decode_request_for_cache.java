package klik.images.caching;

import klik.actor.Aborter;
import klik.actor.Message;

import java.nio.file.Path;
import java.util.Objects;

//**********************************************************
public class Image_decode_request_for_cache implements Message
//**********************************************************
{
    public final Path path;
    //public final boolean high_quality;
    public final Cache_interface cache;
    public final Aborter aborter;

    //**********************************************************
    public Image_decode_request_for_cache(Path path_,
                                          Cache_interface preloaded_, Aborter aborter)
    //**********************************************************
    {
        path = Objects.requireNonNull(path_);
        cache = preloaded_;
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
