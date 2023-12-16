package klik.browser;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import klik.browser.icons.Icon_manager;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.Logger;

import java.util.ArrayList;
import java.util.List;


//**********************************************************
public class Browser_UI
//**********************************************************
{

    public final Browser browser;
    public final Logger logger;
    public final Browser_menus browser_menus;
    MenuItem stop_full_screen_menu_item;
    MenuItem start_full_screen_menu_item;
    List<Button> top_buttons = new ArrayList<>();
    private final boolean level2;

    //**********************************************************
    public Browser_UI(Browser b)
    //**********************************************************
    {
        browser = b;
        logger = browser.logger;
        browser_menus = browser.browser_menus;
        level2 = Static_application_properties.get_level2(logger);

    }

    //**********************************************************
    void define_UI()
    //**********************************************************
    {

        String go_up_text = "";
        if (browser.displayed_folder_path.getParent() != null)
        {
            go_up_text = I18n.get_I18n_string("Parent_Folder", logger);// to: " + parent.toAbsolutePath().toString();
        }
        double font_size = Static_application_properties.get_font_size(logger);
        double height = klik.look.Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
        Button up_button = browser_menus.make_button_that_behaves_like_a_folder(browser,
                browser.displayed_folder_path.getParent(),
                go_up_text,
                height,
                Icon_manager.MIN_PARENT_AND_TRASH_BUTTON_WIDTH,
                false,
                true,
                logger);
        {
            Image icon = Look_and_feel_manager.get_up_icon(height);
            if (icon == null)
                logger.log("WARNING: could not load " + Look_and_feel_manager.get_instance().get_up_icon_path());
            Look_and_feel_manager.set_button_and_image_look(up_button, icon, height, true);

        }
        browser.always_on_front_nodes.add(up_button);
        top_buttons.add(up_button);



        String trash_text = I18n.get_I18n_string("Trash", logger);// to: " + parent.toAbsolutePath().toString();
        Button trash = browser_menus.make_button_that_behaves_like_a_folder(
                browser,
                Static_application_properties.get_trash_dir(logger),
                trash_text,
                height,
                Icon_manager.MIN_PARENT_AND_TRASH_BUTTON_WIDTH,
                true,
                false,
                logger);
        {
            Image icon = Look_and_feel_manager.get_trash_icon(height);
            if (icon == null)
                logger.log("WARNING: could not load " + Look_and_feel_manager.get_instance().get_bookmarks_icon_path());
            Look_and_feel_manager.set_button_and_image_look(trash, icon, height, true);

        }
        browser.always_on_front_nodes.add(trash);
        top_buttons.add(trash);


        Pane top_pane = define_top_bar_using_buttons_deep(height, up_button, trash);


        BorderPane bottom_border_pane = define_bottom_pane(top_pane);

        browser.the_Scene = new Scene(bottom_border_pane);//, W, H);
        browser.error_type = browser.icon_manager.paths_manager.scan_dir(browser.displayed_folder_path, browser.my_Stage.the_Stage);
        switch (browser.error_type) {
            case OK:
                break;
            case DENIED:
                logger.log("access denied");
                browser.set_status("Acces denied for:" + browser.displayed_folder_path);
                break;
            case NOT_FOUND:
            case ERROR:
                logger.log("directory gone");
                browser.set_status("Folder is gone:" + browser.displayed_folder_path);
                break;

        }
        {
            //logger.log("creating vertical slider");
            browser.vertical_slider = new Vertical_slider(browser.the_Scene, browser.the_Pane, browser.icon_manager, logger);
            browser.mandatory_in_pane.add(browser.vertical_slider.the_Slider);
            browser.always_on_front_nodes.add(browser.vertical_slider.the_Slider);
            browser.slider_width = 2 * Vertical_slider.half_slider_width;
        }
        //set the view order (smaller means closer to viewer = on top)
        for (Node n : browser.always_on_front_nodes) n.setViewOrder(0);
        browser.the_Pane.setViewOrder(100);
        browser.apply_font();

    }


    //**********************************************************
    private BorderPane define_bottom_pane(Pane top_pane)
    //**********************************************************
    {
        BorderPane returned = new BorderPane();
        Look_and_feel_manager.set_pane_look(top_pane);


        returned.setTop(top_pane);
        returned.setCenter(browser.the_Pane);
        VBox the_status_bar = new VBox();
        browser.status = new TextField("Status: OK");
        the_status_bar.getChildren().add(browser.status);
        returned.setBottom(the_status_bar);
        return returned;
    }


    //**********************************************************
    private Pane define_top_bar_using_buttons_deep(double height, Button go_up, Button trash)
    //**********************************************************
    {
        Pane top_pane;
        top_pane = new VBox();
        {
            HBox top_pane2 = new HBox();
            top_pane2.setAlignment(Pos.CENTER);
            top_pane2.setSpacing(10);
            top_pane2.getChildren().add(go_up);
            define_top_bar_using_buttons_deep(top_pane2, height);
            Region spacer = new Region();
            top_pane2.getChildren().add(spacer);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            top_pane2.getChildren().add(trash);
            Region spacer2 = new Region();
            top_pane2.getChildren().add(spacer2);
            HBox.setHgrow(spacer2, Priority.SOMETIMES);
            Look_and_feel_manager.set_pane_look(top_pane2);
            top_pane.getChildren().add(top_pane2);
        }
        top_pane.getChildren().add(new Separator());
        //top_pane.setBackground(new Background(new BackgroundFill(Paint.valueOf("0xffffff"), null, null)));
        return top_pane;
    }


    //**********************************************************
    private void define_top_bar_using_buttons_deep(Pane top_pane, double height)
    //**********************************************************
    {
        {
            String bandh = I18n.get_I18n_string("Bookmarks", logger);
            bandh += " & " + I18n.get_I18n_string("History", logger);
            Button bandh_button = new Button(bandh);
            bandh_button.setOnAction(e -> button_bandh(e));
            top_pane.getChildren().add(bandh_button);
            top_buttons.add(bandh_button);
            browser.always_on_front_nodes.add(bandh_button);
            Image icon = Look_and_feel_manager.get_bookmarks_icon(height);
            Look_and_feel_manager.set_button_and_image_look(bandh_button, icon, height, false);
        }
        {
            String files = I18n.get_I18n_string("Files", logger);
            Button files_button = new Button(files);
            files_button.setOnAction(e -> button_files(e));
            top_pane.getChildren().add(files_button);
            top_buttons.add(files_button);
            browser.always_on_front_nodes.add(files_button);
            Image icon = Look_and_feel_manager.get_folder_icon(height);
            Look_and_feel_manager.set_button_and_image_look(files_button, icon, height, false);
        }
        {
            String view = I18n.get_I18n_string("View", logger);
            Button view_button = new Button(view);
            view_button.setOnAction(e -> button_view(e));
            top_pane.getChildren().add(view_button);
            top_buttons.add(view_button);
            browser.always_on_front_nodes.add(view_button);
            Image icon = Look_and_feel_manager.get_view_icon(height);
            Look_and_feel_manager.set_button_and_image_look(view_button, icon, height, false);
        }
        {
            String preferences = I18n.get_I18n_string("Preferences", logger);
            Button preferences_button = new Button(preferences);
            preferences_button.setOnAction(e -> button_preferences(e));
            top_pane.getChildren().add(preferences_button);
            top_buttons.add(preferences_button);
            browser.always_on_front_nodes.add(preferences_button);
            Image icon = Look_and_feel_manager.get_preferences_icon(height);
            Look_and_feel_manager.set_button_and_image_look(preferences_button, icon, height, false);
        }
    }

    //**********************************************************
    private void button_bandh(ActionEvent e)
    //**********************************************************
    {
        ContextMenu pref = define_contextmenu_bandh();
        Button b = (Button) e.getSource();
        pref.show(b, Side.TOP, 0, 0);
    }

    //**********************************************************
    private void button_preferences(ActionEvent e)
    //**********************************************************
    {
        ContextMenu pref = define_contextmenu_preferences();
        Button b = (Button) e.getSource();
        pref.show(b, Side.TOP, 0, 0);
    }

    //**********************************************************
    private void button_files(ActionEvent e)
    //**********************************************************
    {
        ContextMenu files = define_contextmenu_files();
        Button b = (Button) e.getSource();
        files.show(b, Side.TOP, 0, 0);
    }

    //**********************************************************
    private void button_view(ActionEvent e)
    //**********************************************************
    {
        ContextMenu view = define_contextmenu_view();
        Button b = (Button) e.getSource();
        view.show(b, Side.TOP, 0, 0);
    }

    //**********************************************************
    void on_fullscreen_end()
    //**********************************************************
    {
        // this is called either after the menu above OR if user pressed ESCAPE
        start_full_screen_menu_item.setDisable(false);
        stop_full_screen_menu_item.setDisable(true);
    }

    //**********************************************************
    void on_fullscreen_start()
    //**********************************************************
    {
        start_full_screen_menu_item.setDisable(true);
        stop_full_screen_menu_item.setDisable(false);
    }



    //**********************************************************
    private ContextMenu define_contextmenu_bandh()
    //**********************************************************
    {
        ContextMenu bandh_menu = new ContextMenu();
        bandh_menu.getItems().add(browser_menus.make_bookmarks_menu());
        bandh_menu.getItems().add(browser_menus.make_history_menu());
        bandh_menu.getItems().add(browser_menus.make_undos_menu());
        return bandh_menu;
    }

    //**********************************************************
    private ContextMenu define_contextmenu_view()
    //**********************************************************
    {
        ContextMenu view_menu = new ContextMenu();
        view_menu.getItems().add(browser_menus.make_new_window_menu_item());
        {
            start_full_screen_menu_item = browser_menus.make_start_fullscreen_menu_item();
            start_full_screen_menu_item.setDisable(false);
            view_menu.getItems().add(start_full_screen_menu_item);
        }
        {
            stop_full_screen_menu_item = browser_menus.make_stop_fullscreen_menu_item();
            stop_full_screen_menu_item.setDisable(true);
            view_menu.getItems().add(stop_full_screen_menu_item);
        }
        {
            Menu scan = new Menu("Scan show");
            scan.getItems().add(browser_menus.make_start_stop_slideshow_menu_item());
            scan.getItems().add(browser_menus.make_slow_down_scan_slideshow_menu_item());
            scan.getItems().add(browser_menus.make_speed_up_scan_slideshow_menu_item());

            view_menu.getItems().add(scan);
        }
        if (level2) view_menu.getItems().add(browser_menus.make_stored_tag_management_menu_item(logger));
        view_menu.getItems().add(browser_menus.make_about_menu_item(logger));
        if (level2) view_menu.getItems().add(browser_menus.make_refresh_menu_item());
        return view_menu;
    }

    //**********************************************************
    private ContextMenu define_contextmenu_files()
    //**********************************************************
    {
        ContextMenu files_menu = new ContextMenu();
        files_menu.getItems().add(browser_menus.make_undo_menu_item(logger));
        files_menu.getItems().add(browser_menus.make_create_empty_directory_menu_item());
        files_menu.getItems().add(browser_menus.make_select_all_files_menu_item(logger));
        files_menu.getItems().add(browser_menus.make_select_all_folders_menu_item(logger));
        if (level2)
        {
            files_menu.getItems().add(browser_menus.make_search_by_keywords_menu_item());
        }
        {
            String cleanup = "Clean up";//I18n.get_I18n_string("Clean",logger);
            Menu clean = new Menu(cleanup);
            clean.getItems().add(browser_menus.make_clear_icon_disk_cache_menu_item(logger));
            clean.getItems().add(browser_menus.make_clear_folder_icon_disk_cache_menu_item(logger));
            clean.getItems().add(browser_menus.make_clear_trash_menu_item(logger));
            if (level2) clean.getItems().add(browser_menus.make_clean_names_menu_item());
            if (level2) clean.getItems().add(browser_menus.make_remove_corrupted_images_menu_item());
            clean.getItems().add(browser_menus.make_remove_empty_folders_menu_item());
            if (level2) clean.getItems().add(browser_menus.make_remove_recursively_empty_folders_menu_item());
            files_menu.getItems().add(clean);
        }
        if (level2)
        {
            files_menu.getItems().add(browser_menus.make_backup_menu());
        }

        if (level2)
        {
            if (Static_application_properties.get_enable_fusk(logger))
            {
                files_menu.getItems().add(browser_menus.make_fusk_menu());
            }
        }
        return files_menu;
    }

    //**********************************************************
    private ContextMenu define_contextmenu_preferences()
    //**********************************************************
    {
        ContextMenu pref = new ContextMenu();

        pref.getItems().add(browser_menus.make_show_single_column_check_menu_item());
        pref.getItems().add(browser_menus.make_show_icons_for_images_and_videos_check_menu_item());
        pref.getItems().add(browser_menus.make_show_icons_for_folders_check_menu_item());
        pref.getItems().add(browser_menus.make_show_hidden_directories_check_menu_item());
        pref.getItems().add(browser_menus.make_show_hidden_files_check_menu_item());
        if (level2) pref.getItems().add(browser_menus.make_monitor_browsed_folders_check_menu_item());
        if (level2) pref.getItems().add(browser_menus.make_show_how_many_files_menu_item());

        pref.getItems().add(browser_menus.make_file_sort_method_menu());
        //pref.getItems().add(browser_menus.make_sort_files_by_name_vs_decreasing_size_check_menu_item());
        //pref.getItems().add(browser_menus.make_show_gifs_first_check_menu_item());
        if (level2) pref.getItems().add(browser_menus.make_show_folder_size_check_menu_item(browser.my_Stage.the_Stage));

        pref.getItems().add(browser_menus.make_icon_size_menu());
        pref.getItems().add(browser_menus.make_button_width_menu());
        pref.getItems().add(browser_menus.make_font_size_menu_item());
        pref.getItems().add(browser_menus.make_style_menu_item());
        pref.getItems().add(browser_menus.make_language_menu());
        pref.getItems().add(browser_menus.make_escape_menu_item());
        pref.getItems().add(browser_menus.make_invert_vertical_scroll_menu_item(logger));
        if (level2) pref.getItems().add(browser_menus.make_enable_fusk_check_menu_item());
        if (level2) pref.getItems().add(browser_menus.make_video_length_menu());

        return pref;
    }



}
