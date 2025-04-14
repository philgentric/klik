//SOURCES ../Landscape_height_listener.java
//SOURCES ../Scroll_to_listener.java
//SOURCES ./image_properties_cache/Image_properties.java
//SOURCES ../../look/Look_and_feel.java
//SOURCES ../items/Item_folder_with_icon.java
//SOURCES ../items/My_colors.java
package klik.browser.icons;

import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Browser;
import klik.browser.Landscape_height_listener;
import klik.browser.Scroll_to_listener;
import klik.browser.comparators.*;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.browser.items.*;
import klik.properties.File_sort_by;
import klik.util.files_and_paths.*;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.performance_monitor.Performance_monitor;
import klik.util.ui.Hourglass;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Show_running_film_frame;
import klik.util.ui.Show_running_film_frame_with_abort_button;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


//**********************************************************
public class Virtual_landscape
//**********************************************************
{

    public static final boolean dbg = false;
    public static final boolean invisible_dbg = false;
    public static final boolean visible_dbg = false;
    public static final boolean scroll_dbg = false;

    public static final int MIN_PARENT_AND_TRASH_BUTTON_WIDTH = 200;
    public static final int MIN_COLUMN_WIDTH = 300;
    public static final double RIGHT_SIDE_SINGLE_COLUMN_MARGIN = 100;
    private static final double MARGIN_Y = 50;

    private final Aborter aborter;
    private final Logger logger;
    private Landscape_height_listener landscape_height_listener;
    private Scroll_to_listener scroll_to_listener;
    private final Paths_manager paths_manager;
    public Comparator<? super Path> image_file_comparator = null;
    public Comparator<? super Path> other_file_comparator;
    public Icon_factory_actor icon_factory_actor;
    public final Image_properties_RAM_cache image_properties_cache;


    public ConcurrentLinkedQueue<List<Path>> iconized_sorted_queue = new ConcurrentLinkedQueue<>();
    public final BlockingQueue<Boolean> request_queue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<Path, Item> all_items_map = new ConcurrentHashMap<>();
    private final AtomicBoolean items_are_ready = new AtomicBoolean(false);

    private double virtual_landscape_height = -Double.MAX_VALUE;
    private double current_vertical_offset = 0;
    private int how_many_rows;
    private Path top_left;
    public final double icon_height;
    private final AtomicBoolean the_guard = new AtomicBoolean(false);

    boolean show_how_many_files_deep_in_each_folder_done = false;
    boolean show_total_size_deep_in_each_folder_done = false;
    private final Browser the_browser;
    public Error_type error_type = Error_type.OK;


    Map<Path,Long> folder_total_sizes_cache;
    Map<Path,Long> folder_file_count_cache;

    private final List<Item> future_pane_content = new ArrayList<>();
    //**********************************************************
    public Virtual_landscape(Browser the_browser_, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        error_type = Error_type.OK;
        this.the_browser = the_browser_;
        this.aborter = aborter;
        logger = logger_;
        image_properties_cache = new Image_properties_RAM_cache(the_browser.displayed_folder_path, "Image properties cache", aborter, logger);
        icon_factory_actor = new Icon_factory_actor(image_properties_cache, the_browser.my_Stage.the_Stage, aborter, logger);
        paths_manager = new Paths_manager(icon_factory_actor, the_browser.displayed_folder_path, aborter, logger);

        if ( dbg) logger.log("Virtual_landscape constructor");
        double font_size = Static_application_properties.get_font_size(logger);
        icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    try {
                        Boolean x = request_queue.poll(3, TimeUnit.SECONDS);
                        if (x != null) redraw();
                        if (aborter.should_abort()) return;

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        Actor_engine.execute(r,logger);
    }

    //**********************************************************
    private void redraw()
    //**********************************************************
    {
        if ( the_guard.get())
        {
            logger.log("\n\nredraw? nope, guard is set\n\n");
            return;
        }
        the_guard.set(true);
        aborter.add_on_abort(() -> the_guard.set(false));

        long start = System.currentTimeMillis();


        Hourglass running_film = null;
        if (Browser.show_running_film)
        {
            running_film = Show_running_film_frame.show_running_film("Scanning folder", 20*60,  aborter, logger);
        }

        set_comparators();

        the_browser.max_dir_text_length = 0;
        all_items_map.clear();
        paths_manager.iconized_paths.clear();
        paths_manager.non_iconized.clear();
        paths_manager.folders.clear();
        iconized_sorted_queue.clear();

        image_properties_cache.reload_cache_from_disk();
        scan_dir();

        all_image_properties_acquired_4(start, running_film);

    }


    //**********************************************************
    private void scan_dir()
    //**********************************************************
    {
        boolean show_icons_instead_of_text = Static_application_properties.get_show_icons(logger);
        boolean show_hidden_files = Static_application_properties.get_show_hidden_files(logger);
        boolean show_hidden_directories = Static_application_properties.get_show_hidden_directories(logger);
        //boolean show_icons_for_folders = Static_application_properties.get_show_icons_for_folders(logger);

        if ( dbg) if (Platform.isFxApplicationThread()) logger.log(Stack_trace_getter.get_stack_trace("PANIC"));

        try
        {
            File files[] = the_browser.displayed_folder_path.toFile().listFiles();
            for ( File f : files)
            {
                if ( aborter.should_abort())
                {
                    logger.log("path manager aborting1");
                    aborter.on_abort();
                    return;
                }

                Path path  = f.toPath();
                if ( f.isDirectory())
                {
                    if (show_hidden_directories)
                    {
                        paths_manager.do_folder(the_browser, path);
                    }
                    else
                    {
                        if (Guess_file_type.is_this_path_invisible_when_browsing(path))
                        {
                            continue; // invisible
                        }
                        else
                        {
                            paths_manager.do_folder(the_browser, path);
                        }
                    }
                }
                else
                {
                    if (show_hidden_files)
                    {
                        paths_manager.do_file(the_browser, path, show_icons_instead_of_text, the_browser.my_Stage.the_Stage);
                    }
                    else
                    {
                        if (Guess_file_type.is_this_path_invisible_when_browsing(path))
                        {
                            continue;// invisible
                        }
                        else
                        {
                            paths_manager.do_file(the_browser,path, show_icons_instead_of_text, the_browser.my_Stage.the_Stage);
                        }
                    }
                    // this will start one virtual thread per image to prefill the image property cache
                }
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
    }


    //**********************************************************
    private void all_image_properties_acquired_4(long start, Hourglass running_film)
    //**********************************************************
    {
        //logger.log("Image_propertiew_cache::all_image_properties_acquired() ");
        Runnable r = new Runnable() {
            @Override
            public void run() {
                image_properties_cache.save_whole_cache_to_disk();
            }
        };
        Actor_engine.execute(r,logger);

        if (System.currentTimeMillis() - start > 5_000) {
            if (Static_application_properties.get_ding(logger)) {
                Ding.play("all_image_properties_acquired: done acquiring all image properties", logger);
            }
        }
        determine_file_comparator(paths_manager);
        //logger.log("all_image_properties_acquired, going to refresh");
        refresh_UI("all_image_properties_acquired", running_film);

        long end = System.currentTimeMillis();
        Performance_monitor.register_new_record("Browser",paths_manager.folder_path.toString(),end-start,logger);
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
        Path scroll_to = the_browser.get_scroll_to();

        logger.log("refresh_UI_on_fx_thread from: " + from);

        compute_geometry(the_browser.mandatory_in_pane, "scene_geometry_changed from: " + from, scroll_to, running_film);

        if (dbg) logger.log("adapt_slider_to_scene");
        {
            the_browser.vertical_slider.adapt_slider_to_scene(the_browser.my_Stage.the_Stage);
        }

        the_browser.set_title();

        {
            double title_height = the_browser.my_Stage.the_Stage.getHeight() - the_browser.the_Scene.getHeight();
            if (title_height > 60)
            {
                logger.log("WARNING: " +
                        "title_height>60 \nmy_Stage.the_Stage.getHeight()=" +
                        the_browser.my_Stage.the_Stage.getHeight() + "\nthe_Scene.getHeight()=" + the_browser.the_Scene.getHeight());
            }
            else
            {
                for (Button b : the_browser.browser_ui.top_buttons) {
                    b.setMinHeight(title_height);
                }
            }
        }
    }

    //**********************************************************
    public void compute_geometry(
                                //double pane_width,
                                //double pane_height,
                                 List<Node> mandatory,
                                 String reason,
                                 Path scroll_to,
                                 Hourglass running_film)
    //**********************************************************
    {

        logger.log("\ncompute_geometry reason="+reason+" current_vertical_offset="+current_vertical_offset);
        boolean single_column = Static_application_properties.get_single_column(logger);
        if (scroll_dbg) logger.log(("geometry_changed single_column="+single_column));

        long start = System.nanoTime();
        if ( dbg) logger.log("Virtual_landscape map_buttons_and_icons");

        double row_increment_for_dirs = 2 * Static_application_properties.get_font_size(logger);
        int folder_icon_size = Static_application_properties.get_folder_icon_size(logger);
        int column_increment_for_folders = Static_application_properties.get_column_width(logger);
        if ( column_increment_for_folders < folder_icon_size) column_increment_for_folders = folder_icon_size;

        int icon_size = Static_application_properties.get_icon_size(logger);
        int column_increment_for_icons = icon_size;

        if ( single_column)
        {
            // the -100 is to make the button shorter than the full width so that
            // the mouse selection can "start" in the rightmost part of the pane
            column_increment_for_icons = (int)(the_browser.the_Scene.getWidth()-RIGHT_SIDE_SINGLE_COLUMN_MARGIN);
            column_increment_for_folders = column_increment_for_icons;
        }
        double row_increment_for_dirs_with_picture = row_increment_for_dirs + folder_icon_size;

        double scene_width = the_browser.the_Scene.getWidth();

        double top_delta_y = 2 * Static_application_properties.get_font_size(logger);
        if (error_type == Error_type.DENIED) {
            ImageView iv_denied = new ImageView(Look_and_feel_manager.get_denied_icon(icon_size));
            show_error_icon(the_browser, iv_denied,top_delta_y);
            if ( running_film != null) running_film.close();
            the_guard.set(false);
            logger.log("on DENIED the_guard =>"+the_guard.get()+" for "+the_browser.displayed_folder_path);
            return;
        }
        if (error_type == Error_type.NOT_FOUND) {
            ImageView not_found = new ImageView(Look_and_feel_manager.get_not_found_icon(icon_size));
            show_error_icon(the_browser, not_found,top_delta_y);
            if ( running_film != null) running_film.close();
            the_guard.set(false);
            logger.log("on NOT_FOUND the_guard =>"+the_guard.get()+" for "+the_browser.displayed_folder_path);
            return;
        }
        if (error_type == Error_type.ERROR) {
            ImageView unknown_error = new ImageView(Look_and_feel_manager.get_unknown_error_icon(icon_size));
            show_error_icon(the_browser, unknown_error,top_delta_y);
            if ( running_film != null) running_film.close();
            the_guard.set(false);
            logger.log("ON ERROR map_buttons_and_icons_guard =>"+the_guard.get()+" for "+the_browser.displayed_folder_path);
            return;
        }

        {
            the_browser.the_Pane.getChildren().clear();
            the_browser.the_Pane.getChildren().addAll(mandatory);
        }

        int final_column_increment_for_folders = column_increment_for_folders;
        int final_column_increment_for_icons = column_increment_for_icons;

        items_are_ready.set(false);
        future_pane_content.clear();
        how_many_rows = 0;

        Point2D p = new Point2D(0, 0);
        p = process_folders(the_browser, single_column, row_increment_for_dirs, final_column_increment_for_folders, row_increment_for_dirs_with_picture, scene_width, p);
        p = new Point2D(p.getX(),p.getY()+MARGIN_Y);
        p = process_non_iconized_files(the_browser, single_column, final_column_increment_for_folders, scene_width, p);
        p = new Point2D(p.getX(),p.getY()+MARGIN_Y);
        process_iconized_items(the_browser, single_column, icon_size, final_column_increment_for_icons, scene_width, p);

        compute_bounding_rectangle("map_buttons_and_icons() OK "+p.getX()+" "+p.getY());

        logger.log("Going to remap all items");
        long start2 = System.currentTimeMillis();
        future_pane_content.addAll(all_items_map.values());

        items_are_ready.set(true);
        the_guard.set(false);

        logger.log("END, the_guard => "+the_guard.get()+" for "+paths_manager.folder_path);

        Jfx_batch_injector.inject(()->
        {
            if ( running_film != null) running_film.close();

            for (Item item : future_pane_content)
            {
                if (item.visible_in_scene.get())
                {
                    if (!the_browser.the_Pane.getChildren().contains(item.get_Node()))
                    {
                        the_browser.the_Pane.getChildren().add(item.get_Node());
                    }
                }
            }
            logger.log("Scroll to: "+scroll_to);
            scroll_to(scroll_to);

            set_visibility_on_fx_thread(reason+" map_buttons_and_icons ");

        },logger);
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
                        the_browser.set_status("Access denied for:" + the_browser.displayed_folder_path);
                        compute_geometry( the_browser.mandatory_in_pane, "access denied", null, null);
                        break;
                    case NOT_FOUND:
                    case ERROR:
                        logger.log("\n\ndirectory gone\n\n");
                        the_browser.set_status("Folder is gone:" + the_browser.displayed_folder_path);
                        compute_geometry(the_browser.mandatory_in_pane, "gone",  null, null);
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
        List<Path> local_iconized_sorted = new ArrayList<>(paths_manager.iconized_paths);
        for(int tentative =0; tentative<3; tentative++)
        {
            try
            {
                local_iconized_sorted.sort(image_file_comparator);
                break;
            }
            catch (IllegalArgumentException e)
            {
                // let us retry!
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
    private void set_comparators()
    //**********************************************************
    {
        Alphabetical_file_name_comparator alphabetical_file_name_comparator = new Alphabetical_file_name_comparator();

        other_file_comparator = File_sort_by.get_comparator(the_browser.displayed_folder_path,image_properties_cache,aborter,logger);
/*        switch (File_sort_by.get_sort_files_by(logger))
        {
            case SIMILARITY_BY_PURSUIT:
                other_file_comparator = new Similarity_comparator_by_pursuit(the_browser.displayed_folder_path,aborter,logger);
                break;
            case SIMILARITY_BY_PAIRS:
                other_file_comparator = new Similarity_comparator_pairs_of_closests(the_browser.displayed_folder_path, aborter,logger);
                break;
            case NAME, ASPECT_RATIO, RANDOM_ASPECT_RATIO, IMAGE_HEIGHT, IMAGE_WIDTH:
                other_file_comparator = alphabetical_file_name_comparator;
                break;
            case RANDOM:
                other_file_comparator = new Random_comparator();
                break;
            case DATE:
                other_file_comparator = new Date_comparator(logger);
                break;
            case SIZE:
                other_file_comparator = new Decreasing_file_size_comparator();
                break;
            case NAME_GIFS_FIRST:
                other_file_comparator = new Alphabetical_file_name_comparator_gif_first();
                break;
        }

 */
        image_file_comparator = other_file_comparator;

        // these MUST be mutually exclusive:
        paths_manager.folders = new ConcurrentSkipListMap<>(alphabetical_file_name_comparator);
        paths_manager.non_iconized = new ConcurrentSkipListMap<>(other_file_comparator);
    }

    //**********************************************************
    private void set_new_iconized_items_comparator(Comparator<Path> local_file_comparator)
    //**********************************************************
    {
        image_file_comparator = local_file_comparator;
    }



    public void remove_empty_folders(boolean recursively) {
        paths_manager.remove_empty_folders(recursively);
    }


    record File_comp_cache(File_sort_by file_sort_by, Comparator<Path> comparator){}

    private File_comp_cache file_comp_cache;

    //**********************************************************
    private void determine_file_comparator(Paths_manager paths_manager)
    //**********************************************************
    {
        Comparator<Path> local_file_comparator = null;
        if ( file_comp_cache != null)
        {
            if ( file_comp_cache.file_sort_by() == File_sort_by.get_sort_files_by(logger))
            {
                logger.log("getting file comparator from cache="+file_comp_cache);
                local_file_comparator = file_comp_cache.comparator();
            }
        }
        if ( local_file_comparator == null) {
            local_file_comparator = create_new_file_comparator();
        }
        if (local_file_comparator != null)
        {
            //logger.log("setting file_comp_cache ="+file_comp_cache);
            file_comp_cache =  new File_comp_cache(File_sort_by.get_sort_files_by(logger),local_file_comparator);
            set_new_iconized_items_comparator(local_file_comparator);
        }
    }

    //**********************************************************
    private Comparator<Path> create_new_file_comparator()
    //**********************************************************
    {
        Comparator<Path> local_file_comparator = null;
        switch (File_sort_by.get_sort_files_by(logger))
        {
            case File_sort_by.ASPECT_RATIO -> local_file_comparator = new Aspect_ratio_comparator(image_properties_cache);
            case File_sort_by.RANDOM_ASPECT_RATIO -> local_file_comparator = new Aspect_ratio_comparator_random(image_properties_cache);
            case File_sort_by.IMAGE_WIDTH -> local_file_comparator = new Image_width_comparator(image_properties_cache);
            case File_sort_by.IMAGE_HEIGHT -> local_file_comparator = new Image_height_comparator(image_properties_cache,logger);
        }
        return local_file_comparator;
    }








    //**********************************************************
    private List<Path> get_iconized_sorted(String from)
    //**********************************************************
    {
        List<Path> returned = iconized_sorted_queue.poll();
        if ( returned != null) return returned;

        // resort
        logger.log("RESORTING iconized items");
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
    void scroll_to(Path scroll_to)
    //**********************************************************
    {
        current_vertical_offset = get_y_offset_of(scroll_to);
        if ( scroll_to_listener == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return;
        }
        scroll_to_listener.perform_scroll_to(current_vertical_offset,this);
    }
    //**********************************************************
    private void show_error_icon(Browser the_browser, ImageView iv_denied, double top_delta_y)
    //**********************************************************
    {
        iv_denied.setPreserveRatio(true);
        iv_denied.setSmooth(true);
        iv_denied.setY(top_delta_y);
        if ( Platform.isFxApplicationThread())
        {
            the_browser.the_Pane.getChildren().add(iv_denied);
        }
        else
        {
            Jfx_batch_injector.inject(()-> the_browser.the_Pane.getChildren().add(iv_denied),logger);
        }
        compute_bounding_rectangle(error_type.toString());
    }

    //**********************************************************
    private synchronized void process_iconized_items(Browser the_browser, boolean single_column, double icon_size, double column_increment, double scene_width, Point2D point)
    //**********************************************************
    {

        double file_button_height = 2 * Static_application_properties.get_font_size(logger);

        boolean show_icons_instead_of_text = Static_application_properties.get_show_icons(logger);
        double max_y_in_row[] = new double[1];
        max_y_in_row[0] = 0;
        List<Item> current_row = new ArrayList<>();

        int image_properties_in_flight = 0;
        for (Path path : paths_manager.iconized_paths )
        {
            Item item;
            if (show_icons_instead_of_text)
            {
                item = all_items_map.get(path);
                if (item == null)
                {
                    image_properties_in_flight++;
                }
            }
        }

        CountDownLatch wait_for_end = new CountDownLatch(image_properties_in_flight);
        Job_termination_reporter tr = (message, job) -> {
             wait_for_end.countDown();
        };
        for (Path path : paths_manager.iconized_paths )
        {
            if (dbg) logger.log("Virtual_landscape process_iconified_items " + path);
            Item item;
            if (show_icons_instead_of_text)
            {
                item = all_items_map.get(path);
                if (item == null)
                {
                    wait_for_end.countDown();
                    // ask for image properties fetch in threads
                    image_properties_cache.get_from_cache(path,tr);
                }
            }
            else
            {
                // this is an item that could have an image but the user prefers
                // to see it as a text: we use an Item_button
                String size = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(path.toFile().length(), logger);
                item = all_items_map.get(path);
                if (item == null) {
                    item = new Item_button(the_browser, path, null, path.getFileName().toString() + "(" + size + ")",
                            icon_size / 2, false, false, logger);
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
        for ( Path path : paths_manager.iconized_paths)
        {
            Image_properties ip = image_properties_cache.get_from_cache(path,null);
            if ( ip == null) {
                logger.log(Stack_trace_getter.get_stack_trace("FATAL"));
                continue;
            }
            Double cache_aspect_ratio = ip.get_aspect_ratio();
            Item item = new Item_image(the_browser,path, cache_aspect_ratio, logger);
            all_items_map.put(path,item);
            //logger.log("item created: "+path);
        }


        /// at this stage we MUST have get_iconized_sorted() in the proper order
        // that will define the x,y layout
        List<Path> ll = get_iconized_sorted("process_iconified_items");
        for (Path path : ll )
        {
            Item item = all_items_map.get(path);
            if ( item == null)
            {
                logger.log(("should not happen: no item in map for: "+path+" map size="+all_items_map.size() ));
                continue;
            }
            if (dbg)  logger.log("Virtual_landscape process_iconified_items " + path+" ar:"+((Item_image)item).aspect_ratio);

            if (show_icons_instead_of_text) {
                //logger.log("recomputing position for "+item.get_item_path());
                //logger.log(path+" point ="+point.getX()+"-"+point.getY());
                point = compute_next_Point2D_for_icons(point, item,
                        icon_size, icon_size,
                        scene_width, single_column, max_y_in_row, current_row);
            } else {
                point = new_Point_for_files_and_dirs(point, item,
                        column_increment,
                        file_button_height, scene_width, single_column);
                how_many_rows++;
            }
        }
    }

    //**********************************************************
    private Point2D process_non_iconized_files(Browser the_browser, boolean single_column, double column_increment, double scene_width, Point2D p)
    //**********************************************************
    {
        // manage the non-iconifed-files section
        double row_increment_for_files = 2 * Static_application_properties.get_font_size(logger);

        for (Path path : paths_manager.non_iconized.keySet())
        {
            if (dbg) logger.log("Virtual_landscape process_non_iconized_files "+path.toAbsolutePath());
            String text = path.getFileName().toString();
            long size = path.toFile().length() / 1000_000L;
            if (Guess_file_type.is_this_path_a_video(path)) text = size + "MB VIDEO: " + text;
            Item item = all_items_map.get(path);
            if ( item == null)
            {
                item = new Item_button(the_browser,path, null, text,
                        icon_height, false, false, logger);
                all_items_map.put(path,item);
            }
            //item.get_Node().setVisible(false);
            p = new_Point_for_files_and_dirs(p, item,
                    column_increment,
                    row_increment_for_files, scene_width, single_column);

            if (item instanceof Item_button ini)
            {
                ini.get_button().setPrefWidth(column_increment);
                ini.get_button().setMinWidth(column_increment);
            }
        }
        if ( ! paths_manager.non_iconized.isEmpty())
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
    private Point2D process_folders(Browser the_browser,
                                    //Pane pane,
                                    boolean single_column, double row_increment_for_dirs, double column_increment, double row_increment_for_dirs_with_picture, double scene_width, Point2D p)
    //**********************************************************
    {
        if (dbg) logger.log("Virtual_landscape process_folders0 ");

        double actual_row_increment;
        if ( Static_application_properties.get_show_icons_for_folders(logger))
        {
            actual_row_increment = row_increment_for_dirs_with_picture;

            for (Path folder_path : paths_manager.folders.keySet())
            {
                if (dbg) logger.log("Virtual_landscape process_folders1 "+folder_path);
                long start = System.currentTimeMillis();
                if(dbg) logger.log("folder :"+folder_path+" took1 "+(System.currentTimeMillis()-start)+" milliseconds");
                p = process_one_folder_with_picture(the_browser, single_column, column_increment, actual_row_increment, scene_width, p, folder_path, Color.BEIGE);
                if(dbg) logger.log("folder :"+folder_path+" took2 "+(System.currentTimeMillis()-start)+" milliseconds");
            }
        }
        else
        {
            actual_row_increment = row_increment_for_dirs;
            List<Path> paths = new ArrayList<>(paths_manager.folders.keySet());
            if ( show_total_size_deep_in_each_folder_done)
            {
                Comparator<Path> comp = new Comparator<>() {
                    @Override
                    public int compare(Path p1, Path p2) {
                        Long l1 = folder_total_sizes_cache.get(p1);
                        if (l1==null) return 1;
                        Long l2 = folder_total_sizes_cache.get(p2);
                        if (l2==null) return 1;
                        return l2.compareTo(l1);
                    }
                };
                Collections.sort(paths,comp);
            }
            for (Path folder_path : paths)
            {
                if (dbg) logger.log("Virtual_landscape process_folders2 "+folder_path);
                p = process_one_folder_plain(the_browser, single_column, column_increment, actual_row_increment, scene_width, p, folder_path);
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
            Browser the_browser,
           // Pane pane,
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
            folder_item = new Item_folder_with_icon(the_browser, folder_path, color, folder_path.getFileName().toString(), (int)column_increment, logger);
            all_items_map.put(folder_path, folder_item);
        }
        p = new_Point_for_files_and_dirs(p, folder_item, column_increment, row_increment, scene_width, single_column);
        return p;
    }


    //**********************************************************
    private Point2D process_one_folder_plain(
                                       Browser the_browser,
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
            Color color = My_colors.load_color_for_path(folder_path,logger);
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
                    tmp += Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(bytes,logger);
                }
            }
            folder_item = new Item_button(the_browser,folder_path, color, tmp, icon_height, false, false, logger);
            all_items_map.put(folder_path, folder_item);
        }

        p = new_Point_for_files_and_dirs(p, folder_item, column_increment, row_increment, scene_width, single_column);
        if (folder_item instanceof Item_button ini)
        {
            ini.get_button().setPrefWidth(column_increment);
            ini.get_button().setMinWidth(column_increment);
        }
        return p;
    }

    //**********************************************************
    public void clear_image_properties_RAM_cache_fx()
    //**********************************************************
    {
        if ( image_properties_cache == null) return;
        image_properties_cache.clear_image_properties_RAM_cache_fx();
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
            //has_to_call_check_visibility_latter.set(true);
            return;
        }
        //logger.log("check_visibility: "+ all_items_map.values().size()+" items are ready "+from);
        double pane_height = the_browser.the_Pane.getHeight();
        int icon_size = Static_application_properties.get_icon_size(logger);
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
                the_browser.the_Pane.getChildren().remove(item.get_Node());
                //if ( item instanceof Item_image ii) Item_image.currently.remove(ii);
                continue;
            }
            if (item.get_javafx_y()  > pane_height+current_vertical_offset+icon_size)
            {
                if (invisible_dbg) logger.log(item.get_item_path() + " invisible (too far down)");
                item.process_is_invisible(current_vertical_offset);
                the_browser.the_Pane.getChildren().remove(item.get_Node());
                //if ( item instanceof Item_image ii) Item_image.currently.remove(ii);
                continue;
            }
            if (visible_dbg)
                logger.log(item.get_item_path() + " Item is visible at y=" + item.get_javafx_y() + " item height=" + item.get_Height());
            item.process_is_visible(current_vertical_offset);
            if ( !the_browser.the_Pane.getChildren().contains(item.get_Node()))
            {
                the_browser.the_Pane.getChildren().add(item.get_Node());
                //if ( item instanceof Item_image ii) Item_image.currently.add(ii);
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

        //logger.log("currently Item_image (s): "+Item_image.currently.size());
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
    public int how_many_rows()
    //**********************************************************
    {
        return how_many_rows;
    }

    //**********************************************************
    public void show_how_many_files_deep_in_each_folder()
    //**********************************************************
    {
        show_total_size_deep_in_each_folder_done = false;

        folder_file_count_cache = new HashMap<>();

        for ( Item i : all_items_map.values())
        {
            if (i instanceof Item_button ini)
            {
                if(Files.isDirectory(ini.get_true_path()))
                {
                    ini.add_how_many_files_deep_folder(ini.get_button(), ini.text, ini.get_true_path(), folder_file_count_cache,aborter, logger);
                }
            }
        }
        show_how_many_files_deep_in_each_folder_done = true;

    }

    //**********************************************************
    public void show_total_size_deep_in_each_folder(List<Node> mandatory)
    //**********************************************************
    {
        show_how_many_files_deep_in_each_folder_done = false;
        folder_total_sizes_cache = new HashMap<>();
        logger.log("Virtual_landscape: show_total_size_deep_in_each_folder");
        AtomicInteger count = new AtomicInteger(0);
        Show_running_film_frame_with_abort_button show_running_film_frame = Show_running_film_frame_with_abort_button.show_running_film(count,"Computing folder sizes", 300, logger);
        for ( Item i : all_items_map.values())
        {
            if (i instanceof Item_button item_button)
            {
                if(Files.isDirectory(item_button.get_true_path()))
                {
                    item_button.add_total_size_deep_folder(count, item_button.get_button(), item_button.text, item_button.get_true_path(),
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
                    Jfx_batch_injector.inject(()-> compute_geometry(mandatory,"sort by size",null, show_running_film_frame),logger);
                    if ( System.currentTimeMillis()-start > 3000) {
                        Ding.play("display all folder sizes", logger);
                    }
                    return;
                }
            }

        };
        Actor_engine.execute(monitor,logger);
    }


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
        logger.log("\n\nVirtual_landscape::get_y_offset_of "+target+" NOT FOUND");

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

        if (((Item_image)item).aspect_ratio < 1.0)
        {
            if (dbg) logger.log("item is portrait aspect ratio: "+item.get_item_path());

            // portrait image
            width_of_this = column_increment * ((Item_image)item).aspect_ratio;
            double neg_x = 0;//(width_of_this-column_increment)/2.0;
            // shift left to compensate the portrait
            item.set_javafx_x(current_screen_x+neg_x);
            item.set_javafx_y(current_screen_y);
        }
        else
        {
            if( dbg) logger.log("item is landscape aspect ratio: "+item.get_item_path());
            item.set_javafx_x(current_screen_x);
            height_of_this = row_increment/((Item_image)item).aspect_ratio;
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
                if (((Item_image)i).aspect_ratio < 1.0)
                {
                    // portrait image
                    height=row_increment;
                }
                else
                {
                    // landscape image
                    height = row_increment/((Item_image)i).aspect_ratio;
                }
                if (i.get_screen_y_of_image()+height > max_y) max_y = i.get_screen_y_of_image()+height;
            }
            double row_height = (max_y-min_y);
            for(Item i : current_row)
            {
                double height = 0;
                if (((Item_image)i).aspect_ratio < 1.0)
                {
                    // portrait image
                    height=row_increment;
                }
                else
                {
                    // landscape image
                    height = row_increment/((Item_image)i).aspect_ratio;
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
            if ( dbg) logger.log("compute_bounding_rectangle, h="+h+" for "+item.get_string());

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
    public Comparator<? super Path> get_file_comparator()
    //**********************************************************
    {
        if (paths_manager == null) {
            logger.log(Stack_trace_getter.get_stack_trace("PANIC browser getting the current file comparator paths_manager==null"));
            return null;
        }
        if (image_file_comparator == null) {
            logger.log(Stack_trace_getter.get_stack_trace("PANIC browser getting the current file comparator paths_manager.image_file_comparator==null"));
            return null;
        }
        //logger.log(Stack_trace_getter.get_stack_trace("browser getting the current file comparator"+paths_manager.image_file_comparator.toString()));
        return image_file_comparator;
    }

    //**********************************************************
    public List<File> get_file_list()
    //**********************************************************
    {
        return paths_manager.get_file_list();
    }

    //**********************************************************
    public List<File> get_folder_list()
    //**********************************************************
    {
        return paths_manager.get_folder_list();
    }
}
