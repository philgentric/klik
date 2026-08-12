package klikr.browser_core.items;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.Window_type;
import klikr.browser_core.virtual_landscape.Path_comparator_source;
import klikr.browser_core.virtual_landscape.Shutdown_target;
import klikr.path_lists.Path_list_provider;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;

public class Item_context {
    public final boolean is_trash;
    public final Shutdown_target shutdown_target;
    public final Path top_left;
    Path_list_provider path_list_provider;
    public Path item_path;
    public final Iconifiable_item_type item_type;
    Color tag_color; //may be null
    final Scene scene;
    final Path_comparator_source path_comparator_source;
    final Application application;
    final Window_type window_type;
    public final Window owner;
    public final Logger logger;
    public final Aborter aborter;

    public Item_context(
            Iconifiable_item_type  item_type,
            boolean isTrash,
            Shutdown_target shutdownTarget,
            Path topLeft,
            Path itemPath,
            Color tag_color,
            Scene scene,
            Path_comparator_source pathComparatorSource,
            Application application,
            Window_type windowType,
            Window owner,
            Logger logger,
            Aborter aborter,
                    Path_list_provider path_list_provider) {
        item_path = itemPath;
        if (item_type == null) this.item_type = Iconifiable_item_type.determine(item_path,owner,aborter,logger);
        else this.item_type = item_type;
        logger.log("Item_context constructor, item_type: " + this.item_type);
        is_trash = isTrash;
        if ( item_path == null) logger.log(Stack_trace_getter.get_stack_trace("WARNING: null item_path" +dump_item_context()));
        shutdown_target = shutdownTarget;
        top_left = topLeft;

        this.tag_color = tag_color;
        this.scene = scene;
        path_comparator_source = pathComparatorSource;
        this.application = application;
        window_type = windowType;
        this.owner = owner;
        this.logger = logger;
        this.aborter = aborter;
        this.path_list_provider = path_list_provider;
    }

    private String dump_item_context()
    {
        String returned =  " is_trash:"+is_trash+
                "item_type: "+item_type.name();
        if ( item_path != null) returned += " item_path:"+item_path.toString();
        else returned += " item_pqth is null";
        if ( path_list_provider != null)
        {
            returned += "path_list_provider"+path_list_provider.get_key();
        }
        else {
            returned += "path_list_provider is null";
        }
        return returned;
    }
}
