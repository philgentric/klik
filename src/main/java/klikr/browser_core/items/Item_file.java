// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core.items;

import klikr.browser_core.icons.Icon_factory_actor;
import klikr.browser_core.virtual_landscape.Selection_handler;
import klikr.settings.Non_booleans_properties;
import klikr.util.log.Stack_trace_getter;

//**********************************************************
public abstract class Item_file extends Item
//**********************************************************
{
    public final int icon_size;


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

        icon_size = Non_booleans_properties.get_icon_size(item_context.owner);
    }

    //**********************************************************
    @Override
    public Iconifiable_item_type get_item_type()
    //**********************************************************
    {
        return item_context.item_type;
    }

    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        return "is file: " + item_context.item_path;
    }

}
