// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core.items;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.Window_type;
import klikr.util.execute.actor.Aborter;
import klikr.browser_core.icons.Icon_factory_actor;
import klikr.browser_core.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.browser_core.virtual_landscape.Selection_handler;
import klikr.settings.Non_booleans_properties;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public abstract class Item_file extends Item
//**********************************************************
{
    public final int icon_size;
    public final Iconifiable_item_type item_type;


    //**********************************************************
    public Item_file(
            Item_context item_context,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor)
    //**********************************************************
    {
        super(item_context, selection_handler, icon_factory_actor);
        if ( item_context.item_path == null )
        {
            item_context.logger.log(Stack_trace_getter.get_stack_trace("item_path == null ???"));
        }
        if ( item_context.item_path == null)
        {
            item_context.logger.log(Stack_trace_getter.get_stack_trace(""));
        }
        item_type = Iconifiable_item_type.determine(item_context.item_path,item_context.owner,item_context.aborter,item_context.logger);
        icon_size = Non_booleans_properties.get_icon_size(item_context.owner);
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
        return "is file: " + item_context.item_path;
    }

}
