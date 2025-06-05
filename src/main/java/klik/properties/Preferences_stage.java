package klik.properties;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.features.Advanced_feature;
import klik.properties.features.Basic_feature;
import klik.properties.features.Debugging_feature;
import klik.properties.features.Experimental_feature;

//**********************************************************
public class Preferences_stage
//**********************************************************
{
    public static final int WIDTH = 1000;
    public final VBox vbox;
    public final Virtual_landscape virtual_landscape;

    //**********************************************************
    public static Preferences_stage show_Preferences_stage(String title,Virtual_landscape virtual_landscape)
    //**********************************************************
    {
        Preferences_stage returned = new Preferences_stage(title, virtual_landscape);
        return returned;
    }

    //**********************************************************
    private Preferences_stage(String title, Virtual_landscape virtual_landscape)
    //**********************************************************
    {
        this.virtual_landscape = virtual_landscape;
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(WIDTH, 1000);
        vbox = new VBox();
        sp.setContent(vbox);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Stage local_stage = new Stage();
        local_stage.setHeight(1000);
        local_stage.setWidth(1000);

        Scene scene = new Scene(sp, 1000, 600, Color.WHITE);
        local_stage.setTitle(title);
        local_stage.setScene(scene);
        local_stage.show();

        define();

        local_stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            for ( Node x : vbox.getChildren())
            {
                if ( x instanceof Button) {
                    Button b = (Button) x;
                    b.setPrefWidth(local_stage.getWidth()-10);
                }
            }
        });
    }

    //**********************************************************
    public void define()
    //**********************************************************
    {
        //logger.log(Stack_trace_getter.get_stack_trace("define!!!"));
        vbox.getChildren().clear();


        {
            Label lab = new Label("Basic features");
            Look_and_feel_manager.set_region_look(lab);
            vbox.getChildren().add(lab);
        }
        for(Basic_feature f : Basic_feature.values())
        {
            add_one_line2(f);
        }
        vbox.getChildren().add(new Separator());

        {
            Label lab = new Label("Advanced features");
            Look_and_feel_manager.set_region_look(lab);
            vbox.getChildren().add(lab);
        }
        for(Advanced_feature f : Advanced_feature.values())
        {
            add_one_line(f.name());
        }
        vbox.getChildren().add(new Separator());

        {
            Label lab = new Label("Experimental features");
            Look_and_feel_manager.set_region_look(lab);
            vbox.getChildren().add(lab);
        }
        for(Experimental_feature f : Experimental_feature.values())
        {
            add_one_line(f.name());
        }
        vbox.getChildren().add(new Separator());

        {
            Label lab = new Label("Debug");
            Look_and_feel_manager.set_region_look(lab);
            vbox.getChildren().add(lab);
        }
        for(Debugging_feature f : Debugging_feature.values())
        {
            add_one_line(f.name());
        }

    }

    //**********************************************************
    private void add_one_line(String name)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(name,virtual_landscape.logger);
        CheckBox cb = new CheckBox(text);
        cb.setMnemonicParsing(false);
        Boolean v = Booleans.get_boolean(name);
        if ( v == null)
        {
            virtual_landscape.logger.log("warning, no Boolean found for: "+ name);
            v = false;
            Booleans.set_boolean(name,v);
        }
        cb.setSelected(v);
        Look_and_feel_manager.set_CheckBox_look(cb);

        cb.setOnAction(_ ->
        {
            Boolean value = cb.isSelected();
            virtual_landscape.logger.log("Preference changing for: "+ name+ "new value:"+value);
            Booleans.set_boolean(name,value); // this will trigger a file save
        });
        vbox.getChildren().add(cb);
    }

    //**********************************************************
    private void add_one_line2(Basic_feature bf)
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string(bf.name(),virtual_landscape.logger);
        CheckBox cb = new CheckBox(text);
        cb.setMnemonicParsing(false);
        Boolean value0 = Booleans.get_boolean(bf.name());

        if ( value0 == null)
        {
            virtual_landscape.logger.log("warning, no Boolean found for: "+ bf.name());
            value0= false;
            Booleans.set_boolean(bf.name(),value0);
            virtual_landscape.update_cached_boolean(bf,value0);

        }
        cb.setSelected(value0);
        Look_and_feel_manager.set_CheckBox_look(cb);

        cb.setOnAction(_ ->
        {
            Boolean value = cb.isSelected();
            virtual_landscape.logger.log("Preference changing for: "+ bf.name()+ "new value:"+value);
            Booleans.set_boolean(bf.name(),value); // this will trigger a file save
            virtual_landscape.update_cached_boolean(bf,value);
        });
        vbox.getChildren().add(cb);
    }


}
