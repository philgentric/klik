package klikr.javalin.list;

import javafx.application.Application;
import javafx.stage.Window;
import klikr.browser_core.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.search.Results;
import klikr.search.Search_result;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Web_results implements Results
{
    private final Application application;
    private final Logger logger;
    private final List<List_item> items = new ArrayList<>();
    private final Consumer<String> on_click;
    public Web_results(
            Application application,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Aborter aborter,
            Window owner,
            Logger logger) {
        this.application = application;
        this.logger = logger;
        on_click = (String s) ->
        {
            logger.log("on_click: " + s);
        };

    }

    @Override
    public void inject_search_results(Search_result sr, String keys, boolean is_max, Window window)
    {

        items.add(new List_item(keys,sr.path().toAbsolutePath().toString()));

        Javalin_for_list.show(application,"test123",items,on_click,logger);

    }

    @Override
    public void erase_all_non_max() {

    }

    @Override
    public void has_ended() {

    }
}
