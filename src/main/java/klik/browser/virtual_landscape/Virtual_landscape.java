//SOURCES ./Landscape_height_listener.java
//SOURCES ./Scroll_to_listener.java
//SOURCES ../icons/image_properties_cache/Image_properties.java
//SOURCES ../../look/Look_and_feel.java
//SOURCES ../items/Item_folder_with_icon.java
//SOURCES ../items/My_colors.java
//SOURCES ../classic/Folder_path_list_provider.java
//SOURCES ../ram_and_threads_meter/RAM_and_threads_meters_stage.java
//SOURCES ../../experimental/deduplicate/Deduplication_engine.java
//SOURCES ../../image_ml/image_similarity/Deduplication_by_similarity_engine.java
//SOURCES ../items/Top_left_provider.java
//SOURCES ./Path_comparator_source.java
//SOURCES ../../properties/boolean_features/String_change_target.java


package klik.browser.virtual_landscape;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.New_window_context;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.*;
import klik.browser.classic.Browser;
import klik.browser.classic.Folder_path_list_provider;
import klik.browser.comparators.*;
import klik.browser.icons.Error_type;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.items.*;
import klik.browser.locator.Folders_with_large_images_locator;
import klik.browser.ram_and_threads_meter.RAM_and_threads_meters_stage;
import klik.change.Change_gang;
import klik.change.Change_receiver;
import klik.experimental.backup.Backup_singleton;
import klik.experimental.deduplicate.Deduplication_engine;
import klik.experimental.fusk.Fusk_bytes;
import klik.experimental.fusk.Fusk_singleton;
import klik.experimental.fusk.Static_fusk_paths;
import klik.experimental.metadata.Tag_items_management_stage;
import klik.image_ml.image_similarity.Deduplication_by_similarity_engine;
import klik.image_ml.image_similarity.Image_feature_vector_cache;
import klik.images.Image_context;
import klik.look.Font_size;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.*;
import klik.properties.boolean_features.*;
import klik.util.execute.Execute_command;
import klik.util.execute.System_open_actor;
import klik.util.files_and_paths.*;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;



//**********************************************************
public class Virtual_landscape implements Scan_show_slave, Selection_reporter, Top_left_provider, Path_comparator_source, Feature_change_target, String_change_target
//**********************************************************
{


    public static final boolean dbg = false;
    public static final boolean ultra_dbg = false;
    public static final boolean invisible_dbg = false;
    public static final boolean visible_dbg = false;
    public static final boolean scroll_dbg = false;

    public static final int MIN_PARENT_AND_TRASH_BUTTON_WIDTH = 200;
    public static final int MIN_COLUMN_WIDTH = 300;
    public static final double RIGHT_SIDE_SINGLE_COLUMN_MARGIN = 100;
    private static final double MARGIN_Y = 50;
    public static final String CONTACT_SHEET_FILE_NAME = "contact_sheet.pdf";




    public final Aborter aborter;
    public final Logger logger;
    private Landscape_height_listener landscape_height_listener;
    private Scroll_to_listener scroll_to_listener;
    private final Paths_holder paths_holder;


    // otherwise there are 2 sorted lists
    public Comparator<Path> image_file_comparator;
    public Comparator<Path> other_file_comparator;

    public Icon_factory_actor icon_factory_actor;


    public ConcurrentLinkedQueue<List<Path>> iconized_sorted_queue = new ConcurrentLinkedQueue<>();
    public final BlockingQueue<Boolean> request_queue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<Path, Item> all_items_map = new ConcurrentHashMap<>();
    private final AtomicBoolean items_are_ready = new AtomicBoolean(false);

    private double virtual_landscape_height = -Double.MAX_VALUE;
    private double current_vertical_offset = 0;
    private int how_many_rows;
    private Path top_left;
    public final double icon_height;

    boolean show_how_many_files_deep_in_each_folder_done = false;
    boolean show_total_size_deep_in_each_folder_done = false;
    final Window owner;
    public Error_type error_type;

    final Path_list_provider path_list_provider;

    Map<Path,Long> folder_total_sizes_cache;
    Map<Path,Long> folder_file_count_cache;

    private final List<Item> future_pane_content = new ArrayList<>();
    public Vertical_slider vertical_slider;
    public double slider_width;
    public final Scene the_Scene;
    public final Pane the_Pane;
    public final Selection_handler selection_handler;
    public final Virtual_landscape_menus browser_menus;
    MenuItem stop_full_screen_menu_item;
    MenuItem start_full_screen_menu_item;
    public List<Button> top_buttons = new ArrayList<>();

    TextField status;
    public final Shutdown_target shutdown_target;
    private final Title_target title_target;
    private final Full_screen_handler full_screen_handler;

    public static boolean show_running_film = true;

    public final Browsing_caches browsing_caches;
    private Image_feature_vector_cache fv_cache;

    //**********************************************************
    public Virtual_landscape(
            Path_list_provider path_list_provider,
            Window owner,
            Shutdown_target shutdown_target,
            Change_receiver change_receiver,
            Title_target title_target,
            Full_screen_handler full_screen_handler,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        this.full_screen_handler = full_screen_handler;
        this.title_target = title_target;
        this.shutdown_target = shutdown_target;
        this.path_list_provider = path_list_provider;
        error_type = Error_type.OK;
        this.owner = owner;
        this.aborter = aborter;
        this.logger = logger;

        Feature_cache.register_for_all(this);
        Feature_cache.string_register_for(Non_booleans.LANGUAGE_KEY,this);
        Feature_cache.string_register_for(Non_booleans.STYLE_KEY,this);

        the_Pane = new Pane();


        browsing_caches = new Browsing_caches(path_list_provider,owner,aborter,logger);
        icon_factory_actor = new Icon_factory_actor(browsing_caches.image_properties_RAM_cache, owner, aborter, logger);
        paths_holder = new Paths_holder(icon_factory_actor, browsing_caches.image_properties_RAM_cache, aborter, logger);
        selection_handler = new Selection_handler(the_Pane, this, this, logger);

        browser_menus = new Virtual_landscape_menus(this, change_receiver, owner);
        //exit_on_escape_preference = Booleans.get_boolean(Booleans.ESCAPE_FAST_EXIT,logger);


        {
            //logger.log("creating vertical slider");
            vertical_slider = new Vertical_slider(owner, the_Pane, this, logger);
            //always_on_front_nodes.add(vertical_slider.the_Slider);
            slider_width = Vertical_slider.slider_width;
        }

        set_Landscape_height_listener(vertical_slider);
        set_scroll_to_listener(vertical_slider);


        the_Scene = define_UI();


        set_all_event_handlers();

        ((Stage)owner).setScene(the_Scene);

        if ( dbg) logger.log("Virtual_landscape constructor");


        double font_size = Non_booleans.get_font_size(owner,logger);
        icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;
        start_redraw_engine(owner, aborter, logger);
    }

    //**********************************************************
    @Override // String_change_target
    public void update_config_string(String key, String new_value)
    //**********************************************************
    {
        logger.log("virtual_landscape receiving update_config_string key="+key+" val="+new_value);
        if ( key.equals(Non_booleans.LANGUAGE_KEY))
        {
            New_window_context.replace_same_folder(shutdown_target,path_list_provider.get_folder_path(),get_top_left(),owner,logger);
        }
        else if ( key.equals(Non_booleans.STYLE_KEY))
        {
            New_window_context.replace_same_folder(shutdown_target,path_list_provider.get_folder_path(),get_top_left(),owner,logger);
        }
    }


    //**********************************************************
    @Override // Selection_reporter
    public void report(String s)
    //**********************************************************
    {
        set_status(s);
    }


    //**********************************************************
    public void set_status(String s)
    //**********************************************************
    {
        status.setText(s);
        logger.log("Status = " + s);
    }

    //**********************************************************
    public void clear_all_RAM_caches()
    //**********************************************************
    {
        clear_image_properties_RAM_cache();
        clear_scroll_position_cache();
        logger.log("Image properties cache cleared");
        logger.log("Return-to scroll positions cache cleared");
        clear_image_comparators_caches();
        logger.log("Image comparators caches cleared");
        clear_image_feature_vector_RAM_cache();

    }

    //**********************************************************
    public void clear_image_comparators_caches()
    //**********************************************************
    {
        ((Clearable_RAM_cache) (image_file_comparator)).clear_RAM_cache();
        ((Clearable_RAM_cache) (other_file_comparator)).clear_RAM_cache();
    }

    //**********************************************************
    public void clear_image_feature_vector_RAM_cache()
    //**********************************************************
    {
        Browsing_caches.fv_cache_of_caches.clear();
    }

    //**********************************************************
    private void set_all_event_handlers()
    //**********************************************************
    {

        ((Stage)owner).fullScreenProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (dbg) logger.log("fullScreenProperty changed ! new value = " + newValue.booleanValue());
                    if (!newValue.booleanValue()) {
                        on_fullscreen_end();
                    } else {
                        on_fullscreen_start();
                    }
                }
        );


        {
            the_Pane.addEventHandler(MouseEvent.MOUSE_PRESSED, selection_handler::handle_mouse_pressed);
            the_Pane.addEventHandler(MouseEvent.MOUSE_DRAGGED, selection_handler::handle_mouse_dragged);
            the_Pane.addEventHandler(MouseEvent.MOUSE_RELEASED, selection_handler::handle_mouse_released);
        }

        //EventHandler<WindowEvent> on_close_event_handler = new External_close_event_handler(this);
        owner.setOnCloseRequest(event -> shutdown_target.shutdown());


        //Browser local = this;
        the_Scene.setOnKeyTyped(keyEvent -> {

            if (Browser.keyboard_dbg) {
                logger.log(keyEvent + "getCharacter->" + keyEvent.getCharacter() + "<- getCode:" + keyEvent.getCode());
                if (keyEvent.isShiftDown()) logger.log("isShiftDown: true");
                if (keyEvent.isAltDown()) logger.log("isAltDown: true");
                if (keyEvent.isMetaDown()) logger.log("isMetaDown: true");
            }

            if (keyEvent.getCharacter().equals("k")) {
                if (Browser.keyboard_dbg) logger.log("character is k = keyword search");
                search_files_by_keyworks_fx();
            }
            if (keyEvent.getCharacter().equals("s")) {
                if (Browser.keyboard_dbg) logger.log("character is s = start/stop scan");
                handle_scan_switch();
            }
            if (keyEvent.getCharacter().equals("w")) {
                if (Browser.keyboard_dbg) logger.log("character is w = slow down scan");
                slow_down_scan();
            }
            if (keyEvent.getCharacter().equals("x")) {
                if (Browser.keyboard_dbg) logger.log("character is x = speed up scan");
                speed_up_scan();
            }

            if (keyEvent.isMetaDown()) {
                if (keyEvent.getCharacter().equals("a")) {
                    if (Browser.keyboard_dbg) logger.log("character is a + meta = select all");
                    selection_handler.select_all_files_in_folder(path_list_provider);
                }

                if (keyEvent.getCharacter().equals("+")) {
                    if (Browser.keyboard_dbg) logger.log("character is +meta = zoom +");
                    increase_icon_size();
                }
                if (keyEvent.getCharacter().equals("=")) {
                    if (Browser.keyboard_dbg) logger.log("character is  (meta & equal) +meta = zoom +");
                    increase_icon_size();
                }
                if (keyEvent.getCharacter().equals("-")) {
                    if (Browser.keyboard_dbg) logger.log("character is -meta = zoom -");
                    reduce_icon_size();
                }
            }
            if (keyEvent.getCharacter().equals("n")) {
                if (Browser.keyboard_dbg) logger.log("character is n = new browser (clone)");
                //New_window_context.additional_same_folder(local, logger);
            }
        });

        the_Scene.setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_ultra_dbg) logger.log("Browser: OnDragOver handler called");
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

        Path displayed_folder_path = path_list_provider.get_folder_path();
        the_Scene.setOnDragDropped(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Browser: OnDragDropped handler called");
            if (dbg) logger.log("Something has been dropped in browser for dir :" + path_list_provider.get_name());
            int n = Drag_and_drop.accept_drag_dropped_as_a_move_in(path_list_provider.get_move_provider(),owner, drag_event, displayed_folder_path, the_Pane, "browser of dir: " + displayed_folder_path, false, logger);
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
        int new_icon_size = (int) (Non_booleans.get_icon_size(owner) * fac);
        if (new_icon_size < 20) new_icon_size = 20;
        if ( Browser.keyboard_dbg) logger.log("new icon size = "+new_icon_size);
        Non_booleans.set_icon_size(new_icon_size,owner);
        //icon_manager.modify_button_fonts(fac);
        redraw_fx("new icon size "+new_icon_size);
    }






    //**********************************************************
    @Override // Scan_show_slave
    public int how_many_rows()
    //**********************************************************
    {
        return how_many_rows;
    }

    //**********************************************************
    @Override // Scan_show_slave
    public boolean scroll_a_bit(double dy)
    //**********************************************************
    {
        Browsing_caches.scroll_position_cache_write(path_list_provider.get_folder_path(),get_top_left());
        return vertical_slider.request_scroll_relative(dy);
    }



    //**********************************************************
    public void receive_error(Error_type error_type)
    //**********************************************************
    {
        logger.log("receive_error");
        Error_type finalError_type = error_type;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                switch (finalError_type) {
                    case OK:
                        break;
                    case DENIED:
                        logger.log("\n\naccess denied\n\n");
                        set_status("Access denied for:" + path_list_provider.get_name());
                        compute_geometry("access denied", null);
                        break;
                    case NOT_FOUND:
                    case ERROR:
                        logger.log("\n\ndirectory gone\n\n");
                        set_status("Gone:" + path_list_provider.get_name());
                        compute_geometry("gone", null);
                        break;
                }
            }
        };
        Jfx_batch_injector.inject(r, logger);
    }


    //**********************************************************
    synchronized private void sort_iconized_items(String from)
    //**********************************************************
    {
        List<Path> local_iconized_sorted = new ArrayList<>(paths_holder.iconized_paths);
        for(int tentative =0; tentative<3; tentative++)
        {
            try
            {
                if ( dbg)logger.log("sort_iconized_items with "+image_file_comparator.getClass().getName());
                local_iconized_sorted.sort(image_file_comparator);
                break;
            }
            catch (IllegalArgumentException e)
            {
                // let us retry after a reshuffle
                logger.log("image sorting failed, retrying: "+tentative);
                if (image_file_comparator instanceof Similarity_comparator)
                {
                    Similarity_comparator sc = (Similarity_comparator)image_file_comparator;
                    sc.shuffle();
                }
            }
        }
        iconized_sorted_queue.add(local_iconized_sorted);
    }




    public void remove_empty_folders(boolean recursively) {
        paths_holder.remove_empty_folders(recursively);
    }

    public void clear_scroll_position_cache() {
        Browsing_caches.scroll_position_cache_clear();
    }

    public void set_text_background(String text) {

        Text t = new Text(text);
        t.setStyle("-fx-font: 70 arial;");
        Scene dummy_scene = new Scene(new VBox(t));
        WritableImage wi = dummy_scene.snapshot(null);
        Paint ppp = new ImagePattern(wi);
        the_Pane.setBackground(new Background(new BackgroundFill(ppp, CornerRadii.EMPTY, Insets.EMPTY)));

    }







    //**********************************************************
    private List<Path> get_iconized_sorted(String from)
    //**********************************************************
    {
        List<Path> returned = iconized_sorted_queue.poll();
        if ( returned != null) return returned;

        // resort
        if ( dbg) logger.log("RESORTING iconized items");
        sort_iconized_items(from);
        return iconized_sorted_queue.poll();
    }

    //**********************************************************
    public void set_Landscape_height_listener(Landscape_height_listener landscape_height_listener_)
    //**********************************************************
    {
        landscape_height_listener = landscape_height_listener_;
    }
    //**********************************************************
    public void clear_all_selected_images()
    //**********************************************************
    {
        for (Item i : all_items_map.values())
        {
            i.unset_image_is_selected();
        }
    }


    // make sure map_buttons_and_icons is not called again before it is finished
    // cannot use "synchronized" because part of the job is performed
    // on another thread




    //**********************************************************
    void scroll_to()
    //**********************************************************
    {
        Path scroll_to = Browsing_caches.scroll_position_cache_read(path_list_provider.get_folder_path());
        //logger.log("scroll_to folder=\n"+"      "+path_list_provider.get_name()+"\n      scroll_to="+scroll_to);

        current_vertical_offset = get_y_offset_of(scroll_to);
        if ( scroll_to_listener == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return;
        }
        scroll_to_listener.perform_scroll_to(current_vertical_offset,this);
    }
    //**********************************************************
    private void show_error_icon(ImageView iv_denied, double top_delta_y)
    //**********************************************************
    {
        iv_denied.setPreserveRatio(true);
        iv_denied.setSmooth(true);
        iv_denied.setY(top_delta_y);
        if ( Platform.isFxApplicationThread())
        {
            the_Pane.getChildren().add(iv_denied);
        }
        else
        {
            Jfx_batch_injector.inject(()-> the_Pane.getChildren().add(iv_denied),logger);
        }
        compute_bounding_rectangle(error_type.toString());
    }

    //**********************************************************
    private synchronized void process_iconized_items(boolean single_column, double icon_size, double column_increment, double scene_width, Point2D point)
    //**********************************************************
    {

        double file_button_height = 2 * Non_booleans.get_font_size(owner,logger);

        double max_y_in_row[] = new double[1];
        max_y_in_row[0] = 0;
        List<Item> current_row = new ArrayList<>();


        // first compute how many images are in flight
        int image_properties_in_flight = 0;
        boolean show_icons_for_files = Feature_cache.get(Feature.Show_icons_for_files);
        for (Path path : paths_holder.iconized_paths )
        {
            Item item;
            if (show_icons_for_files)
            {
                item = all_items_map.get(path);
                if (item == null)
                {
                    image_properties_in_flight++;
                }
            }
        }

        // block until:
        //  1. all image properties REQUESTS are made
        // the cache will use actors on threads to fetch the properties
        //  2. the cache will call the termination reporter when each is finished
        // so this will effectively block until all image properties are fetched

        if ( dbg) logger.log("process_iconized_items is on javafx thread?"+Platform.isFxApplicationThread());
        CountDownLatch wait_for_end = new CountDownLatch(image_properties_in_flight);
        Job_termination_reporter tr = (message, job) -> wait_for_end.countDown();
        long start = System.currentTimeMillis();
        for (Path path : paths_holder.iconized_paths )
        {
            if (ultra_dbg) logger.log("Virtual_landscape process_iconified_items " + path);
            Item item;
            if (show_icons_for_files)
            {
                item = all_items_map.get(path);
                if (item == null)
                {
                    wait_for_end.countDown();
                    // ask for image properties fetch in threads
                    browsing_caches.image_properties_RAM_cache.get_from_cache(path,tr);
                }
            }
            else
            {
                // this is an item that could have an image but the user prefers not
                String size = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(path.toFile().length(), owner,logger);
                item = all_items_map.get(path);
                if (item == null) {
                    logger.log("Item_file_no_icon (1) path="+path);
                    item = new Item_file_no_icon(
                            the_Scene,
                            selection_handler,
                            icon_factory_actor,
                            null,
                            path.getFileName().toString() + "(" + size + ")",
                            browsing_caches.image_properties_RAM_cache,
                            shutdown_target,
                            path,
                            path_list_provider,
                            this,
                            this,
                            owner,
                            aborter,
                            logger);
                    //new Item2_button(the_browser, path, null, path.getFileName().toString() + "(" + size + ")",
                            //icon_size / 2, false, false, image_properties_RAM_cache, logger);
                    all_items_map.put(path, item);
                }
            }
        }
        if ( image_properties_in_flight > 1) {
            // wait for all properties to become available
            //logger.log("going to wait");
            try {
                wait_for_end.await();
            } catch (InterruptedException e) {
                logger.log("" + e);
            }
            //logger.log("wait terminated");
        }
        if ( dbg) logger.log("getting image properties took " + (System.currentTimeMillis() - start) + " milliseconds");






        start = System.currentTimeMillis();
        long getting_image_properties_from_cache = 0;
        for ( Path path : paths_holder.iconized_paths)
        {
            Double cache_aspect_ratio = Double.valueOf(1.0);
            long local_incr = System.currentTimeMillis();
            // this i a BLOCKING call
            Image_properties ip = browsing_caches.image_properties_RAM_cache.get_from_cache(path,null);
            if ( ip == null)
            {
                logger.log(("Warning: image property cache miss for: "+path));
            }
            else
            {
                cache_aspect_ratio = (Double) ip.get_aspect_ratio();
            }
            getting_image_properties_from_cache += System.currentTimeMillis() - local_incr;
            Supplier<Image_feature_vector_cache> fv_cache_supplier = () ->
            {
                if ( fv_cache != null) return fv_cache;
                double x = owner.getX()+100;
                double y = owner.getY()+100;
                Image_feature_vector_cache.Images_and_feature_vectors images_and_feature_vectors =
                        Image_feature_vector_cache.preload_all_feature_vector_in_cache(path_list_provider,  owner,x,  y,  aborter,  logger);
                fv_cache= images_and_feature_vectors.fv_cache();
                return fv_cache;
            };

            Item item = new Item_file_with_icon(
                    the_Scene,
                    selection_handler,
                    icon_factory_actor,
                    null,
                    cache_aspect_ratio,
                    browsing_caches.image_properties_RAM_cache,
                    fv_cache_supplier,
                    path,
                    path_list_provider,
                    this,
                    
                    owner,
                    aborter,
                    logger);
            all_items_map.put(path,item);
            //logger.log("item created: "+path);
        }
        if ( dbg)
        {
            logger.log("making iconized items took " + (System.currentTimeMillis() - start) + " milliseconds");
            logger.log("     ,of which getting_image_properties_from_cache= " +getting_image_properties_from_cache+ " milliseconds");
        }


        /// at this stage we MUST have get_iconized_sorted() in the proper order
        // that will define the x,y layout
        start = System.currentTimeMillis();
        List<Path> ll = get_iconized_sorted("process_iconified_items");
        for (Path path : ll )
        {
            Item item = all_items_map.get(path);
            if ( item == null)
            {
                logger.log(("should not happen: no item in map for: "+path+" map size="+all_items_map.size() ));
                continue;
            }
            if (dbg)  logger.log("Virtual_landscape process_iconified_items " + path+" ar:"+((Item_file_with_icon)item).aspect_ratio);

            if (show_icons_for_files)
            {
                //logger.log("recomputing position for "+item.get_item_path());
                //logger.log(path+" point ="+point.getX()+"-"+point.getY());
                point = compute_next_Point2D_for_icons(point, item,
                        icon_size, icon_size,
                        scene_width, single_column, max_y_in_row, current_row);
            }
            else
            {
                point = new_Point_for_files_and_dirs(point, item,
                        column_increment,
                        file_button_height, scene_width, single_column);
                how_many_rows++;
            }
        }
        if ( dbg) logger.log("mapping iconized items took " + (System.currentTimeMillis() - start) + " milliseconds");

    }

    //**********************************************************
    private Point2D process_non_iconized_files(boolean single_column, double column_increment, double scene_width, Point2D p)
    //**********************************************************
    {
        // manage the non-iconifed-files section
        double row_increment_for_files = 2 * Non_booleans.get_font_size(owner,logger);

        for (Path path : paths_holder.non_iconized.keySet())
        {
            if (ultra_dbg) logger.log("Virtual_landscape process_non_iconized_files "+path.toAbsolutePath());
            String text = path.getFileName().toString();
            long size = path.toFile().length() / 1000_000L;
            if (Guess_file_type.is_this_path_a_video(path)) text = size + "MB VIDEO: " + text;
            Item item = all_items_map.get(path);
            if ( item == null)
            {
                //logger.log("Item_file_no_icon (3) path="+path);

                item = new Item_file_no_icon(
                        the_Scene,
                        selection_handler,
                        icon_factory_actor,
                        null, 
                        text,
                        browsing_caches.image_properties_RAM_cache,
                        shutdown_target,
                        path,
                        path_list_provider,
                        this,
                        this,
                        
                        owner,
                        aborter,
                        logger);
                all_items_map.put(path,item);
            }
            //item.get_Node().setVisible(false);
            p = new_Point_for_files_and_dirs(p, item,
                    column_increment,
                    row_increment_for_files, scene_width, single_column);

            if (item instanceof Item_file_no_icon ini)
            {
                ini.get_button().setPrefWidth(column_increment);
                ini.get_button().setMinWidth(column_increment);
            }
            if (item instanceof Item_folder ini)
            {
                ini.get_button().setPrefWidth(column_increment);
                ini.get_button().setMinWidth(column_increment);
            }
            if (item instanceof Item_folder_with_icon ini)
            {
                ini.get_button().setPrefWidth(column_increment);
                ini.get_button().setMinWidth(column_increment);
            }
        }
        if ( ! paths_holder.non_iconized.isEmpty())
        {
            if (p.getX() != 0)
            {
                //logger.log("p.getX() != 0"+p.getX());
                p = new Point2D(0, p.getY() + row_increment_for_files);
                how_many_rows++;
            }
        }
        return p;
    }

    long tot_ms = 0;
    //**********************************************************
    private Point2D process_folders( boolean single_column, double row_increment_for_dirs, double column_increment, double row_increment_for_dirs_with_picture, double scene_width, Point2D p)
    //**********************************************************
    {
        if (dbg) logger.log("Virtual_landscape process_folders (0) ");

        double actual_row_increment;
        if ( Feature_cache.get(Feature.Show_icons_for_folders))
        {
            actual_row_increment = row_increment_for_dirs_with_picture;

            for (Path folder_path : paths_holder.folders.keySet())
            {
                if (dbg) logger.log("Virtual_landscape process_folders (1) "+folder_path);
                long start = System.currentTimeMillis();
                if(dbg) logger.log("folder :"+folder_path+" took1 "+(System.currentTimeMillis()-start)+" milliseconds");
                p = process_one_folder_with_picture(single_column, column_increment, actual_row_increment, scene_width, p, folder_path, Color.BEIGE);
                if(dbg) logger.log("folder :"+folder_path+" took2 "+(System.currentTimeMillis()-start)+" milliseconds");
            }
        }
        else
        {
            actual_row_increment = row_increment_for_dirs;
            List<Path> paths = new ArrayList<>(paths_holder.folders.keySet());
            if ( show_total_size_deep_in_each_folder_done)
            {
                Comparator<Path> comp = (p1, p2) -> {
                    Long l1 = folder_total_sizes_cache.get(p1);
                    if (l1==null) return 1;
                    Long l2 = folder_total_sizes_cache.get(p2);
                    if (l2==null) return 1;
                    return l2.compareTo(l1);
                };
                Collections.sort(paths,comp);
            }
            else if ( show_how_many_files_deep_in_each_folder_done)
            {
                Comparator<Path> comp = (p1, p2) -> {
                    Long l1 = folder_file_count_cache.get(p1);
                    if (l1==null) return 1;
                    Long l2 = folder_file_count_cache.get(p2);
                    if (l2==null) return 1;
                    return l2.compareTo(l1);
                };
                Collections.sort(paths,comp);
            }
            if (dbg) logger.log("Virtual_landscape folder_path size "+paths.size());
            for (Path folder_path : paths)
            {
                if (dbg) logger.log("Virtual_landscape process_folders (3) "+folder_path);
                p = process_one_folder_plain(single_column, column_increment, actual_row_increment, scene_width, p, folder_path);
            }
        }

        if (p.getX() != 0)
        {
            p = new Point2D(0, p.getY() + actual_row_increment);
            how_many_rows++;
        }
        return p;
    }

    //**********************************************************
    private Point2D process_one_folder_with_picture(
            boolean single_column,
            double column_increment,
            double row_increment,
            double scene_width,
            Point2D p,
            Path folder_path,
            Color color)
    //**********************************************************
    {
        Item folder_item = all_items_map.get(folder_path);
        if (  folder_item == null)
        {
            if (dbg) logger.log("WARNING:Item_folder_with_icon NO path for" + folder_path);

            folder_item = new Item_folder_with_icon(
                    owner, 
                    the_Scene, 
                    selection_handler,
                    icon_factory_actor,
                    color,
                    folder_path.getFileName().toString(),
                    (int)column_increment,
                    100,
                    false,
                    null,
                    browsing_caches.image_properties_RAM_cache,
                    shutdown_target,
                    new Folder_path_list_provider(folder_path),
                    this,
                    this,
                    aborter,
                    logger);
            all_items_map.put(folder_path, folder_item);
        }
        p = new_Point_for_files_and_dirs(p, folder_item, column_increment, row_increment, scene_width, single_column);
        return p;
    }


    //**********************************************************
    private Point2D process_one_folder_plain(
                                       boolean single_column,
                                       double column_increment,
                                       double row_increment,
                                       double scene_width,
                                       Point2D p,
                                       Path folder_path)
    //**********************************************************
    {
        Item folder_item = all_items_map.get(folder_path);
        if (  folder_item == null)
        {
            Color color = My_colors.load_color_for_path(folder_path,owner,logger);
            // a "plain" folder is "like a file" from a layout point of view
            // the difference is: it will get a border

            String tmp = folder_path.getFileName().toString();

            if ( show_how_many_files_deep_in_each_folder_done)
            {
                Long how_many_files_deep = folder_file_count_cache.get(folder_path);
                if ( how_many_files_deep == null)
                {
                    logger.log("FATAL: folder_file_count_cache not found in cache for "+folder_path);
                }
                else
                {
                    logger.log("OK: folder_file_count_cache found in cache for "+folder_path+ " "+how_many_files_deep);
                    tmp +=   " (" + how_many_files_deep + " files)";
                }
            }
            else if ( show_total_size_deep_in_each_folder_done)
            {

                Long bytes = folder_total_sizes_cache.get(folder_path);
                if ( bytes == null)
                {
                    logger.log("FATAL: folder_total_sizes_cache not found in cache for "+folder_path);
                }
                else
                {
                    logger.log("OK: folder_total_sizes_cache found in cache for "+folder_path+" "+bytes);

                    tmp += "       ";
                    tmp += Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(bytes,owner,logger);
                }
            }

            folder_item = new Item_folder(
                    the_Scene,
                    selection_handler,
                    icon_factory_actor,
                    color,
                    tmp,
                    icon_height,
                    false,
                    null,
                    browsing_caches.image_properties_RAM_cache,
                    shutdown_target,
                    new Folder_path_list_provider(folder_path),
                    this,
                    this,
                    
                    owner,
                    aborter,
                    logger);
            //new Item2_button(the_browser,folder_path, color, tmp, icon_height, false, false, image_properties_RAM_cache,logger);
            all_items_map.put(folder_path, folder_item);
        }

        p = new_Point_for_files_and_dirs(p, folder_item, column_increment, row_increment, scene_width, single_column);
        if (folder_item instanceof Item_folder ini)
        {
            ini.get_button().setPrefWidth(column_increment);
            ini.get_button().setMinWidth(column_increment);
        }
        return p;
    }

    //**********************************************************
    public void clear_image_properties_RAM_cache()
    //**********************************************************
    {
        if ( browsing_caches.image_properties_RAM_cache == null) return;
        browsing_caches.image_properties_RAM_cache.clear_cache();

        Browsing_caches.image_properties_RAM_cache_of_caches.clear();
    }



    // this is the other entry point: SCROLLING
    // when the slider is moved

    //**********************************************************
    public void move_absolute(
            double new_vertical_offset,
            String reason)
    //**********************************************************
    {
        if (scroll_dbg)
            logger.log("move_absolute reason= " + reason + " new_vertical_offset=" + new_vertical_offset);
        current_vertical_offset = new_vertical_offset;
        set_visibility_on_fx_thread( "move_absolute");
    }

    // this is on the FX thread
    // and it is called very often for example when scrolling
    //**********************************************************
    void set_visibility_on_fx_thread(String from)
    //**********************************************************
    {
        if ( !items_are_ready.get())
        {
            logger.log("check_visibility: items are not ready yet ! "+from);
            return;
        }
        //logger.log("check_visibility: "+ all_items_map.values().size()+" items are ready "+from);
        double pane_height = the_Pane.getHeight();
        int icon_size = Non_booleans.get_icon_size(owner);
        double min_y = Double.MAX_VALUE;
        for (Item item : all_items_map.values())
        {
            //if (item.get_y() + item.get_Height() - current_vertical_offset < 0)
            //if (item.get_javafx_y() + item.get_Height() < current_vertical_offset -icon_size)
            if (item.get_javafx_y() + item.get_Height() < current_vertical_offset )
            {
                if (invisible_dbg)
                    logger.log(item.get_item_path() + " invisible (too far up) y=" + item.get_javafx_y() + " item height=" + item.get_Height());
                item.process_is_invisible(current_vertical_offset);
                the_Pane.getChildren().remove(item.get_Node());
                //if ( item instanceof Item2_image ii) Item2_image.currently.remove(ii);
                continue;
            }
            if (item.get_javafx_y()  > pane_height+current_vertical_offset+icon_size)
            {
                if (invisible_dbg) logger.log(item.get_item_path() + " invisible (too far down)");
                item.process_is_invisible(current_vertical_offset);
                the_Pane.getChildren().remove(item.get_Node());
                //if ( item instanceof Item2_image ii) Item2_image.currently.remove(ii);
                continue;
            }
            if (visible_dbg)
                logger.log(item.get_item_path() + " Item is visible at y=" + item.get_javafx_y() + " item height=" + item.get_Height());
            item.process_is_visible(current_vertical_offset);
            if ( !the_Pane.getChildren().contains(item.get_Node()))
            {
                the_Pane.getChildren().add(item.get_Node());
                //if ( item instanceof Item2_image ii) Item2_image.currently.add(ii);
            }


            // look for top left
            if ( item.get_javafx_x() > 0) continue;
            if ( item.get_javafx_y() < min_y)
            {
                min_y = item.get_javafx_y();
                top_left = item.get_item_path();
                //logger.log("       tmp........"+top_left + " is now top left at y=" + min_y);
            }
        }
        //logger.log(top_left + " is now top left at y=" + min_y);

        //logger.log("currently Item2_image (s): "+Item2_image.currently.size());
    }




    private static final double margin = 20;
    private static final double dmargin = 2*margin;
    //**********************************************************
    public List<Item> get_items_in(Pane pane, double x, double y, double w, double h)
    //**********************************************************
    {
        Bounds selection_bounds = new BoundingBox(x, y, w, h);
        //logger.log("selection  X= " + bounds.getMinX() + " " + bounds.getMaxX() + " Y= " + bounds.getMinY() + " " + bounds.getMaxY());
        List<Item> returned = new ArrayList<>();

        for (Item item : all_items_map.values()) {
            Node node = item.get_Node();
            if (!pane.getChildren().contains(node)) continue;
            Bounds b = node.getBoundsInParent();
            //if (b.intersects(bounds))
            if (selection_bounds.contains(
                    b.getMinX()+margin, b.getMinY()+margin,b.getMinZ(),
                    b.getWidth()-dmargin, b.getHeight()-dmargin,b.getDepth()
            )) {
                returned.add(item);
                //logger.log("2YES ! for " + item.get_icon_path() + " we have bounds X= " + b.getMinX() + " " + b.getMaxX() + " Y= " + b.getMinY() + " " + b.getMaxY());
            } else {
                //logger.log("2NO ? for " + item.get_icon_path() + " we have bounds X= " + b.getMinX() + " " + b.getMaxX() + " Y= " + b.getMinY() + " " + b.getMaxY());
            }
        }
        return returned;
    }


    //**********************************************************
    public double get_virtual_landscape_height()
    //**********************************************************
    {
        return virtual_landscape_height;
    }

    //**********************************************************
    public void show_how_many_files_deep_in_each_folder()
    //**********************************************************
    {
        show_total_size_deep_in_each_folder_done = false;

        folder_file_count_cache = new HashMap<>();

        AtomicInteger count = new AtomicInteger(0);
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Show_running_film_frame_with_abort_button show_running_film_frame = Show_running_film_frame_with_abort_button.show_running_film(count,"Computing folder sizes", 300, x,y,logger);
        for ( Item i : all_items_map.values())
        {
            if (i instanceof Item_folder ini)
            {
                if(Files.isDirectory(ini.get_true_path()))
                {
                    ini.add_how_many_files_deep_folder(
                            count,
                            ini.get_button(),
                            ini.text,
                            ini.get_true_path(),
                            folder_file_count_cache,
                            aborter,
                            logger);
                }
            }

        }
        show_how_many_files_deep_in_each_folder_done = true;

        Runnable monitor = () -> {
            long start = System.currentTimeMillis();
            for(;;) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.log("" + e);
                }
                if (count.get() == 0)
                {
                    Jfx_batch_injector.inject(()-> compute_geometry("sort by number of files", show_running_film_frame),logger);
                    if ( System.currentTimeMillis()-start > 3000) {
                        Ding.play("display how many files in each folder", logger);
                    }
                    return;
                }
            }

        };
        Actor_engine.execute(monitor,logger);
    }

    //**********************************************************
    public void show_total_size_deep_in_each_folder()
    //**********************************************************
    {
        if(( File_sort_by.get_sort_files_by(path_list_provider.get_folder_path(),owner)==File_sort_by.SIMILARITY_BY_PAIRS)
                ||
                (File_sort_by.get_sort_files_by(path_list_provider.get_folder_path(),owner)==File_sort_by.SIMILARITY_BY_PURSUIT))
        {
            File_sort_by.set_sort_files_by(path_list_provider.get_folder_path(),File_sort_by.NAME,owner,logger);
        }
        show_how_many_files_deep_in_each_folder_done = false;
        folder_total_sizes_cache = new HashMap<>();
        logger.log("Virtual_landscape: show_total_size_deep_in_each_folder");
        AtomicInteger count = new AtomicInteger(0);
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Show_running_film_frame_with_abort_button show_running_film_frame = Show_running_film_frame_with_abort_button.show_running_film(count,"Computing folder sizes", 300, x,y,logger);
        for ( Item i : all_items_map.values())
        {
            if (i instanceof Item_folder item2_folder)
            {
                if(Files.isDirectory(item2_folder.get_true_path()))
                {
                    item2_folder.add_total_size_deep_folder(count, item2_folder.get_button(), item2_folder.text, item2_folder.get_true_path(),
                            folder_total_sizes_cache,
                            show_running_film_frame.aborter, logger);
                }
            }
        }
        show_total_size_deep_in_each_folder_done = true;

        Runnable monitor = () -> {
            long start = System.currentTimeMillis();
            for(;;) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.log("" + e);
                }
                if (count.get() == 0)
                {
                    Jfx_batch_injector.inject(()-> compute_geometry("sort by folder size on disk", show_running_film_frame),logger);
                    if ( System.currentTimeMillis()-start > 3000) {
                        Ding.play("display all folder sizes", logger);
                    }
                    return;
                }
            }

        };
        Actor_engine.execute(monitor,logger);
    }


    @Override // Top_left_provider
    //**********************************************************
    public Path get_top_left()
    //**********************************************************
    {
        return top_left;
    }

    //**********************************************************
    public double get_y_offset_of(Path target)
    //**********************************************************
    {
        if ( target == null)  return 0.0;
        
        //logger.log("\n\nIcon_manager::get_y_offset_of "+target.toAbsolutePath()+" size="+all_items_map.values().size());
        String t2 = target.toAbsolutePath().toString();
        for ( Item i : all_items_map.values())
        {
            //logger.log("\n\nIcon_manager::get_y_offset_of ... looking at "+i.get_item_path().toAbsolutePath());
            if ( i.get_item_path().toAbsolutePath().toString().equals(t2))
            {
                //logger.log("\n\nIcon_manager::get_y_offset_of "+target+ " FOUND offset = "+i.get_javafx_y());
                return i.get_javafx_y();
            }
        }
        //logger.log(Stack_trace_getter.get_stack_trace("\n\nnot found: Virtual_landscape::get_y_offset_of "+target+" (was typically deleted recently)"));

        return 0;
    }



    //**********************************************************
    private Point2D compute_next_Point2D_for_icons(Point2D p,
                                                   Item item,
                                                   double column_increment,
                                                   double row_increment,
                                                   double scene_width,
                                                   boolean single_column,
                                                   double[] max_screen_y_in_row,
                                                   List<Item> current_row)
    //**********************************************************
    {
        double width_of_this = column_increment;
        double height_of_this = row_increment;

        final double current_screen_x = p.getX();
        final double current_screen_y = p.getY();
        item.set_screen_x_of_image(current_screen_x);
        item.set_screen_y_of_image(current_screen_y);

        if (((Item_file_with_icon)item).aspect_ratio < 1.0)
        {
            if (dbg) logger.log("item is portrait aspect ratio: "+item.get_item_path());

            // portrait image
            width_of_this = column_increment * ((Item_file_with_icon)item).aspect_ratio;
            double neg_x = 0;//(width_of_this-column_increment)/2.0;
            // shift left to compensate the portrait
            item.set_javafx_x(current_screen_x+neg_x);
            item.set_javafx_y(current_screen_y);
        }
        else
        {
            if( dbg) logger.log("item is landscape aspect ratio: "+item.get_item_path());
            item.set_javafx_x(current_screen_x);
            height_of_this = row_increment/((Item_file_with_icon)item).aspect_ratio;
            double neg_y = (height_of_this-row_increment)/2.0;
            // shift up to compensate the landscape
            item.set_javafx_y(current_screen_y+neg_y);
        }

        current_row.add(item);
        if ( max_screen_y_in_row[0] < item.get_screen_y_of_image()+height_of_this) max_screen_y_in_row[0] = item.get_screen_y_of_image()+height_of_this;
        if ( Item.layout_dbg) logger.log(item.get_item_path()+"\n" +
                "width_of_this="+width_of_this+" current_x="+current_screen_x+"\n" +
                "height_of_this="+height_of_this+" current_y="+current_screen_y+ " max_y = "+max_screen_y_in_row[0]);



        /// then compute position of NEXT item
        if ( single_column)
        {
            current_row.clear();
            how_many_rows++;
            double future_x = 0;
            double future_y = current_screen_y + row_increment;
            //logger.log("new row "+row_increment);
            return new Point2D(future_x, future_y);
        }

        double future_x = item.get_screen_x_of_image()+width_of_this;
        if ( Item.layout_dbg) logger.log("width_of_this="+width_of_this+" => future_x: "+future_x);
        if (future_x + column_increment > scene_width)
        {
            if ( Item.layout_dbg) logger.log("NEW ROW, max_screen_y_in_row="+max_screen_y_in_row[0]);

            // adapt the vertical shift up (neg_y)
            // e.g. when the row also contains portraits
            double min_y = Double.MAX_VALUE;
            double max_y = 0;
            for(Item i : current_row)
            {
                if (i.get_screen_y_of_image() < min_y) min_y = i.get_screen_y_of_image();
                double height = 0;
                if (((Item_file_with_icon)i).aspect_ratio < 1.0)
                {
                    // portrait image
                    height=row_increment;
                }
                else
                {
                    // landscape image
                    height = row_increment/((Item_file_with_icon)i).aspect_ratio;
                }
                if (i.get_screen_y_of_image()+height > max_y) max_y = i.get_screen_y_of_image()+height;
            }
            double row_height = (max_y-min_y);
            for(Item i : current_row)
            {
                double height = 0;
                if (((Item_file_with_icon)i).aspect_ratio < 1.0)
                {
                    // portrait image
                    height=row_increment;
                }
                else
                {
                    // landscape image
                    height = row_increment/((Item_file_with_icon)i).aspect_ratio;
                }
                double diff = (row_height-height)/2.0;
                i.set_javafx_y(i.get_javafx_y()+diff);
            }

            // new ROW
            current_row.clear();
            how_many_rows++;
            Point2D returned =  new Point2D(0, max_screen_y_in_row[0]);
            max_screen_y_in_row[0] = 0;
            return returned;
        }

        // continued row
        return new Point2D(future_x, current_screen_y);
    }

    //**********************************************************
    private Point2D new_Point_for_files_and_dirs(Point2D point,
                                                 Item item,
                                                 double column_increment,
                                                 double row_increment,
                                                 double scene_width,
                                                 boolean single_column)
    //**********************************************************
    {
        //logger.log("column_increment: "+column_increment+", row_increment: "+row_increment);

        double old_x = point.getX();
        double old_y = point.getY();
        item.set_javafx_x(old_x);
        item.set_javafx_y(old_y);

        double delta_h = row_increment;
        if ( single_column)
        {
            how_many_rows++;
            double new_x = 0;
            double new_y = old_y + delta_h;
            if ( dbg) logger.log("single_column new row "+delta_h);
            return new Point2D(new_x, new_y);
        }
        double future_x = old_x + column_increment;
        double future_x_with_width = future_x + column_increment;
        if (future_x_with_width > scene_width)
        {
            //logger.log("old_x: "+old_x+" column_increment: "+ column_increment+" future_x_with_width: "+future_x_with_width+">"+ scene_width+" too far right, need to create a new row "+item.get_item_path());
            how_many_rows++;
            double new_x = 0;
            double new_y = old_y + delta_h;
            //logger.log("new row "+delta_h);
            return new Point2D(new_x, new_y);
        }
        // future candidate point is same line, further on the right
        return new Point2D(future_x, old_y);
    }


    //**********************************************************
    private void compute_bounding_rectangle(String reason)
    //**********************************************************
    {
        if (scroll_dbg) logger.log("compute_bounding_rectangle() "+reason);
        // compute bounding rectangle

        double x_min = Double.MAX_VALUE;
        double x_max = -Double.MAX_VALUE;
        double y_min = Double.MAX_VALUE;
        virtual_landscape_height = -Double.MAX_VALUE;
        for (Item item : all_items_map.values())
        {
            if (item.get_javafx_x() < x_min) x_min = item.get_javafx_x();
            if (item.get_javafx_x() + item.get_Width() > x_max)
            {
                x_max = item.get_javafx_x() + item.get_Width();
            }
            if (item.get_javafx_y() < y_min)
            {
                y_min = item.get_javafx_y();
            }
            double h = item.get_Height();
            if ( scroll_dbg) logger.log("compute_bounding_rectangle, h="+h+" for "+item.get_string());

            if (item.get_javafx_y() + h > virtual_landscape_height) virtual_landscape_height = item.get_javafx_y() + h;
        }

        if (get_iconized_sorted("compute_bounding_rectangle").isEmpty())
        {
            // when there is no iconized items in the folder
            // it may happen that the height of the last row of buttons at the bottom is underestimated
            virtual_landscape_height += 100;
        }
        if (scroll_dbg)
            logger.log("landscape_height="+ virtual_landscape_height);
        if ( landscape_height_listener != null)
        {
            landscape_height_listener.browsed_landscape_height_has_changed(virtual_landscape_height,current_vertical_offset);
        }
    }


    //**********************************************************
    public void set_scroll_to_listener(Scroll_to_listener vertical_slider)
    //**********************************************************
    {
        scroll_to_listener = vertical_slider;
    }



    //**********************************************************
    public List<File> get_file_list()
    //**********************************************************
    {
        return paths_holder.get_file_list();
    }

    //**********************************************************
    public List<File> get_folder_list()
    //**********************************************************
    {
        return paths_holder.get_folder_list();
    }




    //**********************************************************
    Scene define_UI()
    //**********************************************************
    {

        double font_size = Non_booleans.get_font_size(owner,logger);
        double height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;

        Button up_button;
        {
            String go_up_text = "";
            if (path_list_provider.has_parent() )
            {
                go_up_text = My_I18n.get_I18n_string("Parent_Folder", owner,logger);// to: " + parent.toAbsolutePath().toString();
            }
            up_button = browser_menus.make_button_that_behaves_like_a_folder(
                    path_list_provider.get_folder_path().getParent(),
                    go_up_text,
                    height,
                    MIN_PARENT_AND_TRASH_BUTTON_WIDTH,
                    false,
                    path_list_provider.get_folder_path(),
                    logger);
            {
                Image icon = Look_and_feel_manager.get_up_icon(height,owner,logger);
                if (icon == null)
                    logger.log("WARNING: could not load " + Look_and_feel_manager.get_instance(owner,logger).get_up_icon_path());
                Look_and_feel_manager.set_button_and_image_look(up_button, icon, height, null,true,owner,logger);

            }
            top_buttons.add(up_button);
        }

        Button trash;
        {
            String trash_text = My_I18n.get_I18n_string("Trash",owner,logger);// to: " + parent.toAbsolutePath().toString();
            trash = browser_menus.make_button_that_behaves_like_a_folder(
                    Non_booleans.get_trash_dir(path_list_provider.get_folder_path(),owner,logger),
                    trash_text,
                    height,
                    MIN_PARENT_AND_TRASH_BUTTON_WIDTH,
                    true,
                    null,
                    logger);
            {
                Image icon = Look_and_feel_manager.get_trash_icon(height,owner,logger);
                if (icon == null)
                    logger.log("WARNING: could not load " + Look_and_feel_manager.get_instance(owner,logger).get_bookmarks_icon_path());
                Look_and_feel_manager.set_button_and_image_look(trash, icon, height,null, true,owner,logger);

            }
            top_buttons.add(trash);
        }

        Pane top_pane = define_top_bar_using_buttons_deep(height, up_button, trash);
        BorderPane border_pane = define_border_pane(top_pane);
        Scene returned = new Scene(border_pane);//, W, H);



        //set the view order (smaller means closer to viewer = on top)
        top_pane.setViewOrder(0);
        the_Pane.setViewOrder(100);
        apply_font();
        return returned;
    }


    //**********************************************************
    public void apply_font()
    //**********************************************************
    {
        if (dbg) logger.log("applying font size " + Non_booleans.get_font_size(owner,logger));
        for (Node x : top_buttons) {
            Font_size.apply_font_size(x, owner,logger);
        }
    }

    //**********************************************************
    String get_status()
    //**********************************************************
    {
        File_sort_by file_sort_by = File_sort_by.get_sort_files_by(path_list_provider.get_folder_path(),owner);
        if (file_sort_by == null)
            return "Status: OK";
        else
        return "Status: OK, files are sorted in this order : "+file_sort_by.name();

    }

    //**********************************************************
    private BorderPane define_border_pane(Pane top_pane)
    //**********************************************************
    {
        BorderPane returned = new BorderPane();
        {
            returned.setTop(top_pane);
            Look_and_feel_manager.set_region_look(top_pane,owner,logger);

        }
        returned.setCenter(the_Pane);
        {
            VBox for_vertical_slider = new VBox();
            for_vertical_slider.getChildren().add(vertical_slider.the_Slider);
            Look_and_feel_manager.set_region_look(for_vertical_slider,owner,logger);

            returned.setRight(for_vertical_slider);
        }
        {
            VBox the_status_bar = new VBox();
            status = new TextField(get_status());
            Look_and_feel_manager.set_region_look(status,owner,logger);
            the_status_bar.getChildren().add(status);
            returned.setBottom(the_status_bar);
        }
        Look_and_feel_manager.set_region_look(returned,owner,logger);
        return returned;
    }


    //**********************************************************
    private Pane define_top_bar_using_buttons_deep(double height, Button go_up, Button trash)
    //**********************************************************
    {
        Pane top_pane;
        top_pane = new VBox();
        {
            HBox top_pane2 = new HBox();
            top_pane2.setAlignment(Pos.CENTER);
            top_pane2.setSpacing(10);
            top_pane2.getChildren().add(go_up);
            define_top_bar_using_buttons_deep(top_pane2, height);
            Region spacer = new Region();
            top_pane2.getChildren().add(spacer);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            top_pane2.getChildren().add(trash);
            Region spacer2 = new Region();
            top_pane2.getChildren().add(spacer2);
            HBox.setHgrow(spacer2, Priority.SOMETIMES);
            Look_and_feel_manager.set_region_look(top_pane2,owner,logger);
            top_pane.getChildren().add(top_pane2);
        }
        top_pane.getChildren().add(new Separator());
        return top_pane;
    }


    //**********************************************************
    private void define_top_bar_using_buttons_deep(Pane top_pane, double height)
    //**********************************************************
    {
        {
            String undo_bookmark_history = My_I18n.get_I18n_string("Bookmarks", owner,logger);
            undo_bookmark_history += " & " + My_I18n.get_I18n_string("History", owner,logger);
            Button undo_bookmark_history_button = new Button(undo_bookmark_history);
            undo_bookmark_history_button.setOnAction(e -> button_undo_and_bookmark_and_history(e));
            top_pane.getChildren().add(undo_bookmark_history_button);
            top_buttons.add(undo_bookmark_history_button);
            Image icon = Look_and_feel_manager.get_bookmarks_icon(height,owner,logger);
            Look_and_feel_manager.set_button_and_image_look(undo_bookmark_history_button, icon, height,null, false,owner,logger);
        }
        {
            String files = My_I18n.get_I18n_string("Files", owner,logger);
            Button files_button = new Button(files);
            files_button.setOnAction(e -> button_files(e));
            top_pane.getChildren().add(files_button);
            top_buttons.add(files_button);
            Image icon = Look_and_feel_manager.get_folder_icon(height,owner,logger);
            Look_and_feel_manager.set_button_and_image_look(files_button, icon, height,null, false,owner,logger);
        }
        {
            String view = My_I18n.get_I18n_string("View", owner,logger);
            Button view_button = new Button(view);
            view_button.setOnAction(e -> button_view(e));
            top_pane.getChildren().add(view_button);
            top_buttons.add(view_button);
            Image icon = Look_and_feel_manager.get_view_icon(height,owner,logger);
            Look_and_feel_manager.set_button_and_image_look(view_button, icon, height,null, false,owner,logger);
        }
        {
            String preferences = My_I18n.get_I18n_string("Preferences", owner,logger);
            Button preferences_button = new Button(preferences);
            preferences_button.setOnAction(e -> button_preferences(e));
            top_pane.getChildren().add(preferences_button);
            top_buttons.add(preferences_button);
            Image icon = Look_and_feel_manager.get_preferences_icon(height,owner,logger);
            Look_and_feel_manager.set_button_and_image_look(preferences_button, icon, height,null, false,owner,logger);
        }
    }

    //**********************************************************
    private void button_undo_and_bookmark_and_history(ActionEvent e)
    //**********************************************************
    {
        ContextMenu undo_and_bookmark_and_history = define_contextmenu_undo_bookmark_history();
        Button b = (Button) e.getSource();
        undo_and_bookmark_and_history.show(b, Side.TOP, 0, 0);
    }

    //**********************************************************
    private void button_preferences(ActionEvent e)
    //**********************************************************
    {
        ContextMenu pref = define_contextmenu_preferences();
        Button b = (Button) e.getSource();
        pref.show(b, Side.TOP, 0, 0);
    }

    //**********************************************************
    private void button_files(ActionEvent e)
    //**********************************************************
    {
        ContextMenu files = define_contextmenu_files();
        Button b = (Button) e.getSource();
        files.show(b, Side.TOP, 0, 0);
    }

    //**********************************************************
    private void button_view(ActionEvent e)
    //**********************************************************
    {
        ContextMenu view = define_contextmenu_view();
        Button b = (Button) e.getSource();
        view.show(b, Side.TOP, 0, 0);
    }

    //**********************************************************
    void on_fullscreen_end()
    //**********************************************************
    {
        // this is called either after the menu above OR if user pressed ESCAPE
        start_full_screen_menu_item.setDisable(false);
        stop_full_screen_menu_item.setDisable(true);
    }

    //**********************************************************
    void on_fullscreen_start()
    //**********************************************************
    {
        start_full_screen_menu_item.setDisable(true);
        stop_full_screen_menu_item.setDisable(false);
    }



    //**********************************************************
    private ContextMenu define_contextmenu_undo_bookmark_history()
    //**********************************************************
    {
        ContextMenu undo_bookmark_history_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(undo_bookmark_history_menu,owner,logger);

        undo_bookmark_history_menu.getItems().add(browser_menus.make_undos_menu());
        undo_bookmark_history_menu.getItems().add(browser_menus.make_bookmarks_menu());
        undo_bookmark_history_menu.getItems().add(browser_menus.make_history_menu());
        undo_bookmark_history_menu.getItems().add(browser_menus.make_roots_menu());
        return undo_bookmark_history_menu;
    }

    //**********************************************************
    private ContextMenu define_contextmenu_view()
    //**********************************************************
    {
        ContextMenu view_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(view_menu,owner,logger);

        Rectangle2D rectangle = new Rectangle2D(owner.getX(),owner.getY(),owner.getWidth(),owner.getHeight());
        view_menu.getItems().add(browser_menus.make_menu_item("New_Window",event -> New_window_context.additional_same_folder(path_list_provider.get_folder_path(),get_top_left(),owner,logger)));
        view_menu.getItems().add(browser_menus.make_menu_item("New_Twin_Window",event -> New_window_context.additional_same_folder_twin(path_list_provider.get_folder_path(),get_top_left(),owner,logger)));
        view_menu.getItems().add(browser_menus.make_menu_item("New_Double_Window",event -> New_window_context.additional_same_folder_fat_tall(path_list_provider.get_folder_path(),get_top_left(),owner,logger)));


        {
            start_full_screen_menu_item = browser_menus.make_menu_item("Go_full_screen",event -> full_screen_handler.go_full_screen());
            start_full_screen_menu_item.setDisable(false);
            view_menu.getItems().add(start_full_screen_menu_item);
        }
        {
            stop_full_screen_menu_item = browser_menus.make_menu_item("Stop_full_screen",event -> full_screen_handler.stop_full_screen());
            stop_full_screen_menu_item.setDisable(true);
            view_menu.getItems().add(stop_full_screen_menu_item);
        }
        {
            String text = My_I18n.get_I18n_string("Scan_show",owner,logger);
            Menu scan = new Menu(text);
            scan.getItems().add(browser_menus.make_menu_item("Start_stop_slow_scan",event -> handle_scan_switch()));
            scan.getItems().add(browser_menus.make_menu_item("Slow_down_scan",event -> slow_down_scan()));
            scan.getItems().add(browser_menus.make_menu_item("Speed_up_scan",event -> speed_up_scan()));
            view_menu.getItems().add(scan);
        }
        view_menu.getItems().add(browser_menus.make_menu_item("Show_How_Many_Files_Are_In_Each_Folder",event -> show_how_many_files_deep_in_each_folder()));
        view_menu.getItems().add(browser_menus.make_menu_item("Show_Each_Folder_Total_Size",event -> show_total_size_deep_in_each_folder()));
        view_menu.getItems().add(browser_menus.make_menu_item("About_klik",event -> About_klik_stage.show_about_klik_stage()));
        view_menu.getItems().add(browser_menus.make_menu_item("Refresh",event -> redraw_fx("refresh")));
        if( !change_events_off) view_menu.getItems().add(browser_menus.make_menu_item("Disable_change_events",event -> change_events_off = true));
        if( change_events_off) view_menu.getItems().add(browser_menus.make_menu_item("Enable_change_events",event -> change_events_off = false));


        view_menu.getItems().add(browser_menus.make_menu_item("Show_Meters",event -> RAM_and_threads_meters_stage.show_stage(owner,logger)));


        if (Feature_cache.get(Feature.Enable_tags))
        {
            view_menu.getItems().add(browser_menus.make_menu_item("Open_tag_management",event -> Tag_items_management_stage.open_tag_management_stage(owner,aborter,logger)));
        }

        return view_menu;
    }

    public boolean change_events_off = false;


    //**********************************************************
    private ContextMenu define_contextmenu_files()
    //**********************************************************
    {
        ContextMenu files_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(files_menu,owner,logger);

        files_menu.getItems().add(browser_menus.make_select_all_files_menu_item(logger));
        files_menu.getItems().add(browser_menus.make_select_all_folders_menu_item(logger));

        {
            String create_string = My_I18n.get_I18n_string("Create",owner,logger);
            Menu create = new Menu(create_string);
            create.getItems().add(browser_menus.make_menu_item("Create_new_empty_directory",event -> create_new_directory()));
            /*if (Feature_cache.get(Feature.Enable_image_playlists))
            {
                logger.log(Stack_trace_getter.get_stack_trace("not implemented"));
                //create.getItems().add(browser_menus.make_menu_item("Create_new_empty_image_playlist",event -> New_window_context.create_new_image_playlist(owner, logger)));
            }*/
            create.getItems().add(browser_menus.make_menu_item("Create_PDF_contact_sheet",event -> create_PDF_contact_sheet()));
            create.getItems().add(browser_menus.make_menu_item("Sort_Files_In_Folders_By_Year",event -> sort_by_year()));
            create.getItems().add(browser_menus.make_import_menu());
            files_menu.getItems().add(create);
        }
        {
            String search_string = My_I18n.get_I18n_string("Search",owner,logger);
            Menu search = new Menu(search_string);
            search.getItems().add(browser_menus.make_menu_item("Search_by_keywords",event -> search_files_by_keyworks_fx()));
            search.getItems().add(browser_menus.make_menu_item("Show_Where_Are_Images",event -> show_where_are_images()));
            search.getItems().add(browser_menus.make_add_to_Enable_face_recognition_training_set_menu_item());


            files_menu.getItems().add(search);
        }
        if (Booleans.get_boolean(Feature.Enable_face_recognition.name(),owner))
        {
            Menu face_recognition = new Menu("Face recognition");
            face_recognition.getItems().add(browser_menus.make_load_face_recog_menu_item());
            face_recognition.getItems().add(browser_menus.make_save_face_recog_menu_item());
            face_recognition.getItems().add(browser_menus.make_reset_face_recog_menu_item());
            face_recognition.getItems().add(browser_menus.make_start_auto_face_recog_menu_item());
            face_recognition.getItems().add(browser_menus.make_start_self_face_recog_menu_item());

            files_menu.getItems().add(face_recognition);
        }
        {
            String cleanup = My_I18n.get_I18n_string("Clean_Up",owner,logger);
            Menu clean = new Menu(cleanup);
            clean.getItems().add(browser_menus.make_remove_empty_folders_menu_item());
            if (Booleans.get_boolean(Feature.Enable_recursive_empty_folders_removal.name(),owner))
            {
                clean.getItems().add(browser_menus.make_menu_item("Remove_empty_folders_recursively", event -> browser_menus.remove_empty_folders_recursively_fx()));
            }
            if (Booleans.get_boolean(Feature.Enable_name_cleaning.name(),owner) )
            {
                clean.getItems().add(browser_menus.make_menu_item("Clean_up_names", event -> browser_menus.clean_up_names_fx()));
            }
            if ( Booleans.get_boolean(Feature.Enable_corrupted_images_removal.name(),owner) )
            {
                clean.getItems().add(browser_menus.make_menu_item("Remove_corrupted_images", event -> browser_menus.remove_corrupted_images_fx()));
            }


            if (Booleans.get_boolean(Feature.Enable_bit_level_deduplication.name(),owner) )
            {
                String txt = My_I18n.get_I18n_string("File_bit_exact_deduplication",owner,logger);
                Menu deduplicate = new Menu(txt);
                deduplicate.getItems().add(create_help_on_deduplication_menu_item());
                deduplicate.getItems().add(create_deduplication_count_menu_item());
                deduplicate.getItems().add(create_manual_deduplication_menu_item());
                deduplicate.getItems().add(create_auto_deduplication_menu_item());
                clean.getItems().add(deduplicate);
            }

            if (Booleans.get_boolean(Feature.Enable_image_similarity.name(),owner) )
            {
                String txt = My_I18n.get_I18n_string("File_ML_similarity_deduplication",owner,logger);
                Menu deduplicate2 = new Menu(txt);

                MenuItem deduplicate_menu_item = create_manual_deduplication_by_similarity_menu_item2();
                deduplicate2.getItems().add(deduplicate_menu_item);

                MenuItem deduplicate_menu_item2 = create_manual_deduplication_by_similarity_menu_item();
                deduplicate2.getItems().add(deduplicate_menu_item2);
                clean.getItems().add(deduplicate2);
            }
            files_menu.getItems().add(clean);
        }

        if (Booleans.get_boolean(Feature.Enable_backup.name(),owner))
        {
            files_menu.getItems().add(browser_menus.make_backup_menu());
        }

        if (Feature_cache.get(Feature.Enable_fusk))
        {
            if (Feature_cache.get(Feature.Fusk_is_on))
            {
                files_menu.getItems().add(browser_menus.make_fusk_menu());
            }
        }
        return files_menu;
    }



    //**********************************************************
    public void import_apple_Photos()
    //**********************************************************
    {
        if (!Popups.popup_ask_for_confirmation( "Importing photos will create COPIES", "Please select a destination drive with enough space", owner,logger))
            return;

        Importer.perform_import(owner, aborter, logger);
    }

    //**********************************************************
    public void estimate_size_of_importing_apple_Photos()
    //**********************************************************
    {
        Importer.estimate_size(owner, aborter, logger);
    }

    //**********************************************************
    public void show_where_are_images()
    //**********************************************************
    {
        Path top = path_list_provider.get_folder_path();
        Folders_with_large_images_locator.locate(top, 10, 200_000, owner, aborter, logger);
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
        List<File> files = path_list_provider.only_files(Feature_cache.get(Feature.Show_hidden_files));
        if (files == null) {
            logger.log("ERROR: cannot list files in " + path_list_provider.get_name());
        }
        if (files.size() == 0) {
            logger.log("WARNING: no file in " + path_list_provider.get_name());
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
            Integer year = (Integer) ldt.getYear();
            Path folder = folders.get(year);
            if (folder == null)
            {
                folder = path_list_provider.resolve( String.valueOf(year));
                try {
                    Files.createDirectory(folder);
                } catch (IOException e) {
                    logger.log("" + e);
                    continue;
                }
            }
            folders.put(year, folder);
            List<Old_and_new_Path> l = new ArrayList<>();
            Path displayed_folder_path = path_list_provider.get_folder_path();
            l.add(new Old_and_new_Path(displayed_folder_path, displayed_folder_path, Command_old_and_new_Path.command_unknown, Status_old_and_new_Path.move_done, false));
            Change_gang.report_changes(l,owner);

            Old_and_new_Path oanp = new Old_and_new_Path(
                    f.toPath(),
                    Path.of(folder.toAbsolutePath().toString(), f.getName()),
                    Command_old_and_new_Path.command_move,
                    Status_old_and_new_Path.before_command, false);
            moves.add(oanp);

        }

        double x = this.owner.getX()+100;
        double y = this.owner.getY()+100;
        Moving_files.perform_safe_moves_in_a_thread(this.owner, x,y, moves, true, aborter, logger);

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
        double x = this.owner.getX()+100;
        double y = this.owner.getY()+100;

        Hourglass hourglass = Show_running_film_frame.show_running_film(owner,x,y,"Making PDF contact sheet",
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
            graphicsMagick_command_line.add(CONTACT_SHEET_FILE_NAME);
        }


        StringBuilder sb = null;
        if ( dbg) sb = new StringBuilder();
        File wd = (path_list_provider.get_folder_path()).toFile();
        if ( Execute_command.execute_command_list(graphicsMagick_command_line, wd, 2000, sb, logger) == null)
        {
            Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
        }
        else
        {
            if ( dbg) logger.log("contact sheet generated "+ sb);
            else
            {
                logger.log("contact sheet generated : "+ CONTACT_SHEET_FILE_NAME);
                System_open_actor.open_with_system(owner,Path.of(path_list_provider.get_folder_path().toAbsolutePath().toString(), CONTACT_SHEET_FILE_NAME),aborter,logger);

                Platform.runLater(() ->set_status("Contact sheet generated : "+ CONTACT_SHEET_FILE_NAME));
            }
        }
        hourglass.close();
    }


    //**********************************************************
    public void create_new_directory()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(My_I18n.get_I18n_string("New_directory", owner,logger));
        Look_and_feel_manager.set_dialog_look(dialog,owner,logger);
        dialog.initOwner(owner);
        dialog.setWidth(owner.getWidth());
        dialog.setTitle(My_I18n.get_I18n_string("New_directory", owner,logger));
        dialog.setHeaderText(My_I18n.get_I18n_string("Enter_name_of_new_directory", owner,logger));
        dialog.setContentText(My_I18n.get_I18n_string("New_directory_name", owner,logger));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();
            for (int i = 0; i < 10; i++)
            {
                try {
                    Path new_dir = path_list_provider.resolve(new_name);
                    Files.createDirectory(new_dir);
                    Browsing_caches.scroll_position_cache_write(path_list_provider.get_folder_path(), new_dir);
                    redraw_fx("created new empty dir");
                    break;
                }
                catch (IOException e)
                {
                    logger.log("new directory creation FAILED: " + e);
                    // n case the issue is the name, we just addd "_" at the end and retry
                    new_name += "_";
                }
            }

        }
    }


    //**********************************************************
    void search_files_by_keyworks_fx()
    //**********************************************************
    {
        List<String> given = new ArrayList<>();
        Image_context.ask_user_and_find(
                path_list_provider,
                this,
                given,
                false,
                owner,
                aborter,
                logger
        );

    }
    //**********************************************************
    private ContextMenu define_contextmenu_preferences()
    //**********************************************************
    {
        ContextMenu pref = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(pref,owner,logger);


        pref.getItems().add(browser_menus.make_file_sort_method_menu());

        pref.getItems().add(browser_menus.make_icon_size_menu());
        if ( Feature_cache.get(Feature.Show_icons_for_folders))
        {
            pref.getItems().add(browser_menus.make_folder_icon_size_menu());
        }
        pref.getItems().add(browser_menus.make_column_width_menu());
        pref.getItems().add(browser_menus.make_font_size_menu_item());
        pref.getItems().add(browser_menus.make_style_menu_item());
        pref.getItems().add(browser_menus.make_language_menu());
        pref.getItems().add(browser_menus.make_video_length_menu());
        //pref.getItems().add(browser_menus.make_ding_menu_item());
        //pref.getItems().add(browser_menus.make_escape_menu_item());
        //pref.getItems().add(browser_menus.make_invert_vertical_scroll_menu_item());
        if (Booleans.get_boolean(Feature.Enable_face_recognition.name(),owner))
        {
            pref.getItems().add(browser_menus.make_start_Enable_face_recognition_menu_item());
        }
        if (Booleans.get_boolean(Feature.Enable_image_similarity.name(),owner))
        {
            pref.getItems().add(browser_menus.make_start_image_similarity_servers_menu_item());
        }

        if (Feature_cache.get(Feature.Enable_fusk))
        {
            pref.getItems().add(browser_menus.make_fusk_check_menu_item());
        }
        pref.getItems().add(browser_menus.make_cache_size_limit_warning_menu_item());
        if ( Feature_cache.get(Feature.max_RAM_is_defined_by_user)) {
            pref.getItems().add(browser_menus.make_max_RAM_menu_item());
        }



        pref.getItems().add(browser_menus.make_menu_item(
                "Clear_Trash_Folder",
                event -> Static_files_and_paths_utilities.clear_trash(true,owner, aborter, logger)));

        pref.getItems().add(browser_menus.make_clear_all_caches_menu_item());
        if (Booleans.get_boolean(Feature.Enable_detailed_cache_cleaning_options.name(),owner))
        {

            Menu cleanup = new Menu(My_I18n.get_I18n_string("Cache_cleaning",owner,logger));
            pref.getItems().add(cleanup);
            {
                Menu ram = new Menu(My_I18n.get_I18n_string("RAM_Caches_Cleaming",owner,logger));
                cleanup.getItems().add(ram);
                ram.getItems().add(browser_menus.make_menu_item("Clear_All_RAM_Caches",
                        event -> clear_all_RAM_caches()));
                ram.getItems().add(browser_menus.make_menu_item("Clear_Image_Properties_RAM_Cache",
                        event -> clear_image_properties_RAM_cache()));
                ram.getItems().add(browser_menus.make_menu_item("Clear_Image_Comparators_Caches",
                        event -> clear_image_comparators_caches()));
                ram.getItems().add(browser_menus.make_menu_item("Clear_Scroll_Position_Cache",
                        event ->         Browsing_caches.scroll_position_cache_clear()));

            }
            {
                Menu disk = new Menu(My_I18n.get_I18n_string("DISK_Caches_Cleaning",owner,logger));
                cleanup.getItems().add(disk);

                disk.getItems().add(browser_menus.make_menu_item(
                        "Clear_All_Disk_Caches",
                        event -> Static_files_and_paths_utilities.clear_all_DISK_caches(owner,aborter,logger)));
                disk.getItems().add(browser_menus.make_menu_item(
                        "Clear_Icon_Cache_On_Disk",
                        event -> Static_files_and_paths_utilities.clear_DISK_cache(Cache_folder.klik_icon_cache,true,owner,aborter,logger)));

                disk.getItems().add(browser_menus.make_menu_item(
                        "Clear_Folders_Icon_Cache_Folder",
                        event -> Static_files_and_paths_utilities.clear_DISK_cache(Cache_folder.klik_folder_icon_cache,false,owner, aborter, logger)));
                disk.getItems().add(browser_menus.make_menu_item(
                        "Clear_Image_Properties_DISK_Cache",
                        event -> Static_files_and_paths_utilities.clear_DISK_cache(Cache_folder.klik_image_properties_cache,false,owner,aborter, logger)));

                disk.getItems().add(browser_menus.make_menu_item("Clear_Image_Feature_Vector_DISK_Cache",
                        event -> Static_files_and_paths_utilities.clear_DISK_cache(Cache_folder.klik_image_feature_vectors_cache,true,owner, aborter, logger)));

                disk.getItems().add(browser_menus.make_menu_item("Clear_Image_Similarity_DISK_Cache",
                        event -> Static_files_and_paths_utilities.clear_DISK_cache(Cache_folder.klik_image_similarity_cache,true,owner, aborter, logger)));

            }

        }
        pref.getItems().add(browser_menus.get_advanced_preferences());


        return pref;
    }



    //**********************************************************
    private MenuItem create_auto_deduplication_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_auto",owner,logger);
        MenuItem menu_item = new MenuItem(text);
        menu_item.setOnAction(event -> {
            //logger.log("Deduplicate auto");

            if ( !Popups.popup_ask_for_confirmation( "EXPERIMENTAL! Are you sure?","Automated deduplication will recurse down this folder and delete (for good = not send them in recycle bin) all duplicate files",owner,logger)) return;
            (new Deduplication_engine(owner, (path_list_provider.get_folder_path()).toFile(), path_list_provider,this,logger)).do_your_job(true);
        });
        return menu_item;
    }



    //**********************************************************
    public Supplier<Image_feature_vector_cache> get_fv_cache = new Supplier<>()
    //**********************************************************
    {
        public Image_feature_vector_cache get() {

            Image_feature_vector_cache.Images_and_feature_vectors local =
                    Image_feature_vector_cache.preload_all_feature_vector_in_cache(path_list_provider, owner,owner.getX()+100, owner.getY()+100, aborter, logger);
            return local.fv_cache();
        }
    };

    //**********************************************************
    private MenuItem create_manual_deduplication_by_similarity_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_with_confirmation_images_looking_a_bit_the_same",owner,logger);

        MenuItem item0 = new MenuItem(text);
        item0.setOnAction(event -> {
            //logger.log("Deduplicate manually");
            (new Deduplication_by_similarity_engine(
                    path_list_provider,
                    this,
                    false,
                    owner,
                    path_list_provider.get_folder_path().toFile(),
                    browsing_caches.image_properties_RAM_cache,
                    get_fv_cache,
                    logger)).do_your_job();
        });
        return item0;
    }


    //**********************************************************
    private MenuItem create_manual_deduplication_by_similarity_menu_item2()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_with_confirmation_quasi_similar_images",owner,logger);

        MenuItem item0 = new MenuItem(text);
        item0.setOnAction(event -> {
            //logger.log("Deduplicate manually");
            (new Deduplication_by_similarity_engine(
                    path_list_provider,
                    this,
                    true,
                    owner,
                    path_list_provider.get_folder_path().toFile(),
                    browsing_caches.image_properties_RAM_cache,
                    get_fv_cache,
                    
                    logger)).do_your_job();
        });
        return item0;
    }

    //**********************************************************
    private MenuItem create_manual_deduplication_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_manual",owner,logger);

        MenuItem item0 = new MenuItem(text);
        item0.setOnAction(event -> {
            //logger.log("Deduplicate manually");
            (new Deduplication_engine(owner, path_list_provider.get_folder_path().toFile(), path_list_provider,this,logger)).do_your_job(false);
        });
        return item0;
    }

    //**********************************************************
    private MenuItem create_deduplication_count_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_count",owner,logger);
        MenuItem item0 = new MenuItem(text);
        item0.setOnAction(event -> {
            //logger.log("count duplicates!");
            (new Deduplication_engine(owner, path_list_provider.get_folder_path().toFile(), path_list_provider,this,logger)).count(false);
        });
        return item0;
    }


    //**********************************************************
    private MenuItem create_help_on_deduplication_menu_item()
    //**********************************************************
    {
        String text = My_I18n.get_I18n_string("Deduplicate_help",owner,logger);
        MenuItem itemhelp = new MenuItem(text);
        itemhelp.setOnAction(event -> Popups.popup_warning(
                "Help on deduplication",
                "The deduplication tool will look recursively down the path starting at:" + path_list_provider.get_name() +
                        "\nLooking for identical files in terms of file content i.e. names/path are different but it IS the same file" +
                        " Then you will be able to either:" +
                        "\n  1. Review each pair of duplicate files one by one" +
                        "\n  2. Or ask for automated deduplication (DANGER!)" +
                        "\n  Beware: automated de-duplication may give unexpected results" +
                        " since you do not choose which file in the pair is deleted." +
                        "\n  However, the files are not actually deleted: they are MOVED to the klik_trash folder," +
                        " which you can visit by clicking on the trash button." +
                        "\n\n WARNING: On folders containing a lot of data, the search can take a long time!",
                false,
                owner,logger));
        return itemhelp;
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

    //**********************************************************
    @Override // Feature_change_target
    public void update(Feature feature, boolean new_val)
    //**********************************************************
    {
        redraw_fx("the Feature ->"+feature+"<- has new value:"+new_val);
    }


    //**********************************************************
    enum Scan_state
    //**********************************************************
    {
        off,
        down,
        up
    }

    private Scan_state scan_state = Scan_state.off;
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
            Popups.popup_warning( "Cannot backup!", "Reason: no backup ORIGIN", false, owner,logger);
            return;
        }
        Path backup_destination = Static_backup_paths.get_backup_destination();
        if (backup_destination == null) {
            logger.log("FATAL, no backup destination");
            Popups.popup_warning( "Cannot backup!", "Reason: no backup DESTINATION", false, owner,logger);

            return;
        }
        Backup_singleton.set_source(backup_source, logger);
        Backup_singleton.set_destination(backup_destination, logger);
        Backup_singleton.start_the_backup(owner);

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
            Popups.popup_warning("Cannot fusk!", "Reason: no fusk ORIGIN", false, owner,logger);
            return;
        }
        Path fusk_destination = Static_fusk_paths.get_fusk_destination();
        if (fusk_destination == null) {
            logger.log("FATAL, no fusk destination");
            Popups.popup_warning( "Cannot fusk!", "Reason: no fusk DESTINATION", false, owner,logger);

            return;
        }
        Fusk_singleton.set_source(fusk_source, aborter, logger);
        Fusk_singleton.set_destination(fusk_destination, aborter, logger);
        Fusk_singleton.start_fusk();

    }

    //**********************************************************
    public void start_defusk()
    //**********************************************************
    {
        logger.log("start defusk");
        Path defusk_source = Static_fusk_paths.get_fusk_source();
        if (defusk_source == null) {
            logger.log("FATAL, no defusk source");
            Popups.popup_warning( "Cannot defusk!", "Reason: no defusk SOURCE", false, owner,logger);
            return;
        }
        Path defusk_destination = Static_fusk_paths.get_fusk_destination();
        if (defusk_destination == null) {
            logger.log("FATAL, no defusk destination");
            Popups.popup_warning( "Cannot defusk!", "Reason: no defusk DESTINATION", false, owner,logger);

            return;
        }
        Fusk_singleton.set_source(defusk_source, aborter, logger);
        Fusk_singleton.set_destination(defusk_destination, aborter, logger);
        Fusk_singleton.start_defusk();

    }


    //**********************************************************
    public void you_are_backup_destination()
    //**********************************************************
    {
        Static_backup_paths.set_backup_destination(path_list_provider.get_folder_path());
        logger.log("backup destination = " + path_list_provider.get_name());

        set_text_background("BACKUP\nDESTINATION");

    }

    //**********************************************************
    public void you_are_backup_source()
    //**********************************************************
    {
        Static_backup_paths.set_backup_source(path_list_provider.get_folder_path());
        logger.log("backup source = " + path_list_provider.get_name());

        set_text_background("BACKUP\nSOURCE");

    }

    //**********************************************************
    public void you_are_fusk_destination()
    //**********************************************************
    {
        Static_fusk_paths.set_fusk_destination(path_list_provider.get_folder_path());
        logger.log("fusk destination = " + path_list_provider.get_name());

        set_text_background("FUSK\nDESTINATION");

    }

    //**********************************************************
    public void you_are_fusk_source()
    //**********************************************************
    {
        Static_fusk_paths.set_fusk_source(path_list_provider.get_folder_path());
        logger.log("fusk source = " + path_list_provider.get_name());

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
        Fusk_bytes.initialize(logger);
    }








    // redrawing engine: in its own thread


    //**********************************************************
    public void redraw_fx(String from)
    //**********************************************************
    {
        if (dbg) logger.log("Browser redraw from:" + from );

        try
        {
            request_queue.put(Boolean.TRUE);
        } catch (InterruptedException e) {
            logger.log(""+e);
        }
    }


    //**********************************************************
    private void start_redraw_engine(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            for(;;)
            {
                try {
                    Boolean b = request_queue.poll(3, TimeUnit.SECONDS);
                    if (b != null) redraw_all_internal(owner, owner.getX(), owner.getY());
                    if (aborter.should_abort()) return;

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Actor_engine.execute(r, logger);
    }

    private final AtomicBoolean the_guard = new AtomicBoolean(false);


    //**********************************************************
    private void redraw_all_internal(Window owner, double x, double y)
    //**********************************************************
    {
        if ( the_guard.get())
        {
            logger.log("\n\n redraw request ignored, guard is set!\n\n");
            return;
        }
        the_guard.set(true);
        aborter.add_on_abort(() -> the_guard.set(false));

        long start = System.currentTimeMillis();

        Hourglass running_film = null;

        if ( show_running_film) {
            running_film = Show_running_film_frame.show_running_film(owner, x, y, "Scanning folder", 20 * 60, aborter, logger);
        }

        set_comparators(x+100,y+200);

        //the_max_dir_text_length = 0;
        all_items_map.clear();
        paths_holder.iconized_paths.clear();
        paths_holder.non_iconized.clear();
        paths_holder.folders.clear();
        iconized_sorted_queue.clear();

        browsing_caches.image_properties_RAM_cache.reload_cache_from_disk();
        scan_list();

        all_image_properties_acquired_4(start, running_film);

    }


    //**********************************************************
    private void set_comparators(double x, double y)
    //**********************************************************
    {
        //logger.log("Virtual_landscape: set_comparators");


        Alphabetical_file_name_comparator alphabetical_file_name_comparator = new Alphabetical_file_name_comparator();

        other_file_comparator = File_sort_by.get_non_image_comparator(path_list_provider, owner,logger);

        image_file_comparator = File_sort_by.get_image_comparator(path_list_provider, this,browsing_caches.image_properties_RAM_cache,
                owner,x,y, aborter,logger);;

        // these MUST be mutually exclusive:
        paths_holder.folders = new ConcurrentSkipListMap<>(alphabetical_file_name_comparator);
        paths_holder.non_iconized = new ConcurrentSkipListMap<>(other_file_comparator);
    }


    //**********************************************************
    private void scan_list()
    //**********************************************************
    {


        if ( dbg)
        {
            logger.log("Virtual_landscape: scan_list");
            if (Platform.isFxApplicationThread()) logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN: scanning disk on javafx thread"));
        }

        try
        {
            //File files[] = the_displayed_folder_path.toFile().listFiles();

            //for ( File f : files)

            for ( Path path : path_list_provider.only_folder_paths(Feature_cache.get(Feature.Show_hidden_folders)))
            {
                if (dbg) logger.log("Virtual_landscape: looking at path " + path.toAbsolutePath());

                if (aborter.should_abort()) {
                    logger.log("path manager aborting (1) scan_list "+ path_list_provider.get_folder_path().toAbsolutePath());
                    aborter.on_abort();
                    return;
                }

                paths_holder.do_folder(path);
            }
            for ( Path path : path_list_provider.only_file_paths(Feature_cache.get(Feature.Show_hidden_files)))
            {
                if (dbg) logger.log("Virtual_landscape: looking at path " + path.toAbsolutePath());

                if (aborter.should_abort()) {
                    logger.log("path manager aborting (2) scan_list "+ path_list_provider.get_folder_path().toAbsolutePath());
                    aborter.on_abort();
                    return;
                }
                paths_holder.do_file( path, Feature_cache.get(Feature.Show_icons_for_files), owner);
                // this will start one virtual thread per image to prefill the image property cache
            }
        }
        catch (InvalidPathException e)
        {
            logger.log("Browsing error: "+e);
            receive_error(Error_type.NOT_FOUND);
        }
        catch (SecurityException e)
        {
            logger.log("Browsing error: "+e);
            receive_error(Error_type.DENIED);
        }
        catch (Exception e)
        {
            logger.log("Browsing error: "+e);
            receive_error(Error_type.ERROR);
        }

        if ( dbg) logger.log("Virtual_landscape: scan_list ends");

    }


    //**********************************************************
    private void all_image_properties_acquired_4(long start, Hourglass running_film)
    //**********************************************************
    {
        if ( dbg) logger.log("Virtual_landscape::all_image_properties_acquired_4() ");
        Runnable r = () -> browsing_caches.image_properties_RAM_cache.save_whole_cache_to_disk();
        Actor_engine.execute(r,logger);

        if (System.currentTimeMillis() - start > 5_000) {
            if (Booleans.get_boolean(Feature.Play_ding_after_long_processes.name(),owner)) {
                Ding.play("all_image_properties_acquired: done acquiring all image properties", logger);
            }
        }
        get_path_comparator();
        //logger.log("all_image_properties_acquired, going to refresh");
        refresh_UI("all_image_properties_acquired", running_film);

        if ( dbg) logger.log("Virtual_landscape::all_image_properties_acquired_4() done");

    }

    //**********************************************************
    private void refresh_UI(String from, Hourglass running_film)
    //**********************************************************
    {
        sort_iconized_items(from);

        Runnable r = () -> {
            //logger.log("refresh_UI_after_scan_dir " + from);
            refresh_UI_on_fx_thread( from,running_film);
        };
        Jfx_batch_injector.inject(r, logger);

    }


    //**********************************************************
    private void refresh_UI_on_fx_thread(String from, Hourglass running_film)
    //**********************************************************
    {

        if ( dbg) logger.log("refresh_UI_on_fx_thread from: " + from);

        compute_geometry("scene_geometry_changed from: " + from, running_film);

        if (dbg) logger.log("adapt_slider_to_scene");

        {
            vertical_slider.adapt_slider_to_scene(owner);
        }

        title_target.set_title();

        {
            double title_height = owner.getHeight() - the_Scene.getHeight();
            if (title_height > 60)
            {
                logger.log("WARNING: " +
                        "title_height>60 \nowner.getHeight()=" +
                        owner.getHeight() + "\nthe_Scene.getHeight()=" + the_Scene.getHeight());
            }
            else
            {
                for (Button b : top_buttons) {
                    b.setMinHeight(title_height);
                }
            }
        }
    }

    //**********************************************************
    public void compute_geometry(String reason, Hourglass running_film)
    //**********************************************************
    {

        if ( dbg) logger.log("\ncompute_geometry reason="+reason+" current_vertical_offset="+current_vertical_offset);
        if (scroll_dbg) logger.log(("geometry_changed single_column="+ Feature_cache.get(Feature.Show_single_column)));

        if ( dbg) logger.log("Virtual_landscape map_buttons_and_icons");

        double row_increment_for_dirs = 2 * Non_booleans.get_font_size(owner,logger);
        int folder_icon_size = Non_booleans.get_folder_icon_size(owner);
        int column_increment_for_folders = Non_booleans.get_column_width(owner);
        if ( column_increment_for_folders < folder_icon_size) column_increment_for_folders = folder_icon_size;

        int icon_size = Non_booleans.get_icon_size(owner);
        int column_increment_for_icons = icon_size;

        if (Feature_cache.get(Feature.Show_single_column))
        {
            // the -100 is to make the button shorter than the full width so that
            // the mouse selection can "start" in the rightmost part of the pane
            column_increment_for_icons = (int)(the_Scene.getWidth()-RIGHT_SIDE_SINGLE_COLUMN_MARGIN);
            column_increment_for_folders = column_increment_for_icons;
        }
        double row_increment_for_dirs_with_picture = row_increment_for_dirs + folder_icon_size;

        double scene_width = the_Scene.getWidth();

        double top_delta_y = 2 * Non_booleans.get_font_size(owner,logger);
        if (error_type == Error_type.DENIED) {
            ImageView iv_denied = new ImageView(Look_and_feel_manager.get_denied_icon(icon_size,owner,logger));
            show_error_icon(iv_denied,top_delta_y);
            if ( running_film != null) running_film.close();
            the_guard.set(false);
            logger.log("on DENIED the_guard =>"+the_guard.get()+" for "+path_list_provider.get_name());
            return;
        }
        if (error_type == Error_type.NOT_FOUND) {
            ImageView not_found = new ImageView(Look_and_feel_manager.get_not_found_icon(icon_size,owner,logger));
            show_error_icon(not_found,top_delta_y);
            if ( running_film != null) running_film.close();
            the_guard.set(false);
            logger.log("on NOT_FOUND the_guard =>"+the_guard.get()+" for "+path_list_provider.get_name());
            return;
        }
        if (error_type == Error_type.ERROR) {
            ImageView unknown_error = new ImageView(Look_and_feel_manager.get_unknown_error_icon(icon_size,owner,logger));
            show_error_icon(unknown_error,top_delta_y);
            if ( running_film != null) running_film.close();
            the_guard.set(false);
            logger.log("ON ERROR map_buttons_and_icons_guard =>"+the_guard.get()+" for "+path_list_provider.get_name());
            return;
        }


        the_Pane.getChildren().clear();
        // now we can be in a thread

        int final_column_increment_for_folders = column_increment_for_folders;
        int final_column_increment_for_icons = column_increment_for_icons;
        Runnable r = () -> {
            items_are_ready.set(false);
            future_pane_content.clear();
            how_many_rows = 0;

            if ( dbg) logger.log("on javafx thread?  "+Platform.isFxApplicationThread());

            Point2D p = new Point2D(0, 0);

            long start = System.currentTimeMillis();
            p = process_folders(Feature_cache.get(Feature.Show_single_column), row_increment_for_dirs, final_column_increment_for_folders, row_increment_for_dirs_with_picture, scene_width, p);
            if ( dbg) logger.log("process_folders took "+(System.currentTimeMillis()-start)+" ms");
            p = new Point2D(p.getX(),p.getY()+MARGIN_Y);
            p = process_non_iconized_files(Feature_cache.get(Feature.Show_single_column), final_column_increment_for_folders, scene_width, p);
            p = new Point2D(p.getX(),p.getY()+MARGIN_Y);
            start = System.currentTimeMillis();
            process_iconized_items(Feature_cache.get(Feature.Show_single_column), icon_size, final_column_increment_for_icons, scene_width, p);
            if ( dbg) logger.log("process_iconized_items took "+(System.currentTimeMillis()-start)+" ms");

            start = System.currentTimeMillis();
            compute_bounding_rectangle("map_buttons_and_icons() OK "+p.getX()+" "+p.getY());
            if ( dbg) logger.log("compute_bounding_rectangle took "+(System.currentTimeMillis()-start)+" ms");

            if ( dbg) logger.log("Going to remap all items");
            future_pane_content.addAll(all_items_map.values());

            items_are_ready.set(true);
            the_guard.set(false);
            if ( dbg) logger.log("END, the_guard => "+the_guard.get()+" for "+path_list_provider.get_name());

            Jfx_batch_injector.inject(()->
            {
                if ( running_film != null) running_film.close();

                for (Item item : future_pane_content)
                {
                    if (item.visible_in_scene.get())
                    {
                        if (!the_Pane.getChildren().contains(item.get_Node()))
                        {
                            the_Pane.getChildren().add(item.get_Node());
                        }
                    }
                }
                scroll_to();

                set_visibility_on_fx_thread(reason+" map_buttons_and_icons ");

            },logger);
        };
        Actor_engine.execute(r,logger);


    }



    record File_comp_cache(File_sort_by file_sort_by, Comparator<Path> comparator){}

    private File_comp_cache file_comp_cache;

    //**********************************************************
    public Comparator<Path> get_path_comparator()
    //**********************************************************
    {
        Comparator<Path> local_file_comparator = image_file_comparator;

        if ( local_file_comparator == null) {
            if (file_comp_cache != null) {
                if (file_comp_cache.file_sort_by() == File_sort_by.get_sort_files_by(path_list_provider.get_folder_path(),owner)) {
                    if (dbg) logger.log("getting file comparator from cache=" + file_comp_cache);
                    local_file_comparator = file_comp_cache.comparator();
                }
            }
        }
        if ( local_file_comparator == null)
        {
            local_file_comparator = create_fast_file_comparator();
        }
        if ( local_file_comparator == null)
        {
            logger.log("FATAL: local_file_comparator is null");
        }
        else
        {
            //logger.log("setting file_comp_cache ="+file_comp_cache);
            file_comp_cache =  new File_comp_cache(File_sort_by.get_sort_files_by(path_list_provider.get_folder_path(),owner),local_file_comparator);
            set_new_iconized_items_comparator(local_file_comparator);
        }

        return local_file_comparator;
    }

    //**********************************************************
    private Comparator<Path> create_fast_file_comparator()
    //**********************************************************
    {

        Comparator<Path> local_file_comparator = null;
        switch (File_sort_by.get_sort_files_by(path_list_provider.get_folder_path(),owner))
        {
            case File_sort_by.ASPECT_RATIO -> local_file_comparator = new Aspect_ratio_comparator(browsing_caches.image_properties_RAM_cache);
            case File_sort_by.RANDOM_ASPECT_RATIO -> local_file_comparator = new Aspect_ratio_comparator_random(browsing_caches.image_properties_RAM_cache);
            case File_sort_by.IMAGE_WIDTH -> local_file_comparator = new Image_width_comparator(browsing_caches.image_properties_RAM_cache);
            case File_sort_by.IMAGE_HEIGHT -> local_file_comparator = new Image_height_comparator(browsing_caches.image_properties_RAM_cache,logger);
            case File_sort_by.RANDOM -> local_file_comparator = new Random_comparator();
            case File_sort_by.DATE -> local_file_comparator = new Date_comparator(logger);
            case File_sort_by.SIZE -> local_file_comparator = new Decreasing_disk_footprint_comparator();

            default -> local_file_comparator = new Alphabetical_file_name_comparator();
        }
        return local_file_comparator;
    }



    //**********************************************************
    private void set_new_iconized_items_comparator(Comparator<Path> local_file_comparator)
    //**********************************************************
    {
        image_file_comparator = local_file_comparator;
    }


}
