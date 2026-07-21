package klikr.browser_core.items;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.Window_type;
import klikr.browser_core.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.nio.file.Path;

public class Item_context {
    public final boolean is_trash;
    public Path item_path;
    public final Iconifiable_item_type item_type;
    Color color;
    final Scene scene;
    final Path_comparator_source path_comparator_source;
    final Application application;
    final Window_type window_type;
    public final Window owner;
    public final Logger logger;
    public final Aborter aborter;
    Path_list_provider path_list_provider;

    public Item_context(boolean isTrash, Path itemPath, Iconifiable_item_type item_type, Color color, Scene scene, Path_comparator_source pathComparatorSource, Application application, Window_type windowType, Window owner, Logger logger, Aborter aborter, Path_list_provider pathListProvider) {
        is_trash = isTrash;
        item_path = itemPath;
        this.item_type = item_type;
        this.color = color;
        this.scene = scene;
        path_comparator_source = pathComparatorSource;
        this.application = application;
        window_type = windowType;
        this.owner = owner;
        this.logger = logger;
        this.aborter = aborter;
        path_list_provider = pathListProvider;
    }
}
