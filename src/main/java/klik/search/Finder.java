//SOURCES ./Finder_frame.java
package klik.search;

import klik.actor.Aborter;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.util.log.Logger;

import java.util.List;

//**********************************************************
public class Finder
//**********************************************************
{
    //**********************************************************
    public static void find(
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            List<String> keywords,
            boolean search_only_images,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        Finder_frame popup = new Finder_frame(
                    keywords,
                    search_only_images,
                    path_list_provider,
                    path_comparator_source,
                    aborter,
                    logger);
            popup.start_search();
    }
}
