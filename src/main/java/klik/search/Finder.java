//SOURCES ./Finder_frame.java
package klik.search;

import klik.browser.Browser;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.List;

//**********************************************************
public class Finder
//**********************************************************
{
    //**********************************************************
    public static void find(
            Path_list_provider path_list_provider,
            List<String> keywords,
            boolean search_only_images,
            Logger logger)
    //**********************************************************
    {
        Finder_frame popup = new Finder_frame(
                    keywords,
                    search_only_images,
                    path_list_provider,
                    logger);
            popup.start_search();
    }
}
