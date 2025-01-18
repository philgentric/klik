//SOURCES ../level2/deduplicate/Deduplication_engine.java
//SOURCES ../image_ml/image_similarity/Deduplication_by_similarity_engine.java
package klik.browser;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import klik.browser.icons.Error_type;
import klik.browser.icons.Virtual_landscape;
import klik.browser.meter.Meters_stage;
import klik.image_ml.image_similarity.Deduplication_by_similarity_engine;
import klik.images.Image_context;
import klik.level2.deduplicate.Deduplication_engine;
import klik.level3.metadata.Tag_items_management_stage;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Static_application_properties;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.performance_monitor.Performance_monitor;
import klik.util.ui.Popups;

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
    private final boolean level3;

    //**********************************************************
    public Browser_UI(Browser b)
    //**********************************************************
    {
        browser = b;
        logger = browser.logger;
        browser_menus = browser.browser_menus;
        level2 = Static_application_properties.get_level2(logger);
        level3 = Static_application_properties.get_level3(logger);

    }

    //**********************************************************
    void define_UI()
    //**********************************************************
    {


        double font_size = Static_application_properties.get_font_size(logger);
        double height = klik.look.Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;

        Button up_button;
        {
            String go_up_text = "";
            if (browser.displayed_folder_path.getParent() != null)
            {
                go_up_text = My_I18n.get_I18n_string("Parent_Folder", logger);// to: " + parent.toAbsolutePath().toString();
            }
            up_button = browser_menus.make_button_that_behaves_like_a_folder(browser,
                    browser.displayed_folder_path.getParent(),
                    go_up_text,
                    height,
                    Virtual_landscape.MIN_PARENT_AND_TRASH_BUTTON_WIDTH,
                    false,
                    true,
                    logger);
            {
                Image icon = Look_and_feel_manager.get_up_icon(height);
                if (icon == null)
                    logger.log("WARNING: could not load " + Look_and_feel_manager.get_instance().get_up_icon_path());
                Look_and_feel_manager.set_button_and_image_look(up_button, icon, height, null,true);

            }
            browser.always_on_front_nodes.add(up_button);
            top_buttons.add(up_button);
        }

        Button trash;
        {
            String trash_text = My_I18n.get_I18n_string("Trash", logger);// to: " + parent.toAbsolutePath().toString();
            trash = browser_menus.make_button_that_behaves_like_a_folder(
                    browser,
                    Static_application_properties.get_trash_dir(browser.displayed_folder_path,logger),
                    trash_text,
                    height,
                    Virtual_landscape.MIN_PARENT_AND_TRASH_BUTTON_WIDTH,
                    true,
                    false,
                    logger);
            {
                Image icon = Look_and_feel_manager.get_trash_icon(height);
                if (icon == null)
                    logger.log("WARNING: could not load " + Look_and_feel_manager.get_instance().get_bookmarks_icon_path());
                Look_and_feel_manager.set_button_and_image_look(trash, icon, height,null, true);

            }
            browser.always_on_front_nodes.add(trash);
            top_buttons.add(trash);
        }

        Pane top_pane = define_top_bar_using_buttons_deep(height, up_button, trash);
        BorderPane bottom_border_pane = define_bottom_pane(top_pane);
        browser.the_Scene = new Scene(bottom_border_pane);//, W, H);
        browser.error_type = Error_type.OK;


        {
            //logger.log("creating vertical slider");
            browser.vertical_slider = new Vertical_slider(browser.the_Scene, browser.the_Fucking_Pane , browser.virtual_landscape, logger);
            browser.mandatory_in_pane.add(browser.vertical_slider.the_Slider);
            browser.always_on_front_nodes.add(browser.vertical_slider.the_Slider);
            browser.slider_width = 2 * Vertical_slider.half_slider_width;
        }
        //set the view order (smaller means closer to viewer = on top)
        for (Node n : browser.always_on_front_nodes) n.setViewOrder(0);
        browser.the_Fucking_Pane.setViewOrder(100);
        browser.apply_font();

    }


    //**********************************************************
    private BorderPane define_bottom_pane(Pane top_pane)
    //**********************************************************
    {
        BorderPane returned = new BorderPane();
        returned.setTop(top_pane);
        returned.setCenter(browser.the_Fucking_Pane);
        VBox the_status_bar = new VBox();
        browser.status = new TextField("Status: OK");
        Look_and_feel_manager.set_region_look(browser.status);
        the_status_bar.getChildren().add(browser.status);
        returned.setBottom(the_status_bar);
        Look_and_feel_manager.set_region_look(returned);
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
            Look_and_feel_manager.set_region_look(top_pane2);
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
            String undo_bookmark_history = My_I18n.get_I18n_string("Bookmarks", logger);
            undo_bookmark_history += " & " + My_I18n.get_I18n_string("History", logger);
            Button undo_bookmark_history_button = new Button(undo_bookmark_history);
            undo_bookmark_history_button.setOnAction(e -> button_undo_bookmark_history(e));
            top_pane.getChildren().add(undo_bookmark_history_button);
            top_buttons.add(undo_bookmark_history_button);
            browser.always_on_front_nodes.add(undo_bookmark_history_button);
            Image icon = Look_and_feel_manager.get_bookmarks_icon(height);
            Look_and_feel_manager.set_button_and_image_look(undo_bookmark_history_button, icon, height,null, false);
        }
        {
            String files = My_I18n.get_I18n_string("Files", logger);
            Button files_button = new Button(files);
            files_button.setOnAction(e -> button_files(e));
            top_pane.getChildren().add(files_button);
            top_buttons.add(files_button);
            browser.always_on_front_nodes.add(files_button);
            Image icon = Look_and_feel_manager.get_folder_icon(height);
            Look_and_feel_manager.set_button_and_image_look(files_button, icon, height,null, false);
        }
        {
            String view = My_I18n.get_I18n_string("View", logger);
            Button view_button = new Button(view);
            view_button.setOnAction(e -> button_view(e));
            top_pane.getChildren().add(view_button);
            top_buttons.add(view_button);
            browser.always_on_front_nodes.add(view_button);
            Image icon = Look_and_feel_manager.get_view_icon(height);
            Look_and_feel_manager.set_button_and_image_look(view_button, icon, height,null, false);
        }
        {
            String preferences = My_I18n.get_I18n_string("Preferences", logger);
            Button preferences_button = new Button(preferences);
            preferences_button.setOnAction(e -> button_preferences(e));
            top_pane.getChildren().add(preferences_button);
            top_buttons.add(preferences_button);
            browser.always_on_front_nodes.add(preferences_button);
            Image icon = Look_and_feel_manager.get_preferences_icon(height);
            Look_and_feel_manager.set_button_and_image_look(preferences_button, icon, height,null, false);
        }
    }

    //**********************************************************
    private void button_undo_bookmark_history(ActionEvent e)
    //**********************************************************
    {
        ContextMenu undo_bookmark_history = define_contextmenu_undo_bookmark_history();
        Button b = (Button) e.getSource();
        undo_bookmark_history.show(b, Side.TOP, 0, 0);
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
    private ContextMenu define_contextmenu_undo_bookmark_history()
    //**********************************************************
    {
        ContextMenu undo_bookmark_history_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(undo_bookmark_history_menu);

        undo_bookmark_history_menu.getItems().add(browser_menus.make_undos_menu());
        undo_bookmark_history_menu.getItems().add(browser_menus.make_bookmarks_menu());
        undo_bookmark_history_menu.getItems().add(browser_menus.make_history_menu());
        undo_bookmark_history_menu.getItems().add(browser_menus.make_roots_menu());
        return undo_bookmark_history_menu;
    }

    //**********************************************************
    private ContextMenu define_contextmenu_view()
    //**********************************************************
    {
        ContextMenu view_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(view_menu);

        view_menu.getItems().add(browser_menus.make_menu_item("New_Window",event -> Browser_creation_context.additional_same_folder(browser,logger)));
        view_menu.getItems().add(browser_menus.make_menu_item("New_Twin_Window",event -> Browser_creation_context.additional_same_folder_twin(browser,logger)));
        view_menu.getItems().add(browser_menus.make_menu_item("New_Double_Window",event -> Browser_creation_context.additional_same_folder_fat_tall(browser,logger)));


        {
            start_full_screen_menu_item = browser_menus.make_menu_item("Go_full_screen",event -> browser.go_full_screen());
            start_full_screen_menu_item.setDisable(false);
            view_menu.getItems().add(start_full_screen_menu_item);
        }
        {
            stop_full_screen_menu_item = browser_menus.make_menu_item("Stop_full_screen",event -> browser.stop_full_screen());
            stop_full_screen_menu_item.setDisable(true);
            view_menu.getItems().add(stop_full_screen_menu_item);
        }
        {
            Menu scan = new Menu("Scan show");
            scan.getItems().add(browser_menus.make_menu_item("Start_stop_slow_scan",event -> browser.handle_scan_switch()));
            scan.getItems().add(browser_menus.make_menu_item("Slow_down_scan",event -> browser.slow_down_scan()));
            scan.getItems().add(browser_menus.make_menu_item("Speed_up_scan",event -> browser.speed_up_scan()));
            view_menu.getItems().add(scan);
        }
        view_menu.getItems().add(browser_menus.make_menu_item("Show_How_Many_Files_Are_In_Each_Folder",event -> browser.show_how_many_files_deep_in_each_folder()));
        view_menu.getItems().add(browser_menus.make_menu_item("Show_Each_Folder_Total_Size",event -> browser.show_total_size_deep_in_each_folder()));
        view_menu.getItems().add(browser_menus.make_menu_item("About_klik",event -> About_klik_stage.show_about_klik_stage()));
        view_menu.getItems().add(browser_menus.make_menu_item("Refresh",event -> browser.redraw_fx_1("refresh")));


        view_menu.getItems().add(browser_menus.make_menu_item("Show_Meters",event -> Meters_stage.show_stage(logger)));
        view_menu.getItems().add(browser_menus.make_menu_item("Show_Perfmon",event -> Performance_monitor.show(logger)));

        if (level3) view_menu.getItems().add(browser_menus.make_menu_item("Open_tag_management",event -> Tag_items_management_stage.open_tag_management_stage(logger)));

        return view_menu;
    }

    //**********************************************************
    private ContextMenu define_contextmenu_files()
    //**********************************************************
    {
        ContextMenu files_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(files_menu);

        files_menu.getItems().add(browser_menus.make_select_all_files_menu_item(logger));
        files_menu.getItems().add(browser_menus.make_select_all_folders_menu_item(logger));

        {
            String create_string = My_I18n.get_I18n_string("Create",logger);
            Menu create = new Menu(create_string);
            create.getItems().add(browser_menus.make_menu_item("Create_new_empty_directory",event -> browser.create_new_directory()));
            create.getItems().add(browser_menus.make_menu_item("Create_PDF_contact_sheet",event -> browser.create_PDF_contact_sheet()));
            create.getItems().add(browser_menus.make_menu_item("Sort_Files_In_Folders_By_Year",event -> browser.sort_by_year()));
            create.getItems().add(browser_menus.make_import_menu());
            files_menu.getItems().add(create);
        }
        {
            String search_string = My_I18n.get_I18n_string("Search",logger);
            Menu search = new Menu(search_string);
            search.getItems().add(browser_menus.make_menu_item("Search_by_keywords",event -> search_files_by_keyworks_fx()));
            search.getItems().add(browser_menus.make_menu_item("Show_Where_Are_Images",event -> browser.show_where_are_images()));
            search.getItems().add(browser_menus.make_add_to_face_recognition_training_set_menu_item());


            files_menu.getItems().add(search);
        }
        if (Static_application_properties.get_level3(logger))
        {
            Menu face_recognition = new Menu("Face recognition");
            face_recognition.getItems().add(browser_menus.make_load_face_recog_menu_item());
            face_recognition.getItems().add(browser_menus.make_save_face_recog_menu_item());
            face_recognition.getItems().add(browser_menus.make_reset_face_recog_menu_item());
            face_recognition.getItems().add(browser_menus.make_start_auto_face_recog_menu_item());
            face_recognition.getItems().add(browser_menus.make_start_self_face_recog_menu_item());

            files_menu.getItems().add(face_recognition);
        }
        {
            String cleanup = My_I18n.get_I18n_string("Clean_Up",logger);
            Menu clean = new Menu(cleanup);
            clean.getItems().add(browser_menus.make_remove_empty_folders_menu_item());
            if (level3)
            {
                clean.getItems().add(browser_menus.make_menu_item("Remove_empty_folders_recursively", event -> browser_menus.remove_empty_folders_recursively_fx()));
                clean.getItems().add(browser_menus.make_menu_item("Clean_up_names", event -> browser_menus.clean_up_names_fx()));
                clean.getItems().add(browser_menus.make_menu_item("Remove_corrupted_images", event -> browser_menus.remove_corrupted_images_fx()));
                //clean.getItems().add(browser_menus.make_menu_item("Compute_similarities", event -> browser_menus.compute_similarities()));
            }


            if (level2)
            {
                Menu deduplicate = new Menu("File deduplication tool");
                deduplicate.getItems().add(create_help_on_deduplication_menu_item());
                deduplicate.getItems().add(create_deduplication_count_menu_item());
                deduplicate.getItems().add(create_manual_deduplication_menu_item());
                deduplicate.getItems().add(create_auto_deduplication_menu_item());
                clean.getItems().add(deduplicate);
            }
            {
                MenuItem deduplicate2_menu_item = create_manual_deduplication_by_similarity_menu_item();
                clean.getItems().add(deduplicate2_menu_item);
            }
            {
                MenuItem deduplicate2_menu_item = create_manual_deduplication_by_similarity_menu_item2();
                clean.getItems().add(deduplicate2_menu_item);

            }
            files_menu.getItems().add(clean);
        }

        if (level2) files_menu.getItems().add(browser_menus.make_backup_menu());

        if (level3)
        {
            if (Static_application_properties.get_enable_fusk(logger))
            {
                files_menu.getItems().add(browser_menus.make_fusk_menu());
            }
        }
        return files_menu;
    }

    //**********************************************************
    void search_files_by_keyworks_fx()
    //**********************************************************
    {
        List<String> given = new ArrayList<>();
        Image_context.ask_user_and_find(
                browser,
                browser.displayed_folder_path,
                given,
                false,
                logger
        );

    }
    //**********************************************************
    private ContextMenu define_contextmenu_preferences()
    //**********************************************************
    {
        ContextMenu pref = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(pref);

        pref.getItems().add(browser_menus.make_show_single_column_check_menu_item());
        pref.getItems().add(browser_menus.make_show_icons_for_images_and_videos_check_menu_item());
        pref.getItems().add(browser_menus.make_show_icons_for_folders_check_menu_item());
        pref.getItems().add(browser_menus.make_show_hidden_directories_check_menu_item());
        pref.getItems().add(browser_menus.make_show_hidden_files_check_menu_item());
        pref.getItems().add(browser_menus.make_dont_zoom_small_images_check_menu_item());



        if (level2)
        {
            pref.getItems().add(browser_menus.make_auto_purge_icon_disk_cache_check_menu_item());
            pref.getItems().add(browser_menus.make_monitor_browsed_folders_check_menu_item());
            pref.getItems().add(browser_menus.make_stop_monitoring_menu_item());
            pref.getItems().add(browser_menus.make_use_RAM_disk_menu_item());
        }

        pref.getItems().add(browser_menus.make_file_sort_method_menu());

        pref.getItems().add(browser_menus.make_icon_size_menu());
        pref.getItems().add(browser_menus.make_folder_icon_size_menu());
        pref.getItems().add(browser_menus.make_column_width_menu());
        pref.getItems().add(browser_menus.make_font_size_menu_item());
        pref.getItems().add(browser_menus.make_style_menu_item());
        pref.getItems().add(browser_menus.make_language_menu());
        if (level2) pref.getItems().add(browser_menus.make_video_length_menu());
        pref.getItems().add(browser_menus.make_ding_menu_item());
        pref.getItems().add(browser_menus.make_escape_menu_item());
        pref.getItems().add(browser_menus.make_invert_vertical_scroll_menu_item(logger));
        if (level3) pref.getItems().add(browser_menus.make_enable_fusk_check_menu_item());
        pref.getItems().add(browser_menus.make_cache_size_limit_warning_menu_item(logger));


        {

            pref.getItems().add(browser_menus.make_menu_item(
                    "Clear_Trash_Folder",
                    event -> Static_files_and_paths_utilities.clear_trash_with_warning_fx(browser.my_Stage.the_Stage, browser.aborter, logger)));

            pref.getItems().add(browser_menus.make_clear_all_caches_menu_item(logger));
            if (level3) {

                Menu cleanup = new Menu(My_I18n.get_I18n_string("Cache_cleaning",logger));
                pref.getItems().add(cleanup);
                {
                    Menu ram = new Menu(My_I18n.get_I18n_string("RAM_Caches_Cleaming",logger));
                    cleanup.getItems().add(ram);
                    ram.getItems().add(browser_menus.make_menu_item("Clear_All_RAM_Caches",
                            event -> browser.clear_all_RAM_caches()));
                    ram.getItems().add(browser_menus.make_menu_item("Clear_Image_Properties_RAM_Cache",
                            event -> browser.virtual_landscape.clear_image_properties_RAM_cache_fx()));
                    ram.getItems().add(browser_menus.make_menu_item("Clear_Image_Comparators_Caches",
                            event -> browser.clear_image_comparators_caches()));
                    ram.getItems().add(browser_menus.make_menu_item("Clear_Scroll_Position_Cache",
                            event ->         Browser.scroll_position_cache.clear()));

                }
                {
                    Menu disk = new Menu(My_I18n.get_I18n_string("DISK_Caches_Cleaning",logger));
                    cleanup.getItems().add(disk);

                    disk.getItems().add(browser_menus.make_menu_item(
                            "Clear_All_Disk_Caches",
                            event -> browser.clear_all_DISK_caches()));
                    disk.getItems().add(browser_menus.make_menu_item(
                            "Clear_Icon_Cache_On_Disk",
                            event -> Static_files_and_paths_utilities.clear_icon_DISK_cache_with_warning_fx(browser.my_Stage.the_Stage,browser.aborter,logger)));

                    disk.getItems().add(browser_menus.make_menu_item(
                            "Clear_Folders_Icon_Cache_Folder",
                            event -> Static_files_and_paths_utilities.clear_folder_icon_cache_on_disk_with_warning_fx(browser.my_Stage.the_Stage, browser.aborter, logger)));
                    disk.getItems().add(browser_menus.make_menu_item(
                            "Clear_Image_Properties_DISK_Cache",
                            event -> Static_files_and_paths_utilities.clear_image_properties_DISK_cache_no_warning_fx(browser.my_Stage.the_Stage,logger)));

                    disk.getItems().add(browser_menus.make_menu_item("Clear_Image_Feature_Vector_DISK_Cache",
                            event -> Static_files_and_paths_utilities.clear_image_feature_vectors_DISK_cache_no_warning_fx(browser.my_Stage.the_Stage, logger)));

                }

            }
        }
        return pref;
    }



    //**********************************************************
    private MenuItem create_auto_deduplication_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_auto",logger);
        MenuItem menu_item = new MenuItem(text);
        menu_item.setOnAction(event -> {
            //logger.log("Deduplicate auto");

            if ( !Popups.popup_ask_for_confirmation(browser.my_Stage.the_Stage, "EXPERIMENTAL! Are you sure?","Automated deduplication will recurse down this folder and delete (for good = not send them in recycle bin) all duplicate files",logger)) return;
            (new Deduplication_engine(browser, browser.displayed_folder_path.toFile(), logger)).do_your_job(true);
        });
        return menu_item;
    }




    //**********************************************************
    private MenuItem create_manual_deduplication_by_similarity_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_manual_similarity",logger);

        MenuItem item0 = new MenuItem(text);
        item0.setOnAction(event -> {
            //logger.log("Deduplicate manually");
            (new Deduplication_by_similarity_engine(false,browser, browser.displayed_folder_path.toFile(), logger)).do_your_job();
        });
        return item0;
    }


    //**********************************************************
    private MenuItem create_manual_deduplication_by_similarity_menu_item2()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_manual_similarity2",logger);

        MenuItem item0 = new MenuItem(text);
        item0.setOnAction(event -> {
            //logger.log("Deduplicate manually");
            (new Deduplication_by_similarity_engine(true,browser, browser.displayed_folder_path.toFile(), logger)).do_your_job();
        });
        return item0;
    }

    //**********************************************************
    private MenuItem create_manual_deduplication_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_manual",logger);

        MenuItem item0 = new MenuItem(text);
        item0.setOnAction(event -> {
            //logger.log("Deduplicate manually");
            (new Deduplication_engine(browser, browser.displayed_folder_path.toFile(), logger)).do_your_job(false);
        });
        return item0;
    }

    //**********************************************************
    private MenuItem create_deduplication_count_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_count",logger);
        MenuItem item0 = new MenuItem(text);
        item0.setOnAction(event -> {
            //logger.log("count duplicates!");
            (new Deduplication_engine(browser, browser.displayed_folder_path.toFile(), logger)).count(false);
        });
        return item0;
    }


    //**********************************************************
    private MenuItem create_help_on_deduplication_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_help",logger);
        MenuItem itemhelp = new MenuItem(text);
        itemhelp.setOnAction(event -> Popups.popup_warning(browser.my_Stage.the_Stage,
                "Help on deduplication",
                "The deduplication tool will look recursively down the path starting at:" + browser.displayed_folder_path.toAbsolutePath() +
                        "\nLooking for identical files in terms of file content i.e. names/path are different but it IS the same file" +
                        " Then you will be able to either:" +
                        "\n  1. Review each pair of duplicate files one by one" +
                        "\n  2. Or ask for automated deduplication (DANGER!)" +
                        "\n  Beware: automated de-duplication may give unexpected results" +
                        " since you do not choose which file in the pair is deleted." +
                        "\n  However, the files are not actually deleted: they are MOVED to the klik_trash folder," +
                        " which you can visit by clicking on the trash button." +
                        "\n\n WARNING: On folders containing a lot of data, the search can take a long time!",
                false,
                logger));
        return itemhelp;
    }



}
