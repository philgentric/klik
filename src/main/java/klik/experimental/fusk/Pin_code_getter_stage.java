// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.fusk;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import klik.util.execute.actor.Actor_engine;
import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Pin_code_getter_stage
//**********************************************************
{
    //public final Aborter aborter;
    public final Logger logger;
    private AtomicBoolean pin_code_validity = new AtomicBoolean(false);
    private String pin_code_string ="";


    //**********************************************************
    public Pin_code_getter_stage(
            //Aborter aborter,
            Logger logger_)
    //**********************************************************
    {
        //this.aborter = aborter;
        logger = logger_;
    }


    //**********************************************************
    public void ask_pin_code_in_a_thread(Pin_code_client client, Logger logger)
    //**********************************************************
    {
        show();

        Runnable get_pin_code = new Runnable() {
            @Override
            public void run() {

                for(;;)
                {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if ( get_pin_code() != null)
                    {
                        break;
                    }
                }
                client.set_pin_code(get_pin_code());

                logger.log("fusk signature init, pin_code="+get_pin_code());
            }
        };
        Actor_engine.execute(get_pin_code,"Ask user for pin code",logger);
    }
    //**********************************************************
    public String get_pin_code()
    //**********************************************************
    {
        if ( !pin_code_validity.get()) return null;
        return pin_code_string;
    }



    //**********************************************************
    private void show()
    //**********************************************************
    {
        Jfx_batch_injector.inject(()->define(),logger);
    }

    //**********************************************************
    private void define()
    //**********************************************************
    {
        Stage local_stage = new Stage();

        VBox vbox = new VBox();
        final Text pin_code_text = new Text("");
        final Text message = new Text("Enter your pin code, minimum 4 digits");
        vbox.getChildren().add(message);
        {
            HBox hbox = new HBox();
            vbox.getChildren().add(hbox);
            Label label = new Label("Pin Code:");
            hbox.getChildren().add(label);
            hbox.getChildren().add(pin_code_text);

        }
        for(int i = 0; i<10;)
        {
            HBox hbox = new HBox();
            vbox.getChildren().add(hbox);

            int start = i;
            for (int j = start; j < start+4; j++,i++)
            {
                if (j == 10)
                {
                    Button erase = new Button("<-");
                    erase.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            String current = pin_code_text.getText();
                            String after_erase = current.substring(0,current.length()-1);
                            pin_code_text.setText(after_erase);
                        }
                    });
                    hbox.getChildren().add(erase);
                    break;
                }
                Button b = new Button(""+j);
                hbox.getChildren().add(b);
                int finalI = i;
                b.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        logger.log("keyboard="+finalI);
                        pin_code_text.setText(pin_code_text.getText()+finalI);
                    }
                });
            }
        }
        {
            Button validate = new Button("Validate");
            validate.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (pin_code_text.getText().length() < 4) {
                        message.setText("failed: MINIMUM 4 digits");
                        return;
                    }
                    pin_code_string = pin_code_text.getText();
                    pin_code_validity.set(true);
                    message.setText("PIN CODE OK");
                    logger.log("pin code is ->" + pin_code_string + "<-");
                    local_stage.close();
                }
            });
            vbox.getChildren().add(validate);
        }
        {
            Button stupid_default = new Button("Set stupid default: 0000");
            stupid_default.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    pin_code_string = "0000";
                    pin_code_validity.set(true);
                    message.setText("STUPID DEFAULT PIN CODE SET: 0000");
                    logger.log("pin code is ->" + pin_code_string + "<-");
                    local_stage.close();
                }
            });
            vbox.getChildren().add(stupid_default);
        }
        {
            Button disable_fusk = new Button("Disable fusk for now");
            disable_fusk.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    Booleans.set_boolean(Feature.Fusk_is_on.name(),false,local_stage);
                    Feature_cache.update_cached_boolean(Feature.Fusk_is_on,false,local_stage);
                    logger.log("fusk disabled");
                    local_stage.close();
                }
            });
            vbox.getChildren().add(disable_fusk);
        }
        local_stage.setHeight(400);
        local_stage.setWidth(400);

        Scene scene = new Scene(vbox, 1000, 600, Color.WHITE);
        local_stage.setTitle("Enter Pin Code");
        local_stage.setScene(scene);
        local_stage.show();

    }

}
