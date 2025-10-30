package klik.browser;
//SOURCES ./virtual_landscape/Shutdown_target.java
//SOURCES ./virtual_landscape/Full_screen_handler.java
//SOURCES ./virtual_landscape/Path_list_provider.java
//SOURCES ../Window_provider.java
//SOURCES ./Title_target.java
//SOURCES ../UI_change.java
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.*;
import klik.util.execute.actor.Aborter;
import klik.browser.virtual_landscape.*;
import klik.change.Change_gang;
import klik.change.Change_receiver;
import klik.change.history.History_engine;
import klik.look.Look_and_feel_manager;
import klik.path_lists.Path_list_provider;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Feature_cache;
import klik.util.files_and_paths.modifications.Filesystem_item_modification_watcher;
import klik.util.log.Logger;
import klik.util.tcp.TCP_client;

import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public abstract class Abstract_browser implements Change_receiver, Shutdown_target, Title_target, Full_screen_handler, Window_provider, UI_change, Background_provider
//**********************************************************
{


    public static final boolean dbg = false;
    public static final boolean keyboard_dbg = false;

    public static final AtomicInteger number_of_windows = new AtomicInteger(0);

    public static final String BROWSER_WINDOW = "BROWSER_WINDOW";
    private static AtomicInteger ID_generator = new AtomicInteger(1000);
    protected final int abstract_browser_ID;

    protected static final int FOLDER_MONITORING_TIMEOUT_IN_MINUTES = 600;


    protected Filesystem_item_modification_watcher filesystem_item_modification_watcher;
    protected My_Stage my_Stage;
    protected Virtual_landscape virtual_landscape;
    protected final Logger logger;
    protected Aborter aborter;
    protected boolean ignore_escape_as_the_stage_is_full_screen = false;
    Context_type context_type;

    protected abstract String get_name();
    protected abstract Path_list_provider get_Path_list_provider();
    protected abstract String signature();
    protected abstract void monitor();

    //**********************************************************
    public Abstract_browser(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        abstract_browser_ID = ID_generator.getAndIncrement();

    }

    @Override // Window_provider
    public Window get_owner()
    {
        return my_Stage.the_Stage;
    }

    //**********************************************************
    public void init_abstract_browser(
            Context_type context_type,
            Shutdown_target shutdown_target,
            Rectangle2D rectangle,
            Change_receiver change_receiver,
            String badge)
    //**********************************************************
    {
        this.context_type = context_type;
        int count = number_of_windows.incrementAndGet();
        if ( dbg) logger.log("Browser constructor browsers_created(1)=" + count);
        if (shutdown_target != null)
        {
            if ( dbg) logger.log("closing previous browser");
            shutdown_target.shutdown();
        }

        aborter = new Aborter("Abstract_browser for: " + get_name(), logger);

        my_Stage = new My_Stage(new Stage(), logger);


        // when starting each browser instance will REGISTER a port
        // on which it will LISTEN for UI_CHANGE messages
        // it will report this port to the launcher (if any)
        // so that the launcher can forward UI_CHANGE messages to all browsers
        int ui_change_receiver_port = UI_change.start_UI_change_server(
                null, this, "Klik-browser ",
                        //+get_Path_list_provider().get_name(),
                aborter, my_Stage.the_Stage, logger);
        if ( Klik_application.ui_change_report_port_at_launcher !=null) // is null when launched from ther audio player
        {
            TCP_client.send_in_a_thread("localhost", Klik_application.ui_change_report_port_at_launcher, UI_change.THIS_IS_THE_PORT_I_LISTEN_TO_FOR_UI_CHANGES + " " + ui_change_receiver_port, logger);
        }

        my_Stage.the_Stage.setOnCloseRequest(event -> {
            //System.out.println("Klik browser window exit");
            //System.exit(0);
            shutdown();
        });


        double x = 0;
        double y = 0;
        double width = 2400 / 2.0;
        double height = 1080 - y;

        if (count == 1)
        {
            Rectangle2D r = Non_booleans_properties.get_window_bounds(BROWSER_WINDOW, my_Stage.the_Stage);
            width = r.getWidth();
            height = r.getHeight();
            x = r.getMinX();
            y = r.getMinY();
        }
        else
        {
            if (rectangle != null)
            {
                x = rectangle.getMinX();
                y = rectangle.getMinY();
                width = rectangle.getWidth();
                height = rectangle.getHeight();
            }
        }
        if (dbg) logger.log("NEW Abstract_browser "+x+","+y);

        my_Stage.the_Stage.setX(x);
        my_Stage.the_Stage.setY(y);
        my_Stage.the_Stage.setWidth(width);
        my_Stage.the_Stage.setHeight(height);
        my_Stage.the_Stage.show();

        logger.log("\n\nset_icon_for_main_window before");
        Look_and_feel_manager.set_icon_for_main_window(my_Stage.the_Stage, badge, Look_and_feel_manager.Icon_type.KLIK, my_Stage.the_Stage, logger);
        logger.log("set_icon_for_main_window after\n\n");

        // RELOAD a fresh history (e.g. if a drive was re-inserted) and record this in history
        History_engine.get(get_owner()).add(get_name());


        Change_gang.register(change_receiver, aborter, logger);
        set_title();


        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            record_stage_bounds();
        };
        my_Stage.the_Stage.xProperty().addListener(change_listener);
        my_Stage.the_Stage.yProperty().addListener(change_listener);

        my_Stage.set_escape_event_handler(this);

        logger.log("Browser init");
        monitor();
        virtual_landscape = new Virtual_landscape(context_type,get_Path_list_provider(),my_Stage.the_Stage,this,this,this,this,this,aborter, logger);
        virtual_landscape.redraw_fx("Browser constructor");

        my_Stage.the_Stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (dbg) logger.log("new browser width =" + newValue.doubleValue());
            record_stage_bounds();
            virtual_landscape.redraw_fx("width changed by user");
        });
        my_Stage.the_Stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            record_stage_bounds();
            virtual_landscape.redraw_fx("height changed by user");
        });


    }

    //**********************************************************
    @Override // UI_change signaled by launcher
    public void define_UI()
    //**********************************************************
    {
        virtual_landscape.redraw_fx("UI changed TCP signal received");
    }

    //**********************************************************
    private void record_stage_bounds()
    //**********************************************************
    {
        if (number_of_windows.get() != 1) {
            // ignore: we store the position of a "unique or last" window
            return;
        }
        if (dbg) logger.log("ChangeListener: image window position and/or size changed");
        Non_booleans_properties.save_window_bounds(my_Stage.the_Stage, BROWSER_WINDOW,logger);
    }


    //**********************************************************
    @Override // Shutdown_target
    public void shutdown()
    //**********************************************************
    {
        aborter.abort("Browser is closed for "+get_Path_list_provider().get_name());
        if (dbg) logger.log("Browser shutdown " + signature());

        int count = number_of_windows.decrementAndGet();
        if ( dbg) logger.log("close_window: browsers_created(2) ="+count);
        if (count ==0)
        {
            if (Klik_application.primary_stage != null)
            {
                if (dbg) logger.log("primary_stage closing = primary_stage.close()");
                Klik_application.primary_stage.close();
                Shared_services.aborter().abort("primary_stage closing");
            }
            else
            {
                logger.log("SHOULD NOT HAPPEN Abstract_browser: primary_stage is null");

            }
            if (dbg) logger.log("primary_stage closing GOING TO CALL Platform.exit()");
            Platform.exit();
            if (dbg) logger.log("primary_stage closing GOING TO CALL System.exit()");
            System.exit(0);
        }
        else
        {
            if ( dbg) logger.log("browsers_created > 0");
        }

        // when we change dir, we need to de-register the old browser
        // otherwise the list in the change_gang keeps growing
        // plus memory leak! ==> the RAM footprint keeps growing
        Change_gang.deregister(this, aborter);
        if (filesystem_item_modification_watcher != null) filesystem_item_modification_watcher.cancel();

        Feature_cache.deregister_for_all(virtual_landscape);
        Feature_cache.string_deregister_all(virtual_landscape);

        virtual_landscape.stop_scan();
        my_Stage.close();
    }


    //**********************************************************
    @Override // Full_screen_handler
    public void go_full_screen()
    //**********************************************************
    {
        ignore_escape_as_the_stage_is_full_screen = true;
        my_Stage.the_Stage.setFullScreen(true);
    }

    //**********************************************************
    //@Override // Full_screen_handler
    public void stop_full_screen()
    //**********************************************************
    {
        // this is the menu action, on_fullscreen_end() will be called
        my_Stage.the_Stage.setFullScreen(false);
    }



}
