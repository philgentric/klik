package klik.search;

import klik.browser.Browser;
import klik.util.Logger;

import java.nio.file.Path;
import java.util.List;

//**********************************************************
public class Finder
//**********************************************************
{
    //**********************************************************
    public static void find(Path path, Browser browser, List<String> keywords, Logger logger)
    //**********************************************************
    {
        Finder_frame popup = new Finder_frame(
                    path,
                    keywords,
                    browser,
                    logger);
            popup.start_search();
    }
}
