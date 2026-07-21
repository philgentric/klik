// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.ui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.Window_builder;
import klikr.Window_type;
import klikr.browser_core.items.Item_context;
import klikr.look.Font_size;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.util.execute.System_open_actor;
import klikr.util.execute.actor.Aborter;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;

import java.nio.file.Path;

//**********************************************************
public class Menu_items
//**********************************************************
{
    private static final boolean dbg =  false;

    static double xxx = 200;
    static double yyy = 200;

    //**********************************************************
    public static void create_open_with_registered_application_menu_item(ContextMenu context_menu, Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Open_With_Registered_Application",true,
                null,
                e -> {
                    logger.log("Open_With_Registered_Application");
                    System_open_actor.open_with_registered_application(path, owner, aborter, logger);
                }, context_menu, owner, logger);
    }


    //**********************************************************
    public static void create_browse_in_new_window_menu_item(Application application, ContextMenu context_menu, Path path, Window owner, Logger logger)
    //**********************************************************
    {
        KeyCodeCombination kc = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);

        add_menu_item_for_context_menu("Browse_in_new_window",true, kc.getDisplayText(),
                e -> {
                    //logger.log("Browse_in_new_window");
                    Path local = path;
                    if (!local.toFile().isDirectory()) local = local.getParent();
                    Window_builder.additional_no_past(application, Window_type.File_system_2D, new Path_list_provider_for_file_system(local, owner, logger), owner, logger);
                }, context_menu, owner, logger);
    }

    //**********************************************************
    public static void create_delete_menu_item(ContextMenu context_menu, Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        KeyCodeCombination kc = new KeyCodeCombination(KeyCode.BACK_SPACE);

        Menu_items.add_menu_item_for_context_menu("Delete",true,kc.getDisplayText(),
                event -> {
                    if (dbg) logger.log("Deleting!");
                    double x = owner.getX()+100;
                    double y = owner.getY()+100;
                    Static_files_and_paths_utilities.move_to_trash(path,owner,x,y, null, aborter,logger);
                },context_menu,owner,logger);
    }
    //**********************************************************
    public static void create_show_file_size_menu_item(ContextMenu context_menu, Item_context item_context)
    //**********************************************************
    {
        Menu_items.add_menu_item_for_context_menu("Show_file_size",true,null,
                event -> {
                    show_file_size(item_context.item_path, item_context.owner, item_context.logger);
                }, context_menu,item_context.owner,item_context.logger);
    }

    //**********************************************************
    public static void show_file_size(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("File length");
        String size_in_bytes = Static_files_and_paths_utilities.get_1_line_string_with_size(path,owner,logger);
        String message = My_I18n.get_I18n_string("File_size_for", owner,logger) +"\n"+ path.getFileName().toString();
        Stage local_stage = new Stage();
        local_stage.setHeight(200);
        local_stage.setWidth(600);
        local_stage.setX(xxx);
        local_stage.setY(yyy);
        yyy+= 200;
        if ( yyy > 600)
        {
            yyy = 200;
            xxx += 600;
            if ( xxx > 1000) xxx = 200;
        }
        TextArea textarea1 = new TextArea(message+"\n"+size_in_bytes);
        Font_size.apply_this_font_size_to_Node(textarea1,24,logger);
        VBox vbox = new VBox(textarea1);
        Scene scene = new Scene(vbox, Color.WHITE);
        local_stage.setTitle(path.toAbsolutePath().toString());
        local_stage.setScene(scene);
        local_stage.show();

        logger.log("size_in_bytes->"+size_in_bytes+"<-");
        //b_.set_status(size_in_bytes);
    }

    //**********************************************************
    public static void add_menu_item_for_context_menu(
            String key,
            boolean is_18n,// this is the My_I18n key
            String addendum, // may be null
            EventHandler<ActionEvent> action,
            ContextMenu context_menu,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        MenuItem mi = make_menu_item(key,is_18n,addendum,action,owner,logger);
        context_menu.getItems().add(mi);
    }

    /*
    //**********************************************************
    public static void add_menu_item_for_context_menu_B(
            String menu_text,
            String addendum, // may be null
            EventHandler<ActionEvent> action,
            ContextMenu context_menu,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        MenuItem mi = make_menu_item(menu_text,false,addendum,action,owner,logger);
        context_menu.getItems().add(mi);
    }
*/
    //**********************************************************
    public static void add_menu_item_for_menu(String key, // this is the My_I18n key@
                                              boolean is_18n,
                                              String addendum,
                                              EventHandler<ActionEvent> action,
                                              Menu menu,
                                              Window owner,
                                              Logger logger)
    //**********************************************************
    {
        MenuItem mi = make_menu_item(key,is_18n,addendum,action,owner,logger);
        menu.getItems().add(mi);
    }

    //**********************************************************
    public static void add_menu_item_for_menubutton(String key,
                                                    boolean is_18n, // key is the My_I18n key@
                                              String addendum,
                                              EventHandler<ActionEvent> action,
                                              MenuButton mb,
                                              Window owner,
                                              Logger logger)
    //**********************************************************
    {
        //logger.log("add_menu_item_for_menubutton->"+key+"<-");
        MenuItem mi = make_menu_item(key,is_18n,addendum,action,owner,logger);

        mb.getItems().add(mi);
    }

    //**********************************************************
    public static MenuItem make_menu_item(
            String key,
            boolean is_18n,
            String addendum, // maybe null
            EventHandler<ActionEvent> ev,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        String menu_text = key;
        if ( is_18n) menu_text =My_I18n.get_I18n_string(key, owner, logger);
        if(menu_text==null) menu_text = key;
        if ( addendum!=null) if ( !addendum.isEmpty()) menu_text +=" ("+addendum+")";
        MenuItem menu_item = new MenuItem(menu_text);
        menu_item.setMnemonicParsing(false);
        Look_and_feel_manager.set_menu_item_look(menu_item, owner, logger);
        menu_item.setOnAction(ev);
        return menu_item;
    }


    //**********************************************************
    public static MenuItem make_menu_item2(
            String menu_text,
            String addendum, // maybe null
            EventHandler<ActionEvent> ev,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        if ( addendum!=null) if ( !addendum.isEmpty()) menu_text+=" ("+addendum+")";
        MenuItem menu_item = new MenuItem(menu_text);
        menu_item.setMnemonicParsing(false);
        Look_and_feel_manager.set_menu_item_look(menu_item, owner, logger);
        menu_item.setOnAction(ev);
        return menu_item;
    }



}
