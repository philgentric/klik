package klik.find;

import klik.actor.Aborter;
import klik.actor.Message;
import klik.browser.Browser;

import java.nio.file.Path;
import java.util.List;

//**********************************************************
public class Finder_message implements Message
//**********************************************************
{

    public final Path path;
    public final List<String> keywords;
    public final Callback_for_image_found_publish callback;
    public final Browser the_browser;
    public final Aborter aborter;

    //**********************************************************
    public Finder_message(Path p_, List<String> keywords, Callback_for_image_found_publish callback_, Browser the_browser_)
    //**********************************************************
    {
        path = p_;
        this.keywords = keywords;
        callback = callback_;
        the_browser = the_browser_;
        aborter = new Aborter();
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "Finder : "+path;
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}