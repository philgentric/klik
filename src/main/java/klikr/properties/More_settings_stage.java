package klikr.properties;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.Shared_services;
import klikr.System_info;
import klikr.browser.comparators.Similarity_comparator;
import klikr.browser.virtual_landscape.Browsing_caches;
import klikr.look.Look_and_feel;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.properties.boolean_features.Booleans;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.Installers;
import klikr.util.execute.Debug_console;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.ui.Items_with_explanation;
import klikr.util.ui.Popups;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class More_settings_stage
{

    public static final Feature[] basic_features ={
            //Feature.Show_single_column_with_details,
            Feature.Show_icons_for_files,
            Feature.Show_icons_for_folders,
            Feature.Show_hidden_files,
            Feature.Show_hidden_folders,
            Feature.Show_file_names_as_tooltips,
            Feature.Reload_last_folder_on_startup,
            Feature.Dont_zoom_small_images,
            Feature.Use_escape_to_close_windows,
    };

    public static final Feature[] advanced_features ={
            Feature.Monitor_folders,
            //Feature.Enable_face_recognition,
            //Feature.Enable_image_similarity,
            Feature.Enable_bit_level_deduplication,
            Feature.Enable_backup,
            Feature.Enable_recursive_empty_folders_removal,
            Feature.Enable_auto_purge_disk_caches,
            Feature.Play_ding_after_long_processes,
            //Feature.Max_RAM_is_defined_by_user,
            //Feature.Shift_d_is_sure_delete,
            Feature.Hide_beginners_text_on_images,
            Feature.Hide_question_mark_buttons_on_mysterious_menus
    };

    public static final Feature[] experimental_features ={
            Feature.Enable_3D,
            Feature.Enable_fusk,
            Feature.Enable_name_cleaning,
            Feature.Enable_corrupted_images_removal,
            Feature.Enable_alternate_image_scaling,
            //Feature.Display_image_distances,
            //Feature.Enable_tags,
            //Feature.Enable_image_playlists,
    };

    public static final Feature[] debugging_features ={
            Feature.Log_to_file,
            Feature.Log_performances,
            //Feature.Enable_detailed_cache_cleaning_options,
            Feature.Fusk_is_on,
            Feature.Show_ffmpeg_install_warning,
            Feature.Show_graphicsmagick_install_warning,
            //Feature.Show_can_use_ESC_to_close_windows,
            //Feature.Enable_feature_vector_monitoring
    };

    private final Window owner;
    private final Logger logger;
    private final Scene scene;

    //**********************************************************
    public More_settings_stage(String title, Window owner, Logger logger)
    //**********************************************************
    {
        double w = 600;
        double icon_size = 128;
        Look_and_feel look_and_feel = Look_and_feel_manager.get_instance(owner, logger);

        this.owner = owner;
        this.logger = logger;
        Accordion accordion = new Accordion();
        scene = new Scene(accordion);

        Look_and_feel_manager.set_region_look(accordion,owner,logger);
        {
            VBox box = new VBox(10);
            for (Feature f : basic_features)
            {
                add_one_line(f, box);
            }
            ScrollPane sp = new ScrollPane(box);
            sp.setFitToWidth(true);          // stretch items horizontally
            sp.setPrefViewportHeight(200);   // limit visible height
            TitledPane pane = new TitledPane("Basic features", sp);
            accordion.getPanes().add(pane);
            accordion.setExpandedPane(pane);

        }

        {
            VBox box = new VBox(10);

            {
                String key = "Set_The_VM_Max_RAM";
                EventHandler<ActionEvent> handler = e -> show_max_ram_dialog(owner, logger);
                HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                        key,
                        handler,
                        w,
                        icon_size,
                        look_and_feel,
                        owner,
                        logger);
                box.getChildren().add(hb);
            }
            {
                String key = "Set_The_Cache_Size_Warning_Limit";
                EventHandler<ActionEvent> handler = e -> show_cache_size_limit_input_dialog();
                HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                        key,
                        handler,
                        w,
                        icon_size,
                        look_and_feel,
                        owner,
                        logger);
                box.getChildren().add(hb);
            }

            for (Feature f : advanced_features)
            {
                add_one_line(f, box);
            }
            {
                String text = My_I18n.get_I18n_string("Length_of_video_sample",owner,logger);
                MenuButton mb = new MenuButton(text);
                Look_and_feel_manager.set_region_look(mb,owner, logger);

                List<CheckMenuItem> all_check_menu_items = new ArrayList<>();
                int[] possible_lenghts ={Non_booleans_properties.DEFAULT_VIDEO_LENGTH,2,3,5,7,10,15,20};
                for ( int l : possible_lenghts)
                {
                    create_menu_item_for_one_video_length(mb, l, all_check_menu_items);
                }
                box.getChildren().add(mb);
            }





            ScrollPane sp = new ScrollPane(box);
            sp.setFitToWidth(true);          // stretch items horizontally
            sp.setPrefViewportHeight(200);   // limit visible height
            TitledPane pane = new TitledPane("Advanced features", sp);
            accordion.getPanes().add(pane);
        }
        {
            VBox box = new VBox(10);
            for (Feature f : experimental_features)
            {
                add_one_line(f, box);
            }
            //if (Feature_cache.get(Feature.Max_RAM_is_defined_by_user))

            ScrollPane sp = new ScrollPane(box);
            sp.setFitToWidth(true);          // stretch items horizontally
            sp.setPrefViewportHeight(200);   // limit visible height
            TitledPane pane = new TitledPane("Experimental features", sp);
            accordion.getPanes().add(pane);
        }
        {
            VBox box = new VBox(10);
            Installers.make_ui_to_install_everything(false,w,icon_size,look_and_feel,box,owner,logger);
            Installers.make_ui_to_install_all_apps(w,icon_size,look_and_feel,box,owner,logger);
            ScrollPane sp = new ScrollPane(box);
            sp.setFitToWidth(true);          // stretch items horizontally
            sp.setPrefViewportHeight(200);   // limit visible height
            TitledPane pane = new TitledPane("Install helper applications", sp);
            accordion.getPanes().add(pane);
        }

        {
            VBox box = new VBox(10);
            Installers.make_ui_to_install_python_libs_for_ML(w, icon_size, look_and_feel,box, owner, logger);

            add_one_line(Feature.Enable_image_similarity,box);
            {
                HBox hb = Installers.make_ui_to_start_image_similarity_servers(w, icon_size, look_and_feel, box, owner, logger);
                if (!Feature_cache.get(Feature.Enable_image_similarity)) {
                    disable_button(hb);
                }
            }
            add_one_line(Feature.Enable_feature_vector_monitoring, box);
            add_one_line(Feature.Display_image_distances, box);
            {
                HBox hb = Installers.make_ui_to_stop_image_similarity_servers(w, icon_size, look_and_feel, box, owner, logger);
                if (!Feature_cache.get(Feature.Enable_image_similarity)) {
                    disable_button(hb);
                }
            }

            add_one_line(Feature.Enable_face_recognition,box);
            {
                HBox hb = Installers.make_ui_to_start_face_recognition_servers(w, icon_size, look_and_feel, box, owner, logger);
                if (!Feature_cache.get(Feature.Enable_face_recognition)) {
                    disable_button(hb);
                }
            }
            {
                HBox hb = Installers.make_ui_to_stop_face_recognition_servers(w, icon_size, look_and_feel, box, owner, logger);
                if (!Feature_cache.get(Feature.Enable_face_recognition)) {
                    disable_button(hb);
                }
            }

            ScrollPane sp = new ScrollPane(box);
            sp.setFitToWidth(true);          // stretch items horizontally
            sp.setPrefViewportHeight(200);   // limit visible height
            TitledPane pane = new TitledPane("Machine Learning (ML) features", sp);
            accordion.getPanes().add(pane);
        }

        {
            VBox box = new VBox(10);
            for (Feature f : debugging_features)
            {
                add_one_line(f, box);
            }
            {
                String key = "Show_Version";
                EventHandler<ActionEvent> handler = e -> Installers.show_version(owner,logger);
                HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                        key,
                        handler,
                        w,
                        icon_size,
                        look_and_feel,
                        owner,
                        logger);
                box.getChildren().add(hb);
            }
            box.getChildren().add(Debug_console.get_button(owner,logger));
            ScrollPane sp = new ScrollPane(box);
            sp.setFitToWidth(true);          // stretch items horizontally
            sp.setPrefViewportHeight(200);   // limit visible height
            TitledPane pane = new TitledPane("Debugging", sp);
            accordion.getPanes().add(pane);
        }


        {
            VBox box = new VBox(10);
            detailed_cache_cleaning_buttons(
                w,
                icon_size,
                look_and_feel,
                box
                );
            ScrollPane sp = new ScrollPane(box);
            sp.setFitToWidth(true);          // stretch items horizontally
            sp.setPrefViewportHeight(200);   // limit visible height
            TitledPane pane = new TitledPane("Cache debugging", sp);
            accordion.getPanes().add(pane);
        }

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.initOwner(owner);
        stage.setMinWidth(1000);
        stage.show();
    }
    //**********************************************************
    public void create_menu_item_for_one_video_length( MenuButton menu, int length, List<CheckMenuItem> all_check_menu_items)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Length_of_video_sample",owner,logger);
        CheckMenuItem item = new CheckMenuItem(text + " = " +length+" s");
        Look_and_feel_manager.set_menu_item_look(item, owner, logger);
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
                Popups.popup_warning( "❗ Note well:","You have to clear the icon cache to see the effect for already visited folders",false,owner,logger);
            }
        });
        menu.getItems().add(item);
        all_check_menu_items.add(item);
    }


    //**********************************************************
    private void add_one_line (Feature bf, VBox vbox)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(bf.name(), owner, logger);
        HBox hbox = new HBox();
        {
            CheckBox cb = new CheckBox(text);
            cb.setMnemonicParsing(false);
            boolean value0 = Feature_cache.get(bf);
            cb.setSelected(value0);
            Look_and_feel_manager.set_CheckBox_look(cb, owner, logger);

            cb.setOnAction((ActionEvent e) ->
            {
                on_change(bf, cb);
            });
            hbox.getChildren().add(cb);

            Button button = Items_with_explanation.make_explanation_button(bf.name(), owner, logger);
            if (button == null) return;
            hbox.getChildren().add(button);

        }
        vbox.getChildren().add(hbox);
    }

    //**********************************************************
    private void on_change(Feature bf, CheckBox cb)
    //**********************************************************
    {
        boolean value = cb.isSelected();
        logger.log("Preference changing for: " + bf + "new value:" + value);
        Booleans.set_boolean(bf.name(), value, owner); // this will trigger a file save
        Feature_cache.update_cached_boolean(bf, value, owner);
    }

    //**********************************************************
    private void disable_button (HBox hb)
    //**********************************************************
    {
        for (Node n : hb.getChildren()) {
            if (n instanceof Button b) {
                if (!b.getText().equals("?")) {
                    //logger.log("button: " + b.getText());
                    b.setDisable(true);
                }
            }
        }
    }


    //**********************************************************
    private void detailed_cache_cleaning_buttons (
            //boolean active,
    double w,
    double icon_size,
    Look_and_feel look_and_feel,
    VBox vbox)
    //**********************************************************
    {
        {
            add_one_button("Clear_All_RAM_Caches",
                    event -> Browsing_caches.clear_all_RAM_caches(logger), w, icon_size, look_and_feel, vbox);
            add_one_button("Clear_Image_Properties_RAM_Cache",
                    event -> Browsing_caches.image_properties_cache_of_caches.clear(), w, icon_size, look_and_feel, vbox);
            add_one_button("Clear_Image_Comparators_Caches",
                    event -> clear_image_comparators_caches(), w, icon_size, look_and_feel, vbox);
            add_one_button("Clear_Scroll_Position_Cache",
                    event -> Browsing_caches.scroll_position_cache_clear(), w, icon_size, look_and_feel, vbox);

        }
        {
            add_one_button("Clear_All_Disk_Caches",
                    event -> Static_files_and_paths_utilities.clear_all_DISK_caches(owner, Shared_services.aborter(), logger), w, icon_size, look_and_feel, vbox);
            add_one_button("Clear_Icon_Cache_On_Disk",
                    event -> Static_files_and_paths_utilities.clear_DISK_cache(Cache_folder.icon_cache, true, owner, Shared_services.aborter(), logger), w, icon_size, look_and_feel, vbox);
            add_one_button("Clear_Folders_Icon_Cache_Folder",
                    event -> Static_files_and_paths_utilities.clear_DISK_cache(Cache_folder.folder_icon_cache, false, owner, Shared_services.aborter(), logger), w, icon_size, look_and_feel, vbox);
            add_one_button("Clear_Image_Properties_DISK_Cache",
                    event -> Static_files_and_paths_utilities.clear_DISK_cache(Cache_folder.image_properties_cache, false, owner, Shared_services.aborter(), logger), w, icon_size, look_and_feel, vbox);
            add_one_button("Clear_Image_Feature_Vector_DISK_Cache",
                    event -> Static_files_and_paths_utilities.clear_DISK_cache(Cache_folder.feature_vectors_cache, true, owner, Shared_services.aborter(), logger), w, icon_size, look_and_feel, vbox);
            add_one_button("Clear_Image_Similarity_DISK_Cache",
                    event -> Static_files_and_paths_utilities.clear_DISK_cache(Cache_folder.similarity_cache, true, owner, Shared_services.aborter(), logger), w, icon_size, look_and_feel, vbox);


        }
    }


    //**********************************************************
    private void add_one_button(
            String key,
            EventHandler<ActionEvent> handler,
            double w,
            double icon_size,
            Look_and_feel look_and_feel,
            VBox vbox)
    //**********************************************************
    {
        HBox hb = Items_with_explanation.make_hbox_with_button_and_explanation(
                key,
                handler,
                w,
                icon_size,
                look_and_feel,
                owner,
                logger);
        vbox.getChildren().add(hb);
    }


    //**********************************************************
    public void clear_image_comparators_caches()
    //**********************************************************
    {
        for ( Similarity_comparator x : Browsing_caches.similarity_comparator_cache.values())
        {
            x.clear_RAM();
        }
    }

    //**********************************************************
    public void clear_image_feature_vector_RAM_cache()
    //**********************************************************
    {
        for( Feature_vector_cache x : Browsing_caches.fv_cache_of_caches.values())
        {
            x.clear_RAM();
        }
        Browsing_caches.fv_cache_of_caches.clear();
    }

    //**********************************************************
    private void show_cache_size_limit_input_dialog()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(""+ Non_booleans_properties.get_folder_warning_size(owner));
        Look_and_feel_manager.set_dialog_look(dialog, owner,logger);
        dialog.initOwner(owner);
        dialog.setWidth(1200);
        dialog.setTitle(My_I18n.get_I18n_string("Cache_Size_Warning_Limit",owner,logger));
        dialog.setHeaderText("If the cache on disk gets larger than this, you will receive a warning. Entering zero means no limit.");
        dialog.setContentText(My_I18n.get_I18n_string("Set_The_Cache_Size_Warning_Limit",owner,logger)+" (MB)");


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
                Popups.popup_warning("❗ Integer only!","Please retry with an integer value!",false,owner,logger);
            }
        }
    }

    //**********************************************************
    private void show_max_ram_dialog(Window owner, Logger logger)
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(""+ Non_booleans_properties.get_java_VM_max_RAM(owner, logger));
        Look_and_feel_manager.set_dialog_look(dialog, owner,logger);
        dialog.initOwner(owner);
        dialog.setWidth(1200);
        dialog.setHeight(800);
        dialog.setTitle("Java VM max RAM size");
        dialog.setHeaderText("This is the max RAM that the java VM will be allowed to reserve THE NEXT TIME you run klik.");
        int max = System_info.get_total_machine_RAM_in_GBytes(owner, logger).orElse(4);
        dialog.setContentText("This machine RAM size is: "+max+ "GB.\nEnter JVM max RAM in GB: ");
        Node old = dialog.getDialogPane().getContent();
        Label lab = new Label(
                "If you are not sure what this means," +
                        "\nbetter not change this value!" +
                        "\nthe Java VM max RAM will not be set larger than your physical machine RAM, " +
                        "\nbecause this would for sure cause system-wide problems ..." +
                        "\nbut you are warned that getting too close to this limit can also cause trouble.");
        VBox vb = new VBox();
        vb.getChildren().add(old);
        vb.getChildren().add(lab);
        dialog.getDialogPane().setContent(vb);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_val = result.get();
            try
            {
                int val = Integer.parseInt(new_val);
                if ( val > max ) val = (int)max;
                Non_booleans_properties.save_java_VM_max_RAM(val,owner, logger);

            }
            catch (NumberFormatException e)
            {
                Popups.popup_warning("❗ Integer only!","Please retry with an integer value!",false,owner,logger);
            }
        }
    }

}
