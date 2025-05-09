package klik.browser;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.virtual_landscape.Full_screen_handler;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.browser.virtual_landscape.Shutdown_target;
import klik.browser.virtual_landscape.Virtual_landscape;
import klik.change.Change_gang;
import klik.change.Change_receiver;
import klik.change.history.History_engine;
import klik.look.Look_and_feel_manager;
import klik.properties.Non_booleans;
import klik.util.files_and_paths.Filesystem_item_modification_watcher;
import klik.util.log.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Abstract_browser implements Change_receiver, Shutdown_target, Title_target, Full_screen_handler
{

    public static final AtomicInteger browsers_created = new AtomicInteger(0);

    public static final boolean dbg = false;
    public static final boolean keyboard_dbg = false;


    public static final String BROWSER_WINDOW = "BROWSER_WINDOW";
    public static Aborter monitoring_aborter;
    private static AtomicInteger ID_generator = new AtomicInteger(1000);
    protected final int ID;

    protected static final int FOLDER_MONITORING_TIMEOUT_IN_MINUTES = 600;


    Filesystem_item_modification_watcher filesystem_item_modification_watcher;
    public final My_Stage my_Stage;
    public Virtual_landscape virtual_landscape;
    public final Logger logger;
    public final Aborter aborter;
    boolean ignore_escape_as_the_stage_is_full_screen = false;

    abstract protected String get_name();
    public abstract Path_list_provider get_Path_list_provider();
    public abstract String signature();

    //**********************************************************
    public Abstract_browser(Browser_creation_context context, Logger logger_)
    //**********************************************************
    {
        logger = logger_;

        int count = browsers_created.incrementAndGet();
        logger.log("Browser constructor browsers_created(1)=" + count);
        if (context.shutdown_target != null) {
            logger.log("closing previous browser");
            context.shutdown_target.shutdown();
        }


        aborter = new Aborter("Browser for: " + get_name(), logger);

        ID = ID_generator.getAndIncrement();
        my_Stage = new My_Stage(new Stage(), logger);// context.stage;//new My_Stage(context.stage,logger);

        my_Stage.the_Stage.setOnCloseRequest(event -> {
            System.out.println("Klik browser window exit");
            System.exit(0);
        });

        if (context.additional_window) {
            //logger.log(Stack_trace_getter.get_stack_trace("\n\n\nBrowser after create: " +context.folder_path +"\n"+ signature()));
        } else {
            //logger.log(Stack_trace_getter.get_stack_trace("\n\n\nBrowser after dir change: " +context.folder_path +"\n"+ signature()));
        }
        //logger.log("top_left_in_parent="+top_left_in_parent);

        double x = 0;
        double y = 0;

        double width = 2400 / 2.0;
        double height = 1080 - y;


        if (count == 1) {
            Rectangle2D r = Non_booleans.get_window_bounds(BROWSER_WINDOW, logger);
            width = r.getWidth();
            height = r.getHeight();
            x = r.getMinX();
            y = r.getMinY();
        } else {
            if (context.rectangle != null) {
                width = context.rectangle.getWidth();//old_browser.the_Stage.getWidth();
                height = context.rectangle.getHeight();//old_browser.the_Stage.getHeight();
                x = context.rectangle.getMinX();//old_browser.the_Stage.getX();
                y = context.rectangle.getMinY();//old_browser.the_Stage.getY();

            }
            if (context.move_a_bit) {
                x += 100;
                y += 100;
            }

        }
        if (dbg) logger.log("NEW browser");

        my_Stage.the_Stage.setX(x);
        my_Stage.the_Stage.setY(y);
        my_Stage.the_Stage.setWidth(width);
        my_Stage.the_Stage.setHeight(height);
        my_Stage.the_Stage.show();

        Look_and_feel_manager.set_icon_for_main_window(my_Stage.the_Stage, "Klik", Look_and_feel_manager.Icon_type.KLIK);
        // RELOAD a fresh history (e.g. if a drive was re-inserted) and record this in history
        History_engine.get_instance(logger).add(get_name());


        Change_gang.register(this, aborter, logger);
        set_title();


        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            record_stage_bounds();
        };
        my_Stage.the_Stage.xProperty().addListener(change_listener);
        my_Stage.the_Stage.yProperty().addListener(change_listener);

        my_Stage.set_escape_event_handler(this);
    }


    //**********************************************************
    public void init()
    //**********************************************************
    {
        logger.log("Browser init");
        monitor();
        virtual_landscape = new Virtual_landscape(get_Path_list_provider(),my_Stage.the_Stage,this,this,this,this,aborter, logger);
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

    abstract void monitor();



    //**********************************************************
    private void record_stage_bounds()
    //**********************************************************
    {
        if (browsers_created.get() != 1) {
            // ignore: we store the position of a "unique or last" window
            return;
        }
        if (dbg) logger.log("ChangeListener: image window position and/or size changed");
        Non_booleans.save_window_bounds(my_Stage.the_Stage, BROWSER_WINDOW, logger);
    }

}
