package klik.browser;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import klik.I18N.I18n;
import klik.look.Look_and_feel_manager;
import klik.properties.Properties;
import klik.util.*;

import java.nio.file.Path;

//**********************************************************
public class Top_pane
//**********************************************************
{
    HBox hBox;
    Logger logger;

    //**********************************************************
    public Top_pane(
            Browser the_browser,
            Stage the_stage,
            Scene scene,
            Path dir,
            double top_button_height,
            Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        hBox = new HBox();
        Button up = make_up_button(the_browser, the_stage, dir, top_button_height);

        double tools_button_width = make_tools_button(the_browser, top_button_height);
        double new_window_button_width = make_new_window_button(dir, top_button_height);
        double trash_button_width = make_trash_button(the_browser, the_stage, top_button_height);

        the_stage.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                up.setPrefWidth(
                        newValue.doubleValue()
                                -trash_button_width
                                -tools_button_width
                                -new_window_button_width);
            }
        });
    }

    //**********************************************************
    private double make_tools_button(Browser the_browser, double top_button_height)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Tools",logger);// to: " + parent.toAbsolutePath().toString();

        Button tools_button =  new Button(text);
        Look_and_feel_manager.get_instance().set_directory_style(tools_button);
        double tools_button_width = 120;

        tools_button.setPrefSize(tools_button_width,top_button_height);
        hBox.getChildren().add(tools_button);

        tools_button.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                double x = event.getScreenX();
                double y = event.getScreenY();
                the_browser.show_popup_menu(x,y);
            }
        });
        return tools_button_width;
    }

    //**********************************************************
    private Button make_up_button(Browser the_browser, Stage the_stage, Path dir, double top_button_height)
    //**********************************************************
    {

        Path parent = dir.getParent();
        String text = "";
        if (parent != null)
        {
            text = I18n.get_I18n_string("Parent_Folder",logger);// to: " + parent.toAbsolutePath().toString();
        }


        Button up = make_folder_button(
                the_browser,
                the_stage,
                hBox,
                parent,
                text,
                top_button_height,
                logger);
        Look_and_feel_manager.set_button_look_as_up(up,top_button_height);


        up.setPrefSize(200,top_button_height);
        hBox.getChildren().add(up);
        return up;
    }

    //**********************************************************
    private double make_new_window_button(Path dir, double top_button_height)
    //**********************************************************
    {
       String text = I18n.get_I18n_string("New_window",logger);// to: " + parent.toAbsolutePath().toString();

        Button new_window =  new Button(text);
        Look_and_feel_manager.get_instance().set_directory_style(new_window);
        double new_window_width = 200;
        new_window.setTextAlignment(TextAlignment.CENTER);

        new_window.setPrefSize(new_window_width,top_button_height);
        hBox.getChildren().add(new_window);
        new_window.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Browser.create_browser(null,false,dir,false,logger);
            }
        });
        return new_window_width;
    }

    //**********************************************************
    private double make_trash_button(Browser the_browser, Stage the_stage, double top_button_height)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Trash",logger);// to: " + parent.toAbsolutePath().toString();


        Button trash = make_folder_button(
                the_browser,
                the_stage,
                hBox,
                Properties.get_trash_dir(logger),
                text,
                top_button_height,
                logger);

        Look_and_feel_manager.set_button_look_as_trash(trash, top_button_height);

        double trash_width = 120;
        trash.setPrefSize(trash_width, top_button_height);
        Look_and_feel_manager.get_instance().set_directory_style(trash);
        trash.setPrefSize(200, top_button_height);
        hBox.getChildren().add(trash);
        return trash_width;
    }

    //**********************************************************
    private Button make_folder_button(
            Browser the_browser,
            Stage the_stage,
            HBox hBox,
            Path path,
            String text,
            double top_button_height,
            Logger logger)
    //**********************************************************
    {
        Item_non_image dummy = new Item_non_image(the_browser,
                the_stage,
                hBox,
                path,
                text,
                null,
                top_button_height,
                top_button_height,
                logger);

        dummy.button_for_a_directory(the_stage,hBox,text,top_button_height, true);


        return dummy.button;
    }


}
