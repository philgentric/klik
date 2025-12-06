package klik.util.ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.log.Logger;

//**********************************************************
public class Items_with_explanation
//**********************************************************
{
    public static final String EXPLANATION = "_Explanation";

    //**********************************************************
    public static MenuItem make_menu_item_with_explanation(String key, EventHandler<ActionEvent> ev, Window owner, Logger logger)
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(My_I18n.get_I18n_string(key, owner,logger));
        Look_and_feel_manager.set_menu_item_look(menu_item,owner,logger);
        menu_item.setMnemonicParsing(false);
        menu_item.setOnAction(ev);
        if ( !Feature_cache.get(Feature.Hide_question_mark_buttons_on_mysterious_menus))
        {
            Button explanation = make_explanation_button(key, owner, logger);
            menu_item.setGraphic(explanation);
        }
        return menu_item;
    }
    //**********************************************************
    public static void add_question_mark_button(String key, MenuItem item, Window owner, Logger logger)
    //**********************************************************
    {
        if ( !Feature_cache.get(Feature.Hide_question_mark_buttons_on_mysterious_menus))
        {
            Button explanation_button = make_explanation_button(key, owner, logger);
            item.setGraphic(explanation_button);
        }
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
            Button explain = make_explanation_button(key, owner, logger);
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
    public static Button make_explanation_button(String key, Window owner, Logger logger)
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
