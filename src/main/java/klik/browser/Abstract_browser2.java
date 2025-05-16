package klik.browser;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.Klik_application2;
import klik.Window_provider;
import klik.actor.Aborter;
import klik.browser.virtual_landscape.Full_screen_handler;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.browser.virtual_landscape.Virtual_landscape2;
import klik.change.Change_gang;
import klik.change.Change_receiver;
import klik.change.history.History_engine;
import klik.look.Look_and_feel_manager;
import klik.properties.Non_booleans;
import klik.util.files_and_paths.Filesystem_item_modification_watcher;
import klik.util.log.Logger;

import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public abstract class Abstract_browser2 implements Change_receiver, Shutdown_target, Title_target, Full_screen_handler, Window_provider
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
    protected My_Stage2 my_Stage;
    protected Virtual_landscape2 virtual_landscape;
    protected final Logger logger;
    protected Aborter aborter;
    protected boolean ignore_escape_as_the_stage_is_full_screen = false;

    protected abstract String get_name();
    protected abstract Path_list_provider get_Path_list_provider();
    protected abstract String signature();
    protected abstract void monitor();

    //**********************************************************
    public Abstract_browser2(Logger logger_)
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
    public void init(New_window_context2 context, Change_receiver change_receiver,String badge)
    //**********************************************************
    {
        int count = number_of_windows.incrementAndGet();
        logger.log("Browser constructor browsers_created(1)=" + count);
        if (context.shutdown_target != null) {
            logger.log("closing previous browser");
            context.shutdown_target.shutdown();
        }

        aborter = new Aborter("Abstract_browser2 for: " + get_name(), logger);

        my_Stage = new My_Stage2(new Stage(), logger);

        my_Stage.the_Stage.setOnCloseRequest(event -> {
            System.out.println("Klik browser window exit");
            System.exit(0);
        });


        double x = 0;
        double y = 0;
        double width = 2400 / 2.0;
        double height = 1080 - y;

        if (count == 1)
        {
            Rectangle2D r = Non_booleans.get_window_bounds(BROWSER_WINDOW);
            width = r.getWidth();
            height = r.getHeight();
            x = r.getMinX();
            y = r.getMinY();
        }
        else
        {
            if (context.rectangle != null)
            {
                x = context.rectangle.getMinX();
                y = context.rectangle.getMinY();
                width = context.rectangle.getWidth();
                height = context.rectangle.getHeight();
            }
        }
        if (dbg) logger.log("NEW Abstract_browser "+x+","+y);

        my_Stage.the_Stage.setX(x);
        my_Stage.the_Stage.setY(y);
        my_Stage.the_Stage.setWidth(width);
        my_Stage.the_Stage.setHeight(height);
        my_Stage.the_Stage.show();

        Look_and_feel_manager.set_icon_for_main_window(my_Stage.the_Stage, badge, Look_and_feel_manager.Icon_type.KLIK);
        // RELOAD a fresh history (e.g. if a drive was re-inserted) and record this in history
        History_engine.get_instance(logger).add(get_name());


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
        virtual_landscape = new Virtual_landscape2(get_Path_list_provider(),my_Stage.the_Stage,this,this,this,this,aborter, logger);
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
    private void record_stage_bounds()
    //**********************************************************
    {
        if (number_of_windows.get() != 1) {
            // ignore: we store the position of a "unique or last" window
            return;
        }
        if (dbg) logger.log("ChangeListener: image window position and/or size changed");
        Non_booleans.save_window_bounds(my_Stage.the_Stage, BROWSER_WINDOW,logger);
    }


    //**********************************************************
    @Override // Shutdown_target
    public void shutdown()
    //**********************************************************
    {
        aborter.abort("Browser is closing for "+get_Path_list_provider().get_name());
        //if (dbg)
        logger.log("Browser close_window " + signature());

        int count = number_of_windows.decrementAndGet();
        logger.log("close_window: browsers_created(2) ="+count);
        if (count ==0)
        {
            if (Klik_application2.primary_stage != null)
            {
                logger.log("primary_stage closing = primary_stage.close()");
                Klik_application2.primary_stage.close();
                Shared_services.shared_services_aborter.abort("primary_stage closing");
            }
            else
            {
                logger.log("SHOULD NOT HAPPEN Abstract_browser2: primary_stage is null");

            }
            logger.log("primary_stage closing = Platform.exit()");
            Platform.exit();
            logger.log("primary_stage closing = System.exit()");
            System.exit(0);
        }
        else {
            logger.log("browsers_created > 0");
        }

        // when we change dir, we need to de-register the old browser
        // otherwise the list in the change_gang keeps growing
        // plus memory leak! ==> the RAM footprint keeps growing
        Change_gang.deregister(this, aborter);
        if (filesystem_item_modification_watcher != null) filesystem_item_modification_watcher.cancel();
        virtual_landscape.stop_scan();
        //the_Pane.getChildren().clear();
        //if (icon_manager != null) icon_manager.cancel_all();
        //logger.log("close_window BEFORE close" + signature());
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
