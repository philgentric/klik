// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Landscape_height_listener.java
//SOURCES ./Scroll_to_listener.java
//SOURCES ../icons/image_properties_cache/Image_properties.java
//SOURCES ../../look/Look_and_feel.java
//SOURCES ../items/Item_folder_with_icon.java
//SOURCES ../items/My_colors.java
//SOURCES ../classic/Path_list_provider_for_file_system.java
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
import klik.*;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.execute.actor.Job_termination_reporter;
import klik.browser.*;
import klik.browser.classic.Browser;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.browser.comparators.*;
import klik.browser.icons.Error_type;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.items.*;
import klik.util.execute.ram_and_threads_meter.RAM_and_threads_meters_stage;
import klik.change.Change_receiver;
import klik.experimental.backup.Backup_singleton;
import klik.experimental.fusk.Fusk_bytes;
import klik.experimental.fusk.Fusk_singleton;
import klik.experimental.fusk.Static_fusk_paths;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.look.Font_size;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.machine_learning.feature_vector.Feature_vector_source;
import klik.machine_learning.image_similarity.Feature_vector_source_for_image_similarity;
import klik.path_lists.Path_list_provider;
import klik.properties.*;
import klik.properties.boolean_features.*;
import klik.util.files_and_paths.*;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.perf.Perf;
import klik.util.ui.*;
import klik.util.ui.progress.Hourglass;
import klik.util.ui.progress.Progress_window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
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


    Map<LocalDateTime,String> the_whole_history;


    public final Aborter aborter;
    public final Logger logger;
    public final Window_type context_type;
    private Landscape_height_listener landscape_height_listener;
    private Scroll_to_listener scroll_to_listener;
    final Paths_holder paths_holder;


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
    public Virtual_landscape_menus virtual_landscape_menus;
    MenuItem stop_full_screen_menu_item;
    MenuItem start_full_screen_menu_item;
    public List<Button> top_buttons = new ArrayList<>();

    TextField status;
    public final Shutdown_target shutdown_target;
    private final Title_target title_target;
    private final Full_screen_handler full_screen_handler;

    public static boolean show_progress_window_on_redraw = true;

    public final Browsing_caches browsing_caches;
    private Feature_vector_cache fv_cache;
    private final Background_provider background_provider;
    //**********************************************************
    public Virtual_landscape(
            Window_type context_type,
            Path_list_provider path_list_provider,
            Window owner,
            Shutdown_target shutdown_target,
            Change_receiver change_receiver,
            Title_target title_target,
            Full_screen_handler full_screen_handler,
            Background_provider background_provider,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        this.context_type = context_type;
        this.background_provider = background_provider;
        this.full_screen_handler = full_screen_handler;
        this.title_target = title_target;
        this.shutdown_target = shutdown_target;
        this.path_list_provider = path_list_provider;
        error_type = Error_type.OK;
        this.owner = owner;
        this.aborter = aborter;
        this.logger = logger;

        Feature_cache.register_for_all(this);
        Feature_cache.string_register_for(Non_booleans_properties.LANGUAGE_KEY,this);
        Feature_cache.string_register_for(Non_booleans_properties.STYLE_KEY,this);

        the_Pane = new Pane();


        browsing_caches = new Browsing_caches(path_list_provider,owner,aborter,logger);
        icon_factory_actor = new Icon_factory_actor(browsing_caches.image_properties_RAM_cache, owner, aborter, logger);
        paths_holder = new Paths_holder(browsing_caches.image_properties_RAM_cache, aborter, logger);
        selection_handler = new Selection_handler(the_Pane, this, this, logger);

        virtual_landscape_menus = new Virtual_landscape_menus(this, change_receiver, owner);
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


        double font_size = Non_booleans_properties.get_font_size(owner,logger);
        icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;
        start_redraw_engine(owner, aborter, logger);
    }

    //**********************************************************
    @Override // String_change_target
    public void update_config_string(String key, String new_value)
    //**********************************************************
    {
        logger.log("virtual_landscape receiving update_config_string key="+key+" val="+new_value);
        if ( key.equals(Non_booleans_properties.LANGUAGE_KEY))
        {
            Instructions.replace_same_folder(shutdown_target, context_type,path_list_provider,get_top_left(),owner,logger);
        }
        else if ( key.equals(Non_booleans_properties.STYLE_KEY))
        {
            Instructions.replace_same_folder(shutdown_target, context_type,path_list_provider,get_top_left(),owner,logger);
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
                //if (Browser.keyboard_dbg)
                logger.log("character is k = keyword search");
                logger.log("character is k = keyword search");
                virtual_landscape_menus.search_files_by_keyworks_fx();
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
                //Instructions.additional_same_folder(local, logger);
            }
        });

        {
            // force pin classes so native image does not strip them
            Class<?> a = javafx.scene.input.Dragboard.class;
            Class<?> b = javafx.scene.input.ClipboardContent.class;
            Class<?> c = javafx.scene.input.DataFormat.class;
        }
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
            int n = Drag_and_drop.accept_drag_dropped_as_a_move_in(path_list_provider.get_move_provider(),drag_event, displayed_folder_path, the_Pane, "browser of dir: " + displayed_folder_path, false, owner, logger);
            set_status(n + " files have been dropped in");
            selection_handler.on_drop();
            drag_event.setDropCompleted(true);
            drag_event.consume();
        });

        the_Scene.setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("Browser: OnDragExited handler called");
            if (dbg) logger.log("OnDragExited in browser for dir :" + displayed_folder_path);
            //set_status(" drag done");
            virtual_landscape_menus.reset_all_files_and_folders();
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
        int new_icon_size = (int) (Non_booleans_properties.get_icon_size(owner) * fac);
        if (new_icon_size < 20) new_icon_size = 20;
        if ( Browser.keyboard_dbg) logger.log("new icon size = "+new_icon_size);
        Non_booleans_properties.set_icon_size(new_icon_size,owner);
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



    //**********************************************************
    public void set_text_background(String text)
    //**********************************************************
    {

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

        double file_button_height = 2 * Non_booleans_properties.get_font_size(owner,logger);

        double max_y_in_row[] = new double[1];
        max_y_in_row[0] = 0;
        List<Item> current_row = new ArrayList<>();


        // first compute how many images are in flight
        int image_properties_in_flight = 0;
        //boolean show_icons_for_files = Feature_cache.get(Feature.Show_icons_for_files);
        //boolean show_single_column = Feature_cache.get(Feature.Show_single_column);
        //if ( show_single_column) show_icons_for_files = false;
        for (Path path : paths_holder.iconized_paths )
        {
            Item item;
            //if (show_icons_for_files)
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
            //if (show_icons_for_files)
            {
                item = all_items_map.get(path);
                if (item == null)
                {
                    wait_for_end.countDown();
                    // ask for image properties fetch in threads
                    browsing_caches.image_properties_RAM_cache.get_from_cache(path,tr);
                }
            }
            /*else
            {
                // this is an item that could have an image but the user prefers not
                String size = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(path.toFile().length(), owner,logger);
                item = all_items_map.get(path);
                if (item == null) {
                    //if ( dbg)
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
            }*/
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
        if ( dbg) logger.log("✅ getting image properties took " + (System.currentTimeMillis() - start) + " milliseconds");






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
                if ( dbg) logger.log(("✅ Warning: image property cache miss for: "+path));
            }
            else
            {
                cache_aspect_ratio = (Double) ip.get_aspect_ratio();
            }
            getting_image_properties_from_cache += System.currentTimeMillis() - local_incr;
            Supplier<Feature_vector_cache> fv_cache_supplier = () ->
            {
                if ( fv_cache != null) return fv_cache;
                double x = owner.getX()+100;
                double y = owner.getY()+230;
                Feature_vector_source fvs = new Feature_vector_source_for_image_similarity(aborter);
                List<Path> paths = path_list_provider.only_image_paths(Feature_cache.get(Feature.Show_hidden_files));
                Feature_vector_cache.Paths_and_feature_vectors images_and_feature_vectors =
                        Feature_vector_cache.preload_all_feature_vector_in_cache(fvs, paths,path_list_provider,  owner,x,  y,  aborter,  logger);
                fv_cache= images_and_feature_vectors.fv_cache();
                return fv_cache;
            };

            Item item = new Item_file_with_icon(
                    the_Scene,
                    selection_handler,
                    icon_factory_actor,
                    null,
                    cache_aspect_ratio,
                    //browsing_caches.image_properties_RAM_cache,
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
            logger.log("✅ making iconized items took " + (System.currentTimeMillis() - start) + " milliseconds");
            logger.log("     ,of which getting_image_properties_from_cache= " +getting_image_properties_from_cache+ " milliseconds");
        }


        boolean show_icons_for_files = Feature_cache.get(Feature.Show_icons_for_files);
        boolean show_single_column = Feature_cache.get(Feature.Show_single_column);
        if ( show_single_column) show_icons_for_files = false;

        /// at this stage we MUST have get_iconized_sorted() in the proper order
        // that will define the x,y layout
        start = System.currentTimeMillis();
        List<Path> ll = get_iconized_sorted("process_iconified_items");
        for (Path path : ll )
        {
            Item item = all_items_map.get(path);
            if ( item == null)
            {
                logger.log(("❌ should not happen: no item in map for: "+path+" map size="+all_items_map.size() ));
                continue;
            }
            if (dbg)  logger.log("✅  Virtual_landscape process_iconified_items " + path+" ar:"+((Item_file_with_icon)item).aspect_ratio);

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
        if ( dbg) logger.log("✅  mapping iconized items took " + (System.currentTimeMillis() - start) + " milliseconds");

    }

    //**********************************************************
    private Point2D process_non_iconized_files(boolean single_column, double column_increment, double scene_width, Point2D p)
    //**********************************************************
    {
        // manage the non-iconifed-files section
        double row_increment_for_files = 2 * Non_booleans_properties.get_font_size(owner,logger);

        for (Path path : paths_holder.non_iconized)
        {
            if (ultra_dbg) logger.log("✅ Virtual_landscape process_non_iconized_files "+path.toAbsolutePath());
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

    //**********************************************************
    private Point2D process_folders( boolean single_column, double row_increment_for_dirs, double column_increment, double row_increment_for_dirs_with_picture, double scene_width, Point2D p)
    //**********************************************************
    {
        if (dbg) logger.log("✅ Virtual_landscape process_folders (0) ");
        try (Perf perf = new Perf("4. process_folders")) {

            double actual_row_increment;
            if (Feature_cache.get(Feature.Show_icons_for_folders)) {
                actual_row_increment = row_increment_for_dirs_with_picture;

                for (Path folder_path : paths_holder.folders) {
                    if (dbg) logger.log("✅ Virtual_landscape process_folders (1) " + folder_path);
                    p = process_one_folder_with_picture(single_column, column_increment, actual_row_increment, scene_width, p, folder_path, Color.BEIGE);
                }
            } else {
                actual_row_increment = row_increment_for_dirs;
                List<Path> paths = new ArrayList<>(paths_holder.folders);
                if (show_total_size_deep_in_each_folder_done) {
                    Comparator<Path> comp = (p1, p2) -> {
                        Long l1 = folder_total_sizes_cache.get(p1);
                        Long l2 = folder_total_sizes_cache.get(p2);
                        if (l1 == null && l2 == null) return 0;
                        if (l1 == null) return 1;
                        if (l2 == null) return -1;
                        return l2.compareTo(l1);
                    };
                    Collections.sort(paths, comp);
                } else if (show_how_many_files_deep_in_each_folder_done) {
                    Comparator<Path> comp = (p1, p2) -> {
                        Long l1 = folder_file_count_cache.get(p1);
                        Long l2 = folder_file_count_cache.get(p2);
                        if (l1 == null && l2 == null) return 0;
                        if (l1 == null) return 1;
                        if (l2 == null) return -1;
                        return l2.compareTo(l1);
                    };
                    Collections.sort(paths, comp);
                }
                if (dbg) logger.log("✅ Virtual_landscape folder_path size " + paths.size());
                for (Path folder_path : paths) {
                    if (dbg) logger.log("✅ Virtual_landscape process_folders (3) " + folder_path);
                    p = process_one_folder_plain(single_column, column_increment, actual_row_increment, scene_width, p, folder_path);
                }
            }

            if (p.getX() != 0) {
                p = new Point2D(0, p.getY() + actual_row_increment);
                how_many_rows++;
            }
            return p;
        }
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
        try ( Perf perf = new Perf("5. process_one_folder_with_picture")) {
            Item folder_item = all_items_map.get(folder_path);
            if (folder_item == null) {
                if (dbg) logger.log("✅ WARNING:Item_folder_with_icon NO path for" + folder_path);

                folder_item = new Item_folder_with_icon(
                        owner,
                        the_Scene,
                        selection_handler,
                        icon_factory_actor,
                        color,
                        folder_path.getFileName().toString(),
                        (int) column_increment,
                        100,
                        false,
                        null,
                        browsing_caches.image_properties_RAM_cache,
                        shutdown_target,
                        new Path_list_provider_for_file_system(folder_path),
                        this,
                        this,
                        aborter,
                        logger);
                all_items_map.put(folder_path, folder_item);
            }
            p = new_Point_for_files_and_dirs(p, folder_item, column_increment, row_increment, scene_width, single_column);
            return p;
        }
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
        try(Perf perf = new Perf("6. process_one_folder_plain")) {
            Item folder_item = all_items_map.get(folder_path);
            if (folder_item == null) {
                Color color = My_colors.load_color_for_path(folder_path, owner, logger);
                // a "plain" folder is "like a file" from a layout point of view
                // the difference is: it will get a border

                String tmp = folder_path.getFileName().toString();

                if (show_how_many_files_deep_in_each_folder_done) {
                    Long how_many_files_deep = folder_file_count_cache.get(folder_path);
                    if (how_many_files_deep == null) {
                        logger.log("❌ FATAL: folder_file_count_cache not found in cache for " + folder_path);
                    } else {
                        logger.log("✅ OK: folder_file_count_cache found in cache for " + folder_path + " " + how_many_files_deep);
                        tmp += " (" + how_many_files_deep + " files)";
                    }
                } else if (show_total_size_deep_in_each_folder_done) {

                    Long bytes = folder_total_sizes_cache.get(folder_path);
                    if (bytes == null) {
                        logger.log("❌ FATAL: folder_total_sizes_cache not found in cache for " + folder_path);
                    } else {
                        logger.log("✅ OK: folder_total_sizes_cache found in cache for " + folder_path + " " + bytes);

                        tmp += "       ";
                        tmp += Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(bytes, owner, logger);
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
                        new Path_list_provider_for_file_system(folder_path),
                        this,
                        this,

                        owner,
                        aborter,
                        logger);
                //new Item2_button(the_browser,folder_path, color, tmp, icon_height, false, false, image_properties_RAM_cache,logger);
                all_items_map.put(folder_path, folder_item);
            }

            p = new_Point_for_files_and_dirs(p, folder_item, column_increment, row_increment, scene_width, single_column);
            if (folder_item instanceof Item_folder ini) {
                ini.get_button().setPrefWidth(column_increment);
                ini.get_button().setMinWidth(column_increment);
            }
            return p;
        }
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
            logger.log("✅ move_absolute reason= " + reason + " new_vertical_offset=" + new_vertical_offset);
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
            logger.log("✅ check_visibility: items are not ready yet ! "+from);
            return;
        }
        //logger.log("check_visibility: "+ all_items_map.values().size()+" items are ready "+from);
        double pane_height = the_Pane.getHeight();
        int icon_size = Non_booleans_properties.get_icon_size(owner);
        double min_y = Double.MAX_VALUE;
        for (Item item : all_items_map.values())
        {
            //if (item.get_y() + item.get_Height() - current_vertical_offset < 0)
            //if (item.get_javafx_y() + item.get_Height() < current_vertical_offset -icon_size)
            if (item.get_javafx_y() + item.get_Height() < current_vertical_offset )
            {
                if (invisible_dbg)
                    logger.log("✅ "+item.get_item_path() + " invisible (too far up) y=" + item.get_javafx_y() + " item height=" + item.get_Height());
                item.process_is_invisible(current_vertical_offset);
                the_Pane.getChildren().remove(item.get_Node());
                //if ( item instanceof Item2_image ii) Item2_image.currently.remove(ii);
                continue;
            }
            if (item.get_javafx_y()  > pane_height+current_vertical_offset+icon_size)
            {
                if (invisible_dbg) logger.log("✅ "+item.get_item_path() + " invisible (too far down)");
                item.process_is_invisible(current_vertical_offset);
                the_Pane.getChildren().remove(item.get_Node());
                //if ( item instanceof Item2_image ii) Item2_image.currently.remove(ii);
                continue;
            }
            if (visible_dbg)
                logger.log("✅ "+item.get_item_path() + " Item is visible at y=" + item.get_javafx_y() + " item height=" + item.get_Height());
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

        background_provider.set_background_color(the_Pane);


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

        LongAdder count = new LongAdder();
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Progress_window progress_window = Progress_window.show(
                true,
                "Computing file count",
                20*60,
                x,
                y,
                owner,
                logger);
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
                if (count.doubleValue() == 0)
                {
                    Jfx_batch_injector.inject(()-> compute_geometry("sort by number of files", progress_window),logger);
                    if ( System.currentTimeMillis()-start > 3000) {
                        Ding.play("display how many files in each folder", logger);
                    }
                    return;
                }
            }

        };
        Actor_engine.execute(monitor,"compute_geometry after computing the number of files", logger);
    }

    //**********************************************************
    public void show_total_size_deep_in_each_folder()
    //**********************************************************
    {
        if(( Sort_files_by.get_sort_files_by(path_list_provider.get_folder_path(),owner)== Sort_files_by.SIMILARITY_BY_PAIRS)
                ||
                (Sort_files_by.get_sort_files_by(path_list_provider.get_folder_path(),owner)== Sort_files_by.SIMILARITY_BY_PURSUIT))
        {
            Sort_files_by.set_sort_files_by(path_list_provider.get_folder_path(), Sort_files_by.NAME,owner,logger);
        }
        show_how_many_files_deep_in_each_folder_done = false;
        folder_total_sizes_cache = new HashMap<>();
        logger.log("✅ Virtual_landscape: show_total_size_deep_in_each_folder");
        LongAdder count = new LongAdder();
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Progress_window progress_window = Progress_window.show(
                true,
                "Computing folder sizes",
                20*60,
                x,
                y,
                owner,
                logger);
        for ( Item i : all_items_map.values())
        {
            if (i instanceof Item_folder item2_folder)
            {
                if(Files.isDirectory(item2_folder.get_true_path()))
                {
                    item2_folder.add_total_size_deep_folder(count, item2_folder.get_button(), item2_folder.text, item2_folder.get_true_path(),
                            folder_total_sizes_cache,
                            progress_window.aborter, logger);
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
                if (count.doubleValue() == 0)
                {
                    Jfx_batch_injector.inject(()-> compute_geometry(
                            "sort by folder size on disk", progress_window),logger);
                    if ( System.currentTimeMillis()-start > 3000) {
                        Ding.play("display all folder sizes", logger);
                    }
                    return;
                }
            }

        };
        Actor_engine.execute(monitor,"compute_geometry after computing folder sizes",logger);
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
            if (dbg) logger.log("✅ item is portrait aspect ratio: "+item.get_item_path());

            // portrait image
            width_of_this = column_increment * ((Item_file_with_icon)item).aspect_ratio;
            double neg_x = 0;//(width_of_this-column_increment)/2.0;
            // shift left to compensate the portrait
            item.set_javafx_x(current_screen_x+neg_x);
            item.set_javafx_y(current_screen_y);
        }
        else
        {
            if( dbg) logger.log("✅ item is landscape aspect ratio: "+item.get_item_path());
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
            if ( Item.layout_dbg) logger.log("✅ NEW ROW, max_screen_y_in_row="+max_screen_y_in_row[0]);

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
            if ( dbg) logger.log("✅ single_column new row "+delta_h);
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
        if (scroll_dbg) logger.log("✅ compute_bounding_rectangle() "+reason);
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
            if ( scroll_dbg) logger.log("✅ compute_bounding_rectangle, h="+h+" for "+item.get_string());

            if (item.get_javafx_y() + h > virtual_landscape_height) virtual_landscape_height = item.get_javafx_y() + h;
        }

        if (get_iconized_sorted("compute_bounding_rectangle").isEmpty())
        {
            // when there is no iconized items in the folder
            // it may happen that the height of the last row of buttons at the bottom is underestimated
            virtual_landscape_height += 100;
        }
        if (scroll_dbg)
            logger.log("✅ landscape_height="+ virtual_landscape_height);
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

        double font_size = Non_booleans_properties.get_font_size(owner,logger);
        double height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;

        Button up_button = null;
        if (context_type == Window_type.File_system_2D)
        {
            String go_up_text = "";
            if (path_list_provider.has_parent() )
            {
                go_up_text = My_I18n.get_I18n_string("Parent_Folder", owner,logger);// to: " + parent.toAbsolutePath().toString();
            }
            up_button = virtual_landscape_menus.make_button_that_behaves_like_a_folder(
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
                {
                    logger.log("❌ BAD: could not load " + Look_and_feel_manager.get_instance(owner, logger).get_up_icon_path());
                }
                else
                {
                    Look_and_feel_manager.set_button_and_image_look(up_button, icon, height, null,true,owner,logger);
                }

            }
            top_buttons.add(up_button);
        }

        Button trash = null;
        if (context_type == Window_type.File_system_2D)
        {
            String trash_text = My_I18n.get_I18n_string("Trash",owner,logger);// to: " + parent.toAbsolutePath().toString();
            trash = virtual_landscape_menus.make_button_that_behaves_like_a_folder(
                    Non_booleans_properties.get_trash_dir(path_list_provider.get_folder_path(),owner,logger),
                    trash_text,
                    height,
                    MIN_PARENT_AND_TRASH_BUTTON_WIDTH,
                    true,
                    null,
                    logger);
            {
                Image icon = Look_and_feel_manager.get_trash_icon(height,owner,logger);
                if (icon == null)
                {
                    logger.log("❌ BAD: could not load " + Look_and_feel_manager.get_instance(owner,logger).get_bookmarks_icon_path());
                }
                else
                {
                    Look_and_feel_manager.set_button_and_image_look(trash, icon, height,null, true,owner,logger);
                }

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
        if (dbg) logger.log("✅ applying font size " + Non_booleans_properties.get_font_size(owner,logger));
        for (Node x : top_buttons) {
            Font_size.apply_global_font_size_to_Node(x, owner,logger);
        }
    }

    //**********************************************************
    String get_status()
    //**********************************************************
    {
        Sort_files_by file_sort_by = Sort_files_by.get_sort_files_by(path_list_provider.get_folder_path(),owner);
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
            if ( go_up != null) top_pane2.getChildren().add(go_up);
            define_top_bar_using_buttons_deep2(top_pane2, height);
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer,owner,logger);
            top_pane2.getChildren().add(spacer);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            if ( trash != null) top_pane2.getChildren().add(trash);
            Region spacer2 = new Region();
            Look_and_feel_manager.set_region_look(spacer2,owner,logger);
            top_pane2.getChildren().add(spacer2);
            HBox.setHgrow(spacer2, Priority.SOMETIMES);
            Look_and_feel_manager.set_region_look(top_pane2,owner,logger);
            top_pane.getChildren().add(top_pane2);
        }
        top_pane.getChildren().add(new Separator());
        return top_pane;
    }


    //**********************************************************
    private void define_top_bar_using_buttons_deep2(Pane top_pane, double height)
    //**********************************************************
    {
        {
            Button undo_bookmark_history_button = make_button_undo_and_bookmark_and_history(
                    the_whole_history,
                    path_list_provider,
                    top_left,
                    shutdown_target,
                    Window_type.File_system_2D, height,owner,logger);

            top_pane.getChildren().add(undo_bookmark_history_button);
            top_buttons.add(undo_bookmark_history_button);
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
    public static Button make_button_undo_and_bookmark_and_history(
            Map<LocalDateTime,String> the_whole_history,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type context_type,
            double height,
            Window owner, Logger logger)
    //**********************************************************
    {
        String undo_bookmark_history = My_I18n.get_I18n_string("Bookmarks", owner,logger);
        undo_bookmark_history += " & " + My_I18n.get_I18n_string("History", owner,logger);
        Button undo_bookmark_history_button = new Button(undo_bookmark_history);
        undo_bookmark_history_button.setOnAction(e -> button_undo_and_bookmark_and_history(
                e,
                the_whole_history,
                path_list_provider,
                top_left,
                shutdown_target,
                context_type,owner,logger));
        Image icon = Look_and_feel_manager.get_bookmarks_icon(height,owner,logger);
        Look_and_feel_manager.set_button_and_image_look(undo_bookmark_history_button, icon, height,null, false,owner,logger);
        return undo_bookmark_history_button;
    }

    //**********************************************************
    private static void button_undo_and_bookmark_and_history(ActionEvent e, Map<LocalDateTime,String> the_whole_history,
                                                             Path_list_provider path_list_provider,
                                                             Path top_left,
                                                             Shutdown_target shutdown_target, Window_type context_type, Window owner, Logger logger)
    //**********************************************************
    {
        ContextMenu undo_and_bookmark_and_history = define_contextmenu_undo_bookmark_history(
                the_whole_history,
                path_list_provider,
                top_left,
                shutdown_target,
                context_type,owner,logger);
        Button b = (Button) e.getSource();
        undo_and_bookmark_and_history.show(b, Side.TOP, 0, 0);
    }

    //**********************************************************
    private void button_preferences(ActionEvent e)
    //**********************************************************
    {
        ContextMenu pref = virtual_landscape_menus.define_contextmenu_preferences();
        Button b = (Button) e.getSource();
        pref.show(b, Side.TOP, 0, 0);
    }

    //**********************************************************
    private void button_files(ActionEvent e)
    //**********************************************************
    {
        ContextMenu files = virtual_landscape_menus.define_files_ContextMenu();
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
    private static ContextMenu define_contextmenu_undo_bookmark_history(
            Map<LocalDateTime,String> the_whole_history,
            Path_list_provider path_list_provider,
            Path top_left,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Window owner, Logger logger)
    //**********************************************************
    {
        ContextMenu undo_bookmark_history_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(undo_bookmark_history_menu,owner,logger);

        undo_bookmark_history_menu.getItems().add(Virtual_landscape_menus.make_undos_menu(owner,logger));
        undo_bookmark_history_menu.getItems().add(Virtual_landscape_menus.make_bookmarks_menu(path_list_provider.get_folder_path(),top_left,shutdown_target, window_type,owner,logger));
        undo_bookmark_history_menu.getItems().add(Virtual_landscape_menus.make_history_menu(
                the_whole_history,
                path_list_provider,
                top_left,
                shutdown_target,
                window_type,
                owner,logger));
        undo_bookmark_history_menu.getItems().add(Virtual_landscape_menus.make_roots_menu(
                path_list_provider,
                top_left,
                shutdown_target,
                window_type,
                owner,logger));
        return undo_bookmark_history_menu;
    }

    //**********************************************************
    private ContextMenu define_contextmenu_view()
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu,owner,logger);

        //Rectangle2D rectangle = new Rectangle2D(owner.getX(),owner.getY(),owner.getWidth(),owner.getHeight());
        Menu_items.add_menu_item("New_Window",event -> Instructions.additional_same_folder(context_type,path_list_provider,get_top_left(),owner,logger),context_menu,owner,logger);
        Menu_items.add_menu_item("New_Twin_Window",event -> Instructions.additional_same_folder_twin(context_type,path_list_provider,get_top_left(),owner,logger),context_menu,owner,logger);
        Menu_items.add_menu_item("New_Double_Window",event -> Instructions.additional_same_folder_fat_tall(context_type,path_list_provider,get_top_left(),owner,logger),context_menu,owner,logger);


        {
            start_full_screen_menu_item = Menu_items.make_menu_item("Go_full_screen",event -> full_screen_handler.go_full_screen(),owner,logger);
            start_full_screen_menu_item.setDisable(false);
            context_menu.getItems().add(start_full_screen_menu_item);
        }
        {
            stop_full_screen_menu_item = Menu_items.make_menu_item("Stop_full_screen",event -> full_screen_handler.stop_full_screen(),owner,logger);
            stop_full_screen_menu_item.setDisable(true);
            context_menu.getItems().add(stop_full_screen_menu_item);
        }
        {
            String text = My_I18n.get_I18n_string("Scan_show",owner,logger);
            Menu scan = new Menu(text);
            Look_and_feel_manager.set_menu_item_look(scan,owner,logger);

            scan.getItems().add(Menu_items.make_menu_item("Start_stop_slow_scan",event -> handle_scan_switch(),owner,logger));
            scan.getItems().add(Menu_items.make_menu_item("Slow_down_scan",event -> slow_down_scan(),owner,logger));
            scan.getItems().add(Menu_items.make_menu_item("Speed_up_scan",event -> speed_up_scan(),owner,logger));
            context_menu.getItems().add(scan);
        }
        Menu_items.add_menu_item("Show_How_Many_Files_Are_In_Each_Folder",event -> show_how_many_files_deep_in_each_folder(),context_menu,owner,logger);
        Menu_items.add_menu_item("Show_Each_Folder_Total_Size",event -> show_total_size_deep_in_each_folder(),context_menu,owner,logger);
        Menu_items.add_menu_item("About_klik",event -> About_klik_stage.show_about_klik_stage(owner,logger),context_menu,owner,logger);
        Menu_items.add_menu_item("Refresh",event -> redraw_fx("refresh"),context_menu,owner,logger);
        if( !change_events_off) Menu_items.add_menu_item("Disable_change_events",event -> change_events_off = true,context_menu,owner,logger);
        if( change_events_off) Menu_items.add_menu_item("Enable_change_events",event -> change_events_off = false,context_menu,owner,logger);


        Menu_items.add_menu_item(
                "Show_Meters",
                event -> RAM_and_threads_meters_stage.show_stage(owner,logger),
                context_menu,owner,logger);

/*
        if (Feature_cache.get(Feature.Enable_tags))
        {
            Menu_items.add_menu_item("Open_tag_management",event -> Tag_items_management_stage.open_tag_management_stage(owner,aborter,logger),context_menu,owner,logger);
        }
*/
        return context_menu;
    }

    public boolean change_events_off = false;



    //**********************************************************
    public void import_apple_Photos()
    //**********************************************************
    {
        if (!Popups.popup_ask_for_confirmation( "❗ Importing photos will create COPIES", "Please select a destination drive with enough space", owner,logger))
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
    enum Sort_by_time
    //**********************************************************
    {
        year,
        month,
        day
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
        logger.log("✅ aborting backup");
        Backup_singleton.abort();
    }

    //**********************************************************
    public void start_backup()
    //**********************************************************
    {
        logger.log("✅ starting backup");
        Path backup_source = Static_backup_paths.get_backup_source();
        if (backup_source == null) {
            logger.log("❗ no backup_source");
            Popups.popup_warning( "❗ Cannot backup!", "Reason: no backup ORIGIN", false, owner,logger);
            return;
        }
        Path backup_destination = Static_backup_paths.get_backup_destination();
        if (backup_destination == null) {
            logger.log("❗ no backup destination");
            Popups.popup_warning( "❗ Cannot backup!", "Reason: no backup DESTINATION", false, owner,logger);

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
        logger.log("✅ aborting fusk");
        Fusk_singleton.abort();
    }

    //**********************************************************
    public void start_fusk()
    //**********************************************************
    {
        logger.log("✅ starting fusk");
        Path fusk_source = Static_fusk_paths.get_fusk_source();
        if (fusk_source == null) {
            logger.log("❗ no fusk_source");
            Popups.popup_warning("❗ Cannot fusk!", "Reason: no fusk ORIGIN", false, owner,logger);
            return;
        }
        Path fusk_destination = Static_fusk_paths.get_fusk_destination();
        if (fusk_destination == null) {
            logger.log("❗ no fusk destination");
            Popups.popup_warning( "❗ Cannot fusk!", "Reason: no fusk DESTINATION", false, owner,logger);

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
        logger.log("✅ starting defusk");
        Path defusk_source = Static_fusk_paths.get_fusk_source();
        if (defusk_source == null) {
            logger.log("❗ no defusk source");
            Popups.popup_warning( "❗ Cannot defusk!", "Reason: no defusk SOURCE", false, owner,logger);
            return;
        }
        Path defusk_destination = Static_fusk_paths.get_fusk_destination();
        if (defusk_destination == null) {
            logger.log("❗ no defusk destination");
            Popups.popup_warning( "❗ Cannot defusk!", "Reason: no defusk DESTINATION", false, owner,logger);

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
        logger.log("✅ backup destination = " + path_list_provider.get_name());

        set_text_background("BACKUP\nDESTINATION");

    }

    //**********************************************************
    public void you_are_backup_source()
    //**********************************************************
    {
        Static_backup_paths.set_backup_source(path_list_provider.get_folder_path());
        logger.log("✅ backup source = " + path_list_provider.get_name());

        set_text_background("BACKUP\nSOURCE");

    }

    //**********************************************************
    public void you_are_fusk_destination()
    //**********************************************************
    {
        Static_fusk_paths.set_fusk_destination(path_list_provider.get_folder_path());
        logger.log("✅ fusk destination = " + path_list_provider.get_name());

        set_text_background("FUSK\nDESTINATION");

    }

    //**********************************************************
    public void you_are_fusk_source()
    //**********************************************************
    {
        Static_fusk_paths.set_fusk_source(path_list_provider.get_folder_path());
        logger.log("✅ fusk source = " + path_list_provider.get_name());

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
        if (dbg) logger.log("✅ Virtual_landscape redraw from:" + from );

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
        Actor_engine.execute(r, "Redraw engine", logger);
    }

    private final AtomicBoolean the_guard = new AtomicBoolean(false);


    //**********************************************************
    private void redraw_all_internal(Window owner, double x, double y)
    //**********************************************************
    {
        if ( the_guard.get())
        {
            //logger.log("\n\n redraw request ignored, guard is set!\n\n");
            return;
        }
        the_guard.set(true);
        aborter.add_on_abort(() -> the_guard.set(false));


        long start = System.currentTimeMillis();

        Hourglass progress_window = null;

        if (show_progress_window_on_redraw) {
            progress_window = Progress_window.show(
                    false,
                    "Scanning folder",
                    20*60,
                    x,
                    y,
                    owner,
                    logger);
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

        all_image_properties_acquired_4(start, progress_window);

    }


    //**********************************************************
    private void set_comparators(double x, double y)
    //**********************************************************
    {
        //logger.log("Virtual_landscape: set_comparators");


        Alphabetical_file_name_comparator alphabetical_file_name_comparator = new Alphabetical_file_name_comparator();

        other_file_comparator = Sort_files_by.get_non_image_comparator(path_list_provider, owner,aborter,logger);

        image_file_comparator = Sort_files_by.get_image_comparator(path_list_provider, this,browsing_caches.image_properties_RAM_cache,
                owner,x,y, aborter,logger);;

        paths_holder.folders = new ConcurrentSkipListSet<>(alphabetical_file_name_comparator);
        paths_holder.non_iconized = new ConcurrentSkipListSet<>(other_file_comparator);
    }


    //**********************************************************
    private void scan_list()
    //**********************************************************
    {
        boolean show_icons = Feature_cache.get(Feature.Show_icons_for_files);
        if ( Feature_cache.get(Feature.Show_single_column)) show_icons = false;

        if ( dbg)
        {
            logger.log("✅ Virtual_landscape: scan_list");
            if (Platform.isFxApplicationThread()) logger.log(Stack_trace_getter.get_stack_trace("❌ SHOULD NOT HAPPEN: scanning disk on javafx thread"));
        }

        try
        {
            //File files[] = the_displayed_folder_path.toFile().listFiles();

            //for ( File f : files)

            for ( Path path : path_list_provider.only_folder_paths(Feature_cache.get(Feature.Show_hidden_folders)))
            {
                if (dbg) logger.log("✅ Virtual_landscape: looking at path " + path.toAbsolutePath());

                if (aborter.should_abort()) {
                    logger.log("❗ path manager aborting (1) scan_list "+ path_list_provider.get_folder_path().toAbsolutePath());
                    aborter.on_abort();
                    return;
                }

                paths_holder.add_folder(path);
            }
            for ( Path path : path_list_provider.only_file_paths(Feature_cache.get(Feature.Show_hidden_files)))
            {
                if (dbg) logger.log("✅ Virtual_landscape: looking at path " + path.toAbsolutePath());

                if (aborter.should_abort()) {
                    logger.log("❗ path manager aborting (2) scan_list "+ path_list_provider.get_folder_path().toAbsolutePath());
                    aborter.on_abort();
                    return;
                }
                paths_holder.add_file( path, show_icons, owner);
                // this will start one virtual thread per image to prefill the image property cache
            }
        }
        catch (InvalidPathException e)
        {
            logger.log("❗ Browsing error: "+e);
            receive_error(Error_type.NOT_FOUND);
        }
        catch (SecurityException e)
        {
            logger.log("❗ Browsing error: "+e);
            receive_error(Error_type.DENIED);
        }
        catch (Exception e)
        {
            logger.log("❗ Browsing error: "+e);
            receive_error(Error_type.ERROR);
        }

        if ( dbg) logger.log("✅ Virtual_landscape: looking at path Virtual_landscape: scan_list ends");

    }


    //**********************************************************
    private void all_image_properties_acquired_4(long start, Hourglass progress_window)
    //**********************************************************
    {
        if ( dbg) logger.log("✅ Virtual_landscape: looking at path Virtual_landscape::all_image_properties_acquired_4() ");
        Runnable r = () -> browsing_caches.image_properties_RAM_cache.save_whole_cache_to_disk();
        Actor_engine.execute(r,"Save whole image property cache",logger);

        if (System.currentTimeMillis() - start > 5_000) {
            if (Booleans.get_boolean(Feature.Play_ding_after_long_processes.name(),owner)) {
                Ding.play("all_image_properties_acquired: done acquiring all image properties", logger);
            }
        }
        get_path_comparator();
        //logger.log("all_image_properties_acquired, going to refresh");
        refresh_UI("all_image_properties_acquired", progress_window);

        if ( dbg) logger.log("✅ Virtual_landscape::all_image_properties_acquired_4() done");

    }

    //**********************************************************
    private void refresh_UI(String from, Hourglass progress_window)
    //**********************************************************
    {
        sort_iconized_items(from);

        Runnable r = () -> {
            //logger.log("refresh_UI_after_scan_dir " + from);
            refresh_UI_on_fx_thread( from,progress_window);
        };
        Jfx_batch_injector.inject(r, logger);

    }


    //**********************************************************
    private void refresh_UI_on_fx_thread(String from, Hourglass progress_window)
    //**********************************************************
    {

        try (Perf p = new Perf("1. refresh_UI_on_fx_thread"))
        {
            if (dbg) logger.log("✅ refresh_UI_on_fx_thread from: " + from);

            compute_geometry("scene_geometry_changed from: " + from, progress_window);

            if (dbg) logger.log("✅ adapt_slider_to_scene");

            {
                vertical_slider.adapt_slider_to_scene(owner);
            }

            title_target.set_title();

            {
                double title_height = owner.getHeight() - the_Scene.getHeight();
                if (title_height > 60) {
                    logger.log("❗ WARNING: " +
                            "title_height>60 \nowner.getHeight()=" +
                            owner.getHeight() + "\nthe_Scene.getHeight()=" + the_Scene.getHeight());
                } else {
                    for (Button b : top_buttons) {
                        b.setMinHeight(title_height);
                    }
                }
            }
        }
        logger.log("✅ Klik start time = "+ (System.currentTimeMillis() - Klik_application.start_time) + " ms");
    }

    //**********************************************************
    public void compute_geometry(String reason, Hourglass progress_window)
    //**********************************************************
    {
        try (Perf p1 = new Perf("2. compute_geometry")) {
            if (dbg)
                logger.log("\n✅ compute_geometry reason=" + reason + " current_vertical_offset=" + current_vertical_offset);
            if (scroll_dbg)
                logger.log(("✅ compute_geometry single_column=" + Feature_cache.get(Feature.Show_single_column)));


            double magic = 2.0;
            double row_increment_for_dirs = magic * Non_booleans_properties.get_font_size(owner, logger);
            int folder_icon_size = Non_booleans_properties.get_folder_icon_size(owner);
            int column_increment_for_folders = Non_booleans_properties.get_column_width(owner);
            if (column_increment_for_folders < folder_icon_size) column_increment_for_folders = folder_icon_size;

            int icon_size = Non_booleans_properties.get_icon_size(owner);
            int column_increment_for_icons = icon_size;

            if (Feature_cache.get(Feature.Show_single_column)) {
                // the -100 is to make the button shorter than the full width so that
                // the mouse selection can "start" in the rightmost part of the pane
                column_increment_for_icons = (int) (the_Scene.getWidth() - RIGHT_SIDE_SINGLE_COLUMN_MARGIN);
                column_increment_for_folders = column_increment_for_icons;
            }
            double row_increment_for_dirs_with_picture = row_increment_for_dirs + folder_icon_size;

            double scene_width = the_Scene.getWidth();

            double top_delta_y = 2 * Non_booleans_properties.get_font_size(owner, logger);
            if (error_type == Error_type.DENIED) {
                ImageView iv_denied = new ImageView(Look_and_feel_manager.get_denied_icon(icon_size, owner, logger));
                show_error_icon(iv_denied, top_delta_y);
                if (progress_window != null) progress_window.close();
                the_guard.set(false);
                logger.log("✅ on DENIED the_guard =>" + the_guard.get() + " for " + path_list_provider.get_name());
                return;
            }
            if (error_type == Error_type.NOT_FOUND) {
                ImageView not_found = new ImageView(Look_and_feel_manager.get_not_found_icon(icon_size, owner, logger));
                show_error_icon(not_found, top_delta_y);
                if (progress_window != null) progress_window.close();
                the_guard.set(false);
                logger.log("✅ on NOT_FOUND the_guard =>" + the_guard.get() + " for " + path_list_provider.get_name());
                return;
            }
            if (error_type == Error_type.ERROR) {
                ImageView unknown_error = new ImageView(Look_and_feel_manager.get_unknown_error_icon(icon_size, owner, logger));
                show_error_icon(unknown_error, top_delta_y);
                if (progress_window != null) progress_window.close();
                the_guard.set(false);
                logger.log("✅ ON ERROR map_buttons_and_icons_guard =>" + the_guard.get() + " for " + path_list_provider.get_name());
                return;
            }


            the_Pane.getChildren().clear();

            // now we can be in a thread

            int final_column_increment_for_folders = column_increment_for_folders;
            int final_column_increment_for_icons = column_increment_for_icons;
            Runnable r = () -> {
                try (Perf p2 = new Perf("3. compute_geometry THREADED part")) {

                    items_are_ready.set(false);
                    future_pane_content.clear();
                    how_many_rows = 0;

                    if (dbg) logger.log("✅ on javafx thread?  " + Platform.isFxApplicationThread());

                    Point2D p = new Point2D(0, 0);

                    long start = System.currentTimeMillis();
                    p = process_folders(Feature_cache.get(Feature.Show_single_column), row_increment_for_dirs, final_column_increment_for_folders, row_increment_for_dirs_with_picture, scene_width, p);
                    if (dbg) logger.log("✅ process_folders took " + (System.currentTimeMillis() - start) + " ms");
                    p = new Point2D(p.getX(), p.getY() + MARGIN_Y);
                    p = process_non_iconized_files(Feature_cache.get(Feature.Show_single_column), final_column_increment_for_folders, scene_width, p);
                    p = new Point2D(p.getX(), p.getY() + MARGIN_Y);
                    start = System.currentTimeMillis();
                    process_iconized_items(Feature_cache.get(Feature.Show_single_column), icon_size, final_column_increment_for_icons, scene_width, p);
                    if (dbg) logger.log("✅ process_iconized_items took " + (System.currentTimeMillis() - start) + " ms");

                    start = System.currentTimeMillis();
                    compute_bounding_rectangle("map_buttons_and_icons() OK " + p.getX() + " " + p.getY());
                    if (dbg)
                        logger.log("✅ compute_bounding_rectangle took " + (System.currentTimeMillis() - start) + " ms");

                    if (dbg) logger.log("✅ Going to remap all items");
                    future_pane_content.addAll(all_items_map.values());

                    items_are_ready.set(true);
                    the_guard.set(false);
                    if (dbg)
                        logger.log("✅ END, the_guard => " + the_guard.get() + " for " + path_list_provider.get_name());

                    Jfx_batch_injector.inject(() ->
                    {
                        if (progress_window != null) progress_window.close();

                        for (Item item : future_pane_content) {
                            if (item.visible_in_scene.get()) {
                                if (!the_Pane.getChildren().contains(item.get_Node())) {
                                    the_Pane.getChildren().add(item.get_Node());
                                }
                            }
                        }
                        scroll_to();

                        set_visibility_on_fx_thread(reason + " map_buttons_and_icons ");

                    }, logger);
                }
            };
            Actor_engine.execute(r, "compute_geometry, threaded part",logger);

        }
    }



    record File_comp_cache(Sort_files_by file_sort_by, Comparator<Path> comparator){}

    private File_comp_cache file_comp_cache;

    //**********************************************************
    public Comparator<Path> get_path_comparator()
    //**********************************************************
    {
        Comparator<Path> local_file_comparator = image_file_comparator;

        if ( local_file_comparator == null) {
            if (file_comp_cache != null) {
                if (file_comp_cache.file_sort_by() == Sort_files_by.get_sort_files_by(path_list_provider.get_folder_path(),owner)) {
                    if (dbg) logger.log("✅ getting file comparator from cache=" + file_comp_cache);
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
            logger.log("❌ FATAL: local_file_comparator is null");
        }
        else
        {
            //logger.log("setting file_comp_cache ="+file_comp_cache);
            file_comp_cache =  new File_comp_cache(Sort_files_by.get_sort_files_by(path_list_provider.get_folder_path(),owner),local_file_comparator);
            set_new_iconized_items_comparator(local_file_comparator);
        }

        return local_file_comparator;
    }

    //**********************************************************
    private Comparator<Path> create_fast_file_comparator()
    //**********************************************************
    {

        Comparator<Path> local_file_comparator = null;
        switch (Sort_files_by.get_sort_files_by(path_list_provider.get_folder_path(),owner))
        {
            case ASPECT_RATIO :
                local_file_comparator = new Aspect_ratio_comparator(browsing_caches.image_properties_RAM_cache);
                break;
            case RANDOM_ASPECT_RATIO :
                local_file_comparator =  new Aspect_ratio_comparator_random(browsing_caches.image_properties_RAM_cache);
                break;
            case IMAGE_WIDTH :
                local_file_comparator = new Image_width_comparator(browsing_caches.image_properties_RAM_cache);
                break;
            case IMAGE_HEIGHT :
                local_file_comparator = new Image_height_comparator(browsing_caches.image_properties_RAM_cache,logger);
                break;
            case RANDOM :
                local_file_comparator = new Random_comparator();
                break;
            case DATE :
                local_file_comparator = new Date_comparator(logger);
                break;
            case SIZE :
                local_file_comparator = new Decreasing_disk_footprint_comparator(aborter);
                break;

            default : local_file_comparator = new Alphabetical_file_name_comparator();
                break;
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
