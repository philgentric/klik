package klik.properties;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;

//**********************************************************
public class Preferences_stage
//**********************************************************
{
    public static final int WIDTH = 1000;
    public final VBox vbox;
    public final Logger logger;

    //**********************************************************
    public static Preferences_stage show_Preferences_stage(String title,Logger logger_)
    //**********************************************************
    {
        Preferences_stage returned = new Preferences_stage(title, logger_);
        return returned;
    }

    //**********************************************************
    private Preferences_stage(String title, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
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
        for(Basic_features f : Basic_features.values())
        {
            add_one_line(f.name());
        }
        vbox.getChildren().add(new Separator());

        {
            Label lab = new Label("Advanced features");
            Look_and_feel_manager.set_region_look(lab);
            vbox.getChildren().add(lab);
        }
        for(Advanced_features f : Advanced_features.values())
        {
            add_one_line(f.name());
        }
        vbox.getChildren().add(new Separator());

        {
            Label lab = new Label("Experimental features");
            Look_and_feel_manager.set_region_look(lab);
            vbox.getChildren().add(lab);
        }
        for(Experimental_features f : Experimental_features.values())
        {
            add_one_line(f.name());
        }
        vbox.getChildren().add(new Separator());

        {
            Label lab = new Label("Debug");
            Look_and_feel_manager.set_region_look(lab);
            vbox.getChildren().add(lab);
        }
        for(Debugging_features f : Debugging_features.values())
        {
            add_one_line(f.name());
        }

    }

    //**********************************************************
    private void add_one_line(String name)
    //**********************************************************
    {
        String addendum = "";
        if (name.equals(Basic_features.icons_for_folders.name()))
        {
            addendum = " (requires restart)";
        }
        CheckBox cb = new CheckBox(name+addendum);
        cb.setMnemonicParsing(false);
        Boolean v = Booleans.get_boolean(name);
        if ( v == null)
        {
            v = false;
            Booleans.set_boolean(name,v);
        }
        cb.setSelected(v);
        Look_and_feel_manager.set_CheckBox_look(cb);

        cb.setOnAction(_ ->
        {
            Boolean value = cb.isSelected();
            logger.log("Preference changing for: "+ name+ "new value:"+value);
            Booleans.set_boolean(name,value);
        });
        vbox.getChildren().add(cb);
    }

}
