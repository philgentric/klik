//SOURCES ./Finder_frame.java
package klik.search;

import klik.browser.Browser;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.List;

//**********************************************************
public class Finder
//**********************************************************
{
    //**********************************************************
    public static void find(Path path,
                            //Browser browser,
                            List<String> keywords, boolean search_only_images, Logger logger)
    //**********************************************************
    {
        Finder_frame popup = new Finder_frame(
                    path,
                    keywords,
                    search_only_images,
                    //browser,
                    logger);
            popup.start_search();
    }
}
