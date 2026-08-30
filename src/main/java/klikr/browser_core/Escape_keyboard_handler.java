// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser_core;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import klikr.browsers.Browser_for_file_system_in_2D;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.log.Logger;

//**********************************************************
public class Escape_keyboard_handler implements javafx.event.EventHandler<KeyEvent>
//**********************************************************
{
    private final Abstract_browser browser;
    //**********************************************************
    public Escape_keyboard_handler(Abstract_browser browser)
    //**********************************************************
    {
        //browser.logger.log("creating Escape_keyboard_handler for: "+browser.signature());
        this.browser = browser;
    }
    //**********************************************************
    @Override
    public void handle(KeyEvent key_event)
    //**********************************************************
    {

        if ( Browser_for_file_system_in_2D.kbd_dbg) browser.logger.log("KeyEvent="+key_event);
        if (key_event.getCode() == KeyCode.ESCAPE)
        {
            if ( Browser_for_file_system_in_2D.kbd_dbg) browser.logger.log(Logger.ok+" Window RECEIVED ESCAPE = "+browser.signature());
            if ( browser.my_Stage.escape>0) return;
            browser.my_Stage.escape++;
            key_event.consume();

            if (Feature_cache.get(Feature.Use_escape_to_close_windows))
            {
                if ( Browser_for_file_system_in_2D.kbd_dbg) browser.logger.log(Logger.ok+" Escape event handler, ignore_escape_as_the_stage_is_full_screen="+browser.ignore_escape_as_the_stage_is_full_screen);
                if ( browser.ignore_escape_as_the_stage_is_full_screen)
                {
                    if ( Browser_for_file_system_in_2D.kbd_dbg) browser.logger.log(Logger.ok+" ESCAPE is enabled by user preference, but frame is in fullscreen so ESCAPE => out of full-screen (press ESCAPE again if you want to exit)");
                    browser.ignore_escape_as_the_stage_is_full_screen = false;
                    if ( Browser_for_file_system_in_2D.kbd_dbg) browser.logger.log(Logger.ok+" Escape event handler, ignore_escape_as_the_stage_is_full_screen="+browser.ignore_escape_as_the_stage_is_full_screen);
                }
                else
                {
                    if ( Browser_for_file_system_in_2D.kbd_dbg) browser.logger.log(Logger.ok+" ESCAPE is enabled by user preference, so ESCAPE => close "+browser.signature());
                    browser.shutdown();
                }
            }
            else
            {
                if ( Browser_for_file_system_in_2D.kbd_dbg) browser.logger.log(Logger.ok+" ESCAPE ignored by user preference");
            }
        }
    }
}

