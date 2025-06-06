package klik.properties;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.features.*;
import klik.util.log.Logger;

//**********************************************************
public class Preferences_stage
//**********************************************************
{
    public static final int WIDTH = 1000;
    public final VBox vbox_basic;
    public final VBox vbox_advanced;
    public final VBox vbox_experimental;
    public final VBox vbox_debug;
    public final Logger logger;
    private static Preferences_stage instance;
    private final Stage local_stage;

    public static final Feature[] advanced_features ={
            Feature.Monitor_folders,
            Feature.Enable_face_recognition,
            Feature.Enable_image_similarity,
            Feature.Enable_bit_level_deduplication,
            Feature.Enable_recursive_empty_folders_removal,
            Feature.Enable_auto_purge_disk_caches,
            Feature.Display_image_distances,
            Feature.Play_ding_after_long_processes,
            Feature.Shift_d_is_sure_delete};

    public static final Feature[] basic_features ={
            Feature.Show_icons_for_files,
            Feature.Show_icons_for_folders,
            Feature.Show_hidden_files,
            Feature.Show_hidden_folders,
            Feature.Show_single_column,
            Feature.Reload_last_folder_on_startup,
            Feature.Dont_zoom_small_images,
            Feature.Use_escape_to_close_windows
    };

    public static final Feature[] debugging_features ={
            Feature.Log_to_file,
            Feature.Enable_detailed_cache_cleaning_options,
            Feature.Fusk_is_active,
            Feature.Show_ffmpeg_install_warning,
            Feature.Show_GraphicsMagick_install_warning
    };

    public static final Feature[] experimental_features ={
            Feature.Enable_backup,
            Feature.Enable_tags,
            Feature.Enable_fusk,
            Feature.Enable_name_cleaning,
            Feature.Enable_corrupted_images_removal,
            Feature.Enable_image_playlists,
            Feature.Enable_different_image_scaling
    };
    //**********************************************************
    public static void show_Preferences_stage(String title, Logger logger)
    //**********************************************************
    {
        if ( instance != null)
        {
            instance.show();
            return;
        }
        instance = new Preferences_stage(title,logger);
    }

    //**********************************************************
    private void show()
    //**********************************************************
    {
        local_stage.show();
    }

    //**********************************************************
    private Preferences_stage(String title, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(WIDTH, 1000);
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

        local_stage = new Stage();
        local_stage.setHeight(1000);
        local_stage.setWidth(1000);

        Scene scene = new Scene(sp, 1000, 600, Color.WHITE);
        local_stage.setTitle(title);
        local_stage.setScene(scene);
        local_stage.show();

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
            Look_and_feel_manager.set_region_look(lab);
            vbox_basic.getChildren().add(lab);
        }
        for(Feature f : basic_features)
        {
            add_one_line(f,vbox_basic);
        }
        //vbox_basic.getChildren().add(new Separator());

        {
            Label lab = new Label("Advanced features");
            Look_and_feel_manager.set_region_look(lab);
            vbox_advanced.getChildren().add(lab);
        }
        for(Feature f : advanced_features)
        {
            add_one_line(f,vbox_advanced);
        }





        {
            Label lab = new Label("Experimental features");
            Look_and_feel_manager.set_region_look(lab);
            vbox_experimental.getChildren().add(lab);
        }
        for(Feature f : experimental_features)
        {
            add_one_line(f,vbox_experimental);
        }
        //vbox_right.getChildren().add(new Separator());

        {
            Label lab = new Label("Debug");
            Look_and_feel_manager.set_region_look(lab);
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
        String text = My_I18n.get_I18n_string(bf.name(),logger);
        CheckBox cb = new CheckBox(text);
        cb.setMnemonicParsing(false);
        Boolean value0 = Booleans.get_boolean(bf.name());

        if ( value0 == null)
        {
            logger.log("warning, no Boolean found for: "+ bf.name());
            value0= false;
            Booleans.set_boolean(bf.name(),value0);
            Feature_cache.update_cached_feature(bf,value0);

        }
        cb.setSelected(value0);
        Look_and_feel_manager.set_CheckBox_look(cb);

        cb.setOnAction(_ ->
        {
            Boolean value = cb.isSelected();
            logger.log("Preference changing for: "+ bf.name()+ "new value:"+value);
            Booleans.set_boolean(bf.name(),value); // this will trigger a file save
            Feature_cache.update_cached_feature(bf,value);
        });
        vbox.getChildren().add(cb);
    }


}
