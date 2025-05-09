//SOURCES ../actor/Actor_engine.java
//SOURCES ../actor/Aborter.java
//SOURCES ../util/ui/Show_running_film_frame.java
//SOURCES ../util/ui/Popups.java
//SOURCES ../util/log/Stack_trace_getter.java
//SOURCES ../util/files_and_paths/Old_and_new_Path.java
//SOURCES ../util/files_and_paths/Filesystem_item_modification_watcher.java
//SOURCES ../util/files_and_paths/Guess_file_type.java
//SOURCES ../util/files_and_paths/Ding.java
//SOURCES ../change/Change_gang.java
//SOURCES ../change/Change_receiver.java
//SOURCES ../change/history/History_engine.java
//SOURCES ../unstable/backup/Backup_singleton.java
//SOURCES ../unstable/fusk/Fusk_bytes.java
//SOURCES ../unstable/fusk/Fusk_singleton.java
//SOURCES ../unstable/fusk/Static_fusk_paths.java
//SOURCES ../look/Look_and_feel_manager.java
//SOURCES ../look/my_i18n/My_I18n.java
//SOURCES ../look/Font_size.java
//SOURCES ../look/Look_and_feel_manager.java
//SOURCES ../look/my_i18n/My_I18n.java
//SOURCES ../look/Jar_utils.java

//SOURCES ./items/Item_image.java
//SOURCES ./items/Item.java

//SOURCES ../properties/Non_booleans.java
//SOURCES ../properties/Non_booleans.java
//SOURCES ./icons/image_properties_cache/Image_properties_RAM_cache.java
//SOURCES ./icons/Refresh_target.java
//SOURCES ./icons/Icon_factory_actor.java
//SOURCES ./icons/Paths_manager.java
//SOURCES ./locator/Folders_with_large_images_locator.java
//SOURCES ../images/decoding/Fast_date_from_OS.java
//SOURCES ./Virtual_landscape_UI.java
//SOURCES ./Scan_show.java
//SOURCES ./External_close_event_handler.java
//SOURCES ./Static_backup_paths.java
//SOURCES ./Error_receiver.java
//SOURCES ./Scan_show_slave.java
//SOURCES ./Selection_reporter.java
//SOURCES ./Selection_handler.java
//SOURCES ./Importer.java
//SOURCES ./Get_folder_files.java

package klik.browser;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import klik.Klik_application;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.virtual_landscape.*;
import klik.change.Change_gang;
import klik.change.Change_receiver;
import klik.change.history.History_engine;
import klik.properties.Booleans;
import klik.properties.Non_booleans;
import klik.util.files_and_paths.*;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;
import klik.util.ui.Jfx_batch_injector;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


//**********************************************************
public class Browser implements Change_receiver, Shutdown_target, Title_target, Full_screen_handler
//**********************************************************
{
    public static final AtomicInteger browsers_created = new AtomicInteger(0);

    public static final boolean dbg = false;
    public static final boolean keyboard_dbg = false;


    public static final String BROWSER_WINDOW = "BROWSER_WINDOW";
    public static Aborter monitoring_aborter;
    private static AtomicInteger ID_generator = new AtomicInteger(1000);
    private final int ID;

    private static final int FOLDER_MONITORING_TIMEOUT_IN_MINUTES = 600;


    Filesystem_item_modification_watcher filesystem_item_modification_watcher;
    public final My_Stage my_Stage;

    public final Virtual_landscape virtual_landscape;
    public final Logger logger;
    public final Path displayed_folder_path;
    public final Aborter aborter;

    boolean ignore_escape_as_the_stage_is_full_screen = false;

    //**********************************************************
    public Browser(
            Browser_creation_context context,
            Logger logger_)
    //**********************************************************
    {
        logger = logger_;

        int count = browsers_created.incrementAndGet();
        logger.log("Browser constructor browsers_created(1)="+count);
        if (context.shutdown_target != null)
        {
            logger.log("closing previous browser");
            context.shutdown_target.shutdown();
        }

        if ( context.folder_path == null)
        {
            displayed_folder_path = Paths.get(System.getProperty(Non_booleans.USER_HOME));
        }
        else
        {
            displayed_folder_path = Path.of(context.folder_path);
        }
        if ( dbg) logger.log("\n\n\n\n\n\n\n\n\n\n\nNEW BROWSER "+displayed_folder_path);

        aborter = new Aborter("Browser for: " + displayed_folder_path.toAbsolutePath().toString(), logger);

        ID = ID_generator.getAndIncrement();
        my_Stage = new My_Stage(new Stage(),logger);// context.stage;//new My_Stage(context.stage,logger);

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


        if (count == 1)
        {
            Rectangle2D r = Non_booleans.get_window_bounds(BROWSER_WINDOW, logger);
            width = r.getWidth();
            height = r.getHeight();
            x = r.getMinX();
            y = r.getMinY();
        }
        else
        {
            if (context.rectangle != null)
            {
                width = context.rectangle.getWidth();//old_browser.the_Stage.getWidth();
                height = context.rectangle.getHeight();//old_browser.the_Stage.getHeight();
                x = context.rectangle.getMinX();//old_browser.the_Stage.getX();
                y = context.rectangle.getMinY();//old_browser.the_Stage.getY();

            }
            if (context.move_a_bit)
            {
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
        History_engine.get_instance(logger).add(displayed_folder_path);


        Change_gang.register(this, aborter, logger);
        set_title();

        Path_list_provider path_list_provider = new Folder_path_list_provider(displayed_folder_path);
        virtual_landscape = new Virtual_landscape(path_list_provider,my_Stage.the_Stage,this,this,this,this,aborter, logger);


        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
           record_stage_bounds();
        };
        my_Stage.the_Stage.xProperty().addListener(change_listener);
        my_Stage.the_Stage.yProperty().addListener(change_listener);
        monitor_folder();

        my_Stage.set_escape_event_handler(this);

        my_Stage.the_Stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (dbg) logger.log("new browser width =" + newValue.doubleValue());
            record_stage_bounds();
            virtual_landscape.redraw_fx("width changed by user");
        });
        my_Stage.the_Stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            record_stage_bounds();
            virtual_landscape.redraw_fx("height changed by user");
        });


        virtual_landscape.redraw_fx("Browser constructor");
    }


    void monitor_folder()
    {
        boolean monitor_this_folder = false;

        // ALWAYS monitor external drives
        monitor_this_folder = Filesystem_item_modification_watcher.is_this_folder_showing_external_drives(displayed_folder_path, logger);

        if (!monitor_this_folder) {
            if (Booleans.get_boolean(Booleans.MONITOR_BROWSED_FOLDERS,logger)) {
                monitor_this_folder = true;
            }
        }

        if (monitor_this_folder) {
            Runnable r = () -> {
                filesystem_item_modification_watcher = Filesystem_item_modification_watcher.monitor_folder(displayed_folder_path, FOLDER_MONITORING_TIMEOUT_IN_MINUTES, monitoring_aborter, logger);
                if (filesystem_item_modification_watcher == null) {
                    logger.log("WARNING: cannot monitor folder " + displayed_folder_path);
                }
            };
            Actor_engine.execute(r, logger);
        }
    }

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



    //**********************************************************
    String signature()
    //**********************************************************
    {
        return "  Browser ID= " + ID + " total window count: " + browsers_created.get() + " esc=" + my_Stage.escape;
    }

    //**********************************************************
    @Override // Shutdown_target
    public void shutdown()
    //**********************************************************
    {
        aborter.abort("Browser is closing for "+displayed_folder_path);
        //if (dbg)
            logger.log("Browser close_window " + signature());

        int count = browsers_created.decrementAndGet();
        logger.log("close_window: browsers_created(2) ="+count);
        if (count ==0)
        {
            if (Klik_application.primary_stage != null)
            {
                logger.log("primary_stage closing = primary_stage.close()");
                Klik_application.primary_stage.close();
            }
            else
            {
                logger.log("primary_stage is null");

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

    //**********************************************************
    public Path get_top_left()
    //**********************************************************
    {
        return virtual_landscape.get_top_left();
    }

    //**********************************************************
    public Rectangle2D get_rectangle()
    //**********************************************************
    {
        return new Rectangle2D(my_Stage.the_Stage.getX(), my_Stage.the_Stage.getY(), my_Stage.the_Stage.getWidth(), my_Stage.the_Stage.getHeight());
    }


    ConcurrentLinkedQueue<Integer> life = new ConcurrentLinkedQueue<>();
    double last_dy;
    long last_scroll_event = -1;
    double last_scroll_speed;

    static AtomicInteger threadid_gen = new AtomicInteger(0);





    //**********************************************************
    @Override // Title_target
    public void set_title()
    //**********************************************************
    {
        if (displayed_folder_path == null) return;
        my_Stage.the_Stage.setTitle(displayed_folder_path.toAbsolutePath().toString());// fast temporary
        Browser browser = this;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                //listFiles can be super slow on network drives or slow drives
                // (e.g. USB)  ==> run in a thread
                int how_many_files = Get_folder_files.how_many_files(browser,logger);
                Jfx_batch_injector.inject(() -> my_Stage.the_Stage.setTitle(displayed_folder_path.toAbsolutePath() + " :     " + (long) how_many_files + " files & folders"), logger);

            }
        };
        Actor_engine.execute(r, logger);


    }



    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Logger logger)
    //**********************************************************
    {

        if (!my_Stage.the_Stage.isShowing()) {
            logger.log("you_receive_this_because_a_file_event_occurred_somewhere event ignored");
            return;
        }

        logger.log("Browser for: "+displayed_folder_path+ ", CHANGE GANG CALL received");

        switch (Change_gang.is_my_directory_impacted(displayed_folder_path, l, logger))
        {
            case more_changes: {
                //if (dbg)
                    logger.log("1 Browser of: " + displayed_folder_path + " RECOGNIZED change gang notification: " + l);

                for ( Old_and_new_Path oan : l)
                {
                    // the events of interest are ONLY the ones
                    // when a file is dropped in.
                    // if a file was moved away or deleted
                    // recording its new path would be a bad bug
                    if ( oan.new_Path != null) {
                        if (oan.new_Path.startsWith(displayed_folder_path)) {
                            Virtual_landscape.scroll_position_cache.put(displayed_folder_path.toAbsolutePath().toString(), oan.new_Path);
                        }
                    }
                }
                virtual_landscape.redraw_fx("change gang for dir: " + displayed_folder_path);
            }
            ;
            break;
            case one_new_file, one_file_gone: {
                //if (dbg)
                    logger.log("2 Browser of: " + displayed_folder_path + " RECOGNIZED change gang notification: " + l);
                virtual_landscape.redraw_fx("change gang for dir: " + displayed_folder_path);
            }
            break;
            default:
                break;
        }
    }


    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        return "Browser:" + displayed_folder_path.toAbsolutePath() + " " + ID;
    }


















}
