package klik.browser;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.WindowEvent;
import klik.actor.Aborter;
import klik.backup.Backup_singleton;
import klik.browser.icons.Error_type;
import klik.browser.icons.Icon_manager;
import klik.browser.items.Item;
import klik.change.Change_gang;
import klik.change.Change_receiver;
import klik.experimental.JavaFX_to_Swing;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Filesystem_item_modification_watcher;
import klik.files_and_paths.Guess_file_type;
import klik.files_and_paths.Old_and_new_Path;
import klik.fusk.Fusk_singleton;
import klik.fusk.Static_fusk_paths;
import klik.look.Font_size;
import klik.look.Look_and_feel_manager;
import klik.my_i18n.I18n;
import klik.properties.History;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.awt.Taskbar.Feature.ICON_IMAGE;


//**********************************************************
public class Browser implements Change_receiver, Scan_show_slave, Selection_reporter
//**********************************************************
{

    static AtomicInteger windows_count = new AtomicInteger(0);
    private static AtomicInteger ID_generator = new AtomicInteger(1000);
    private final int ID;

    public static final boolean dbg = false;
    private static final int FOLDER_MONITORING_TIMEOUT_IN_MINUTES = 600;
    public final Path top_left_in_parent;
//    public double button_width;


    List<Node> mandatory_in_pane = new ArrayList<>();
    List<Node> always_on_front_nodes = new ArrayList<>();

    Filesystem_item_modification_watcher filesystem_item_modification_watcher;
    public final My_Stage my_Stage;
    public Scene the_Scene;
    final Pane the_Pane;
    final Icon_manager icon_manager;
    final Logger logger;
    final Path displayed_folder_path;
    public final Selection_handler selection_handler;
    public final Aborter aborter;
    TextField status;
    Vertical_slider vertical_slider;
    public double slider_width = 400;

    boolean exit_on_escape_preference;
    boolean ignore_escape_as_the_stage_is_full_screen = false;

    final Browser_menus browser_menus;
    public final Browser_UI browser_ui;
    public Error_type error_type = Error_type.OK;

    //static boolean was_escaped = false;
    static Path home = Paths.get(System.getProperty(Static_application_properties.USER_HOME));


    enum Scan_state {
        off,
        down,
        up
    }

    private Scan_state scan_state = Scan_state.off;

    //**********************************************************
    public Browser(Browser_creation_context context, Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        aborter = new Aborter();
        ID = ID_generator.getAndIncrement();
        my_Stage = context.stage;//new My_Stage(context.stage,logger);

        if (context.additional_window)
        {
            windows_count.incrementAndGet();
            //logger.log(Stack_trace_getter.get_stack_trace("\n\n\nBrowser after create: " +context.folder_path +"\n"+ signature()));
        } else {
            //logger.log(Stack_trace_getter.get_stack_trace("\n\n\nBrowser after dir change: " +context.folder_path +"\n"+ signature()));
        }
        top_left_in_parent = context.top_left_in_parent;
        //logger.log("top_left_in_parent="+top_left_in_parent);

        double x = 0;
        double y = 0;

        double width = 2400 / 2.0;
        double height = 1080 - y;
        if (context.rectangle != null) {
            width = context.rectangle.getWidth();//old_browser.the_Stage.getWidth();
            height = context.rectangle.getHeight();//old_browser.the_Stage.getHeight();
            x = context.rectangle.getMinX();//old_browser.the_Stage.getX();
            y = context.rectangle.getMinY();//old_browser.the_Stage.getY();
            if (context.old_browser != null) {
                context.old_browser.cleanup();
            }
        }
        if (!context.same_place) {
            x += 100;
            y += 100;
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
        my_Stage.the_Stage.show();
        my_Stage.the_Stage.setX(x);
        my_Stage.the_Stage.setY(y);
        my_Stage.the_Stage.setWidth(width);
        my_Stage.the_Stage.setHeight(height);

        set_icon();
        // RELOAD a fresh history (e.g. if a drive was re-inserted) and record this in history
        History.get_History_instance(logger).add(context.folder_path);

        displayed_folder_path = context.folder_path;

        Change_gang.register(this, logger);
        set_title();
        the_Pane = new Pane();


        icon_manager = new Icon_manager(my_Stage.the_Stage, logger);
        selection_handler = new Selection_handler(the_Pane, icon_manager, this, logger);
        browser_menus = new Browser_menus(this, selection_handler, logger_);
        exit_on_escape_preference = Static_application_properties.get_escape(logger);
        {
            browser_ui = new Browser_UI(this);
            browser_ui.define_UI();
        }
        icon_manager.set_Landscape_height_listener(vertical_slider);
        set_all_event_handlers();

        my_Stage.the_Stage.setScene(the_Scene);

        Platform.runLater(() -> {

            scene_geometry_changed("Browser constructor", true, false);
            if (context.scroll_to != null) {
                if (dbg) logger.log("got a scroll_to : " + context.scroll_to);
                double y1 = icon_manager.get_y_offset_of(context.scroll_to);
                vertical_slider.transform_pixel_value(y1, icon_manager);

            } else {
                if (dbg) logger.log((" scroll_to == null in context"));
            }
        });
    }

    private void set_icon() {
        my_Stage.the_Stage.getIcons().clear();
        Image taskbar_icon = null;
        int[] icon_sizes = {16, 32, 64, 128};
        for (int s : icon_sizes) {
            Image icon = Look_and_feel_manager.load_icon_fx_from_jar(Objects.requireNonNull(Look_and_feel_manager.get_instance()).get_klik_image_path(), s);
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
        //logger.log("cleanup " + signature());
        // when we change dir, we need to de-register the old browser
        // otherwise the list in the change_gang keeps growing
        // plus memory leak! ==> the RAM footprint keeps growing
        aborter.abort();
        Change_gang.deregister(this);
        if (filesystem_item_modification_watcher != null) filesystem_item_modification_watcher.cancel();
        stop_scan();
        the_Pane.getChildren().clear();
        if (icon_manager != null) icon_manager.cancel_all();
    }

    //**********************************************************
    void close_window()
    //**********************************************************
    {
        cleanup();
        logger.log("close_window BEFORE close" + signature());
        my_Stage.close();
        windows_count.decrementAndGet();
        logger.log("close_window AFTER close" + signature());
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
    public void show_how_many_files_in_each_folder()
    //**********************************************************
    {
        icon_manager.show_how_many_files_in_each_folder();


    }

    //**********************************************************
    public void update_slider(double pixels)
    //**********************************************************
    {
        vertical_slider.transform_pixel_value(pixels, icon_manager);
    }

    //**********************************************************
    public Path get_top_left()
    //**********************************************************
    {
        return icon_manager.get_top_left(the_Pane);
    }

    public Rectangle2D get_rectangle() {
        return new Rectangle2D(my_Stage.the_Stage.getX(), my_Stage.the_Stage.getY(), my_Stage.the_Stage.getWidth(), my_Stage.the_Stage.getHeight());
    }

    //**********************************************************
    private void set_all_event_handlers()
    //**********************************************************
    {
        if (displayed_folder_path.toAbsolutePath().toString().equals("/Volumes")) {
            // NON PORTABLE
            // this is a trick to refresh the view in case an external storage is inserted/removed
            // but watching "/Volumes" works only in MACOS afaik

            filesystem_item_modification_watcher = Filesystem_item_modification_watcher.monitor_folder(displayed_folder_path, FOLDER_MONITORING_TIMEOUT_IN_MINUTES, logger);
            if (filesystem_item_modification_watcher == null) {
                logger.log("WARNING: cannot monitor folder /Volume");
            }
        } else if (Static_application_properties.get_monitor_browsed_folders(logger)) {
            filesystem_item_modification_watcher = Filesystem_item_modification_watcher.monitor_folder(displayed_folder_path, FOLDER_MONITORING_TIMEOUT_IN_MINUTES, logger);
            if (filesystem_item_modification_watcher == null) {
                logger.log("WARNING: cannot monitor folder " + displayed_folder_path);
            }
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
        Look_and_feel_manager.set_pane_look(the_Pane);
        {
            the_Pane.addEventHandler(MouseEvent.MOUSE_PRESSED, selection_handler::handle_mouse_pressed);
            the_Pane.addEventHandler(MouseEvent.MOUSE_DRAGGED, selection_handler::handle_mouse_dragged);
            the_Pane.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseEvent -> selection_handler.handle_mouse_released(mouseEvent, this_browser));
        }

        EventHandler<WindowEvent> on_close_event_handler = new External_close_event_handler(this);
        my_Stage.the_Stage.setOnCloseRequest(on_close_event_handler);
        my_Stage.set_escape_event_handler(this);


        Browser local = this;
        the_Scene.setOnKeyTyped(keyEvent -> {

            logger.log(keyEvent + "getCharacter->" + keyEvent.getCharacter() + "<- getCode:" + keyEvent.getCode());
            if (keyEvent.isShiftDown()) logger.log("isShiftDown: true");
            if (keyEvent.isAltDown()) logger.log("isAltDown: true");
            if (keyEvent.isMetaDown()) logger.log("isMetaDown: true");

            if (keyEvent.getCharacter().equals("s")) {
                logger.log("character is s = start/stop scan");

                handle_scan_switch();
            }
            if (keyEvent.getCharacter().equals("w")) {
                logger.log("character is w = slow down scan");
                slow_down_scan();
            }
            if (keyEvent.getCharacter().equals("x")) {
                logger.log("character is x = speed up scan");
                speed_up_scan();
            }

            if (keyEvent.isMetaDown()) {
                if (keyEvent.getCharacter().equals("a")) {
                    logger.log("character is a + meta = select all");
                    selection_handler.select_all_files_in_folder(local);
                }

                if (keyEvent.getCharacter().equals("+")) {
                    logger.log("character is + + meta = zoom +");
                    zoom_plus();
                }
                if (keyEvent.getCharacter().equals("=")) {
                    logger.log("character is = + meta = zoom +");
                    zoom_plus();
                }
                if (keyEvent.getCharacter().equals("-")) {
                    logger.log("character is - + meta = zoom -");
                    zoom_minus();
                }
            }
            if (keyEvent.getCharacter().equals("n")) {
                logger.log("character is n = new broser (clone)");
                Browser_creation_context.additional_same_folder(local, logger);
            }
        });

        the_Scene.setOnDragOver(drag_event -> {
            if (Drag_and_drop.dbg_drag_and_drop) logger.log("Browser: OnDragOver handler called");
            selection_handler.on_drag_over();
            Object source = drag_event.getGestureSource();
            if (source == null) {
                //logger.log("source class is null " + event.toString());
            } else {
                if ( ! (source instanceof Item))
                {
                    if (dbg) logger.log("drag reception for scene: source is not an item but a: "+source.getClass().getName());
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
            if (Drag_and_drop.dbg_drag_and_drop) logger.log("Browser: OnDragDropped handler called");
            if (dbg) logger.log("Something has been dropped in browser for dir :" + displayed_folder_path);
            int n = Drag_and_drop.accept_drag_dropped_as_a_move_in(my_Stage.the_Stage, drag_event, displayed_folder_path, the_Pane, "browser of dir: " + displayed_folder_path, logger);
            set_status(n + " files have been dropped in");
            selection_handler.on_drop();
            drag_event.setDropCompleted(true);
            drag_event.consume();
        });

        the_Scene.setOnDragExited(drag_event -> {
            if (Drag_and_drop.dbg_drag_and_drop) logger.log("Browser: OnDragExited handler called");
            if (dbg) logger.log("OnDragExited in browser for dir :" + displayed_folder_path);
            //set_status(" drag done");
            browser_menus.reset_all_files_and_folders();
            selection_handler.on_drag_exited();
            drag_event.consume();
        });
        the_Scene.setOnDragDone(drag_event -> {
            if (Drag_and_drop.dbg_drag_and_drop) logger.log("Browser: setOnDragDone handler called");
            selection_handler.on_drag_done();
            drag_event.consume();
        });

        //the_stage.setMinWidth(860);
        my_Stage.the_Stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (dbg) logger.log("new browser width =" + newValue.doubleValue());
            scene_geometry_changed("width changed by user", false, false);
        });
        my_Stage.the_Stage.heightProperty().

                addListener((observable, oldValue, newValue) -> scene_geometry_changed("height changed by user", false, false));

        the_Scene.setOnScroll(event -> {
            double dy = event.getDeltaY();
            vertical_slider.scroll(dy);
        });
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
        return icon_manager.how_many_rows();
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
        Path backup_sink = Static_backup_paths.get_backup_sink();
        if (backup_sink == null) {
            logger.log("FATAL, no backup_sink");
            Popups.popup_warning(my_Stage.the_Stage, "Cannot backup!", "Reason: no backup DESTINATION", false, logger);

            return;
        }
        Backup_singleton.set_source(backup_source, logger);
        Backup_singleton.set_sink(backup_sink, logger);
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
        Path fusk_sink = Static_fusk_paths.get_fusk_sink();
        if (fusk_sink == null) {
            logger.log("FATAL, no fusk_sink");
            Popups.popup_warning(my_Stage.the_Stage, "Cannot fusk!", "Reason: no fusk DESTINATION", false, logger);

            return;
        }
        Fusk_singleton.set_source(fusk_source, logger);
        Fusk_singleton.set_sink(fusk_sink, logger);
        Fusk_singleton.start_fusk(my_Stage.the_Stage);

    }

    //**********************************************************
    public void start_defusk()
    //**********************************************************
    {
        logger.log("start defusk");
        Path defusk_source = Static_fusk_paths.get_fusk_source();
        if (defusk_source == null) {
            logger.log("FATAL, no defusk_source");
            Popups.popup_warning(my_Stage.the_Stage, "Cannot defusk!", "Reason: no defusk_source ORIGIN", false, logger);
            return;
        }
        Path defusk_sink = Static_fusk_paths.get_fusk_sink();
        if (defusk_sink == null) {
            logger.log("FATAL, no defusk_sink");
            Popups.popup_warning(my_Stage.the_Stage, "Cannot defusk!", "Reason: no defusk_sink DESTINATION", false, logger);

            return;
        }
        Fusk_singleton.set_source(defusk_source, logger);
        Fusk_singleton.set_sink(defusk_sink, logger);
        Fusk_singleton.start_defusk(my_Stage.the_Stage);

    }


    //**********************************************************
    public void you_are_backup_sink()
    //**********************************************************
    {
        Static_backup_paths.set_backup_sink(displayed_folder_path);
        logger.log("backup sink = " + displayed_folder_path.toAbsolutePath());

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
    public void you_are_fusk_sink()
    //**********************************************************
    {
        Static_fusk_paths.set_fusk_sink(displayed_folder_path);
        logger.log("fusk sink = " + displayed_folder_path.toAbsolutePath());

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
    private void set_text_background(String text)
    //**********************************************************
    {
        // writes in the browser background a HUGE (70!) text
        Text t = new Text(text);
        t.setStyle("-fx-font: 70 arial;");
        Scene scene = new Scene(new VBox(t));
        WritableImage wi = scene.snapshot(null);
        Paint ppp = new ImagePattern(wi);
        the_Pane.setBackground(new Background(new BackgroundFill(ppp, CornerRadii.EMPTY, Insets.EMPTY)));
    }


    //**********************************************************
    private void set_title()
    //**********************************************************
    {
        if (displayed_folder_path == null) return;
        //if (displayed_folder_path.toFile() == null) return;
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
        my_Stage.the_Stage.setTitle(displayed_folder_path.toAbsolutePath() + " :     " + how_many_files + " files & folders");


    }

    //**********************************************************
    private void zoom_plus() {
        zoom(1.1);
    }
//**********************************************************

    //**********************************************************
    private void zoom_minus() {
        zoom(0.9);
    }
//**********************************************************

    //**********************************************************
    private void zoom(double fac)
    //**********************************************************
    {
        Static_application_properties.set_icon_size((int) (Static_application_properties.get_icon_size(logger) * fac), logger);
        icon_manager.modify_button_fonts(fac);
        scene_geometry_changed("zoom", false, true);
    }


    //**********************************************************
    public void scene_geometry_changed(String from, boolean rebuild_all_items, boolean keep_scroll)
    //**********************************************************
    {
        if (dbg)
            logger.log("the_pane scene_geometry_changed " + the_Pane.getBoundsInLocal().toString() + " from:" + from);

        error_type = icon_manager.paths_manager.scan_dir(displayed_folder_path, my_Stage.the_Stage);
        if (error_type != Error_type.OK) {
            logger.log(true, true, "scene_geometry_changed() scan dir failed for :" + displayed_folder_path + " error=" + status);
        }
        icon_manager.geometry_changed(this, the_Pane, mandatory_in_pane,
                "scene_geometry_changed from: " + from + " keep_scroll=" + keep_scroll,
                //error_type,
                rebuild_all_items);

        if (dbg) logger.log("the_pane scene_geometry_changed adapt_slider_to_scene");
        if (!keep_scroll) {
            vertical_slider.adapt_slider_to_scene(the_Scene, the_Pane);
        }
        set_title();
        {
            double title_height = my_Stage.the_Stage.getHeight() - the_Scene.getHeight();
            for (Button b : browser_ui.top_buttons)
            {
                b.setMinHeight(title_height);
            }
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
    public void create_new_directory()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(I18n.get_I18n_string("New_directory", logger));
        dialog.initOwner(my_Stage.the_Stage);
        dialog.setWidth(my_Stage.the_Stage.getWidth());
        dialog.setTitle(I18n.get_I18n_string("New_directory", logger));
        dialog.setHeaderText(I18n.get_I18n_string("Enter_name_of_new_directory", logger));
        dialog.setContentText(I18n.get_I18n_string("New_directory_name", logger));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();

            for (; ; ) {
                try {
                    Path new_dir = displayed_folder_path.resolve(new_name);
                    Files.createDirectory(new_dir);
                    scene_geometry_changed("created new empty dir", true, false);
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
        if (Change_gang.is_my_directory_impacted(displayed_folder_path, l, logger)) {
            if (dbg)
                logger.log("Browser of: " + displayed_folder_path + " RECOGNIZED change gang notification: " + l);

            // can be called from a thread which is NOT the FX event thread

            Platform.runLater(() -> {
                scene_geometry_changed("change gang for dir: " + displayed_folder_path, true, true);
                //get_a_browser(this_local,the_stage,dir,true,logger);
            });
        } else {
            if (dbg) {
                logger.log("Browser of: " + displayed_folder_path + " IGNORED change gang notification: ");
                for (Old_and_new_Path x : l) {
                    logger.log(x.old_Path + " => " + x.new_Path);
                }
            }

        }
    }


    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        return "Browser_scene:" + displayed_folder_path.toAbsolutePath() + " " + ID;
    }


    //**********************************************************
    public List<File> get_file_list()
    //**********************************************************
    {
        return icon_manager.paths_manager.get_file_list();
    }

    //**********************************************************
    public List<File> get_folder_list()
    //**********************************************************
    {
        return icon_manager.paths_manager.get_folder_list();
    }


    //**********************************************************
    @Override
    public boolean scroll_a_bit(double dy)
    //**********************************************************
    {
        return vertical_slider.scroll(dy);
    }

    Scan_show the_scan_show;

    //**********************************************************
    private void start_scan()
    //**********************************************************
    {
        the_scan_show = new Scan_show(this, vertical_slider, logger);
    }

    //**********************************************************
    public void stop_scan()
    //**********************************************************
    {
        if (the_scan_show == null) return;
        the_scan_show.stop_the_show();
        the_scan_show = null;

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
    }





    //**********************************************************
    @Override // Selection reporter
    public void report(String s)
    //**********************************************************
    {
        set_status(s);
    }

}