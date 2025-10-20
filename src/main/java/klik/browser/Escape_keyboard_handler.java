package klik.browser;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;

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

        if ( browser.keyboard_dbg) browser.logger.log("KeyEvent="+key_event);
        if (key_event.getCode() == KeyCode.ESCAPE)
        {
            if ( browser.keyboard_dbg) browser.logger.log("\n\n\n\nWindow RECEIVED ESCAPE = "+browser.signature());
            if ( browser.my_Stage.escape>0) return;
            browser.my_Stage.escape++;
            key_event.consume();

            if (Feature_cache.get(Feature.Use_escape_to_close_windows))
            {
                if ( browser.keyboard_dbg) browser.logger.log(" Escape event handler, ignore_escape_as_the_stage_is_full_screen="+browser.ignore_escape_as_the_stage_is_full_screen);
                if ( browser.ignore_escape_as_the_stage_is_full_screen)
                {
                    if ( browser.keyboard_dbg) browser.logger.log("ESCAPE is enabled by user preference, but frame is in fullscreen so ESCAPE => out of full-screen (press ESCAPE again if you want to exit)");
                    browser.ignore_escape_as_the_stage_is_full_screen = false;
                    if ( browser.keyboard_dbg) browser.logger.log("Escape event handler, ignore_escape_as_the_stage_is_full_screen="+browser.ignore_escape_as_the_stage_is_full_screen);
                }
                else
                {
                    if ( browser.keyboard_dbg) browser.logger.log("\n\nESCAPE is enabled by user preference, so ESCAPE => close "+browser.signature());
                    browser.shutdown();
                    /*
                    if a launcher has been used, we do not need this
                    if (browser.windows_count.get() ==0)
                    {
                        //browser.logger.log(browser.signature()+" opening a HOME Browser");
                        New_file_browser_context.additional_no_past(browser.home,browser.logger);
                        //browser.logger.log(browser.signature()+" after opening a HOME Browser");
                    }
                    */

                }
            }
            else
            {
                if ( browser.keyboard_dbg) browser.logger.log("ESCAPE ignored by user preference");
            }
        }
    }
}

