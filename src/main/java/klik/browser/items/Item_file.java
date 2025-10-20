package klik.browser.items;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.path_lists.Path_list_provider;
import klik.browser.virtual_landscape.Selection_handler;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public abstract class Item_file extends Item
//**********************************************************
{
    protected Path path;
    public final int icon_size;
    public final Iconifiable_item_type item_type;


    //**********************************************************
    public Item_file(
            Scene scene,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            Color color,
            Path path_,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Window owner,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        super(scene, selection_handler, icon_factory_actor, color, path_list_provider, path_comparator_source, owner, aborter, logger);
        this.path = path_;
        item_type = Iconifiable_item_type.from_extension(get_item_path());
        icon_size = Non_booleans_properties.get_icon_size(owner);
    }

    //**********************************************************
    @Override
    public Iconifiable_item_type get_item_type()
    //**********************************************************
    {
        return item_type;
    }

    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        return "is file: " + path;
    }

}
