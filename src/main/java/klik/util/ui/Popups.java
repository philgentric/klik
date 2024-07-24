package klik.util.ui;

import javafx.animation.PauseTransition;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.util.Optional;

//**********************************************************
public class Popups
//**********************************************************
{


    //**********************************************************
    public static void popup_Exception(Exception e, double icon_size, String title, Logger logger)
    //**********************************************************
    {
        logger.log(Stack_trace_getter.get_stack_trace("Going to popup exception(1): " + e));
        Jfx_batch_injector.inject(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.getDialogPane().setMinWidth(1000);
            alert.setTitle(title);
            alert.setHeaderText("Operation was denied");
            alert.setContentText(
                    "The error was: \n" + e);

            logger.log("Going to popup exception(2): " + e);
            alert.setGraphic(new ImageView(Look_and_feel_manager.get_denied_icon(icon_size)));
            alert.showAndWait();
        },logger);
    }

    //**********************************************************
    public static void popup_warning(Stage owner, String header, String content, boolean for_3_seconds_only, Logger logger)
    //**********************************************************
    {
        Jfx_batch_injector.inject(() -> {
            logger.log("Warning Popup: "+header+" "+content);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(My_I18n.get_I18n_string("Warning", logger));
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.initOwner(owner);
            if (for_3_seconds_only) {
                PauseTransition delay = new PauseTransition(Duration.millis(1000));
                delay.setOnFinished(e -> alert.hide());

                alert.show();
                delay.play();
            } else {
                alert.showAndWait();
            }
        },logger);
    }

    //**********************************************************
    public static boolean popup_ask_for_confirmation(Stage owner, String header, String content, Logger logger)
    //**********************************************************
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle(My_I18n.get_I18n_string("Please_confirm", logger));
        alert.setHeaderText(header);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent())
        {
            if (result.get() == ButtonType.OK)
            {
                return true;
            }
        }

        // ... user chose CANCEL or closed the dialog
        return false;
    }


    //**********************************************************
    public static void simple_alert(String s)
    //**********************************************************
    {
        Alert a = new Alert(Alert.AlertType.INFORMATION,s, ButtonType.CLOSE);
        a.show();
    }
}
