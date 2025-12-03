// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.util.log.Logger;

public class Menu_items
{

    //**********************************************************
    public static void add_menu_item_for_context_menu(
            String key, // this is the My_I18n key
            EventHandler<ActionEvent> action,
            ContextMenu context_menu,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        MenuItem mi = make_menu_item(key,action,owner,logger);
        context_menu.getItems().add(mi);
    }

    //**********************************************************
    public static void add_menu_item_for_menu(String key, // this is the My_I18n key@
                                              EventHandler<ActionEvent> action,
                                              Menu menu,
                                              Window owner,
                                              Logger logger)
    //**********************************************************
    {
        MenuItem mi = make_menu_item(key,action,owner,logger);
        menu.getItems().add(mi);
    }

    //**********************************************************
    public static MenuItem make_menu_item(
            String key,
            EventHandler<ActionEvent> ev,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(key, owner, logger);
        MenuItem menu_item = new MenuItem(text);
        menu_item.setMnemonicParsing(false);
        Look_and_feel_manager.set_menu_item_look(menu_item, owner, logger);
        menu_item.setOnAction(ev);
        return menu_item;
    }


}
