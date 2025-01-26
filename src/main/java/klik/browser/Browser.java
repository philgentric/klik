//SOURCES ../actor/Actor_engine.java
//SOURCES ../actor/Aborter.java
//SOURCES ../util/ui/Show_running_man_frame.java
//SOURCES ../util/ui/Popups.java
//SOURCES ../util/log/Stack_trace_getter.java
//SOURCES ../util/files_and_paths/Old_and_new_Path.java
//SOURCES ../util/files_and_paths/Filesystem_item_modification_watcher.java
//SOURCES ../util/files_and_paths/Guess_file_type.java
//SOURCES ../util/files_and_paths/Ding.java
//SOURCES ../change/Change_gang.java
//SOURCES ../change/Change_receiver.java
//SOURCES ../change/history/History_engine.java
//SOURCES ../level2/backup/Backup_singleton.java
//SOURCES ../level3/fusk/Fusk_bytes.java
//SOURCES ../level3/fusk/Fusk_singleton.java
//SOURCES ../level3/fusk/Static_fusk_paths.java
//SOURCES ../look/Look_and_feel_manager.java
//SOURCES ../look/my_i18n/My_I18n.java
//SOURCES ../look/Font_size.java
//SOURCES ../look/Look_and_feel_manager.java
//SOURCES ../look/my_i18n/My_I18n.java
//SOURCES ../look/Jar_utils.java

//SOURCES ./items/Item_image.java
//SOURCES ./items/Item.java

//SOURCES ../properties/Static_application_properties.java
//SOURCES ../properties/Static_application_properties.java
//SOURCES ./icons/image_properties_cache/Image_properties_RAM_cache.java
//SOURCES ./icons/Refresh_target.java
//SOURCES ./icons/Icon_factory_actor.java
//SOURCES ./icons/Paths_manager.java
//SOURCES ./locator/Folders_with_large_images_locator.java
//SOURCES ../images/decoding/Fast_date_from_OS.java
//SOURCES ./Browser_UI.java
//SOURCES ./Scan_show.java
//SOURCES ./External_close_event_handler.java
//SOURCES ./Static_backup_paths.java
//SOURCES ./Error_receiver.java
//SOURCES ./Scan_show_slave.java
//SOURCES ./Selection_reporter.java
//SOURCES ./Selection_handler.java
//SOURCES ./Importer.java

package klik.browser;

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.icons.*;
import klik.browser.items.Item;
import klik.browser.locator.Folders_with_large_images_locator;
import klik.change.Change_gang;
import klik.change.Change_receiver;
import klik.change.history.History_engine;
import klik.image_ml.image_similarity.Image_feature_vector_cache;
import klik.look.my_i18n.My_I18n;
import klik.util.execute.Execute_command;
import klik.util.files_and_paths.*;
import klik.level2.backup.Backup_singleton;
import klik.level3.fusk.Fusk_bytes;
import klik.level3.fusk.Fusk_singleton;
import klik.level3.fusk.Static_fusk_paths;
import klik.look.Font_size;
import klik.look.Jar_utils;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Hourglass;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;
import klik.util.ui.Show_running_man_frame;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.awt.Taskbar.Feature.ICON_IMAGE;
import static klik.browser.icons.animated_gifs.Animated_gif_from_folder.warning_GraphicsMagick;


//**********************************************************
public class Browser implements Change_receiver, Scan_show_slave, Selection_reporter
//**********************************************************
{
    public static final boolean dbg = false;
    public static final boolean keyboard_dbg = false;


    public static final String BROWSER_WINDOW = "BROWSER_WINDOW";
    public static Aborter monitoring_aborter;
    public static boolean show_running_man = true;
    static AtomicInteger windows_count = new AtomicInteger(0);
    private static AtomicInteger ID_generator = new AtomicInteger(1000);
    private final int ID;

    private static final int FOLDER_MONITORING_TIMEOUT_IN_MINUTES = 600;

    public List<Node> mandatory_in_pane = new ArrayList<>();
    List<Node> always_on_front_nodes = new ArrayList<>();

    Filesystem_item_modification_watcher filesystem_item_modification_watcher;
    public final My_Stage my_Stage;
    public Scene the_Scene;
    final Pane the_Fucking_Pane;
    public final Virtual_landscape virtual_landscape;
    public final Logger logger;
    public final Path displayed_folder_path;
    public final Selection_handler selection_handler;
    public final Aborter aborter;


    TextField status;
    public Vertical_slider vertical_slider;
    public double slider_width = 400;

    boolean exit_on_escape_preference;
    boolean ignore_escape_as_the_stage_is_full_screen = false;

    final Browser_menus browser_menus;
    public final Browser_UI browser_ui;
    static Path home = Paths.get(System.getProperty(Static_application_properties.USER_HOME));

    // make sure we go again at the same scroll point when we enter a given folder
    static Map<Path, Path> scroll_position_cache = new HashMap<>();
    public double max_dir_text_length;



    //**********************************************************
    public void import_apple_Photos()
    //**********************************************************
    {
        if (!Popups.popup_ask_for_confirmation(my_Stage.the_Stage, "Importing photos will create COPIES", "Please select a destination drive with enough space", logger))
            return;

        Importer.perform_import(my_Stage.the_Stage, aborter, logger);
    }

    //**********************************************************
    public void estimate_size_of_importing_apple_Photos()
    //**********************************************************
    {
        Importer.estimate_size(my_Stage.the_Stage, aborter, logger);
    }

    //**********************************************************
    public void show_where_are_images()
    //**********************************************************
    {
        Folders_with_large_images_locator.locate(displayed_folder_path, 10, 200_000, this, logger);
    }



    //**********************************************************
    public void clear_all_RAM_caches()
    //**********************************************************
    {
        virtual_landscape.clear_image_properties_RAM_cache_fx();
        logger.log("Image properties cache cleared");
        Browser.scroll_position_cache.clear();
        logger.log("Return-to scroll positions cache cleared");
        clear_image_comparators_caches();
        logger.log("Image comparators caches cleared");
        clear_image_similarity_RAM_cache();
    }



    //**********************************************************
    public void clear_image_comparators_caches()
    //**********************************************************
    {
        ((Clearable_RAM_cache) (virtual_landscape.image_file_comparator)).clear_RAM_cache();
        ((Clearable_RAM_cache) (virtual_landscape.other_file_comparator)).clear_RAM_cache();
    }

    //**********************************************************
    public void clear_image_similarity_RAM_cache()
    //**********************************************************
    {
        Image_feature_vector_cache.images_and_feature_vectors_cache.clear();
    }

    public Comparator<? super Path> get_file_comparator() {
        return virtual_landscape.get_file_comparator();
    }

    enum Scan_state {
        off,
        down,
        up
    }

    private Scan_state scan_state = Scan_state.off;

    //**********************************************************
    public Browser(
            Browser_creation_context context,
            Logger logger_)
    //**********************************************************
    {
        if (context.old_browser != null)
        {
            context.old_browser.cleanup();
        }

        logger = logger_;
        displayed_folder_path = context.folder_path;

        aborter = new Aborter("Browser for: " + displayed_folder_path.toAbsolutePath().toString(), logger);

        //logger.log("\n\n\n\n\n\n\n\n\n\n\nNEW BROWSER "+aborter.name);
        ID = ID_generator.getAndIncrement();
        my_Stage = new My_Stage(new Stage(), logger);// context.stage;//new My_Stage(context.stage,logger);

        if (context.additional_window) {
            windows_count.incrementAndGet();
            //logger.log(Stack_trace_getter.get_stack_trace("\n\n\nBrowser after create: " +context.folder_path +"\n"+ signature()));
        } else {
            //logger.log(Stack_trace_getter.get_stack_trace("\n\n\nBrowser after dir change: " +context.folder_path +"\n"+ signature()));
        }
        //logger.log("top_left_in_parent="+top_left_in_parent);

        double x = 0;
        double y = 0;

        double width = 2400 / 2.0;
        double height = 1080 - y;

        if (windows_count.get() == 1) {
            Rectangle2D r = Static_application_properties.get_window_bounds(BROWSER_WINDOW, logger);
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
        {
            double font_size = Static_application_properties.get_font_size(logger);
            double icon_height = klik.look.Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;

            //String icon_path = Look_and_feel_manager.get_instance().get_folder_icon_path();
            //if (icon_path == null) logger.log("WARNING: could not load folder icon");

            Image icon = Look_and_feel_manager.get_folder_icon(icon_height);//  load_icon_fx_from_jar(icon_path, icon_height);

            if (icon == null) {
                logger.log("WARNING: cannot get default icon for directory");
            } else {
                my_Stage.the_Stage.getIcons().add(icon);
            }
        }
        my_Stage.the_Stage.setX(x);
        my_Stage.the_Stage.setY(y);
        my_Stage.the_Stage.setWidth(width);
        my_Stage.the_Stage.setHeight(height);
        my_Stage.the_Stage.show();

        set_icon();
        // RELOAD a fresh history (e.g. if a drive was re-inserted) and record this in history
        History_engine.get_instance(logger).add(displayed_folder_path);


        Change_gang.register(this, aborter, logger);
        set_title();
        the_Fucking_Pane = new Pane();

        logger.log("BROWSER creating Image_properties_RAM_cache with aborter: "+aborter.name);
        virtual_landscape = new Virtual_landscape(this, the_Fucking_Pane,aborter, logger);
        selection_handler = new Selection_handler(the_Fucking_Pane, virtual_landscape, this, logger);
        browser_menus = new Browser_menus(this, selection_handler, logger_);
        exit_on_escape_preference = Static_application_properties.get_escape(logger);
        {
            browser_ui = new Browser_UI(this);
            browser_ui.define_UI();
        }
        virtual_landscape.set_Landscape_height_listener(vertical_slider);
        virtual_landscape.set_scroll_to_listener(vertical_slider);

        set_all_event_handlers(the_Fucking_Pane);

        my_Stage.the_Stage.setScene(the_Scene);

        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
           record_stage_bounds();
        };
        my_Stage.the_Stage.xProperty().addListener(change_listener);
        my_Stage.the_Stage.yProperty().addListener(change_listener);


        redraw_fx("Browser constructor");
    }

    //**********************************************************
    private void record_stage_bounds()
    //**********************************************************
    {
        if (windows_count.get() != 1) {
            // ignore: we store the position of a "unique or last" window
            return;
        }
        if (dbg) logger.log("ChangeListener: image window position and/or size changed");
        Static_application_properties.save_window_bounds(my_Stage.the_Stage, BROWSER_WINDOW, logger);
    }

    //**********************************************************
    private void set_icon()
    //**********************************************************
    {
        Look_and_feel look_and_feel = Look_and_feel_manager.get_instance();
        if (look_and_feel == null) {
            logger.log("BAD WARNING: cannot get look and feel instance");
            return;
        }
        String klik_image_path = look_and_feel.get_klik_icon_path();

        my_Stage.the_Stage.getIcons().clear();
        Image taskbar_icon = null;
        int[] icon_sizes = {16, 32, 64, 128};
        for (int s : icon_sizes)
        {
            Image icon = Jar_utils.load_jfx_image_from_jar(klik_image_path, s, logger);
            if (icon != null) {
                my_Stage.the_Stage.getIcons().add(icon);
                taskbar_icon = icon;
            }
        }
        if (taskbar_icon != null) {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar task_bar = Taskbar.getTaskbar();
                if (task_bar.isSupported(ICON_IMAGE)) {
                    BufferedImage bim = JavaFX_to_Swing.fromFXImage(taskbar_icon, null, logger);
                    task_bar.setIconImage(bim);
                }
                if (task_bar.isSupported(Taskbar.Feature.ICON_BADGE_TEXT)) {
                    task_bar.setIconBadge("Klik");
                }
            }
        }
    }


    //**********************************************************
    String signature()
    //**********************************************************
    {
        return "Stage:" + my_Stage.the_Stage + "  Browser ID= " + ID + " total window count: " + windows_count.get() + " esc=" + my_Stage.escape;
    }

    //**********************************************************
    void cleanup()
    //**********************************************************
    {
        aborter.abort("Browser is closing for "+displayed_folder_path);
        if (dbg) logger.log("Browser cleanup " + signature());
        // when we change dir, we need to de-register the old browser
        // otherwise the list in the change_gang keeps growing
        // plus memory leak! ==> the RAM footprint keeps growing
        Change_gang.deregister(this, aborter);
        if (filesystem_item_modification_watcher != null) filesystem_item_modification_watcher.cancel();
        stop_scan();
        the_Fucking_Pane.getChildren().clear();
        //if (icon_manager != null) icon_manager.cancel_all();
    }

    //**********************************************************
    void close_window()
    //**********************************************************
    {
        cleanup();

        //logger.log("close_window BEFORE close" + signature());
        my_Stage.close();
        windows_count.decrementAndGet();
        //logger.log("close_window AFTER close" + signature());
    }


    //**********************************************************
    public void go_full_screen()
    //**********************************************************
    {
        ignore_escape_as_the_stage_is_full_screen = true;
        my_Stage.the_Stage.setFullScreen(true);
    }

    //**********************************************************
    //@Override // Browser_interface
    public void stop_full_screen()
    //**********************************************************
    {
        // this is the menu action, on_fullscreen_end() will be called
        my_Stage.the_Stage.setFullScreen(false);
    }


    //**********************************************************
    public void set_status(String s)
    //**********************************************************
    {
        status.setText(s);
        logger.log("Status = " + s);
    }

    //**********************************************************
    public void set_escape_preference(boolean value)
    //**********************************************************
    {
        exit_on_escape_preference = value;
    }

    //**********************************************************
    public boolean get_escape_preference()
    //**********************************************************
    {
        return exit_on_escape_preference;
    }

    //**********************************************************
    public void show_how_many_files_deep_in_each_folder()
    //**********************************************************
    {
        virtual_landscape.show_how_many_files_deep_in_each_folder();
    }

    //**********************************************************
    public void show_total_size_deep_in_each_folder()
    //**********************************************************
    {
        virtual_landscape.show_total_size_deep_in_each_folder(this, the_Fucking_Pane.getWidth(), the_Fucking_Pane.getHeight(), mandatory_in_pane);
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

    //**********************************************************
    private void set_all_event_handlers(Pane pane)
    //**********************************************************
    {
        boolean monitor_this_folder = false;

        // ALWAYS monitor external drives
        monitor_this_folder = Filesystem_item_modification_watcher.is_this_folder_showing_external_drives(displayed_folder_path, logger);

        if (!monitor_this_folder) {
            if (Static_application_properties.get_monitor_browsed_folders(logger)) {
                monitor_this_folder = true;
            }
        }

        if (monitor_this_folder) {
            Runnable r = () -> {
                filesystem_item_modification_watcher = Filesystem_item_modification_watcher.monitor_folder(displayed_folder_path, FOLDER_MONITORING_TIMEOUT_IN_MINUTES, Browser.monitoring_aborter, logger);
                if (filesystem_item_modification_watcher == null) {
                    logger.log("WARNING: cannot monitor folder " + displayed_folder_path);
                }
            };
            Actor_engine.execute(r, logger);
        }
        my_Stage.the_Stage.fullScreenProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (dbg) logger.log("fullScreenProperty changed ! new value = " + newValue.booleanValue());
                    if (!newValue.booleanValue()) {
                        browser_ui.on_fullscreen_end();
                    } else {
                        browser_ui.on_fullscreen_start();
                    }
                }
        );


        Browser this_browser = this;
        {
            pane.addEventHandler(MouseEvent.MOUSE_PRESSED, selection_handler::handle_mouse_pressed);
            pane.addEventHandler(MouseEvent.MOUSE_DRAGGED, selection_handler::handle_mouse_dragged);
            pane.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseEvent -> selection_handler.handle_mouse_released(mouseEvent, this_browser));
        }

        EventHandler<WindowEvent> on_close_event_handler = new External_close_event_handler(this);
        my_Stage.the_Stage.setOnCloseRequest(on_close_event_handler);
        my_Stage.set_escape_event_handler(this);


        Browser local = this;
        the_Scene.setOnKeyTyped(keyEvent -> {

            if (keyboard_dbg) {
                logger.log(keyEvent + "getCharacter->" + keyEvent.getCharacter() + "<- getCode:" + keyEvent.getCode());
                if (keyEvent.isShiftDown()) logger.log("isShiftDown: true");
                if (keyEvent.isAltDown()) logger.log("isAltDown: true");
                if (keyEvent.isMetaDown()) logger.log("isMetaDown: true");
            }

            if (keyEvent.getCharacter().equals("k")) {
                if (keyboard_dbg) logger.log("character is k = keyword search");
                browser_ui.search_files_by_keyworks_fx();
            }
            if (keyEvent.getCharacter().equals("s")) {
                if (keyboard_dbg) logger.log("character is s = start/stop scan");
                handle_scan_switch();
            }
            if (keyEvent.getCharacter().equals("w")) {
                if (keyboard_dbg) logger.log("character is w = slow down scan");
                slow_down_scan();
            }
            if (keyEvent.getCharacter().equals("x")) {
                if (keyboard_dbg) logger.log("character is x = speed up scan");
                speed_up_scan();
            }

            if (keyEvent.isMetaDown()) {
                if (keyEvent.getCharacter().equals("a")) {
                    if (keyboard_dbg) logger.log("character is a + meta = select all");
                    selection_handler.select_all_files_in_folder(local);
                }

                if (keyEvent.getCharacter().equals("+")) {
                    if (keyboard_dbg) logger.log("character is +meta = zoom +");
                    increase_icon_size();
                }
                if (keyEvent.getCharacter().equals("=")) {
                    if (keyboard_dbg) logger.log("character is  (meta & equal) +meta = zoom +");
                    increase_icon_size();
                }
                if (keyEvent.getCharacter().equals("-")) {
                    if (keyboard_dbg) logger.log("character is -meta = zoom -");
                    reduce_icon_size();
                }
            }
            if (keyEvent.getCharacter().equals("n")) {
                if (keyboard_dbg) logger.log("character is n = new broser (clone)");
                Browser_creation_context.additional_same_folder(local, logger);
            }
        });

        the_Scene.setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Browser: OnDragOver handler called");
            selection_handler.on_drag_over();
            Object source = drag_event.getGestureSource();
            if (source == null) {
                //logger.log("source class is null " + event.toString());
            } else {
                if (!(source instanceof Item)) {
                    if (dbg)
                        logger.log("drag reception for scene: source is not an item but a: " + source.getClass().getName());
                    drag_event.consume();
                    return;
                }
                //logger.log("source class is:" + source.getClass().getName());
                try {
                    Item item = (Item) source;
                    Scene scene_of_source = item.getScene();

                    // data is dragged over the target
                    // accept it only if it is not dragged from the same node
                    if (scene_of_source == the_Scene) {
                        if (dbg) logger.log("drag reception for scene: same scene, giving up<<");
                        drag_event.consume();
                        return;
                    }
                } catch (ClassCastException e) {
                    logger.log(Stack_trace_getter.get_stack_trace("ERROR: " + e));
                    drag_event.consume();
                    return;
                }
            }
            if (dbg) logger.log("Ready to accept MOVE!");
            drag_event.acceptTransferModes(TransferMode.MOVE);
            drag_event.consume();
        });

        the_Scene.setOnDragDropped(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Browser: OnDragDropped handler called");
            if (dbg) logger.log("Something has been dropped in browser for dir :" + displayed_folder_path);
            int n = Drag_and_drop.accept_drag_dropped_as_a_move_in(my_Stage.the_Stage, drag_event, displayed_folder_path, the_Fucking_Pane, "browser of dir: " + displayed_folder_path, false, logger);
            set_status(n + " files have been dropped in");
            selection_handler.on_drop();
            drag_event.setDropCompleted(true);
            drag_event.consume();
        });

        the_Scene.setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Browser: OnDragExited handler called");
            if (dbg) logger.log("OnDragExited in browser for dir :" + displayed_folder_path);
            //set_status(" drag done");
            browser_menus.reset_all_files_and_folders();
            selection_handler.on_drag_exited();
            drag_event.consume();
        });
        the_Scene.setOnDragDone(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Browser: setOnDragDone handler called");
            selection_handler.on_drag_done();
            drag_event.consume();
        });

        //the_stage.setMinWidth(860);
        my_Stage.the_Stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (dbg) logger.log("new browser width =" + newValue.doubleValue());
            record_stage_bounds();
            redraw_fx("width changed by user");
        });
        my_Stage.the_Stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            record_stage_bounds();
            redraw_fx("height changed by user");
        });

        the_Scene.setOnScroll(event -> {
            double dy = event.getDeltaY();
            //logger.log("\n\n setOnScroll event: "+dy);
            scroll_a_bit(dy);
            /*long now = System.currentTimeMillis();
            if ( last_scroll_event < 0)
            {
                logger.log("start "+dy);
                scroll_a_bit(dy);
                last_scroll_event = now;
                return;
            }
            long delta_t = now-last_scroll_event;
            last_scroll_event = now;
            last_dy = dy;
            if (( delta_t > 200) ||(delta_t <=0))
            {
                logger.log("slow or too fast delta_t="+delta_t+" dy="+dy);
                scroll_a_bit(dy);
                return;
            }
            {
                double scroll_speed = Math.abs(50.0*(dy)/(double)(delta_t));
                int id = threadid_gen.incrementAndGet();
                life.clear();
                life.add(id);
                double[] dyy = {dy * scroll_speed};
                logger.log(id+" ##### dy="+dy+" #  scroll_speed: "+String.format("%.2f",scroll_speed));
                Runnable r = () -> {
                    for ( int ii = 0; ii < 100; ii++)
                    {
                        if( aborter.should_abort()) return;
                        if ( !life.contains(id))
                        {
                            logger.log(id+ " BREAK ");
                            return;
                        }
                        logger.log(id+" "+ii+" dyy[0]: "+String.format("%.3f",dyy[0]));
                        Jfx_batch_injector.now(()->scroll_a_bit(dyy[0]));
                        try
                        {
                            Thread.sleep(30);
                        }
                        catch (InterruptedException e)
                        {
                            throw new RuntimeException(e);
                        }
                        dyy[0] = dyy[0]/3.0;
                        if ( Math.abs(dyy[0]) < 1) return;
                    }
                };
                Actor_engine.execute(r,logger);
                last_scroll_speed = scroll_speed;

            }
*/
        });
    }

    ConcurrentLinkedQueue<Integer> life = new ConcurrentLinkedQueue<>();
    double last_dy;
    long last_scroll_event = -1;
    double last_scroll_speed;

    static AtomicInteger threadid_gen = new AtomicInteger(0);

    //**********************************************************
    @Override
    public boolean scroll_a_bit(double dy)
    //**********************************************************
    {
        record_scroll_to();
        return vertical_slider.scroll_relative(dy);
    }


    //**********************************************************
    void handle_scan_switch()
    //**********************************************************
    {
        switch (scan_state) {
            case off -> {
                scan_state = Scan_state.down;
                start_scan();
            }
            case down -> {
                scan_state = Scan_state.up;
                invert_scan();
            }
            case up -> {
                scan_state = Scan_state.off;
                stop_scan();
            }
        }
    }


    //**********************************************************
    @Override // Scan_show_slave
    public int how_many_rows()
    //**********************************************************
    {
        return virtual_landscape.how_many_rows();
    }

    //**********************************************************
    public void abort_backup()
    //**********************************************************
    {
        logger.log("abort backup");
        Backup_singleton.abort();
    }

    //**********************************************************
    public void start_backup()
    //**********************************************************
    {
        logger.log("start backup");
        Path backup_source = Static_backup_paths.get_backup_source();
        if (backup_source == null) {
            logger.log("FATAL, no backup_source");
            Popups.popup_warning(my_Stage.the_Stage, "Cannot backup!", "Reason: no backup ORIGIN", false, logger);
            return;
        }
        Path backup_destination = Static_backup_paths.get_backup_destination();
        if (backup_destination == null) {
            logger.log("FATAL, no backup destination");
            Popups.popup_warning(my_Stage.the_Stage, "Cannot backup!", "Reason: no backup DESTINATION", false, logger);

            return;
        }
        Backup_singleton.set_source(backup_source, logger);
        Backup_singleton.set_destination(backup_destination, logger);
        Backup_singleton.start_the_backup(my_Stage.the_Stage);

    }

    //**********************************************************
    public void abort_fusk()
    //**********************************************************
    {
        logger.log("abort fusk");
        Fusk_singleton.abort();
    }

    //**********************************************************
    public void start_fusk()
    //**********************************************************
    {
        logger.log("start fusk");
        Path fusk_source = Static_fusk_paths.get_fusk_source();
        if (fusk_source == null) {
            logger.log("FATAL, no fusk_source");
            Popups.popup_warning(my_Stage.the_Stage, "Cannot fusk!", "Reason: no fusk ORIGIN", false, logger);
            return;
        }
        Path fusk_destination = Static_fusk_paths.get_fusk_destination();
        if (fusk_destination == null) {
            logger.log("FATAL, no fusk destination");
            Popups.popup_warning(my_Stage.the_Stage, "Cannot fusk!", "Reason: no fusk DESTINATION", false, logger);

            return;
        }
        Fusk_singleton.set_source(fusk_source, aborter, logger);
        Fusk_singleton.set_destination(fusk_destination, aborter, logger);
        Fusk_singleton.start_fusk(my_Stage.the_Stage);

    }

    //**********************************************************
    public void start_defusk()
    //**********************************************************
    {
        logger.log("start defusk");
        Path defusk_source = Static_fusk_paths.get_fusk_source();
        if (defusk_source == null) {
            logger.log("FATAL, no defusk source");
            Popups.popup_warning(my_Stage.the_Stage, "Cannot defusk!", "Reason: no defusk SOURCE", false, logger);
            return;
        }
        Path defusk_destination = Static_fusk_paths.get_fusk_destination();
        if (defusk_destination == null) {
            logger.log("FATAL, no defusk destination");
            Popups.popup_warning(my_Stage.the_Stage, "Cannot defusk!", "Reason: no defusk DESTINATION", false, logger);

            return;
        }
        Fusk_singleton.set_source(defusk_source, aborter, logger);
        Fusk_singleton.set_destination(defusk_destination, aborter, logger);
        Fusk_singleton.start_defusk(my_Stage.the_Stage);

    }


    //**********************************************************
    public void you_are_backup_destination()
    //**********************************************************
    {
        Static_backup_paths.set_backup_destination(displayed_folder_path);
        logger.log("backup destination = " + displayed_folder_path.toAbsolutePath());

        set_text_background("BACKUP\nDESTINATION");

    }

    //**********************************************************
    public void you_are_backup_source()
    //**********************************************************
    {
        Static_backup_paths.set_backup_source(displayed_folder_path);
        logger.log("backup source = " + displayed_folder_path.toAbsolutePath());

        set_text_background("BACKUP\nSOURCE");

    }

    //**********************************************************
    public void you_are_fusk_destination()
    //**********************************************************
    {
        Static_fusk_paths.set_fusk_destination(displayed_folder_path);
        logger.log("fusk destination = " + displayed_folder_path.toAbsolutePath());

        set_text_background("FUSK\nDESTINATION");

    }

    //**********************************************************
    public void you_are_fusk_source()
    //**********************************************************
    {
        Static_fusk_paths.set_fusk_source(displayed_folder_path);
        logger.log("fusk source = " + displayed_folder_path.toAbsolutePath());

        set_text_background("FUSK\nSOURCE");

    }


    //**********************************************************
    public void enter_fusk_pin_code()
    //**********************************************************
    {
        //Pin_code_getter_stage pin_code_getter_stage = Pin_code_getter_stage.ask_pin_code_in_a_thread(logger);

        if (Fusk_bytes.is_initialized()) {
            Fusk_bytes.reset(logger);
        }
        Fusk_bytes.initialize(aborter, logger);
    }


    //**********************************************************
    private void set_text_background(String text)
    //**********************************************************
    {
        // writes in the browser background a HUGE (70!) text
        Text t = new Text(text);
        t.setStyle("-fx-font: 70 arial;");
        Scene scene = new Scene(new VBox(t));
        WritableImage wi = scene.snapshot(null);
        Paint ppp = new ImagePattern(wi);
        the_Fucking_Pane.setBackground(new Background(new BackgroundFill(ppp, CornerRadii.EMPTY, Insets.EMPTY)));
    }


    //**********************************************************
    public void set_title()
    //**********************************************************
    {
        if (displayed_folder_path == null) return;
        my_Stage.the_Stage.setTitle(displayed_folder_path.toAbsolutePath().toString());// fast temporary
        Runnable r = new Runnable() {
            @Override
            public void run() {
                //listFiles can be super slow on network drives or slow drives  (e.g. USB)  ==> run in a thread

                File[] x = displayed_folder_path.toFile().listFiles();
                if (x == null) return;
                long how_many_files = x.length;
                if (!Static_application_properties.get_show_hidden_files(logger)) {
                    for (File f : x) {
                        if (Guess_file_type.is_this_path_invisible_when_browsing(f.toPath())) {
                            how_many_files--;
                        }
                    }
                }
                long finalHow_many_files = how_many_files;
                Jfx_batch_injector.inject(() -> my_Stage.the_Stage.setTitle(displayed_folder_path.toAbsolutePath() + " :     " + finalHow_many_files + " files & folders"), logger);

            }
        };
        Actor_engine.execute(r, logger);


    }

    //**********************************************************
    public void increase_icon_size()
    //**********************************************************
    {
        change_icon_size(1.1);
    }

    //**********************************************************
    public void reduce_icon_size()
    //**********************************************************
    {
        change_icon_size(0.9);
    }

    //**********************************************************
    private void change_icon_size(double fac)
    //**********************************************************
    {
        int new_icon_size = (int) (Static_application_properties.get_icon_size(logger) * fac);
        if (new_icon_size < 20) new_icon_size = 20;
        if ( keyboard_dbg) logger.log("new icon size = "+new_icon_size);
        Static_application_properties.set_icon_size(new_icon_size, logger);
        //icon_manager.modify_button_fonts(fac);
        redraw_fx("new icon size "+new_icon_size);
    }



    //**********************************************************
    public void redraw_fx(String from)
    //**********************************************************
    {
        //if (dbg)
        logger.log("Browser redraw from:" + from );
        try {
            virtual_landscape.request_queue.put(Boolean.TRUE);
        } catch (InterruptedException e) {
            logger.log(""+e);
        }
    }



    //**********************************************************
    public void apply_font()
    //**********************************************************
    {
        if (dbg) logger.log("applying font size " + Static_application_properties.get_font_size(logger));
        for (Node x : always_on_front_nodes) {
            Font_size.apply_font_size(x, logger);
        }
        for (Node x : browser_ui.top_buttons) {
            Font_size.apply_font_size(x, logger);
        }
    }


    //**********************************************************
    public void sort_by_year()
    //**********************************************************
    {
        Runnable r = () -> sort_by_year_internal();
        Actor_engine.execute(r, logger);
    }

    //**********************************************************
    public void sort_by_year_internal()
    //**********************************************************
    {
        File[] files = displayed_folder_path.toFile().listFiles();
        if (files == null) {
            logger.log("ERROR: cannot list files in " + displayed_folder_path);
        }
        if (files.length == 0) {
            logger.log("WARNING: no file in " + displayed_folder_path);
        }
        Map<Integer, Path> folders = new HashMap<>();
        List<Old_and_new_Path> moves = new ArrayList<>();
        for (File f : files) {
            BasicFileAttributes x = null;
            try {
                x = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
            } catch (IOException e) {
                logger.log("" + e);
                continue;
            }

            FileTime ft = x.creationTime();
            LocalDateTime ldt = ft.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            int year = ldt.getYear();
            Path folder = folders.get(year);
            if (folder == null) {
                folder = Path.of(displayed_folder_path.toAbsolutePath().toString(), String.valueOf(year));
                try {
                    Files.createDirectory(folder);
                } catch (IOException e) {
                    logger.log("" + e);
                    continue;
                }
            }
            folders.put(year, folder);
            List<Old_and_new_Path> l = new ArrayList<>();
            l.add(new Old_and_new_Path(displayed_folder_path, displayed_folder_path, Command_old_and_new_Path.command_unknown, Status_old_and_new_Path.move_done, false));
            Change_gang.report_changes(l);

            Old_and_new_Path oanp = new Old_and_new_Path(
                    f.toPath(),
                    Path.of(folder.toAbsolutePath().toString(), f.getName()),
                    Command_old_and_new_Path.command_move,
                    Status_old_and_new_Path.before_command, false);
            moves.add(oanp);

        }
        Moving_files.perform_safe_moves_in_a_thread(this.my_Stage.the_Stage, moves, true, aborter, logger);

    }

    //**********************************************************
    public void create_PDF_contact_sheet()
    //**********************************************************
    {
        Runnable r = this::create_PDF_contact_sheet_in_a_thread;
        Actor_engine.execute(r,logger);
    }
    //**********************************************************
    public void create_PDF_contact_sheet_in_a_thread()
    //**********************************************************
    {
        Hourglass x = Show_running_man_frame.show_running_man("Making PDF contact sheet",
        20_000,new Aborter("contact sheet",logger),logger);
        List<String> graphicsMagick_command_line = new ArrayList<>();

        boolean formula1 = false;
        if ( formula1)
        {
            graphicsMagick_command_line.add("gm");
            graphicsMagick_command_line.add("convert");
            graphicsMagick_command_line.add("vid:*.jpg");
            graphicsMagick_command_line.add("contact_sheet.pdf");

        }
        else {
            graphicsMagick_command_line.add("gm");
            graphicsMagick_command_line.add("montage");
            graphicsMagick_command_line.add("-label");
            graphicsMagick_command_line.add("'%f'");
            graphicsMagick_command_line.add("-font");
            graphicsMagick_command_line.add("Helvetica");
            graphicsMagick_command_line.add("-pointsize");
            graphicsMagick_command_line.add("10");
            graphicsMagick_command_line.add("-background");
            graphicsMagick_command_line.add("#000000");
            graphicsMagick_command_line.add("-fill");
            graphicsMagick_command_line.add("#ffffff");
            graphicsMagick_command_line.add("-define");
            graphicsMagick_command_line.add("jpeg:size=300x200");
            graphicsMagick_command_line.add("-geometry");
            graphicsMagick_command_line.add("300x200+2+2");
            graphicsMagick_command_line.add("*.jpg");
            graphicsMagick_command_line.add("contact_sheet.pdf");
        }


        StringBuilder sb = null;
        //if ( dbg)
            sb = new StringBuilder();
        if ( !Execute_command.execute_command_list(graphicsMagick_command_line, displayed_folder_path.toFile(), 2000, sb, logger))
        {

            Static_application_properties.manage_show_GraphicsMagick_install_warning(my_Stage.the_Stage,logger);

            Popups.popup_warning(my_Stage.the_Stage, "Contact sheet generation command failed:", warning_GraphicsMagick,false,logger);
        }
        else
        {
            logger.log("contact sheet generated "+ sb);
        }
        x.close();
    }


    //**********************************************************
    public void create_new_directory()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(My_I18n.get_I18n_string("New_directory", logger));
        Look_and_feel_manager.set_dialog_look(dialog);
        dialog.initOwner(my_Stage.the_Stage);
        dialog.setWidth(my_Stage.the_Stage.getWidth());
        dialog.setTitle(My_I18n.get_I18n_string("New_directory", logger));
        dialog.setHeaderText(My_I18n.get_I18n_string("Enter_name_of_new_directory", logger));
        dialog.setContentText(My_I18n.get_I18n_string("New_directory_name", logger));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();

            for (int i = 0; i < 10; i++) {
                try {
                    Path new_dir = displayed_folder_path.resolve(new_name);
                    Files.createDirectory(new_dir);
                    scroll_position_cache.put(displayed_folder_path, new_dir);
                    redraw_fx("created new empty dir");
                    break;
                } catch (IOException e) {
                    logger.log("new directory creation FAILED: " + e);
                    new_name += "_";
                }
            }

        }
    }


    //**********************************************************
    @Override
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
                            scroll_position_cache.put(displayed_folder_path, oan.new_Path);
                        }
                    }
                }
                redraw_fx("change gang for dir: " + displayed_folder_path);
            }
            ;
            break;
            case one_new_file, one_file_gone: {
                //if (dbg)
                    logger.log("2 Browser of: " + displayed_folder_path + " RECOGNIZED change gang notification: " + l);
                redraw_fx("change gang for dir: " + displayed_folder_path);
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


    //**********************************************************
    public List<File> get_file_list()
    //**********************************************************
    {
        return virtual_landscape.get_file_list();
    }

    //**********************************************************
    public List<File> get_folder_list()
    //**********************************************************
    {
        return virtual_landscape.get_folder_list();
    }


    //**********************************************************
    @Override // Selection reporter
    public void report(String s)
    //**********************************************************
    {
        set_status(s);
    }


















    //**********************************************************
    private void record_scroll_to()
    //**********************************************************
    {
        Path tl = get_top_left();
        //logger.log("record_scroll_to " + displayed_folder_path + " => " + tl);
        scroll_position_cache.put(displayed_folder_path, tl);
    }

    //**********************************************************
    public Path get_scroll_to()
    //**********************************************************
    {
        Path scroll_to = scroll_position_cache.get(displayed_folder_path);
        if (scroll_to == null) {
            //if (dbg)
            logger.log((" scroll_to == null "));
        }
        return scroll_to;
    }




    /*
    Scan show
     */



    Scan_show the_scan_show;

    private static final String msg = "(press \"s\" to start/stop/change direction, \"x\"=faster, \"w\"=slower) speed = ";

    //**********************************************************
    private void start_scan()
    //**********************************************************
    {
        the_scan_show = new Scan_show(this, vertical_slider, aborter, logger);
        set_status("Scan show starting ! " + msg + the_scan_show.get_speed());
    }

    //**********************************************************
    public void stop_scan()
    //**********************************************************
    {
        if (the_scan_show == null) return;
        the_scan_show.stop_the_show();
        the_scan_show = null;
        set_status("Scan show stopped " + msg);

    }

    //**********************************************************
    public void invert_scan()
    //**********************************************************
    {
        if (the_scan_show == null) start_scan();
        else the_scan_show.invert_scan_direction();
    }

    //**********************************************************
    public void slow_down_scan()
    //**********************************************************
    {
        if (the_scan_show == null) {
            start_scan();
            return;
        }
        the_scan_show.slow_down();
        set_status("Scan show running " + msg + the_scan_show.get_speed());

    }

    //**********************************************************
    public void speed_up_scan()
    //**********************************************************
    {
        if (the_scan_show == null) {
            start_scan();
            return;
        }
        the_scan_show.hurry_up();
        set_status("Scan show running " + msg + the_scan_show.get_speed());

    }


}
