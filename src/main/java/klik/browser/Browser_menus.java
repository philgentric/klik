package klik.browser;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Stage;

import klik.browser.icons.Icon_manager;
import klik.browser.items.Item_button;
import klik.change.Change_receiver;
import klik.change.Undo_engine;
import klik.change.Undo_item;
import klik.change.active_list_stage.Datetime_to_signature_source;
import klik.files_and_paths.*;
import klik.images.Image_context;
import klik.images.decoding.Exif_metadata_extractor;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.metadata.Tag_items_management_stage;
import klik.my_i18n.I18n;
import klik.my_i18n.Language_manager;
import klik.properties.*;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;
import klik.change.active_list_stage.Active_list_stage;
import klik.change.active_list_stage.Active_list_stage_action;
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
    public Menu make_backup_menu()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Backup",logger);// to: " + parent.toAbsolutePath().toString();
        Menu menu = new Menu(text);
        {
            MenuItem mi = make_set_as_backup_source_menu_item();
            menu.getItems().add(mi);
        }
        {
            MenuItem mi = make_set_as_backup_sink_menu_item();
            menu.getItems().add(mi);
        }
        {
            MenuItem mi = make_start_backup_menu_item();
            menu.getItems().add(mi);
        }
        {
            MenuItem mi = make_abort_backup_menu_item();
            menu.getItems().add(mi);
        }

        {
            String text2 = I18n.get_I18n_string("Backup_help",logger);// to: " + parent.toAbsolutePath().toString();
            MenuItem item = new MenuItem(text2);
            item.setOnAction(event -> show_backup_help(logger));
            menu.getItems().add(item);
        }
        return menu;
    }
    //**********************************************************
    public Menu make_fusk_menu()
    //**********************************************************
    {
        String text = "Fusk (experimental!)"; //I18n.get_I18n_string("Backup",logger);// to: " + parent.toAbsolutePath().toString();
        Menu menu = new Menu(text);
        {
            MenuItem mi = make_set_as_fusk_source_menu_item();
            menu.getItems().add(mi);
        }
        {
            MenuItem mi = make_set_as_fusk_sink_menu_item();
            menu.getItems().add(mi);
        }
        {
            MenuItem mi = make_start_fusk_menu_item();
            menu.getItems().add(mi);
        }
        {
            MenuItem mi = make_abort_fusk_menu_item();
            menu.getItems().add(mi);
        }
        {
            MenuItem mi = make_start_defusk_menu_item();
            menu.getItems().add(mi);
        }
        {
            String text2 = "Fusk help";//I18n.get_I18n_string("Backup_help",logger);// to: " + parent.toAbsolutePath().toString();
            MenuItem item = new MenuItem(text2);
            item.setOnAction(event -> show_fusk_help(logger));
            menu.getItems().add(item);
        }
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
    private void show_fusk_help(Logger logger)
    //**********************************************************
    {

        List<Line_for_info_stage> l = new ArrayList<>();
        l.add(new Line_for_info_stage(false,"Fusk tool: create obsfuscated files that can only be decoded by Klik"));
        l.add(new Line_for_info_stage(false,"The fusk tool will copy recursively down the paths starting in the SOURCE folder"));
        l.add(new Line_for_info_stage(false,"into the DESTINATION folder"));
        l.add(new Line_for_info_stage(false,"It obfuscates all files in the destination"));
        l.add(new Line_for_info_stage(false,"WARNING: this is like encryption, but there is a single hardcoded password in Klik code"));

        Info_stage.show_info_stage("Help on fusk",l, null);
    }
    //**********************************************************
    public MenuItem make_set_as_fusk_source_menu_item()
    //**********************************************************
    {
        String text = "Set this folder as fusk source";//I18n.get_I18n_string("Set_as_backup_source_folder",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.you_are_fusk_source());
        return item;
    }

    //**********************************************************
    public MenuItem make_set_as_fusk_sink_menu_item()
    //**********************************************************
    {
        String text = "Set this folder as fusk sink";//I18n.get_I18n_string("Set_as_backup_sink_folder",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.you_are_fusk_sink());
        return item;
    }

    //**********************************************************
    public MenuItem make_start_fusk_menu_item()
    //**********************************************************
    {
        String text = "start fusk (experimental!)";//I18n.get_I18n_string("Start_backup",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.start_fusk());
        return item;
    }
    //**********************************************************
    public MenuItem make_start_defusk_menu_item()
    //**********************************************************
    {
        String text = "start defusk (experimental!)";//I18n.get_I18n_string("Start_backup",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.start_defusk());
        return item;
    }
    //**********************************************************
    public MenuItem make_abort_fusk_menu_item()
    //**********************************************************
    {
        String text = "Abort fusk";//I18n.get_I18n_string("Abort_backup",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.abort_fusk());
        return item;
    }

    //**********************************************************
    public MenuItem make_set_as_backup_source_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Set_as_backup_source_folder",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.you_are_backup_source());
        return item;
    }

    //**********************************************************
    public MenuItem make_set_as_backup_sink_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Set_as_backup_sink_folder",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.you_are_backup_sink());
        return item;
    }

    //**********************************************************
    public MenuItem make_start_backup_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Start_backup",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.start_backup());
        return item;
    }
    //**********************************************************
    public MenuItem make_abort_backup_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Abort_backup",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.abort_backup());
        return item;
    }

    /*
    // can be used instead of a menu bar
    //**********************************************************
    public void show_popup_menu(double x, double y)
    //**********************************************************
    {
        logger.log("Browser_menus.show_popup_menu()");
        ContextMenu context_menu;
        context_menu = new ContextMenu();
        context_menu.getItems().add(make_bookmarks_menu(browser, change_receiver, logger));
        context_menu.getItems().add(make_history_menu(browser, change_receiver, logger));
        context_menu.getItems().add(make_refresh_menu_item(browser, logger));
        context_menu.getItems().add(make_start_fullscreen_menu_item(browser, logger));
        context_menu.getItems().add(make_stop_fullscreen_menu_item(browser, logger));
        context_menu.getItems().add(make_create_empty_directory_menu_item(browser, logger));
        context_menu.getItems().add(make_new_window_menu_item(browser, change_receiver, logger));
        context_menu.getItems().add(make_start_stop_slideshow_menu_item(browser, logger));
        context_menu.getItems().add(make_select_all_files_menu_item(sh, logger));
        context_menu.getItems().add(make_select_all_folders_menu_item(sh, logger));
        context_menu.getItems().add(make_remove_empty_folders_menu_item(browser, logger));
        context_menu.getItems().add(make_remove_recursively_empty_folders_menu_item(browser, logger));
        context_menu.getItems().add(make_search_by_keywords_menu_item(browser, logger));
        context_menu.getItems().add(make_undo_menu_item(logger));
        context_menu.getItems().add(make_show_hidden_directories_check_menu_item(browser, logger));
        context_menu.getItems().add(make_show_hidden_files_check_menu_item(browser, logger));
        context_menu.getItems().add(make_show_gifs_first_check_menu_item(browser, logger));
        context_menu.getItems().add(make_sort_files_by_name_vs_decreasing_size_check_menu_item(browser, logger));
        context_menu.getItems().add(make_show_icons_for_images_and_videos_check_menu_item(browser, logger));
        context_menu.getItems().add(make_icon_size_menu(browser, logger));
        context_menu.getItems().add(make_font_size_menu_item(browser, logger));
        context_menu.getItems().add(make_style_menu_item(browser, change_receiver, logger));
        context_menu.getItems().add(make_language_menu(browser, change_receiver, logger));
        context_menu.getItems().add(make_invert_vertical_scroll_menu_item(logger));
        context_menu.getItems().add(make_clear_icon_cache_menu_item(logger));
        context_menu.getItems().add(make_clear_trash_menu_item(logger));
        context_menu.getItems().add(make_about_menu_item(logger));


        context_menu.show(browser.get_stage(), x, y);
    }
*/
    //**********************************************************
    public MenuItem make_about_menu_item(Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("About_klik",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> About_klik_stage.show_about_klik_stage());
        return item;
    }

    //**********************************************************
    public MenuItem make_clear_trash_menu_item(Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Clear_Trash_Folder",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Files_and_Paths.clear_trash(browser.my_Stage.the_Stage,browser.aborter,logger));
        return item;
    }
    //**********************************************************
    public MenuItem make_stored_tag_management_menu_item(Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Open_tag_management",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Tag_items_management_stage.open_tag_management_stage(logger));
        return item;
    }



    //**********************************************************
    public MenuItem make_clear_icon_disk_cache_menu_item(Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Clear_Icon_Cache_Folder",logger);// to: " + parent.toAbsolutePath().toString();

        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> {
            Files_and_Paths.clear_icon_cache_on_disk(browser.my_Stage.the_Stage,browser.aborter,logger);
            browser.icon_manager.clear_aspect_ratio_cache();
                });
        return item;
    }




    //**********************************************************
    public MenuItem make_clear_folder_icon_disk_cache_menu_item(Logger logger)
    //**********************************************************
    {
        String text = "Clear FOLDER icon cache on disk";//I18n.get_I18n_string("Clear_Icon_Cache_Folder",logger);// to: " + parent.toAbsolutePath().toString();

        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Files_and_Paths.clear_folder_icon_cache_on_disk(browser.my_Stage.the_Stage,browser.aborter,logger));
        return item;
    }

    //**********************************************************
    public CheckMenuItem make_invert_vertical_scroll_menu_item(Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Invert_vertical_scroll_direction",logger);// to: " + parent.toAbsolutePath().toString();

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_vertical_scroll_inverted(logger));
        item.setOnAction(actionEvent -> Static_application_properties.set_vertical_scroll_inverted(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger));
        return item;
    }

    //**********************************************************
    public CheckMenuItem make_show_icons_for_images_and_videos_check_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Show_icons_for_images_and_videos",logger);// to: " + parent.toAbsolutePath().toString();
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_show_icons(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_show_icons(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.scene_geometry_changed("show icons="+((CheckMenuItem) actionEvent.getSource()).isSelected(),true, true);
        });
        return item;
    }



    //**********************************************************
    public CheckMenuItem make_show_icons_for_folders_check_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Show_icons_for_folders",logger);// to: " + parent.toAbsolutePath().toString();
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_show_icons_for_folders(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_show_icons_for_folders(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.scene_geometry_changed("show icons for folders="+((CheckMenuItem) actionEvent.getSource()).isSelected(),true, false);
        });
        return item;
    }
    //**********************************************************
    public CheckMenuItem make_show_single_column_check_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Show_single_column",logger);// to: " + parent.toAbsolutePath().toString();
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_single_column(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_single_column(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.scene_geometry_changed("single column="+((CheckMenuItem) actionEvent.getSource()).isSelected(),false, false);
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
                text,
                height,
                is_trash,
                is_parent,
                logger);
        dummy.button_for_a_directory(text, min_width, height);
        return dummy.button;
    }

    //**********************************************************
    public CheckMenuItem make_show_folder_size_check_menu_item(Stage stage)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Show_number_of_files_in_folder_buttons",logger);// to: " + parent.toAbsolutePath().toString();
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_show_folder_size(logger));
        item.setOnAction(actionEvent -> {
            logger.log("Show_number_of_files_in_folder_buttons = "+Static_application_properties.get_show_folder_size(logger));
            if (!Static_application_properties.get_show_folder_size(logger) )
            {
                if (!Popups.popup_ask_for_confirmation(stage, "Warning", "This makes browsing into a new folder much slower", logger))
                {
                    ((CheckMenuItem) actionEvent.getSource()).setSelected(false);
                    return;
                }
            }
            Static_application_properties.set_show_folder_size(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.scene_geometry_changed("show folder size boolean changed",true,true);
        });
        return item;
    }



    //**********************************************************
    public MenuItem make_show_hidden_directories_check_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Show_hidden_directories",logger);// to: " + parent.toAbsolutePath().toString();

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_show_hidden_directories(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_show_hidden_directories(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.scene_geometry_changed("show hidden file boolean changed",true,false);
        });
        return item;
    }

    //**********************************************************
    public MenuItem make_monitor_browsed_folders_check_menu_item()
    //**********************************************************
    {
        String text = "Monitor browsed folders";//I18n.get_I18n_string("Show_hidden_directories",logger);// to: " + parent.toAbsolutePath().toString();

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_monitor_browsed_folders(logger));
        item.setOnAction(actionEvent -> Static_application_properties.set_monitor_browsed_folders(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger));
        return item;
    }

    //**********************************************************
    public MenuItem make_enable_fusk_check_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Enable_fusk",logger);

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_enable_fusk(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_enable_fusk(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.scene_geometry_changed("enable fusk boolean changed",true,false);
        });
        return item;
    }

    //**********************************************************
    public MenuItem make_show_hidden_files_check_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Show_hidden_files",logger);// to: " + parent.toAbsolutePath().toString();

        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(Static_application_properties.get_show_hidden_files(logger));
        item.setOnAction(actionEvent -> {
            Static_application_properties.set_show_hidden_files(((CheckMenuItem) actionEvent.getSource()).isSelected(),logger);
            browser.scene_geometry_changed("show hidden file boolean changed",true,false);
        });
        return item;
    }


    //**********************************************************
    public MenuItem make_undo_menu_item(Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Undo_LAST_move_or_delete",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Undo_engine.perform_last_undo(browser.my_Stage.the_Stage,logger));
        return item;
    }


    //**********************************************************
    public MenuItem make_remove_corrupted_images_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Remove_corrupted_images",logger);
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> remove_corrupted_images());
        return item;
    }
    //**********************************************************
    public MenuItem make_clean_names_menu_item()
    //**********************************************************
    {
        String text = "Clean up names (experimental)";//I18n.get_I18n_string("Search_images_by_keywords",logger);// to: " + parent.toAbsolutePath().toString();

        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> clean_up_names());
        return item;
    }

    //**********************************************************
    public MenuItem make_search_by_keywords_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Search_images_by_keywords",logger);// to: " + parent.toAbsolutePath().toString();

        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> search_images_by_keyworks());
        return item;
    }

    //**********************************************************
    public MenuItem make_remove_recursively_empty_folders_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Remove_empty_folders_recursively",logger);// to: " + parent.toAbsolutePath().toString();

        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> remove_empty_folders_recursively());
        return item;
    }

    //**********************************************************
    public void remove_empty_folders()
    //**********************************************************
    {
        remove_empty_folders(false);
    }

    //**********************************************************
    public void remove_empty_folders_recursively()
    //**********************************************************
    {
        remove_empty_folders(true);
    }

    //**********************************************************
    public void remove_empty_folders(boolean recursively)
    //**********************************************************
    {
        browser.icon_manager.paths_manager.remove_empty_folders(recursively);
        // can be called from a thread which is NOT the FX event thread
        Platform.runLater(() -> browser.scene_geometry_changed("remove empty folder", true, false));
    }

    //**********************************************************
    public MenuItem make_remove_empty_folders_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Remove_empty_folders",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> remove_empty_folders());
        return item;
    }

    //**********************************************************
    public MenuItem make_select_all_folders_menu_item(Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Select_all_folders_for_drag_and_drop",logger);// to: " + parent.toAbsolutePath().toString();

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
        String text = I18n.get_I18n_string("Select_all_files_for_drag_and_drop",logger);// to: " + parent.toAbsolutePath().toString();

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
    public MenuItem make_start_stop_slideshow_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Start_stop_slow_scan",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.handle_scan_switch());
        return item;
    }
    //**********************************************************
    public MenuItem make_slow_down_scan_slideshow_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Slow_down_scan",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.slow_down_scan());
        return item;
    }
    //**********************************************************
    public MenuItem make_speed_up_scan_slideshow_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Speed_up_scan",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.speed_up_scan());
        return item;
    }

    //**********************************************************
    public MenuItem make_new_window_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("New_window",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> Browser_creation_context.additional_same_folder(browser,logger));
        return item;
    }

    //**********************************************************
    public MenuItem make_create_empty_directory_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Create_new_empty_directory",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.create_new_directory());
        return item;
    }

    //**********************************************************
    public MenuItem make_stop_fullscreen_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Stop_full_screen",logger);

        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.stop_full_screen());
        return item;
    }

    //**********************************************************
    public MenuItem make_start_fullscreen_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Go_full_screen",logger);

        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.go_full_screen());
        return item;
    }

    //**********************************************************
    public MenuItem make_refresh_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Refresh",logger);// to: " + parent.toAbsolutePath().toString();

        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.scene_geometry_changed("refresh",false, false));
        return item;
    }

    //**********************************************************
    public Menu make_history_menu()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("History",logger);// to: " + parent.toAbsolutePath().toString();

        Menu history_menu = new Menu(text);
        create_history_menu(browser, history_menu, logger);
        return history_menu;
    }

    //**********************************************************
    public Menu make_bookmarks_menu()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Bookmarks",logger);// to: " + parent.toAbsolutePath().toString();
        Menu bookmarks_menu = new Menu(text);
        create_bookmarks_menu(browser,bookmarks_menu, logger);
        return bookmarks_menu;
    }
    //**********************************************************
    public Menu make_undos_menu()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Undo",logger);// to: " + parent.toAbsolutePath().toString();
        Menu undos_menu = new Menu(text);
        create_undos_menu(browser,undos_menu, logger);
        return undos_menu;
    }



    //**********************************************************
    public void create_history_menu(Browser browser, Menu history_menu, Logger logger)
    //**********************************************************
    {
        {
            String text = I18n.get_I18n_string("Clear_History",logger);// to: " + parent.toAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> {
                logger.log("clearing history");
                History.clear(logger);
                Browser_creation_context.replace_same_folder(browser,logger);

            });
            history_menu.getItems().add(item);
        }

        int max_on_screen = 20;
        int on_screen = 0;
        MenuItem more = null;
        Map<String,History_item> path_already_done = new HashMap<>();
        for (History_item hi : History.get_History_instance(logger).get_list())
        {
            if ( on_screen < max_on_screen)
            {
                if ( path_already_done.get(hi.path) != null)
                {
                    continue;
                }
                MenuItem item = new MenuItem(hi.path);
                if ( hi.path.equals(browser.displayed_folder_path.toAbsolutePath().toString()))
                {
                    // show the one we are in as inactive
                    item.setDisable(true);
                }
                item.setOnAction(event -> Browser_creation_context.replace_different_folder(Path.of(hi.path), browser,null,logger));
                path_already_done.put(hi.path,hi);
                history_menu.getItems().add(item);
                on_screen++;
            }
            else
            {
                //logger.log(max_on_screen+" exceeded creating 1 more menu");
                if ( more == null)
                {
                    String text = I18n.get_I18n_string("Show_Whole_History",logger);// to: " + parent.toAbsolutePath().toString();
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
        Active_list_stage_action action = text -> Browser_creation_context.replace_different_folder(Path.of(text),browser, null, logger);
        Datetime_to_signature_source source = new Datetime_to_signature_source() {
            @Override
            public Map<LocalDateTime, String> get() {
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
            String text = I18n.get_I18n_string("Bookmark_this",logger);// to: " + parent.toAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> Bookmarks.get_bookmarks(logger).add(browser.displayed_folder_path));
            bookmarks_menu.getItems().add(item);
        }
        {
            String text = I18n.get_I18n_string("Clear_Bookmarks",logger);// to: " + parent.toAbsolutePath().toString();
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
            item.setOnAction(event -> Browser_creation_context.replace_different_folder(Path.of(hi), browser, null, logger));
            bookmarks_menu.getItems().add(item);

        }
    }


    //**********************************************************
    public void create_undos_menu(Browser browser, Menu undos_menu, Logger logger)
    //**********************************************************
    {
        {
            MenuItem item = make_undo_menu_item(logger);
            undos_menu.getItems().add(item);
        }

        {
            String text = I18n.get_I18n_string("Clear_Undos",logger);// to: " + parent.toAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> Undo_engine.remove_all_undo_items(logger));
            undos_menu.getItems().add(item);
        }
        {
            String text = I18n.get_I18n_string("Show_Undos",logger);// to: " + parent.toAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            item.setOnAction(event -> pop_up_whole_undo_history());
            undos_menu.getItems().add(item);
        }
    }

    //**********************************************************
    private void pop_up_whole_undo_history()
    //**********************************************************
    {
        Active_list_stage_action action = signature ->
        {
            Map<String,Undo_item> signature_to_undo_item = Undo_engine.get_instance(logger).get_signature_to_undo_item();
            Undo_item item = signature_to_undo_item.get(signature);
            if ( item == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("item == null for signature="+signature));
                return;
            }

            if ( !Undo_engine.check_validity(item, browser.my_Stage.the_Stage,logger))
            {
                Popups.popup_warning(browser.my_Stage.the_Stage,"Invalid undo item ignored","The file was probably moved since?",true,logger);
                return;
            }
            logger.log("\n\n\n undo_item="+item.to_string());
            String header = I18n.get_I18n_string("Going_To_Undo_This",logger);// to: " + parent.toAbsolutePath().toString();
            if (Popups.popup_ask_for_confirmation(browser.my_Stage.the_Stage,header,signature,logger))
            {
                Undo_engine.perform_undo(item,browser.my_Stage.the_Stage,logger);
            }
        };
        String title = I18n.get_I18n_string("Whole_Undo_History",logger);
        Undo_engine.undo_stages.add(Active_list_stage.show_active_list_stage(title, Undo_engine.get_instance(logger), action, logger));
    }



    //**********************************************************
    public Menu make_style_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Style",logger);// to: " + parent.toAbsolutePath().toString();

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
        Look_and_feel current_style = Style.read_look_and_feel_from_properties_file(logger);
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
        String text = I18n.get_I18n_string("Language",logger);// to: " + parent.toAbsolutePath().toString();

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
        String text = I18n.get_I18n_string("Escape",logger);// to: " + parent.toAbsolutePath().toString();
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
                I18n.reset();
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
        String text = I18n.get_I18n_string("Length_of_video_sample",logger);
        CheckMenuItem item = new CheckMenuItem(text + " = " +length);
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
    public void create_menu_item_for_one_button_width(Browser browser, Menu menu, int length, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Button_width",logger);
        CheckMenuItem item = new CheckMenuItem(text + " = " +length);
        int actual_size = Static_application_properties.get_button_width(logger);
        item.setSelected(actual_size == length);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Static_application_properties.set_button_width(length,logger);
                browser.scene_geometry_changed("button width changed",true,false);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }
    //**********************************************************
    public void create_menu_item_for_one_icon_size(Browser browser, Menu menu, int target_size, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(I18n.get_I18n_string("Icon_size",logger) + " = " +target_size);
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
                browser.scene_geometry_changed("icon size changed",true,false);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }

    //**********************************************************
    public void create_menu_item_for_one_font_size(Browser browser, Menu menu, double target_size, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(I18n.get_I18n_string("Font_size",logger) + " = " +target_size);
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
                browser.scene_geometry_changed("font size changed", true, true);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }


    //**********************************************************
    public Menu make_video_length_menu()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Length_of_video_sample",logger);
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
    public Menu make_button_width_menu()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Button_width",logger);
        Menu menu = new Menu(text);
        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();

        int[] possible_lenghts ={Icon_manager.MIN_BUTTON_WIDTH,400,500,600,800,1000,2000,4000};
        for ( int l : possible_lenghts)
        {
            create_menu_item_for_one_button_width(browser, menu, l, all_check_menu_items, logger);
        }

        return menu;
    }
    //**********************************************************
    public Menu make_file_sort_method_menu()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("File_Sorting_Method",logger);// to: " + parent.toAbsolutePath().toString();

        Menu menu = new Menu(text);

        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();

        for ( File_sorter sort_by : File_sorter.values())
        {
            create_menu_item_for_one_file_sort_method(browser, menu, sort_by, all_check_menu_items, logger);
        }

        return menu;
    }

    //**********************************************************
    public void create_menu_item_for_one_file_sort_method(Browser browser, Menu menu, File_sorter sort_by, List<CheckMenuItem> all_check_menu_items, Logger logger)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem(I18n.get_I18n_string(sort_by.name(),logger));
        File_sorter actual = Static_application_properties.get_sort_files_by(logger);
        item.setSelected(actual == sort_by);
        item.setOnAction(actionEvent -> {
            CheckMenuItem local = (CheckMenuItem) actionEvent.getSource();
            if (local.isSelected()) {
                for ( CheckMenuItem cmi : all_check_menu_items)
                {
                    if ( cmi != local) cmi.setSelected(false);
                }
                Static_application_properties.set_sort_files_by(sort_by,logger);
                browser.scene_geometry_changed("file sorting method changed",true,false);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);

    }
    //**********************************************************
    public Menu make_icon_size_menu()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Icon_size",logger);// to: " + parent.toAbsolutePath().toString();

        Menu menu = new Menu(text);

        List<CheckMenuItem> all_check_menu_items = new ArrayList<>();

        int[] possible_sizes ={32,64,128, Static_application_properties.DEFAULT_ICON_SIZE,300,400,512,600,700,800,900,1024};
        for ( int size : possible_sizes)
        {
            create_menu_item_for_one_icon_size(browser, menu, size, all_check_menu_items, logger);
        }

        return menu;
    }
    //**********************************************************
    public Menu make_font_size_menu_item()
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Font_size",logger);// to: " + parent.toAbsolutePath().toString();

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
    private void clean_up_names()
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
            Old_and_new_Path oandn = new Old_and_new_Path(old_path, new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command);
            l.add(oandn);
        }
        Moving_files.perform_safe_moves_in_a_thread(browser.my_Stage.the_Stage,l, browser.aborter,true, logger);

    }



    //**********************************************************
    private void remove_corrupted_images()
    //**********************************************************
    {
        Path dir = browser.displayed_folder_path;
        File[] files = dir.toFile().listFiles();
        if ( files == null) return;
        for (File f : files)
        {
            if ( f.isDirectory()) continue;
            if ( !Guess_file_type.is_file_a_image(f)) continue;

            Exif_metadata_extractor e = new Exif_metadata_extractor(f.toPath(),logger);
            e.get_exif_metadata(0, browser.aborter);
            if( !e.is_image_damaged()) continue;
            Files_and_Paths.move_to_trash(browser.my_Stage.the_Stage,f.toPath(), null, browser.aborter, logger);
        }

    }

    //**********************************************************
    private void search_images_by_keyworks()
    //**********************************************************
    {

        List<String> given = new ArrayList<>();
        Image_context.ask_user_and_find(
                browser,
                browser.displayed_folder_path,
                given,
                logger
        );

    }

    public MenuItem make_show_how_many_files_menu_item()
    {
        String text = "Show how many files are in each folder";//I18n.get_I18n_string("Create_new_empty_directory",logger);// to: " + parent.toAbsolutePath().toString();
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> browser.show_how_many_files_in_each_folder());
        return item;
    }
}
