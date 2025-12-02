// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.properties;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.Installers;
import klik.util.log.Logger;


//**********************************************************
public class More_settings_stage
//**********************************************************
{
    public static final String EXPLANATION = "_Explanation";
    /*public final VBox left;
    public final VBox left;
    public final VBox right;
    public final VBox right;
    */
    public final VBox left;
    public final VBox right;

    public final Logger logger;
    private static More_settings_stage instance;
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
            Feature.Log_performances,
            Feature.Enable_detailed_cache_cleaning_options,
            Feature.Fusk_is_on,
            Feature.Show_ffmpeg_install_warning,
            Feature.Show_graphicsmagick_install_warning,
            Feature.Show_can_use_ESC_to_close_windows,
    };

    public static final Feature[] experimental_features ={
            Feature.Enable_3D,
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
        instance = new More_settings_stage(title,owner,logger);
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
    private More_settings_stage(String title, Window owner, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        //sp.setPrefSize(WIDTH, HEIGHT);
        HBox hbox = new HBox();
        //HBox top = new HBox();
        //vBox.getChildren().add(top);
        //HBox bottom = new HBox();
        //vBox.getChildren().add(bottom);
        left = new VBox();
        hbox.getChildren().add(left);
        right = new VBox();
        hbox.getChildren().add(right);

        /*left = new VBox();
        top.getChildren().add(left);
        right = new VBox();
        top.getChildren().add(right);
        left = new VBox();
        bottom.getChildren().add(left);
        right = new VBox();
        bottom.getChildren().add(right);
        
         */
        define();



        stage = new Stage();
        //stage.setHeight(HEIGHT);
        //stage.setWidth(WIDTH);


        ScrollPane sp = new ScrollPane(hbox);
        sp.setFitToHeight(true);
        sp.setFitToWidth(true);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        Scene scene = new Scene(sp, Color.WHITE);


        //Scene scene = new Scene(vBox, Color.WHITE);

        stage.setTitle(title);
        stage.setScene(scene);
        stage.initOwner(owner);
        stage.setX(owner.getX()+100);
        stage.setY(owner.getY()+10);
        stage.show();
        stage.sizeToScene();
    }

    //**********************************************************
    public void define()
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("define!!!"));
        left.getChildren().clear();
        right.getChildren().clear();
        left.getChildren().clear();
        right.getChildren().clear();


        {
            Label lab = new Label("Basic features");
            Look_and_feel_manager.set_region_look(lab,stage,logger);
            left.getChildren().add(lab);
        }
        for(Feature f : basic_features)
        {
            add_one_line(f,left);
        }
        //left.getChildren().add(new Separator());

        {
            Label lab = new Label("Advanced features");
            Look_and_feel_manager.set_region_look(lab,stage,logger);
            left.getChildren().add(lab);
        }
        for(Feature f : advanced_features)
        {
            add_one_line(f,left);
        }





        {
            Label lab = new Label("Experimental features");
            Look_and_feel_manager.set_region_look(lab,stage,logger);
            right.getChildren().add(lab);
        }
        for(Feature f : experimental_features)
        {
            add_one_line(f,right);
        }
        //vbox_right.getChildren().add(new Separator());

        {
            Label lab = new Label("Debug");
            Look_and_feel_manager.set_region_look(lab,stage,logger);
            right.getChildren().add(lab);
        }
        for(Feature f : debugging_features)
        {
            add_one_line(f,right);
        }

        {
            Look_and_feel look_and_feel = Look_and_feel_manager.get_instance(stage,logger);
            double w = 600;
            double icon_size = 128;
            if (Booleans.get_boolean(Feature.Enable_image_similarity.name()))
            {
                Installers.make_ui_to_start_image_similarity_servers(w, icon_size, look_and_feel, right, stage, logger);
                Installers.make_ui_to_stop_image_similarity_servers(w, icon_size, look_and_feel, right, stage, logger);
            }
            if (Booleans.get_boolean(Feature.Enable_face_recognition.name()))
            {
                Installers.make_ui_to_start_face_recognition_servers(w,icon_size,look_and_feel, right, stage, logger);
                Installers.make_ui_to_stop_face_recognition_servers(w,icon_size,look_and_feel, right, stage, logger);
            }

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
            boolean value0 = Booleans.get_boolean(bf.name());
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

            Button button =make_explanation_button(bf.name(), stage,logger);
            if (button == null) return;
            hbox.getChildren().add(button);

        }
        vbox.getChildren().add(hbox);
    }

    //**********************************************************
    public static HBox make_hbox_with_button_and_explanation(String key, EventHandler<ActionEvent> handler, double width, double icon_size, Look_and_feel look_and_feel, Window owner, Logger logger)
    //**********************************************************
    {
        HBox hb = new HBox();
        Button b = new Button(My_I18n.get_I18n_string(key, owner, logger));
        Look_and_feel_manager.set_button_look(b,true,owner,logger);
        //look_and_feel.set_Button_look(b, width, icon_size, null, owner, logger);
        b.setOnAction(handler);
        hb.getChildren().add(b);
        if ( !Feature_cache.get(Feature.Hide_question_mark_buttons_on_mysterious_menus))
        {
            Button explain = More_settings_stage.make_explanation_button(key, owner, logger);
            hb.getChildren().add(explain);
            b.setPrefWidth(width-70);
        }
        else
        {
            b.setPrefWidth(width);
        }
        return hb;
    }
    //**********************************************************
    public static Button make_explanation_button(String key,Window owner, Logger logger)
    //**********************************************************
    {
        Button button = new Button("?");
        String explanation = My_I18n.get_I18n_string(key + EXPLANATION, owner, logger);
        if (explanation == null || explanation.isBlank())
        {
            logger.log("No explanation found for: " + key);
            return null;
        }
        if ( explanation.equals(key+ EXPLANATION))
        {
            // means that no explanation was found in the resources
            // a 'not too bad' default is to copy the key removing underscore ...
            explanation = My_I18n.get_I18n_string(key, owner, logger).replaceAll("_", " ");
        }
        button.setTooltip(new Tooltip(explanation));
        Look_and_feel_manager.set_button_look(button, true,owner, logger);
        String finalExplanation = explanation;
        button.setOnAction(event -> show_explanation(finalExplanation, owner, logger));
        return button;
    }

    //**********************************************************
    private static void show_explanation(String explanation, Window owner, Logger logger)
    //**********************************************************
    {

        Stage explanation_stage = new Stage();
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
        explanation_stage.initOwner(owner);
        explanation_stage.setAlwaysOnTop(true);
        explanation_stage.show();
    }


}
