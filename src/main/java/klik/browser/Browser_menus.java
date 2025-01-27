//SOURCES ./About_klik_stage.java
//SOURCES ./Icon_size.java
//SOURCES ../image_ml/face_recognition/Face_recognition_service.java
//SOURCES ../image_ml/Ml_servers_util.java
//SOURCES ../image_ml/image_similarity/Image_feature_vector_cache.java
//SOURCES ../util/files_and_paths/Name_cleaner.java
//SOURCES ../level3/experimental/RAM_disk.java
package klik.browser;

import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import klik.actor.Aborter;
import klik.browser.icons.Virtual_landscape;
import klik.image_ml.Ml_servers_util;
import klik.browser.items.Item_button;
import klik.change.Change_receiver;
import klik.change.active_list_stage.Active_list_stage;
import klik.change.active_list_stage.Active_list_stage_action;
import klik.change.active_list_stage.Datetime_to_signature_source;
import klik.change.history.History_engine;
import klik.change.history.History_item;
import klik.change.undo.Undo_engine;
import klik.change.undo.Undo_item;
import klik.image_ml.face_recognition.Face_recognition_service;
import klik.look.my_i18n.My_I18n;
import klik.util.files_and_paths.*;
import klik.images.decoding.Exif_metadata_extractor;
import klik.level3.experimental.RAM_disk;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.Language_manager;
import klik.properties.Bookmarks;
import klik.properties.File_sort_by;
import klik.properties.Static_application_properties;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.log.Stack_trace_getter;
import klik.util.info_stage.Info_stage;
import klik.util.info_stage.Line_for_info_stage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

//**********************************************************
public class Browser_menus
//**********************************************************
{
    private static final int MAX_MENU_ITEM_STRING_LENGTH = 150;
    Logger logger;
    Selection_handler selection_handler;
    Browser browser;
    Change_receiver change_receiver;
    CheckMenuItem select_all_files_menu_item;
    CheckMenuItem select_all_folders_menu_item;
    Map<LocalDateTime,String> the_whole_history;

    //**********************************************************
    Browser_menus(Browser b_, Selection_handler selection_handler_, Logger logger_)
    //**********************************************************
    {
        browser = b_;
        selection_handler = selection_handler_;
        change_receiver = b_;
        logger = logger_;
    }


    //**********************************************************
    public MenuItem make_menu_item(String key, EventHandler<ActionEvent> ev)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(key,logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(ev);
        return item;
    }




    //**********************************************************
    public MenuItem make_clear_all_caches_menu_item(Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Clear_All_Caches",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> {
            browser.clear_all_RAM_caches();
            Static_files_and_paths_utilities.clear_all_DISK_caches(browser.my_Stage.the_Stage,browser.aborter,logger);
        });
        return item;
    }


    //**********************************************************
    public CheckMenuItem make_invert_vertical_scroll_menu_item(Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Invert_vertical_scroll_direction",logger);

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_vertical_scroll_inverted(logger));
        item.setOnAction(actionEvent -> Static_application_properties.set_vertical_scroll_inverted(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger));
        return item;
    }

    //**********************************************************
    public MenuItem make_start_servers_menu_item(Logger logger)
    //**********************************************************
    {
        String text = "Show manual about how to start servers";//My_I18n.get_I18n_string("Invert_vertical_scroll_direction",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> {
            Ml_servers_util.show_manual();
        });
        return item;
    }

    //**********************************************************
    public CheckMenuItem make_show_icons_for_images_and_videos_check_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Show_icons_for_images_and_videos",logger);
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_show_icons(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_show_icons(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.redraw_fx("show icons="+((CheckMenuItem) actionEvent.getSource()).isSelected());
        });
        return item;
    }



    //**********************************************************
    public CheckMenuItem make_show_icons_for_folders_check_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Show_icons_for_folders",logger);
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_show_icons_for_folders(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_show_icons_for_folders(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.redraw_fx("show icons for folders="+((CheckMenuItem) actionEvent.getSource()).isSelected());
        });
        return item;
    }
    //**********************************************************
    public CheckMenuItem make_show_single_column_check_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Show_single_column",logger);
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_single_column(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_single_column(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.redraw_fx("single column="+((CheckMenuItem) actionEvent.getSource()).isSelected());
        });
        return item;
    }

    //**********************************************************
    public Button make_button_that_behaves_like_a_folder(
            Browser browser,
            Path path,
            String text,
            double height,
            double min_width,
            boolean is_trash,
            boolean is_parent,
            Logger logger)
    //**********************************************************
    {
        // we make a Item_button but are only interested in the button...
        Item_button dummy = new Item_button(browser,
                path,
                null,
                text,
                height,
                is_trash,
                is_parent,
                logger);
        dummy.button_for_a_directory(text, min_width, height, null);
        return dummy.button;
    }



    //**********************************************************
    public MenuItem make_show_hidden_directories_check_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Show_hidden_directories",logger);

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_show_hidden_directories(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_show_hidden_directories(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.redraw_fx("show hidden file boolean changed");
        });
        return item;
    }

    //**********************************************************
    public MenuItem make_dont_zoom_small_images_check_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("DONT_ZOOM_SMALL_IMAGES",logger);

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_dont_zoom_small_images(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_dont_zoom_small_images(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.redraw_fx("dont_zoom_small_images boolean changed");
        });
        return item;
    }



    //**********************************************************
    public MenuItem make_use_RAM_disk_menu_item()
    //**********************************************************
    {
        String text = "Use RAM disk for caches (requires restart)";// My_I18n.get_I18n_string("Monitor_Browsed_Folders",logger);

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(RAM_disk.get_use_RAM_disk(logger));
        item.setOnAction(actionEvent -> RAM_disk.set_use_RAM_disk(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger));
        return item;

    }


    //**********************************************************
    public MenuItem make_stop_monitoring_menu_item()
    //**********************************************************
    {
        String text = "Stop all monitoring (requires restart to get it back on)";// My_I18n.get_I18n_string("Monitor_Browsed_Folders",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(actionEvent->{
            Browser.monitoring_aborter.abort("user stopped all monitoring");
            item.setDisable(true); // have to restart to reactivate
        });
        return item;
    }
    //**********************************************************
    public MenuItem make_monitor_browsed_folders_check_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Monitor_Browsed_Folders",logger);

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_monitor_browsed_folders(logger));
        item.setOnAction(actionEvent -> Static_application_properties.set_monitor_browsed_folders_fx(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger));
        return item;
    }




    //**********************************************************
    public MenuItem make_cache_size_limit_warning_menu_item(Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Set_The_Cache_Size_Warning_Limit",logger);

        MenuItem item = new MenuItem(text);

        item.setOnAction(actionEvent -> {
            //Static_application_properties.set_cache_size_limit_warning(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            TextInputDialog dialog = new TextInputDialog(""+Static_application_properties.get_folder_warning_size(logger));
            Look_and_feel_manager.set_dialog_look(dialog);
            dialog.initOwner(browser.my_Stage.the_Stage);
            dialog.setWidth(800);
            dialog.setTitle(My_I18n.get_I18n_string("Cache_Size_Warning_Limit", logger));
            dialog.setHeaderText("When the cache on disk is larger than this, you will receive a warning when klik starts. Zero means no limit.");
            dialog.setContentText(My_I18n.get_I18n_string("Set_The_Cache_Size_Warning_Limit", logger));


            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String new_val = result.get();
                try
                {
                    int val = Integer.parseInt(new_val);
                    Static_application_properties.set_cache_size_limit_warning_megabytes_fx(val,logger);

                }
                catch (NumberFormatException e)
                {
                    Popups.popup_warning(browser.my_Stage.the_Stage,"Integer only!","Please retry with an integer value!",false,logger);
                }
            }

        });
        return item;
    }



    //**********************************************************
    public MenuItem make_enable_fusk_check_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Enable_fusk",logger);

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_enable_fusk(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_enable_fusk(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.redraw_fx("enable fusk boolean changed");

        });
        return item;
    }


    //**********************************************************
    public MenuItem make_auto_purge_icon_disk_cache_check_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Auto_purge_cache",logger);

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_auto_purge_disk_caches(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_auto_purge_icon_disk_cache(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
        });
        return item;
    }

    //**********************************************************
    public MenuItem make_show_hidden_files_check_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Show_hidden_files",logger);

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_show_hidden_files(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_show_hidden_files(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.redraw_fx("show hidden file boolean changed");
        });
        return item;
    }

    //**********************************************************
    public MenuItem make_add_to_face_recognition_training_set_menu_item()
    //**********************************************************
    {
        String text = "Add all images to face recognition training set";
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> {
            Face_recognition_service i = Face_recognition_service.get_instance(browser);
            logger.log("NOT IMPLEMENTED add_all_pictures_to_training_set for "+browser.displayed_folder_path);

        });
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
        item.setOnAction(event -> Face_recognition_service.load(browser));
        return item;
    }


    //**********************************************************
    public MenuItem make_reset_face_recog_menu_item()
    //**********************************************************
    {
        String text = "Reset/init face recognition";
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Face_recognition_service.start_new(browser));
        return item;
    }


    //**********************************************************
    public MenuItem make_start_auto_face_recog_menu_item()
    //**********************************************************
    {
        String text = "Auto face recognition";
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Face_recognition_service.auto(browser));
        return item;
    }

    //**********************************************************
    public MenuItem make_start_self_face_recog_menu_item()
    //**********************************************************
    {
        String text = "SELF face recognition";
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Face_recognition_service.self(browser));
        return item;
    }


    //**********************************************************
    public MenuItem make_remove_recursively_empty_folders_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Remove_empty_folders_recursively",logger);
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
        browser.virtual_landscape.remove_empty_folders(recursively);
        // can be called from a thread which is NOT the FX event thread
        Jfx_batch_injector.inject(() -> browser.redraw_fx("remove empty folder"),logger);
    }

    //**********************************************************
    public MenuItem make_remove_empty_folders_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Remove_empty_folders",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> remove_empty_folders_fx());
        return item;
    }

    //**********************************************************
    public MenuItem make_select_all_folders_menu_item(Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Select_all_folders_for_drag_and_drop",logger);

        select_all_folders_menu_item = new CheckMenuItem(text);
        select_all_folders_menu_item.setSelected(false);
        select_all_folders_menu_item.setOnAction(event -> {
            if ( ((CheckMenuItem) event.getSource()).isSelected())
            {
                selection_handler.reset_selection();
                selection_handler.add_into_selected_files(browser.get_folder_list());
                selection_handler.set_select_all_folders(true);
            }
            else
            {
                selection_handler.reset_selection();
                selection_handler.set_select_all_folders(false);
            }
        });
        return select_all_folders_menu_item;
    }

    //**********************************************************
    public MenuItem make_select_all_files_menu_item(Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Select_all_files_for_drag_and_drop",logger);

        select_all_files_menu_item= new CheckMenuItem(text);
        select_all_files_menu_item.setSelected(false);
        select_all_files_menu_item.setOnAction(event -> {
            if ( ((CheckMenuItem) event.getSource()).isSelected())
            {
                selection_handler.select_all_files_in_folder(browser);
            }
            else
            {
                selection_handler.reset_selection();
                selection_handler.set_select_all_files_colors(false);
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
    public MenuItem make_create_empty_directory_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Create_new_empty_directory",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.create_new_directory());
        return item;
    }

    //**********************************************************
    public MenuItem make_create_PDF_contact_sheet_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Create_PDF_contact_sheet",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.create_PDF_contact_sheet());
        return item;
    }


    //**********************************************************
    public MenuItem make_sort_by_year_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Sort_Files_In_Folders_By_Year",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.sort_by_year());
        return item;
    }



    //**********************************************************
    public MenuItem make_stop_fullscreen_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Stop_full_screen",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.stop_full_screen());
        return item;
    }

    //**********************************************************
    public MenuItem make_start_fullscreen_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Go_full_screen",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.go_full_screen());
        return item;
    }

    //**********************************************************
    public MenuItem make_refresh_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Refresh",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.redraw_fx("refresh"));
        return item;
    }

    //**********************************************************
    public Menu make_history_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("History",logger);

        Menu history_menu = new Menu(text);
        create_history_menu(browser, history_menu, logger);
        return history_menu;
    }

    //**********************************************************
    public Menu make_bookmarks_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Bookmarks",logger);
        Menu bookmarks_menu = new Menu(text);
        create_bookmarks_menu(browser,bookmarks_menu, logger);
        return bookmarks_menu;
    }
    //**********************************************************
    public Menu make_roots_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("File_System_Roots",logger);
        Menu roots_menu = new Menu(text);
        create_roots_menu(browser,roots_menu, logger);
        return roots_menu;
    }
    //**********************************************************
    public Menu make_undos_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Undo",logger);
        Menu undos_menu = new Menu(text);
        create_undos_menu(browser,undos_menu, logger);
        return undos_menu;
    }



    //**********************************************************
    public void create_history_menu(Browser browser, Menu history_menu, Logger logger)
    //**********************************************************
    {
        {
            String text = My_I18n.get_I18n_string("Clear_History",logger);
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> {
                logger.log("clearing history");
                History_engine.clear(logger);
                Browser_creation_context.replace_same_folder(browser,logger);

            });
            history_menu.getItems().add(item);
        }

        int max_on_screen = 20;
        int on_screen = 0;
        MenuItem more = null;
        Map<String, History_item> path_already_done = new HashMap<>();
        for (History_item hi : History_engine.get_instance(logger).get_all_history_items())
        {
            if ( on_screen < max_on_screen)
            {
                if ( path_already_done.get(hi.path) != null)
                {
                    continue;
                }
                String displayed_string = hi.path;

                if ( displayed_string.length() > MAX_MENU_ITEM_STRING_LENGTH)
                {
                    // trick to avoid that the menu is not displayed when items are very wide
                    // which may happens for the largest fonts
                    displayed_string = displayed_string.substring(0,MAX_MENU_ITEM_STRING_LENGTH)+" ...";
                }
                MenuItem item = new MenuItem(displayed_string);
                if ( hi.path.equals(browser.displayed_folder_path.toAbsolutePath().toString()))
                {
                    // show the one we are in as inactive
                    item.setDisable(true);
                }
                if ( !hi.get_available())
                {
                    item.setDisable(true);
                }
                item.setOnAction(event -> Browser_creation_context.replace_different_folder(Path.of(hi.path), browser,logger));
                path_already_done.put(hi.path,hi);
                history_menu.getItems().add(item);
                on_screen++;
            }
            else
            {
                if ( more == null)
                {
                    String text = My_I18n.get_I18n_string("Show_Whole_History",logger);
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
        the_whole_history.put(hi.time_stamp,hi.path);
    }


    //**********************************************************
    private void pop_up_whole_history()
    //**********************************************************
    {
        Active_list_stage_action action = text -> Browser_creation_context.replace_different_folder(Path.of(text),browser, logger);
        Datetime_to_signature_source source = new Datetime_to_signature_source() {
            @Override
            public Map<LocalDateTime, String> get_map_of_date_to_signature() {
                return the_whole_history;
            }
        };
        Active_list_stage.show_active_list_stage("Whole history: "+the_whole_history.size()+" items", source, action, logger);
    }

    //**********************************************************
    public void create_bookmarks_menu(Browser browser, Menu bookmarks_menu, Logger logger)
    //**********************************************************
    {
        {
            String text = My_I18n.get_I18n_string("Bookmark_this",logger);
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> Bookmarks.get_bookmarks(logger).add(browser.displayed_folder_path));
            bookmarks_menu.getItems().add(item);
        }
        {
            String text = My_I18n.get_I18n_string("Clear_Bookmarks",logger);
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> Bookmarks.get_bookmarks(logger).clear());
            bookmarks_menu.getItems().add(item);
        }

        for (String hi : Bookmarks.get_bookmarks(logger).get_list())
        {
            if ( hi.equals(browser.displayed_folder_path.toAbsolutePath().toString()))
            {
                // no interrest in showing the one we are in !
                continue;
            }
            MenuItem item = new MenuItem(hi);
            item.setOnAction(event -> Browser_creation_context.replace_different_folder(Path.of(hi), browser, logger));
            bookmarks_menu.getItems().add(item);

        }
    }


    //**********************************************************
    public void create_undos_menu(Browser browser, Menu undos_menu, Logger logger)
    //**********************************************************
    {
        undos_menu.getItems().add(make_menu_item(
                "Undo_LAST_move_or_delete",
                event -> Undo_engine.perform_last_undo_fx(browser.my_Stage.the_Stage,browser.aborter, logger)));
        undos_menu.getItems().add(make_menu_item(
                "Show_Undos",
                event -> pop_up_whole_undo_history(browser.aborter)));
        undos_menu.getItems().add(make_menu_item(
                "Clear_Undos",
                event -> Undo_engine.remove_all_undo_items(browser.my_Stage.the_Stage,browser.aborter, logger)));
    }

    //**********************************************************
    public void create_roots_menu(Browser browser, Menu roots_menu, Logger logger)
    //**********************************************************
    {
        for ( File f : File.listRoots())
        {
            String text = f.getAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> {
                    Browser_creation_context.replace_different_folder(f.toPath(),browser,logger);
            });
            roots_menu.getItems().add(item);
        }
    }

    //**********************************************************
    private void pop_up_whole_undo_history(Aborter aborter)
    //**********************************************************
    {
        Active_list_stage_action action = signature ->
        {
            Map<String, Undo_item> signature_to_undo_item = Undo_engine.get_instance(aborter, logger).get_signature_to_undo_item();
            Undo_item item = signature_to_undo_item.get(signature);
            if ( item == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("item == null for signature="+signature));
                return;
            }
            if ( !Undo_engine.check_validity(item, browser.my_Stage.the_Stage,browser.aborter,logger))
            {
                Popups.popup_warning(browser.my_Stage.the_Stage,"Invalid undo item ignored","The file was probably moved since?",true,logger);
                return;
            }
            logger.log("\n\n\n undo_item="+item.to_string());
            Undo_engine.perform_undo(item,browser.my_Stage.the_Stage,aborter, logger);
        };
        String title = My_I18n.get_I18n_string("Whole_Undo_History",logger);
        Undo_engine.undo_stages.add(Active_list_stage.show_active_list_stage(title, Undo_engine.get_instance(aborter, logger), action, logger));
    }



    //**********************************************************
    public Menu make_style_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Style",logger);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        Menu menu = new Menu(text);
        for( Look_and_feel s : Look_and_feel_manager.registered)
        {
            create_check_menu_item_for_style(browser, menu, s, all_check_menu_items, logger);
        }
        return menu;
    }

    //**********************************************************
    public void create_check_menu_item_for_style(Browser browser, Menu menu, Look_and_feel style, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        CheckMenuItem check_menu_item = new CheckMenuItem(style.name);
        Look_and_feel current_style = Look_and_feel.read_look_and_feel_from_properties_file(logger);
        check_menu_item.setSelected(current_style.name.equals(style.name));
        check_menu_item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Look_and_feel_manager.set_look_and_feel(style);
                Browser_creation_context.replace_same_folder(browser,logger);
            }
        });
        menu.getItems().add(check_menu_item);
        all_check_menu_items.add(check_menu_item);
    }

    //**********************************************************
    public Menu make_language_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Language",logger);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        Menu menu = new Menu(text);
        for( String locale : Language_manager.get_registered_languages())
        {
            create_check_menu_item_for_language(browser, menu, locale, all_check_menu_items, logger);
        }
        return menu;
    }
    //**********************************************************
    public MenuItem make_escape_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Escape",logger);
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_escape(logger));
        item.setOnAction(actionEvent -> {
            boolean value = ((CheckMenuItem) actionEvent.getSource()).isSelected();
            Static_application_properties.set_escape(value,logger);
            browser.set_escape_preference(value);
        });
        return item;
    }

    //**********************************************************
    public MenuItem make_ding_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Play_Ding_When_Long_Operations_End",logger);
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_ding(logger));
        item.setOnAction(actionEvent -> {
            boolean value = ((CheckMenuItem) actionEvent.getSource()).isSelected();
            Static_application_properties.set_ding(value,logger);
        });
        return item;
    }

    //**********************************************************
    public void create_check_menu_item_for_language(Browser browser, Menu menu, String language, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(language);
        String current = Static_application_properties.get_language(logger);
        item.setSelected(current.equals(language));
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Language_manager.set_current_language(language);
                My_I18n.reset();
                Browser_creation_context.replace_same_folder(browser,logger);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }

    //**********************************************************
    public void create_menu_item_for_one_video_length(Browser browser, Menu menu, int length, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Length_of_video_sample",logger);
        CheckMenuItem item = new CheckMenuItem(text + " = " +length+" s");
        int actual_size = Static_application_properties.get_animated_gif_duration_for_a_video(logger);
        item.setSelected(actual_size == length);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Static_application_properties.set_animated_gif_duration_for_a_video(length,logger);
                Popups.popup_warning(browser.my_Stage.the_Stage, "Note well:","You have to clear the icon cache to see the effect for already visited folders",false,logger);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }

    //**********************************************************
    public void create_menu_item_for_one_column_width(Browser browser, Menu menu, int length, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(Static_application_properties.COLUMN_WIDTH,logger);
        CheckMenuItem item = new CheckMenuItem(text + " = " +length);
        int actual_size = Static_application_properties.get_column_width(logger);
        item.setSelected(actual_size == length);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Static_application_properties.set_column_width(length,logger);
                browser.redraw_fx("column width changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }
    //**********************************************************
    public void create_menu_item_for_one_icon_size(Browser browser, Menu menu, Icon_size icon_size, List<CheckMenuItem> all_check_menu_items, Logger logger)
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
            txt = My_I18n.get_I18n_string("Icon_Size",logger) + " = " +target_size;
        }
        CheckMenuItem item = new CheckMenuItem(txt);
        int actual_size = Static_application_properties.get_icon_size(logger);
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Static_application_properties.set_icon_size(target_size,logger);
                logger.log("icon size changed to "+target_size);
                browser.redraw_fx("icon size changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    public void create_menu_item_for_one_folder_icon_size(Browser browser, Menu menu, int target_size, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string("Folder_Icon_Size",logger) + " = " +target_size);
        int actual_size = Static_application_properties.get_folder_icon_size(logger);
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Static_application_properties.set_folder_icon_size(target_size,logger);
                browser.redraw_fx("folder icon size changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }

    //**********************************************************
    public void create_menu_item_for_one_font_size(Browser browser, Menu menu, double target_size, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string("Font_size",logger) + " = " +target_size);
        double actual_size = Static_application_properties.get_font_size(logger);
        item.setSelected(actual_size == target_size);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected())
            {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if (cmi != local) cmi.setSelected(false);
                }
                Static_application_properties.set_font_size(target_size,logger);
                browser.redraw_fx("font size changed");
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    public Menu make_video_length_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Length_of_video_sample",logger);
        Menu menu = new Menu(text);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        int[] possible_lenghts ={Static_application_properties.DEFAULT_VIDEO_LENGTH,2,3,5,7,10,15,20};
        for ( int l : possible_lenghts)
        {
            create_menu_item_for_one_video_length(browser, menu, l, all_check_menu_items, logger);
        }
        return menu;
    }
    //**********************************************************
    public Menu make_column_width_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(Static_application_properties.COLUMN_WIDTH,logger);
        Menu menu = new Menu(text);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        int[] possible_lengths ={Virtual_landscape.MIN_COLUMN_WIDTH,400,500,600,800,1000,2000,4000};
        for ( int l : possible_lengths)
        {
            create_menu_item_for_one_column_width(browser, menu, l, all_check_menu_items, logger);
        }
        return menu;
    }
    //**********************************************************
    public Menu make_file_sort_method_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("File_Sorting_Method",logger);
        Menu menu = new Menu(text);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for ( File_sort_by sort_by : File_sort_by.values())
        {
            create_menu_item_for_one_file_sort_method(browser, menu, sort_by, all_check_menu_items, logger);
        }
        return menu;
    }

    //**********************************************************
    public void create_menu_item_for_one_file_sort_method(Browser browser, Menu menu, File_sort_by sort_by, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(My_I18n.get_I18n_string(sort_by.name(),logger));
        File_sort_by actual = File_sort_by.get_sort_files_by(logger);
        item.setSelected(actual == sort_by);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                File_sort_by.set_sort_files_by(sort_by,logger);
                logger.log("new sorting order= "+sort_by);
                Browser_creation_context.replace_same_folder(browser,logger);
                //browser.scene_geometry_changed("file sorting method changed",true,false);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }

    //**********************************************************
    public Menu make_icon_size_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Icon_Size",logger);
        Menu menu = new Menu(text);
        {
            MenuItem plus = new MenuItem("+ 10%");
            menu.getItems().add(plus);
            plus.setOnAction(actionEvent -> browser.increase_icon_size());
        }
        {
            MenuItem moins = new MenuItem("- 10%");
            menu.getItems().add(moins);
            moins.setOnAction(actionEvent -> browser.reduce_icon_size());
        }
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        List<Icon_size> icon_sizes = get_icon_sizes();
        for ( Icon_size icon_size : icon_sizes)
        {
            create_menu_item_for_one_icon_size(browser, menu, icon_size, all_check_menu_items, logger);
        }
        return menu;
    }

    //**********************************************************
    private List<Icon_size> get_icon_sizes()
    //**********************************************************
    {
        List<Icon_size> icon_sizes = new ArrayList<>();
        {
            int[] possible_sizes = {32, 64, 128, Static_application_properties.DEFAULT_ICON_SIZE, 512, 1024};
            for (int size : possible_sizes)
            {
                icon_sizes.add(new Icon_size(size, false, 0));
            }
        }
        {
            //compute icon size for N icons in a row
            double W = browser.my_Stage.the_Stage.getWidth()- browser.slider_width;
            int[] possible_dividers = {3,4,5,10};
            for ( int divider : possible_dividers)
            {
                int size = (int) (W/divider);
                icon_sizes.add(new Icon_size(size, true, divider));
            }
        }
        int current_icon_size = Static_application_properties.get_icon_size(logger);
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
        String text = My_I18n.get_I18n_string("Folder_Icon_Size",logger);
        Menu menu = new Menu(text);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        int[] possible_sizes ={Static_application_properties.DEFAULT_FOLDER_ICON_SIZE,64,128,256, 300,400,512};
        for ( int size : possible_sizes)
        {
            create_menu_item_for_one_folder_icon_size(browser, menu, size, all_check_menu_items, logger);
        }
        return menu;
    }
    //**********************************************************
    public Menu make_font_size_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Font_size",logger);

        Menu menu = new Menu(text);
        double[] candidate_sizes = {10,12,14,16,18,20,22,24,26};
        List<Double> possible_sizes = new ArrayList<>();
        possible_sizes.add(Static_application_properties.get_font_size(logger));
        for (double candidateSize : candidate_sizes) {
            if (possible_sizes.contains(candidateSize)) continue;
            possible_sizes.add(candidateSize);
        }
        Collections.sort(possible_sizes);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
        for ( Double i : possible_sizes)
        {
            create_menu_item_for_one_font_size(browser, menu, i, all_check_menu_items, logger);
        }
        return menu;
    }


    //**********************************************************
    void clean_up_names_fx()
    //**********************************************************
    {
        if ( !Popups.popup_ask_for_confirmation(browser.my_Stage.the_Stage, "EXPERIMENTAL! Are you sure?","Name cleaning will try to change all names in this folder, which may have very nasty consequences in a home or system folder",logger)) return;

        Path dir = browser.displayed_folder_path;
        File[] files = dir.toFile().listFiles();
        if (files == null) return;
        List<Old_and_new_Path> l = new ArrayList<>();
        for (File f : files)
        {

            Path old_path = f.toPath();

            String old_name = old_path.getFileName().toString();

            boolean check_extension = !f.isDirectory();
            String new_name = Name_cleaner.clean(old_name,check_extension,logger);
            if (new_name.equals(old_name))
            {
                logger.log("skipping " + old_name + " as it is conformant");
                continue;
            }
            logger.log("processing "+old_name+" as it is NOT conformant, will try: "+new_name);
            Path new_path = Paths.get(dir.toString(),new_name);
            Old_and_new_Path oandn = new Old_and_new_Path(old_path, new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command,false);
            l.add(oandn);
        }
        Moving_files.perform_safe_moves_in_a_thread(browser.my_Stage.the_Stage,l, true, browser.aborter,logger);

    }


    //**********************************************************
    void remove_corrupted_images_fx()
    //**********************************************************
    {
        Path dir = browser.displayed_folder_path;
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
                logger.log("file name starts with ._, removed "+f.getName());
                to_be_deleted.add(f.toPath());
                continue;
            }

            if ( !Guess_file_type.is_file_an_image(f)) continue;
            Exif_metadata_extractor e = new Exif_metadata_extractor(f.toPath(),logger);
            e.get_exif_metadata(0, true, browser.aborter,false);
            if( !e.is_image_damaged()) continue;
            to_be_deleted.add(f.toPath());
        }
        Static_files_and_paths_utilities.move_to_trash(browser.my_Stage.the_Stage,to_be_deleted, null, browser.aborter, logger);

    }




    //**********************************************************
    public Menu make_backup_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Backup",logger);
        Menu menu = new Menu(text);
        menu.getItems().add(make_menu_item("Set_as_backup_source_folder",event -> browser.you_are_backup_source()));
        menu.getItems().add(make_menu_item("Set_as_backup_destination_folder",event -> browser.you_are_backup_destination()));
        menu.getItems().add(make_menu_item("Start_backup",event -> browser.start_backup()));
        menu.getItems().add(make_menu_item("Abort_backup",event -> browser.abort_backup()));
        menu.getItems().add(make_menu_item("Backup_help",event -> show_backup_help(logger)));
        return menu;
    }
    //**********************************************************
    public Menu make_import_menu()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Import",logger);
        Menu menu = new Menu(text);
        menu.getItems().add(make_menu_item("Import_Apple_Photos",event -> browser.import_apple_Photos()));
        menu.getItems().add(make_menu_item("Estimate_Size_Of_Import_Apple_Photos",event -> browser.estimate_size_of_importing_apple_Photos()));
        return menu;
    }
    //**********************************************************
    public Menu make_fusk_menu()
    //**********************************************************
    {
        String text = "Fusk (experimental!)"; //My_I18n.get_I18n_string("Backup",logger);
        Menu menu = new Menu(text);
        menu.getItems().add(make_menu_item("Enter fusk pin code",event -> browser.enter_fusk_pin_code()));
        menu.getItems().add(make_menu_item("Set this folder as fusk source",event -> browser.you_are_fusk_source()));
        menu.getItems().add(make_menu_item("Set this folder as fusk destination",event -> browser.you_are_fusk_destination()));
        menu.getItems().add(make_menu_item("Start fusk (experimental!)",event -> browser.start_fusk()));
        menu.getItems().add( make_menu_item("Abort fusk",event -> browser.abort_fusk()));
        menu.getItems().add(make_menu_item("Start defusk (experimental!)",event -> browser.start_defusk()));
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










}
