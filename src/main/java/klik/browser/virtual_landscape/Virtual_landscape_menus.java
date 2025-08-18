//SOURCES ../About_klik_stage.java
//SOURCES ../Icon_size.java
//SOURCES ../../image_ml/face_recognition/Face_recognition_service.java
//SOURCES ../../image_ml/ML_servers_util.java
//SOURCES ../../image_ml/image_similarity/Similarity_cache.java
//SOURCES ../../image_ml/image_similarity/Image_feature_vector_cache.java
//SOURCES ../../util/files_and_paths/Name_cleaner.java
//SOURCES ../../experimental/metadata/Tag_items_management_stage.java
//SOURCES ../../properties/boolean_features/Preferences_stage.java
//SOURCES ../items/Item_folder.java
//SOURCES ../../look/Look_and_feel_style.java
//SOURCES ../../look/my_i18n/Language.java



package klik.browser.virtual_landscape;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.browser.Icon_size;
import klik.New_window_context;
import klik.browser.classic.Folder_path_list_provider;
import klik.browser.items.Item_folder;
import klik.change.Change_receiver;
import klik.change.active_list_stage.Active_list_stage;
import klik.change.active_list_stage.Active_list_stage_action;
import klik.change.active_list_stage.Datetime_to_signature_source;
import klik.change.history.History_engine;
import klik.change.bookmarks.Bookmarks;
import klik.change.history.History_item;
import klik.change.undo.Undo_for_moves;
import klik.change.undo.Undo_item;
import klik.image_ml.ML_servers_util;
import klik.image_ml.face_recognition.Face_recognition_service;
import klik.images.decoding.Exif_metadata_extractor;
import klik.look.Look_and_feel_manager;
import klik.look.Look_and_feel_style;
import klik.look.my_i18n.My_I18n;
import klik.look.my_i18n.Language;
import klik.properties.*;
import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.properties.boolean_features.Preferences_stage;
import klik.util.files_and_paths.*;
import klik.util.info_stage.Info_stage;
import klik.util.info_stage.Line_for_info_stage;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

//**********************************************************
public class Virtual_landscape_menus
//**********************************************************
{
    private static final int MAX_MENU_ITEM_STRING_LENGTH = 150;
    public final Virtual_landscape virtual_landscape;
    public final Change_receiver change_receiver;
    public final Window owner;
    CheckMenuItem select_all_files_menu_item;
    CheckMenuItem select_all_folders_menu_item;
    Map<LocalDateTime,String> the_whole_history;

    //**********************************************************
    Virtual_landscape_menus(Virtual_landscape virtual_landscape, Change_receiver change_receiver, Window owner)
    //**********************************************************
    {
        this.virtual_landscape = virtual_landscape;
        this.change_receiver = change_receiver;
        this.owner = owner;
    }


    //**********************************************************
    public MenuItem make_menu_item(String key, EventHandler<ActionEvent> ev)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(key, virtual_landscape.owner, virtual_landscape.logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(ev);
        return item;
    }




    //**********************************************************
    public MenuItem make_clear_all_caches_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Clear_All_Caches",virtual_landscape.owner,virtual_landscape.logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(Yevent -> {
            virtual_landscape.clear_all_RAM_caches();
            Static_files_and_paths_utilities.clear_all_DISK_caches(owner,virtual_landscape.aborter,virtual_landscape.logger);
        });
        return item;
    }

    //**********************************************************
    private void add_question_mark_button(String key, MenuItem item)
    //**********************************************************
    {
        if ( !Feature_cache.get(Feature.Hide_question_mark_buttons_on_mysterious_menus))
        {
            Button explanation_button = Preferences_stage.make_explanation_button(key, virtual_landscape.owner, virtual_landscape.logger);
            item.setGraphic(explanation_button);
        }
    }

    //**********************************************************
    public MenuItem make_start_Enable_face_recognition_menu_item()
    //**********************************************************
    {
        String key = "Show_manual_about_face_reco";
        String text = My_I18n.get_I18n_string(key,virtual_landscape.owner, virtual_landscape.logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> {
            ML_servers_util.show_face_recognition_manual(virtual_landscape.owner, virtual_landscape.logger);
        });
        add_question_mark_button(key, item);
        return item;
    }



    //**********************************************************
    public MenuItem make_start_image_similarity_servers_menu_item()
    //**********************************************************
    {
        String key = "Show_manual_about_image_similarity";
        String text = My_I18n.get_I18n_string(key,virtual_landscape.owner, virtual_landscape.logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> {
            ML_servers_util.show_image_similarity_manual(virtual_landscape.owner, virtual_landscape.logger);
        });
        add_question_mark_button(key, item);
        return item;
    }


    //**********************************************************
    public Button make_button_that_behaves_like_a_folder(
            Path path,
            String text,
            double height,
            double min_width,
            boolean is_trash_button,
            Path is_parent_of,
            Logger logger)
    //**********************************************************
    {
        // we make a Item_button but are only interested in the button...
        Item_folder dummy = new Item_folder(
                virtual_landscape.the_Scene,
                virtual_landscape.selection_handler,
                virtual_landscape.icon_factory_actor,
                null,
                text,
                height,
                is_trash_button,
                is_parent_of,
                virtual_landscape.browsing_caches.image_properties_RAM_cache,
                virtual_landscape.shutdown_target,
                new Folder_path_list_provider(path),
                virtual_landscape,
                virtual_landscape,
                
                owner,
                virtual_landscape.aborter,
                logger);
        dummy.button_for_a_directory(text, min_width, height, null);
        return dummy.button;
    }




    //**********************************************************
    public MenuItem make_cache_size_limit_warning_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Set_The_Cache_Size_Warning_Limit",virtual_landscape.owner,virtual_landscape.logger);

        MenuItem item = new MenuItem(text);

        item.setOnAction(actionEvent -> {
            //Non_booleans_properties.set_cache_size_limit_warning(((CheckMenuItem) actionEvent.getSource()).isSelected(),virtual_landscape.logger);
            TextInputDialog dialog = new TextInputDialog(""+ Non_booleans_properties.get_folder_warning_size(owner));
            Look_and_feel_manager.set_dialog_look(dialog, virtual_landscape.owner,virtual_landscape.logger);
            dialog.initOwner(owner);
            dialog.setWidth(800);
            dialog.setTitle(My_I18n.get_I18n_string("Cache_Size_Warning_Limit",virtual_landscape.owner,virtual_landscape.logger));
            dialog.setHeaderText("If the cache on disk gets larger than this, you will receive a warning. Entering zero means no limit.");
            dialog.setContentText(My_I18n.get_I18n_string("Set_The_Cache_Size_Warning_Limit",virtual_landscape.owner,virtual_landscape.logger));


            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String new_val = result.get();
                try
                {
                    int val = Integer.parseInt(new_val);
                    Non_booleans_properties.set_cache_size_limit_warning_megabytes_fx(val,owner);

                }
                catch (NumberFormatException e)
                {
                    Popups.popup_warning("Integer only!","Please retry with an integer value!",false,owner,virtual_landscape.logger);
                }
            }

        });
        return item;
    }


    //**********************************************************
    public MenuItem make_max_RAM_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Set_The_VM_Max_RAM",virtual_landscape.owner,virtual_landscape.logger);

        MenuItem item = new MenuItem(text);

        item.setOnAction(actionEvent -> {
            TextInputDialog dialog = new TextInputDialog(""+ Non_booleans_properties.get_java_VM_max_RAM(owner, virtual_landscape.logger));
            Look_and_feel_manager.set_dialog_look(dialog, virtual_landscape.owner,virtual_landscape.logger);
            dialog.initOwner(owner);
            dialog.setWidth(800);
            dialog.setTitle("Java VM max RAM size");
            dialog.setHeaderText("This is the max RAM that the java VM will be allowed to allocated THE NEXT TIME you run klik.");
            dialog.setContentText("If you do not know what this means,\ndont change this value");


            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String new_val = result.get();
                try
                {
                    int val = Integer.parseInt(new_val);
                    Non_booleans_properties.save_java_VM_max_RAM(val,owner, virtual_landscape.logger);

                }
                catch (NumberFormatException e)
                {
                    Popups.popup_warning("Integer only!","Please retry with an integer value!",false,owner,virtual_landscape.logger);
                }
            }

        });
        return item;
    }



    //**********************************************************
    public MenuItem make_fusk_check_menu_item()
    //**********************************************************
    {
        String key = Feature.Fusk_is_on.name();
        String text = My_I18n.get_I18n_string(key,virtual_landscape.owner,virtual_landscape.logger);

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Feature_cache.get(Feature.Fusk_is_on));
        item.setOnAction(actionEvent ->
        {
            boolean val = ((CheckMenuItem) actionEvent.getSource()).isSelected();
            Feature_cache.update_cached_boolean(Feature.Fusk_is_on,val,owner);

        });
        add_question_mark_button(key, item);
        return item;
    }


    //**********************************************************
    public MenuItem make_add_to_Enable_face_recognition_training_set_menu_item()
    //**********************************************************
    {
        String key = "Add_all_images_to_face_recognition_training_set";
        String text = My_I18n.get_I18n_string(key,virtual_landscape.owner,virtual_landscape.logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> {

            Face_recognition_service i = Face_recognition_service.get_instance(owner,virtual_landscape.logger);
            virtual_landscape.logger.log("NOT IMPLEMENTED add_all_pictures_to_training_set for "+virtual_landscape.path_list_provider.get_name());

        });
        add_question_mark_button(key, item);
        return item;
    }

    //**********************************************************
    public MenuItem make_save_face_recog_menu_item()
    //**********************************************************
    {
        String text = "Save face recognition";
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Face_recognition_service.save());
        return item;
    }

    //**********************************************************
    public MenuItem make_load_face_recog_menu_item()
    //**********************************************************
    {
        String text = "Load face recognition";
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Face_recognition_service.load(owner,virtual_landscape.logger));
        return item;
    }


    //**********************************************************
    public MenuItem make_reset_face_recog_menu_item()
    //**********************************************************
    {
        String text = "Reset/init face recognition";
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Face_recognition_service.start_new(owner,virtual_landscape.logger));
        return item;
    }


    //**********************************************************
    public MenuItem make_start_auto_face_recog_menu_item()
    //**********************************************************
    {
        String text = "Auto face recognition";
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Face_recognition_service.auto(Path.of(virtual_landscape.path_list_provider.get_name()),owner,virtual_landscape.logger));
        return item;
    }

    //**********************************************************
    public MenuItem make_start_self_face_recog_menu_item()
    //**********************************************************
    {
        String text = "SELF face recognition";
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Face_recognition_service.self(owner,virtual_landscape.logger));
        return item;
    }


    //**********************************************************
    public MenuItem make_remove_recursively_empty_folders_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Remove_empty_folders_recursively",virtual_landscape.owner,virtual_landscape.logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> remove_empty_folders_recursively_fx());
        return item;
    }

    //**********************************************************
    public void remove_empty_folders_fx()
    //**********************************************************
    {
        remove_empty_folders_fx(false);
    }

    //**********************************************************
    public void remove_empty_folders_recursively_fx()
    //**********************************************************
    {
        remove_empty_folders_fx(true);
    }

    //**********************************************************
    public void remove_empty_folders_fx(boolean recursively)
    //**********************************************************
    {
        virtual_landscape.remove_empty_folders(recursively);
        // can be called from a thread which is NOT the FX event thread
        Jfx_batch_injector.inject(() -> virtual_landscape.redraw_fx("remove empty folder"),virtual_landscape.logger);
    }

    //**********************************************************
    public MenuItem make_remove_empty_folders_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Remove_empty_folders",virtual_landscape.owner,virtual_landscape.logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> remove_empty_folders_fx());
        return item;
    }

    //**********************************************************
    public MenuItem make_select_all_folders_menu_item(Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Select_all_folders_for_drag_and_drop",virtual_landscape.owner,virtual_landscape.logger);

        select_all_folders_menu_item = new CheckMenuItem(text);
        select_all_folders_menu_item.setSelected(false);
        select_all_folders_menu_item.setOnAction(event -> {
            if ( ((CheckMenuItem) event.getSource()).isSelected())
            {
                virtual_landscape.selection_handler.reset_selection();
                virtual_landscape.selection_handler.add_into_selected_files(virtual_landscape.get_folder_list());
                virtual_landscape.selection_handler.set_select_all_folders(true);
            }
            else
            {
                virtual_landscape.selection_handler.reset_selection();
                virtual_landscape.selection_handler.set_select_all_folders(false);
            }
        });
        return select_all_folders_menu_item;
    }

    //**********************************************************
    public MenuItem make_select_all_files_menu_item(Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Select_all_files_for_drag_and_drop",virtual_landscape.owner,virtual_landscape.logger);

        select_all_files_menu_item= new CheckMenuItem(text+ " (meta-A)");
        select_all_files_menu_item.setSelected(false);
        select_all_files_menu_item.setOnAction(event -> {
            if ( ((CheckMenuItem) event.getSource()).isSelected())
            {
                virtual_landscape.selection_handler.select_all_files_in_folder(virtual_landscape.path_list_provider);
            }
            else
            {
                virtual_landscape.selection_handler.reset_selection();
                virtual_landscape.selection_handler.set_select_all_files(false);
            }
        });
        return select_all_files_menu_item;
    }


    //**********************************************************
    public void reset_all_files_and_folders()
    //**********************************************************
    {
        if(select_all_files_menu_item != null) select_all_files_menu_item.setSelected(false);
        if(select_all_folders_menu_item != null) select_all_folders_menu_item.setSelected(false);
    }

    //**********************************************************
    public Menu make_history_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("History",virtual_landscape.owner,virtual_landscape.logger);

        Menu history_menu = new Menu(text);
        create_history_menu(history_menu);
        return history_menu;
    }

    //**********************************************************
    public Menu make_bookmarks_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Bookmarks",virtual_landscape.owner,virtual_landscape.logger);
        Menu bookmarks_menu = new Menu(text);
        create_bookmarks_menu(bookmarks_menu);
        return bookmarks_menu;
    }
    //**********************************************************
    public Menu make_roots_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("File_System_Roots",virtual_landscape.owner,virtual_landscape.logger);
        Menu roots_menu = new Menu(text);
        create_roots_menu(roots_menu);
        return roots_menu;
    }
    //**********************************************************
    public Menu make_undos_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Undo",virtual_landscape.owner,virtual_landscape.logger);
        Menu undos_menu = new Menu(text);
        create_undos_menu(undos_menu);
        return undos_menu;
    }



    //**********************************************************
    public void create_history_menu(Menu history_menu)
    //**********************************************************
    {
        {
            String text = My_I18n.get_I18n_string("Clear_History",virtual_landscape.owner,virtual_landscape.logger);
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> {
                virtual_landscape.logger.log("clearing history");
                History_engine.get(virtual_landscape.owner, virtual_landscape.aborter,virtual_landscape.logger).clear();
                New_window_context.replace_same_folder( virtual_landscape.shutdown_target,virtual_landscape.path_list_provider.get_folder_path(),virtual_landscape.get_top_left(),owner,virtual_landscape.logger);

            });
            history_menu.getItems().add(item);
        }

        int max_on_screen = 20;
        int on_screen = 0;
        MenuItem more = null;
        Map<String, History_item> path_already_done = new HashMap<>();
        for (History_item hi : History_engine.get(virtual_landscape.owner, virtual_landscape.aborter,virtual_landscape.logger).get_all_history_items())
        {
            if ( on_screen < max_on_screen)
            {
                if ( path_already_done.get(hi.value) != null)
                {
                    continue;
                }
                String displayed_string = hi.value;

                if ( displayed_string.length() > MAX_MENU_ITEM_STRING_LENGTH)
                {
                    // trick to avoid that the menu is not displayed when items are very wide
                    // which may happens for the largest fonts
                    displayed_string = displayed_string.substring(0,MAX_MENU_ITEM_STRING_LENGTH)+" ...";
                }
                MenuItem item = new MenuItem(displayed_string);
                item.setMnemonicParsing(false);
                if ( hi.value.equals(virtual_landscape.path_list_provider.get_folder_path().toAbsolutePath().toString()))
                {
                    // show the one we are in as inactive
                    item.setDisable(true);
                }
                if ( !hi.get_available())
                {
                    item.setDisable(true);
                }
                item.setOnAction(event ->
                {
                    Path path = Path.of(hi.value);
                    /*
                    if (Feature_cache.get(Feature.Enable_image_playlists))
                    {
                        if ( Guess_file_type.is_this_path_an_image_playlist(path))
                        {
                            virtual_landscape.logger.log("not implemented yet");
                            //New_window_context.replace_image_playlist(virtual_landscape.shutdown_target, path, owner,virtual_landscape.logger);
                            return;
                        }

                    }
                    */
                    Path old_folder_path = virtual_landscape.path_list_provider.get_folder_path();
                    Browsing_caches.scroll_position_cache_write(old_folder_path,virtual_landscape.get_top_left());

                    New_window_context.replace_different_folder( virtual_landscape.shutdown_target, Path.of(hi.value), owner,virtual_landscape.logger);
                });
                path_already_done.put(hi.value,hi);
                history_menu.getItems().add(item);
                on_screen++;
            }
            else
            {
                if ( more == null)
                {
                    String text = My_I18n.get_I18n_string("Show_Whole_History",virtual_landscape.owner,virtual_landscape.logger);
                    more =  new MenuItem(text);
                    history_menu.getItems().add(more);
                    more.setOnAction(actionEvent -> pop_up_whole_history());
                }
                add_to_whole_history(hi);
            }
        }
    }

    //**********************************************************
    private void add_to_whole_history(History_item hi)
    //**********************************************************
    {
        if ( the_whole_history == null) the_whole_history = new HashMap<>();
        the_whole_history.put(hi.time_stamp,hi.value);
    }


    //**********************************************************
    private void pop_up_whole_history()
    //**********************************************************
    {
        Active_list_stage_action action = text -> {
            Browsing_caches.scroll_position_cache_write(virtual_landscape.path_list_provider.get_folder_path(),virtual_landscape.get_top_left());
            New_window_context.replace_different_folder( virtual_landscape.shutdown_target, Path.of(text), owner, virtual_landscape.logger);
        };
        Datetime_to_signature_source source = new Datetime_to_signature_source() {
            @Override
            public Map<LocalDateTime, String> get_map_of_date_to_signature() {
                return the_whole_history;
            }
        };
        Active_list_stage.show_active_list_stage("Whole history: "+the_whole_history.size()+" items", source, action,virtual_landscape.logger);
    }

    //**********************************************************
    public void create_bookmarks_menu( Menu bookmarks_menu)
    //**********************************************************
    {
        Path local = virtual_landscape.path_list_provider.get_folder_path();
        {
            String text = My_I18n.get_I18n_string("Bookmark_this",virtual_landscape.owner,virtual_landscape.logger);
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> Bookmarks.get(virtual_landscape.owner, virtual_landscape.aborter,virtual_landscape.logger).add(local.toAbsolutePath().toString()));
            bookmarks_menu.getItems().add(item);
        }
        {
            String text = My_I18n.get_I18n_string("Clear_Bookmarks",virtual_landscape.owner,virtual_landscape.logger);
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> Bookmarks.get(virtual_landscape.owner,virtual_landscape.aborter,virtual_landscape.logger).clear());
            bookmarks_menu.getItems().add(item);
        }

        for (String hi : Bookmarks.get(virtual_landscape.owner,virtual_landscape.aborter,virtual_landscape.logger).get_list())
        {
            //if ( hi.equals(local.toAbsolutePath().toString()))
            //{
                // no interrest in showing the one we are in !
            //    continue;
            //}
            MenuItem item = new MenuItem(hi);
            item.setOnAction(event -> {
                Browsing_caches.scroll_position_cache_write(virtual_landscape.path_list_provider.get_folder_path(),virtual_landscape.get_top_left());
                New_window_context.replace_different_folder( virtual_landscape.shutdown_target, Path.of(hi), owner,virtual_landscape.logger);
            });
            bookmarks_menu.getItems().add(item);

        }
    }


    //**********************************************************
    public void create_undos_menu(Menu undos_menu)
    //**********************************************************
    {
        double x = owner.getX()+100;
        double y = owner.getY()+100;

        undos_menu.getItems().add(make_menu_item(
                "Undo_LAST_move_or_delete",
                event -> Undo_for_moves.perform_last_undo_fx(owner, x, y, virtual_landscape.logger)));
        undos_menu.getItems().add(make_menu_item(
                "Show_Undos",
                event -> pop_up_whole_undo_history()));
        undos_menu.getItems().add(make_menu_item(
                "Clear_Undos",
                event -> Undo_for_moves.remove_all_undo_items(owner, virtual_landscape.logger)));
    }

    //**********************************************************
    public void create_roots_menu(Menu roots_menu)
    //**********************************************************
    {
        for ( File f : File.listRoots())
        {
            String text = f.getAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> {
                Browsing_caches.scroll_position_cache_write(virtual_landscape.path_list_provider.get_folder_path(),virtual_landscape.get_top_left());
                New_window_context.replace_different_folder( virtual_landscape.shutdown_target,f.toPath(),owner,virtual_landscape.logger);
            });
            roots_menu.getItems().add(item);
        }
    }

    //**********************************************************
    private void pop_up_whole_undo_history()
    //**********************************************************
    {
        Active_list_stage_action action = signature ->
        {
            Map<String, Undo_item> signature_to_undo_item = Undo_for_moves.get_instance(virtual_landscape.owner, virtual_landscape.logger).get_signature_to_undo_item();
            Undo_item item = signature_to_undo_item.get(signature);
            if ( item == null)
            {
                virtual_landscape.logger.log(Stack_trace_getter.get_stack_trace("item == null for signature="+signature));
                return;
            }
            if ( !Undo_for_moves.check_validity(item, virtual_landscape.owner,virtual_landscape.logger))
            {
                Popups.popup_warning("Invalid undo item ignored","The file was probably moved since?",true,owner,virtual_landscape.logger);
                Undo_for_moves.remove_invalid_undo_item(item, virtual_landscape.owner,virtual_landscape.logger);
                return;
            }
            virtual_landscape.logger.log("\n\n\n undo_item="+item.to_string());
            double x = owner.getX()+100;
            double y = owner.getY()+100;

            boolean ok = Popups.popup_ask_for_confirmation("Please confirm.",
                    "Undoing this will move the file(s) back to their original location.\n" +
                            item.to_string()+"\n"+
                            item.to_string(),
                    owner, virtual_landscape.logger);

            if ( ok) Undo_for_moves.perform_undo(item, owner, x, y, virtual_landscape.logger);
        };
        String title = My_I18n.get_I18n_string("Whole_Undo_History",virtual_landscape.owner,virtual_landscape.logger);
        Undo_for_moves.undo_stages.add(Active_list_stage.show_active_list_stage(title, Undo_for_moves.get_instance(virtual_landscape.owner,virtual_landscape.logger), action, virtual_landscape.logger));
    }



    //**********************************************************
    public Menu make_style_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Style",virtual_landscape.owner,virtual_landscape.logger);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        Menu menu = new Menu(text);
        for( Look_and_feel_style s : Look_and_feel_style.values())
        {
            create_check_menu_item_for_style(menu, s, all_check_menu_items);
        }
        Look_and_feel_style current_style = Look_and_feel_manager.get_instance(virtual_landscape.owner,virtual_landscape.logger).get_look_and_feel_style();
        if ( current_style == Look_and_feel_style.custom_color)
        {
            MenuItem custom_color_item = new MenuItem(My_I18n.get_I18n_string("Choose_Custom_Color",virtual_landscape.owner,virtual_landscape.logger));
            custom_color_item.setOnAction((ActionEvent e) -> invoke_color_picker());
            menu.getItems().add(custom_color_item);
        }
        return menu;
    }

    //**********************************************************
    private void invoke_color_picker()
    //**********************************************************
    {
        virtual_landscape.logger.log(("color picker !"));
        Color default_color = Non_booleans_properties.get_custom_color(virtual_landscape.owner);
        ColorPicker color_picker = new ColorPicker(default_color);
        Look_and_feel_manager.set_region_look(color_picker, virtual_landscape.owner,virtual_landscape.logger);
        color_picker.setOnAction((ActionEvent e) -> {
            Color new_color = color_picker.getValue();
            virtual_landscape.logger.log(("color picker new color = "+new_color));
            Non_booleans_properties.set_custom_color(new_color, virtual_landscape.owner);
            virtual_landscape.redraw_fx("custom color changed");
        });
        Stage color_picker_stage = new Stage();
        color_picker_stage.setTitle("Custom color picker (requires restart!)");
        color_picker_stage.initOwner(virtual_landscape.owner); // Set owner window
        // Create layout container
        VBox layout = new VBox(10); // 10 pixels spacing
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(10));
        layout.getChildren().add(color_picker);
        Scene color_picker_scene = new Scene(layout, 400, 200);
        color_picker_stage.setScene(color_picker_scene);
        color_picker_stage.show();
        virtual_landscape.logger.log(("color picker shown"));
    }

    //**********************************************************
    public void create_check_menu_item_for_style(Menu menu, Look_and_feel_style style, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem check_menu_item = new CheckMenuItem(style.name());
        Look_and_feel_style current_style = Look_and_feel_manager.get_instance(virtual_landscape.owner,virtual_landscape.logger).get_look_and_feel_style();
        check_menu_item.setSelected(current_style.name().equals(style.name()));
        check_menu_item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Look_and_feel_manager.set_look_and_feel(style,  virtual_landscape.owner,virtual_landscape.logger);
            }
        });
        menu.getItems().add(check_menu_item);
        all_check_menu_items.add(check_menu_item);
    }

    //**********************************************************
    public Menu make_language_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Language",virtual_landscape.owner,virtual_landscape.logger);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        Menu menu = new Menu(text);
        String current = Non_booleans_properties.get_language_key(owner);

        for( Language language_key : Language.values())
        {
            create_check_menu_item_for_language(menu, language_key, current, all_check_menu_items);
        }
        return menu;
    }

    //**********************************************************
    public void create_check_menu_item_for_language(Menu menu, Language language_key, String current, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(language_key.name());
        item.setSelected(current.equals(language_key.name()));
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                My_I18n.set_new_language(language_key,  virtual_landscape.owner,virtual_landscape.logger); /// will trigger a repaint via String_change_target
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }

    //**********************************************************
    public void create_menu_item_for_one_video_length( Menu menu, int length, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Length_of_video_sample",virtual_landscape.owner,virtual_landscape.logger);
        CheckMenuItem item = new CheckMenuItem(text + " = " +length+" s");
        int actual_size = Non_booleans_properties.get_animated_gif_duration_for_a_video(owner);
        item.setSelected(actual_size == length);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_animated_gif_duration_for_a_video(length,owner);
                Popups.popup_warning( "Note well:","You have to clear the icon cache to see the effect for already visited folders",false,owner,virtual_landscape.logger);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }

    //**********************************************************
    public void create_menu_item_for_one_column_width(Menu menu, int length, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(Non_booleans_properties.COLUMN_WIDTH,virtual_landscape.owner,virtual_landscape.logger);
        CheckMenuItem item = new CheckMenuItem(text + " = " +length);
        int actual_size = Non_booleans_properties.get_column_width(owner);
        item.setSelected(actual_size == length);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_column_width(length,owner);
                virtual_landscape.redraw_fx("column width changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }
    //**********************************************************
    public void create_menu_item_for_one_icon_size(Menu menu, Icon_size icon_size, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        int target_size = icon_size.size();
        String txt = "";
        if (icon_size.is_divider())
        {
            txt = icon_size.divider()+" icons per row";
        }
        else
        {
            txt = My_I18n.get_I18n_string("Icon_Size",virtual_landscape.owner,virtual_landscape.logger) + " = " +target_size;
        }
        CheckMenuItem item = new CheckMenuItem(txt);
        int actual_size = Non_booleans_properties.get_icon_size(owner);
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_icon_size(target_size,owner);
                virtual_landscape.logger.log("icon size changed to "+target_size);
                virtual_landscape.redraw_fx("icon size changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    public void create_menu_item_for_one_folder_icon_size(Menu menu, int target_size, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string("Folder_Icon_Size",virtual_landscape.owner,virtual_landscape.logger) + " = " +target_size);
        int actual_size = Non_booleans_properties.get_folder_icon_size(owner);
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_folder_icon_size(target_size,owner);
                virtual_landscape.redraw_fx("folder icon size changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }

    //**********************************************************
    public void create_menu_item_for_one_font_size( Menu menu, double target_size, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string("Font_size",virtual_landscape.owner,virtual_landscape.logger) + " = " +target_size);
        double actual_size = Non_booleans_properties.get_font_size(owner,virtual_landscape.logger);
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if (cmi != local) cmi.setSelected(false);
                }
                Non_booleans_properties.set_font_size(target_size, owner);
                virtual_landscape.redraw_fx("font size changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    public Menu make_video_length_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Length_of_video_sample",virtual_landscape.owner,virtual_landscape.logger);
        Menu menu = new Menu(text);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        int[] possible_lenghts ={Non_booleans_properties.DEFAULT_VIDEO_LENGTH,2,3,5,7,10,15,20};
        for ( int l : possible_lenghts)
        {
            create_menu_item_for_one_video_length(menu, l, all_check_menu_items);
        }
        return menu;
    }
    //**********************************************************
    public Menu make_column_width_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(Non_booleans_properties.COLUMN_WIDTH,virtual_landscape.owner,virtual_landscape.logger);
        Menu menu = new Menu(text);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        int[] possible_lengths ={Virtual_landscape.MIN_COLUMN_WIDTH,400,500,600,800,1000,2000,4000};
        for ( int l : possible_lengths)
        {
            create_menu_item_for_one_column_width(menu, l, all_check_menu_items);
        }
        return menu;
    }
    //**********************************************************
    public Menu make_file_sort_method_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("File_Sorting_Method",virtual_landscape.owner,virtual_landscape.logger);
        Menu menu = new Menu(text);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for ( File_sort_by sort_by : File_sort_by.values())
        {
            if (( sort_by == File_sort_by.SIMILARITY_BY_PAIRS)||(sort_by == File_sort_by.SIMILARITY_BY_PURSUIT))
            {
                if ( !Booleans.get_boolean(Feature.Enable_image_similarity.name(),owner))
                {
                    continue;
                }
            }
            create_menu_item_for_one_file_sort_method(menu, sort_by, all_check_menu_items);
        }
        return menu;
    }

    //**********************************************************
    public void create_menu_item_for_one_file_sort_method( Menu menu, File_sort_by sort_by, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string(sort_by.name(),virtual_landscape.owner,virtual_landscape.logger));
        File_sort_by actual = File_sort_by.get_sort_files_by(virtual_landscape.path_list_provider.get_folder_path(),owner);
        item.setSelected(actual == sort_by);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                if ( actual != sort_by)
                {
                    File_sort_by.set_sort_files_by(virtual_landscape.path_list_provider.get_folder_path(),sort_by,owner,virtual_landscape.logger);
                    virtual_landscape.logger.log("new file/image sorting order= "+sort_by);
                    /*if (Feature_cache.get(Feature.Enable_image_playlists))
                    {
                        if (virtual_landscape.path_list_provider instanceof Playlist_path_list_provider)
                        {
                            virtual_landscape.logger.log("not implemented");
                            //New_window_context.replace_image_playlist(virtual_landscape.shutdown_target, virtual_landscape.path_list_provider.get_folder_path(), owner, virtual_landscape.logger);
                            return;
                        }
                    }*/
                    New_window_context.replace_same_folder( virtual_landscape.shutdown_target, virtual_landscape.path_list_provider.get_folder_path(), virtual_landscape.get_top_left(), owner,virtual_landscape.logger);
                }
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }

    //**********************************************************
    public Menu make_icon_size_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Icon_Size",virtual_landscape.owner,virtual_landscape.logger);
        Menu menu = new Menu(text);
        {
            MenuItem plus = new MenuItem("+ 10%");
            menu.getItems().add(plus);
            plus.setOnAction(actionEvent -> virtual_landscape.increase_icon_size());
        }
        {
            MenuItem moins = new MenuItem("- 10%");
            menu.getItems().add(moins);
            moins.setOnAction(actionEvent -> virtual_landscape.reduce_icon_size());
        }
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        List<Icon_size> icon_sizes = get_icon_sizes();
        for ( Icon_size icon_size : icon_sizes)
        {
            create_menu_item_for_one_icon_size(menu, icon_size, all_check_menu_items);
        }
        return menu;
    }

    //**********************************************************
    private List<Icon_size> get_icon_sizes()
    //**********************************************************
    {
        List<Icon_size> icon_sizes = new ArrayList<>();
        {
            int[] possible_sizes = {32, 64, 128, Non_booleans_properties.DEFAULT_ICON_SIZE, 512, 1024};
            for (int size : possible_sizes)
            {
                icon_sizes.add(new Icon_size(size, false, 0));
            }
        }
        {
            //compute icon size for N icons in a row
            double W = owner.getWidth()- virtual_landscape.slider_width;
            int[] possible_dividers = {3,4,5,10};
            for ( int divider : possible_dividers)
            {
                int size = (int) (W/divider);
                icon_sizes.add(new Icon_size(size, true, divider));
            }
        }
        int current_icon_size = Non_booleans_properties.get_icon_size(owner);
        Icon_size cur = new Icon_size(current_icon_size,false,0);
        if ( !icon_sizes.contains(cur)) icon_sizes.add(cur);
        Comparator<? super Icon_size> comp = new Comparator<Icon_size>() {
            @Override
            public int compare(Icon_size o1, Icon_size o2) {
                return Integer.valueOf(o1.size()).compareTo(Integer.valueOf(o2.size()));
            }
        };
        Collections.sort(icon_sizes,comp);
        return icon_sizes;
    }

    //**********************************************************
    public Menu make_folder_icon_size_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Folder_Icon_Size",virtual_landscape.owner,virtual_landscape.logger);
        Menu menu = new Menu(text);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        int[] possible_sizes ={Non_booleans_properties.DEFAULT_FOLDER_ICON_SIZE,64,128,256, 300,400,512};
        for ( int size : possible_sizes)
        {
            create_menu_item_for_one_folder_icon_size(menu, size, all_check_menu_items);
        }
        return menu;
    }
    //**********************************************************
    public Menu make_font_size_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Font_size",virtual_landscape.owner,virtual_landscape.logger);

        Menu menu = new Menu(text);
        double[] candidate_sizes = {10,12,14,16,18,20,22,24,26};
        List<Double> possible_sizes = new ArrayList<>();
        possible_sizes.add(Double.valueOf(Non_booleans_properties.get_font_size(owner,virtual_landscape.logger)));
        for (double candidateSize : candidate_sizes) {
            if (possible_sizes.contains((Double)candidateSize)) continue;
            possible_sizes.add((Double)candidateSize);
        }
        Collections.sort(possible_sizes);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for ( Double i : possible_sizes)
        {
            create_menu_item_for_one_font_size( menu, i, all_check_menu_items);
        }
        return menu;
    }


    //**********************************************************
    void clean_up_names_fx()
    //**********************************************************
    {
        if ( !Popups.popup_ask_for_confirmation( "EXPERIMENTAL! Are you sure?","Name cleaning will try to change all names in this folder, which may have very nasty consequences in a home or system folder",owner,virtual_landscape.logger)) return;

        Path dir = virtual_landscape.path_list_provider.get_folder_path();
        File[] files = dir.toFile().listFiles();
        if (files == null) return;
        List<Old_and_new_Path> l = new ArrayList<>();
        for (File f : files)
        {

            Path old_path = f.toPath();

            String old_name = old_path.getFileName().toString();

            boolean check_extension = !f.isDirectory();
            String new_name = Name_cleaner.clean(old_name,check_extension, virtual_landscape.logger);
            if (new_name.equals(old_name))
            {
                virtual_landscape.logger.log("skipping " + old_name + " as it is conformant");
                continue;
            }
            virtual_landscape.logger.log("processing "+old_name+" as it is NOT conformant, will try: "+new_name);
            Path new_path = Paths.get(dir.toString(),new_name);
            Old_and_new_Path oandn = new Old_and_new_Path(old_path, new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command,false);
            l.add(oandn);
        }
        double x = owner.getX()+100;
        double y = owner.getY()+100;

        Moving_files.perform_safe_moves_in_a_thread(owner,x,y,l, true, virtual_landscape.aborter, virtual_landscape.logger);

    }


    //**********************************************************
    void remove_corrupted_images_fx()
    //**********************************************************
    {
        Path dir = virtual_landscape.path_list_provider.get_folder_path();
        File[] files = dir.toFile().listFiles();
        if ( files == null) return;
        List<Path> to_be_deleted =  new ArrayList<>();
        for (File f : files)
        {
            if ( f.isDirectory()) continue;

            if ( f.getName().startsWith("._"))
            {
                // this is 'debatable' since it removes MacOs extended attributes in extFat
                // but is suoer useful e.g when one reloads files from a backup USB drive
                virtual_landscape.logger.log("file name starts with ._, removed "+f.getName());
                to_be_deleted.add(f.toPath());
                continue;
            }

            if ( !Guess_file_type.is_file_an_image(f)) continue;
            Exif_metadata_extractor e = new Exif_metadata_extractor(f.toPath(),virtual_landscape.owner,virtual_landscape.logger);
            e.get_exif_metadata(0, true,virtual_landscape.aborter, false);
            if( !e.is_image_damaged()) continue;
            to_be_deleted.add(f.toPath());
        }
        if ( to_be_deleted.size() == 0)
        {
            virtual_landscape.logger.log("no corrupted images found");
            return;
        }
        double x = owner.getX()+100;
        double y = owner.getY()+100;

        virtual_landscape.path_list_provider.delete_multiple(to_be_deleted,owner,x,y,virtual_landscape.aborter,virtual_landscape.logger);

    }




    //**********************************************************
    public Menu make_backup_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Backup",virtual_landscape.owner,virtual_landscape.logger);
        Menu menu = new Menu(text);
        menu.getItems().add(make_menu_item("Set_as_backup_source_folder",event -> virtual_landscape.you_are_backup_source()));
        menu.getItems().add(make_menu_item("Set_as_backup_destination_folder",event -> virtual_landscape.you_are_backup_destination()));
        menu.getItems().add(make_menu_item("Start_backup",event -> virtual_landscape.start_backup()));
        menu.getItems().add(make_menu_item("Abort_backup",event -> virtual_landscape.abort_backup()));
        menu.getItems().add(make_menu_item("Backup_help",event -> show_backup_help(virtual_landscape.logger)));
        return menu;
    }
    //**********************************************************
    public Menu make_import_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Import",virtual_landscape.owner,virtual_landscape.logger);
        Menu menu = new Menu(text);
        menu.getItems().add(make_menu_item("Estimate_Size_Of_Import_Apple_Photos",event -> virtual_landscape.estimate_size_of_importing_apple_Photos()));
        menu.getItems().add(make_menu_item("Import_Apple_Photos",event -> virtual_landscape.import_apple_Photos()));
        return menu;
    }
    //**********************************************************
    public Menu make_fusk_menu()
    //**********************************************************
    {
        String text = "Fusk (experimental!)"; //My_I18n.get_I18n_string("Backup",virtual_landscape.logger);
        Menu menu = new Menu(text);
        menu.getItems().add(make_menu_item("Enter fusk pin code",event -> virtual_landscape.enter_fusk_pin_code()));
        menu.getItems().add(make_menu_item("Set this folder as fusk source",event -> virtual_landscape.you_are_fusk_source()));
        menu.getItems().add(make_menu_item("Set this folder as fusk destination",event -> virtual_landscape.you_are_fusk_destination()));
        menu.getItems().add(make_menu_item("Start fusk (experimental!)",event -> virtual_landscape.start_fusk()));
        menu.getItems().add( make_menu_item("Abort fusk",event -> virtual_landscape.abort_fusk()));
        menu.getItems().add(make_menu_item("Start defusk (experimental!)",event -> virtual_landscape.start_defusk()));
        menu.getItems().add(make_menu_item("Fusk help",event -> show_fusk_help()));
        return menu;
    }

    //**********************************************************
    private void show_backup_help(Logger logger)
    //**********************************************************
    {
        List<Line_for_info_stage> l = new ArrayList<>();
        l.add(new Line_for_info_stage(false,"The backup tool will copy recursively down the paths starting in the SOURCE folder"));
        l.add(new Line_for_info_stage(false,"into the DESTINATION folder"));
        l.add(new Line_for_info_stage(false,"It detects if identical names designate identical files in terms of file content"));
        l.add(new Line_for_info_stage(false,"If names and content are the same, the file is not copied (it is not a brute force copy)"));
        l.add(new Line_for_info_stage(false,"If names are matching but content is different, the source file is copied"));
        l.add(new Line_for_info_stage(false,"and the previous file in the destination is renamed"));
        Info_stage.show_info_stage("Help on backup",l, null);
    }
    //**********************************************************
    private void show_fusk_help()
    //**********************************************************
    {
        List<Line_for_info_stage> l = new ArrayList<>();
        l.add(new Line_for_info_stage(false,"Fusk tool: create obsfuscated files that can only be decoded by Klik"));
        l.add(new Line_for_info_stage(false,"The fusk tool will copy recursively down the paths starting in the SOURCE folder"));
        l.add(new Line_for_info_stage(false,"into the DESTINATION folder"));
        l.add(new Line_for_info_stage(false,"It obfuscates all files in the destination"));
        l.add(new Line_for_info_stage(false,"You will be asked for a pin code"));
        l.add(new Line_for_info_stage(false,"You can have multiple pin codes, but at any point of time, klik uses only one"));
        l.add(new Line_for_info_stage(false,"If the pin code is not the good one the images are not displayed"));
        l.add(new Line_for_info_stage(false,"WARNING: this is encryption, if you forget your pin code, recovering your files will be painful"));
        l.add(new Line_for_info_stage(false,"(recovery: someone will have to make a brute force attack code i.e. try all possible pin codes!)"));
        Info_stage.show_info_stage("Help on fusk",l, null);
    }


    //**********************************************************
    public MenuItem get_advanced_preferences()
    //**********************************************************
    {
        return make_menu_item("Advanced_And_Experimental_Features",event -> Preferences_stage.show_Preferences_stage("Preferences", virtual_landscape.owner,virtual_landscape.logger));
    }
}
