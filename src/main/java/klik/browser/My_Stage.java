// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Escape_keyboard_handler.java

package klik.browser;

import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import klik.util.log.Logger;

// the reason to create this class is a strange behavior in javafx:
// the keyboard handler gets installed as many times as the frame is "reused"
// so events are duplicated and sent to each "previous" stage...
// so you press escape once and you kill 3 windows...
// unexpected collateral damage
//**********************************************************
public class My_Stage
//**********************************************************
{
    public final Logger logger;
    public final Stage the_Stage;
    private int usage_count = 0;
    public int escape = 0;

    //**********************************************************
    public My_Stage(Stage the_Stage, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.the_Stage = the_Stage;
    }

    //**********************************************************
    public void close()
    //**********************************************************
    {
        the_Stage.close();
        //logger.log("the_Stage closing");
    }

    //**********************************************************
    public void set_escape_event_handler(Abstract_browser b)
    //**********************************************************
    {
        if ( usage_count == 0) {
            the_Stage.addEventHandler(KeyEvent.KEY_PRESSED, new Escape_keyboard_handler(b));
        }
        usage_count++;
        //logger.log("My_Stage usage_count="+usage_count);

    }
}
