// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.properties.boolean_features;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

//**********************************************************
public class Preferences_stage
//**********************************************************
{
    public static final int WIDTH = 1200;
    public static final String EXPLANATION = "_Explanation";
    public static final int HEIGHT = 1000;
    public final VBox vbox_basic;
    public final VBox vbox_advanced;
    public final VBox vbox_experimental;
    public final VBox vbox_debug;
    public final Logger logger;
    private static Preferences_stage instance;
    private final Stage stage;

    public static final Feature[] advanced_features ={
            Feature.Monitor_folders,
            Feature.Enable_face_recognition,
            Feature.Enable_image_similarity,
            Feature.Enable_bit_level_deduplication,
            Feature.Enable_recursive_empty_folders_removal,
            Feature.Enable_auto_purge_disk_caches,
            Feature.Display_image_distances,
            Feature.Play_ding_after_long_processes,
            Feature.max_RAM_is_defined_by_user,
            Feature.Shift_d_is_sure_delete};

    public static final Feature[] basic_features ={
            Feature.Show_icons_for_files,
            Feature.Show_icons_for_folders,
            Feature.Show_hidden_files,
            Feature.Show_hidden_folders,
            Feature.Show_single_column,
            Feature.Show_file_names_as_tooltips,
            Feature.Reload_last_folder_on_startup,
            Feature.Dont_zoom_small_images,
            Feature.Use_escape_to_close_windows,
            Feature.Hide_beginners_text_on_images,
            Feature.Hide_question_mark_buttons_on_mysterious_menus
    };

    public static final Feature[] debugging_features ={
            Feature.Log_to_file,
            Feature.Enable_detailed_cache_cleaning_options,
            Feature.Fusk_is_on,
            Feature.Show_ffmpeg_install_warning,
            Feature.Show_graphicsmagick_install_warning,
            Feature.Show_can_use_ESC_to_close_windows,
    };

    public static final Feature[] experimental_features ={
            Feature.Enable_backup,
            //Feature.Enable_tags,
            Feature.Enable_fusk,
            Feature.Enable_name_cleaning,
            Feature.Enable_corrupted_images_removal,
            Feature.Enable_alternate_image_scaling
            //Feature.Enable_image_playlists,
    };
    //**********************************************************
    public static void show_Preferences_stage(String title, Window owner, Logger logger)
    //**********************************************************
    {
        if ( instance != null)
        {
            instance.show();
            return;
        }
        instance = new Preferences_stage(title,owner,logger);
    }

    //**********************************************************
    public static void reset()
    //**********************************************************
    {
        instance = null;
    }

    //**********************************************************
    private void show()
    //**********************************************************
    {
        stage.show();
    }

    //**********************************************************
    private Preferences_stage(String title, Window owner, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(WIDTH, HEIGHT);
        VBox vBox = new VBox();
        sp.setContent(vBox);
        HBox top = new HBox();
        vBox.getChildren().add(top);
        HBox bottom = new HBox();
        vBox.getChildren().add(bottom);

        vbox_basic = new VBox();
        top.getChildren().add(vbox_basic);

        vbox_experimental = new VBox();
        top.getChildren().add(vbox_experimental);

        vbox_advanced = new VBox();
        bottom.getChildren().add(vbox_advanced);

        vbox_debug = new VBox();
        bottom.getChildren().add(vbox_debug);


        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        stage = new Stage();
        stage.setHeight(HEIGHT);
        stage.setWidth(WIDTH);
        stage.initOwner(owner);

        Scene scene = new Scene(sp,WIDTH , HEIGHT, Color.WHITE);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();

        define();
    }

    //**********************************************************
    public void define()
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("define!!!"));
        vbox_basic.getChildren().clear();
        vbox_experimental.getChildren().clear();
        vbox_advanced.getChildren().clear();
        vbox_debug.getChildren().clear();


        {
            Label lab = new Label("Basic features");
            Look_and_feel_manager.set_region_look(lab,stage,logger);
            vbox_basic.getChildren().add(lab);
        }
        for(Feature f : basic_features)
        {
            add_one_line(f,vbox_basic);
        }
        //vbox_basic.getChildren().add(new Separator());

        {
            Label lab = new Label("Advanced features");
            Look_and_feel_manager.set_region_look(lab,stage,logger);
            vbox_advanced.getChildren().add(lab);
        }
        for(Feature f : advanced_features)
        {
            add_one_line(f,vbox_advanced);
        }





        {
            Label lab = new Label("Experimental features");
            Look_and_feel_manager.set_region_look(lab,stage,logger);
            vbox_experimental.getChildren().add(lab);
        }
        for(Feature f : experimental_features)
        {
            add_one_line(f,vbox_experimental);
        }
        //vbox_right.getChildren().add(new Separator());

        {
            Label lab = new Label("Debug");
            Look_and_feel_manager.set_region_look(lab,stage,logger);
            vbox_debug.getChildren().add(lab);
        }
        for(Feature f : debugging_features)
        {
            add_one_line(f,vbox_debug);
        }

    }

    //**********************************************************
    private void add_one_line(Feature bf, VBox vbox)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(bf.name(),stage,logger);
        HBox hbox = new HBox();
        {
            CheckBox cb = new CheckBox(text);
            cb.setMnemonicParsing(false);
            Boolean value0 = Booleans.get_boolean(bf.name(), stage);

            if (value0 == null) {
                logger.log("warning, no Boolean found for: " + bf.name());
                value0 = (Boolean) false;
                Booleans.set_boolean(bf.name(), value0, stage);
                Feature_cache.update_cached_boolean(bf, value0, stage);

            }
            cb.setSelected(value0);
            Look_and_feel_manager.set_CheckBox_look(cb, stage, logger);

            cb.setOnAction((ActionEvent e) ->
            {
                Boolean value = (Boolean) cb.isSelected();
                logger.log("Preference changing for: " + bf.name() + "new value:" + value);
                Booleans.set_boolean(bf.name(), value, stage); // this will trigger a file save
                Feature_cache.update_cached_boolean(bf, value, stage);
            });
            hbox.getChildren().add(cb);

            Button button = make_explanation_button(bf.name(), stage,logger);
            if (button == null) return;
            hbox.getChildren().add(button);

        }
        vbox.getChildren().add(hbox);
    }

    //**********************************************************
    public static Button make_explanation_button(String key,Window stage, Logger logger)
    //**********************************************************
    {
        Button button = new Button("?");
        String explanation = My_I18n.get_I18n_string(key + EXPLANATION, stage, logger);
        if (explanation == null || explanation.isBlank())
        {
            logger.log("No explanation found for: " + key);
            return null;
        }
        if ( explanation.equals(key+ EXPLANATION))
        {
            explanation = My_I18n.get_I18n_string(key, stage, logger).replaceAll("_", " ");
        }
        button.setTooltip(new Tooltip(explanation));
        Look_and_feel_manager.set_button_look(button, true,stage, logger);
        String finalExplanation = explanation;
        button.setOnAction(event -> show_explanation(finalExplanation, stage, logger));
        return button;
    }

    //**********************************************************
    private static void show_explanation(String explanation, Window owner, Logger logger)
    //**********************************************************
    {

        Stage explanation_stage = new Stage();
        explanation_stage.initOwner(owner);
        VBox vb = new VBox();

        TextArea tf = new TextArea(explanation);
        Look_and_feel_manager.set_region_look(tf,owner,logger);
        tf.setEditable(false);
        tf.setWrapText(true);
        vb.getChildren().add(tf);

        Scene scene = new Scene(vb);
        explanation_stage.setScene(scene);
        explanation_stage.setWidth(500);
        explanation_stage.setHeight(300);
        explanation_stage.show();
        explanation_stage.setAlwaysOnTop(true);
    }


}
