package klik.browser;

import javafx.event.EventHandler;
import javafx.stage.WindowEvent;

//**********************************************************
public class External_close_event_handler implements EventHandler<WindowEvent>
//**********************************************************
{

    private final Browser browser;

    //**********************************************************
    public External_close_event_handler(Browser browser)
    //**********************************************************
    {
        //browser.logger.log("creating External_close_event_handler for: "+browser.signature());
        this.browser = browser;
    }

    public String signature(){ return browser.signature();}

    //**********************************************************
    @Override
    public void handle(WindowEvent windowEvent)
    //**********************************************************
    {
        //browser.logger.log("\n\n\nBrowser External_close_event_handler = "+browser.signature());
        browser.close_window();
        //browser.logger.log("After close = "+ browser.signature());

        if (Browser.windows_count.get() ==0)
        {
            browser.logger.log(browser.signature()+"... calling System.exit(0)");
            System.exit(0);
        }
    }
}
