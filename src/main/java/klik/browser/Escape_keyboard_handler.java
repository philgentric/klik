package klik.browser;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.WindowEvent;

//**********************************************************
public class Escape_keyboard_handler implements javafx.event.EventHandler<KeyEvent>
//**********************************************************
{
    private final Browser browser;
    //**********************************************************
    public Escape_keyboard_handler(Browser browser)
    //**********************************************************
    {
        //browser.logger.log("creating External_close_event_handler for: "+browser.signature());
        this.browser = browser;
    }
    //**********************************************************
    @Override
    public void handle(KeyEvent key_event)
    //**********************************************************
    {

        if ( browser.dbg) browser.logger.log("KeyEvent="+key_event);
        if (key_event.getCode() == KeyCode.ESCAPE)
        {
            browser.logger.log("\n\n\n\nWindows RECEIVES ESCAPE = "+browser.signature());
            if ( browser.my_Stage.escape>0) return;
            browser.my_Stage.escape++;
            key_event.consume();

            if ( browser.exit_on_escape_preference)
            {
                browser.logger.log(" Escape event handler, ignore_escape_as_the_stage_is_full_screen="+browser.ignore_escape_as_the_stage_is_full_screen);
                if ( browser.ignore_escape_as_the_stage_is_full_screen)
                {
                    browser.logger.log("ESCAPE is enabled by user preference, but frame is in fullscreen so ESCAPE => out of full-screen (press ESCAPE again if you want to exit)");
                    browser.ignore_escape_as_the_stage_is_full_screen = false;
                    browser.logger.log("Escape event handler, ignore_escape_as_the_stage_is_full_screen="+browser.ignore_escape_as_the_stage_is_full_screen);
                }
                else
                {
                    browser.logger.log("\n\nESCAPE is enabled by user preference, so ESCAPE => close "+browser.signature());
                    browser.close_window();
                    if (browser.windows_count.get() ==0)
                    {
                        browser.logger.log(browser.signature()+" opening a HOME Browser");
                        Browser_creation_context.additional_no_past(browser.home,browser.logger);
                        browser.logger.log(browser.signature()+" after opening a HOME Browser");
                    }
                }
            }
            else
            {
                browser.logger.log("ESCAPE ignored by user preference");
            }
        }
    }
}

