// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.ui;

import javafx.animation.PauseTransition;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
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
    public static void popup_Exception(Exception e, double icon_size, String title, Window owner, Logger logger)
    //**********************************************************
    {
        logger.log(Stack_trace_getter.get_stack_trace("Going to popup exception(1): " + e));
        Jfx_batch_injector.inject(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(owner);
            Look_and_feel_manager.set_dialog_look(alert, owner,logger);
            alert.getDialogPane().setMinWidth(1000);
            alert.setTitle(title);
            alert.setHeaderText("Operation was denied");
            if ( e==null)
            {
                alert.setContentText(title);
            }
            else
            {
                alert.setContentText("The error was: \n" + e);
            }

            logger.log("Going to popup exception(2): " + e);
            alert.setGraphic(new ImageView(Look_and_feel_manager.get_denied_icon(icon_size,owner,logger)));
            alert.showAndWait();

        },logger);
    }

    //**********************************************************
    public static void popup_warning(String header, String content, boolean for_3_seconds_only, Window owner, Logger logger)
    //**********************************************************
    {
        Jfx_batch_injector.inject(() -> {
            logger.log("Warning Popup: "+header+" "+content);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            Look_and_feel_manager.set_dialog_look(alert, owner,logger);
            alert.setTitle(My_I18n.get_I18n_string("Warning", owner,logger));
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
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
    public static boolean popup_ask_for_confirmation(String header, String content, Window owner, Logger logger)
    //**********************************************************
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        Look_and_feel_manager.set_dialog_look(alert, owner,logger);
        alert.initOwner(owner);
        alert.setTitle(My_I18n.get_I18n_string("Please_confirm", owner,logger));
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
    public static void simple_alert(String s, Window owner, Logger logger)
    //**********************************************************
    {
        // this is a BLOCKING window
        Alert alert = new Alert(Alert.AlertType.INFORMATION,s, ButtonType.CLOSE);
        Look_and_feel_manager.set_dialog_look(alert, owner,logger);
        alert.initOwner(owner);
        alert.show();
    }

    // if returns true, means "dont show me this again"
    //**********************************************************
    public static boolean info_popup(String s, String OK_button_alternate_text, Window owner, Logger logger)
    //**********************************************************
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        Look_and_feel_manager.set_dialog_look(alert, owner,logger);
        alert.initOwner(owner);
        alert.setTitle("Information");
        alert.setHeaderText(s);
        alert.setContentText("");
        Button ok = (Button)alert.getDialogPane().lookupButton(ButtonType.OK);
        if ( OK_button_alternate_text != null) ok.setText(OK_button_alternate_text);

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
}
