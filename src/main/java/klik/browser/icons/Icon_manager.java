//SOURCES ../Landscape_height_listener.java
//SOURCES ../Scroll_to_listener.java
//SOURCES ./caches/Image_properties.java
//SOURCES ../../look/Look_and_feel.java
//SOURCES ../items/Item_folder_with_icon.java
//SOURCES ../items/My_colors.java
package klik.browser.icons;

import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job_termination_reporter;
import klik.browser.Browser;
import klik.browser.Change_type;
import klik.browser.Landscape_height_listener;
import klik.browser.Scroll_to_listener;
import klik.browser.icons.caches.Image_properties;
import klik.browser.items.*;
import klik.util.files_and_paths.*;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Hourglass;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Show_running_man_frame_with_abort_button;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


//**********************************************************
public class Icon_manager
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

    // state:
    private final ConcurrentHashMap<Path, Item> all_items_map = new ConcurrentHashMap<>();
    AtomicBoolean items_are_ready = new AtomicBoolean(false);

    private double landscape_height = -Double.MAX_VALUE;
    private double current_vertical_offset = 0;
    private int how_many_rows;
    private Path top_left;
    public final double icon_height;

    boolean show_how_many_files_deep_in_each_folder = false;
    boolean show_total_size_deep_in_each_folder = false;

    Map<Path,Long> folder_total_sizes_cache;
    Map<Path,Long> folder_file_count_cache;

    //**********************************************************
    public Icon_manager(Paths_manager paths_manager, Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.paths_manager = paths_manager;
        this.aborter = aborter;
        logger = logger_;
        if ( dbg) logger.log("Icon_manager constructor");
        double font_size = Static_application_properties.get_font_size(logger);
        icon_height = Look_and_feel.MAGIC_HEIGHT_FACTOR*font_size;

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
    AtomicBoolean map_buttons_and_icons_guard = new AtomicBoolean(false);

    long map_buttons_and_icons_elapsed = 0;
    //**********************************************************
    private void map_buttons_and_icons_8(Browser the_browser,
                                         Pane pane,
                                         List<Node> mandatory,
                                         boolean single_column,
                                         String reason,
                                         Change_type change_type,
                                         Path scroll_to,
                                         Hourglass running_man)
    //**********************************************************
    {
        if ( map_buttons_and_icons_guard.get())
        {
            logger.log("map_buttons_and_icons_guard activated " +reason);
            if ( running_man != null) running_man.close();
            return;
        }
        map_buttons_and_icons_guard.set(true);

        long start = System.nanoTime();
         if ( dbg) logger.log("Icon_manager map_buttons_and_icons");

        double row_increment_for_dirs = 2 * Static_application_properties.get_font_size(logger);
        int folder_icon_size = Static_application_properties.get_folder_icon_size(logger);
        int column_width = Static_application_properties.get_column_width(logger);
        int column_increment_for_folders = column_width;
        if ( column_increment_for_folders < folder_icon_size) column_increment_for_folders = folder_icon_size;

        int icon_size = Static_application_properties.get_icon_size(logger);
        int column_increment_for_icons = icon_size;

        if ( single_column)
        {
            // the -100 is to make the button shorter than the full width so that
            // the mouse selection can "start" in the rightmost part of the pane
            column_increment_for_icons = (int)(pane.getWidth()-RIGHT_SIDE_SINGLE_COLUMN_MARGIN);
            column_increment_for_folders = column_increment_for_icons;
        }
        double row_increment_for_dirs_with_picture = row_increment_for_dirs + folder_icon_size;

        double scene_width = the_browser.the_Scene.getWidth();

        double top_delta_y = 2 * Static_application_properties.get_font_size(logger);
        if (the_browser.error_type == Error_type.DENIED) {
            ImageView iv_denied = new ImageView(Look_and_feel_manager.get_denied_icon(icon_size));
            show_error_icon(the_browser, pane, iv_denied, top_delta_y);
            if ( running_man != null) running_man.close();
            return;
        }
        if (the_browser.error_type == Error_type.NOT_FOUND) {
            ImageView iv_denied = new ImageView(Look_and_feel_manager.get_not_found_icon(icon_size));
            show_error_icon(the_browser, pane, iv_denied, top_delta_y);
            if ( running_man != null) running_man.close();
            return;
        }
        if (the_browser.error_type == Error_type.ERROR) {
            ImageView iv_denied = new ImageView(Look_and_feel_manager.get_unknown_error_icon(icon_size));
            show_error_icon(the_browser, pane, iv_denied, top_delta_y);
            if ( running_man != null) running_man.close();
            return;
        }


        if (( change_type ==  Change_type.files_or_folders_changed) || (change_type == Change_type.layout_changed))
        {
            pane.getChildren().clear();
            pane.getChildren().addAll(mandatory);
        }

        int final_column_increment_for_folders = column_increment_for_folders;
        int final_column_increment_for_icons = column_increment_for_icons;

        Runnable r = () -> map_it(the_browser, pane, single_column, reason, change_type, scroll_to, running_man, row_increment_for_dirs, final_column_increment_for_folders, row_increment_for_dirs_with_picture, scene_width, icon_size, final_column_increment_for_icons, start);
        Actor_engine.execute(r,logger);
    }

    //**********************************************************
    private void map_it(Browser the_browser, Pane pane, boolean single_column, String reason, Change_type change_type, Path scroll_to, Hourglass running_man, double row_increment_for_dirs, int final_column_increment_for_folders, double row_increment_for_dirs_with_picture, double scene_width, int icon_size, int final_column_increment_for_icons, long start)
    //**********************************************************
    {
        //logger.log("map_buttons_and_icons thread started");

        if ( change_type ==  Change_type.files_or_folders_changed)
        {
            all_items_map.clear();
            items_are_ready.set(false);
        }

        how_many_rows = 0;

        Point2D p = new Point2D(0, 0);
        p = process_folders(the_browser, single_column, row_increment_for_dirs, final_column_increment_for_folders, row_increment_for_dirs_with_picture, scene_width, p);
        p = new Point2D(p.getX(),p.getY()+MARGIN_Y);
        p = process_non_iconized_files(the_browser, single_column, final_column_increment_for_folders, scene_width, p);
        p = new Point2D(p.getX(),p.getY()+MARGIN_Y);


        process_iconified_items(the_browser, single_column, icon_size, final_column_increment_for_icons, scene_width, p);

        compute_bounding_rectangle("map_buttons_and_icons() OK "+p.getX()+" "+p.getY());
        map_buttons_and_icons_elapsed += (System.nanoTime()- start);
        //logger.log("map_buttons_and_icons_elapsed= "+String.format("%.2f",map_buttons_and_icons_elapsed/1000_000.0)+" ms "+reason);


        if (( change_type ==  Change_type.files_or_folders_changed) || (change_type == Change_type.layout_changed))
        {
            Jfx_batch_injector.inject(() -> {
                for (Item ii : all_items_map.values())
                {
                    if (pane.getChildren().contains(ii.get_Node()))
                    {
                        logger.log("shit happens " + ii.get_item_path());
                    }
                    else
                    {
                        pane.getChildren().add(ii.get_Node());
                    }
                }
                end_map(pane, reason, true, scroll_to, running_man);
            }, logger);
        }
        else
        {
            end_map(pane, reason, false, scroll_to, running_man);
        }
    }

    //**********************************************************
    private void end_map(Pane pane, String reason, boolean from_thread, Path scroll_to, Hourglass running_man)
    //**********************************************************
    {
        items_are_ready.set(true);
        map_buttons_and_icons_guard.set(false);
        //has_to_call_check_visibility_latter.set(false);
        Jfx_batch_injector.inject(()->
                {
                    check_visibility_11(pane, reason+" map_buttons_and_icons "+from_thread);
                    if ( running_man != null) running_man.close();
                    scroll_to(scroll_to);
                },logger);
    }

    //**********************************************************
    void scroll_to(Path scroll_to)
    //**********************************************************
    {
        current_vertical_offset = get_y_offset_of(scroll_to);
        if ( scroll_to_listener == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOUKLD NOT HAPPEN"));
            return;
        }
        scroll_to_listener.perform_scroll_to(current_vertical_offset,this);
    }
    //**********************************************************
    private void show_error_icon(Browser the_browser, Pane pane, ImageView iv_denied, double top_delta_y)
    //**********************************************************
    {
        iv_denied.setPreserveRatio(true);
        iv_denied.setSmooth(true);
        iv_denied.setY(top_delta_y);
        if ( Platform.isFxApplicationThread())
        {
            pane.getChildren().add(iv_denied);
        }
        else
        {
            Jfx_batch_injector.inject(()-> pane.getChildren().add(iv_denied),logger);
        }
        compute_bounding_rectangle(the_browser.error_type.toString());
    }

    //**********************************************************
    private synchronized void process_iconified_items(Browser the_browser, boolean single_column, double icon_size, double column_increment, double scene_width, Point2D point)
    //**********************************************************
    {

        double file_button_height = 2 * Static_application_properties.get_font_size(logger);

        boolean show_icons_instead_of_text = Static_application_properties.get_show_icons(logger);
        double max_y_in_row[] = new double[1];
        max_y_in_row[0] = 0;
        List<Item> current_row = new ArrayList<>();
        // manage iconized items
        AtomicInteger image_properties_in_flight = new AtomicInteger(0);
        Job_termination_reporter tr = (message, job) -> image_properties_in_flight.decrementAndGet();

        for (Path path : paths_manager.iconized_paths )
        {
            if (dbg) logger.log("Icon_manager process_iconified_items " + path);
            Item item;
            if (show_icons_instead_of_text)
            {
                item = all_items_map.get(path);
                if (item == null)
                {
                    image_properties_in_flight.incrementAndGet();
                    // ask for image properties fetch in threads
                    paths_manager.image_properties_cache.get_from_cache(path,tr, false);
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
        // wait for all properties to become available
        long start = System.currentTimeMillis();
        for(;;)
        {
           try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.log(Stack_trace_getter.get_stack_trace("oho"));
            }
            int k = image_properties_in_flight.get();
            if (dbg) logger.log("icons in flight = "+k);
            if (k == 0 ) break;
        }
        if (dbg) logger.log("total wait time for image properties = "+(System.currentTimeMillis()-start)+"ms");

        for ( Path path : paths_manager.iconized_paths)
        {
            Image_properties ip = paths_manager.image_properties_cache.get_from_cache(path,null, true);
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
        List<Path> ll = paths_manager.get_iconized_sorted("process_iconified_items");
        for (Path path : ll )
        {
            Item item = all_items_map.get(path);
            if ( item == null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("should not happen: no item in map for: "+path+" map size="+all_items_map.size() ));
                return;
            }
            if (dbg)  logger.log("Icon_manager process_iconified_items " + path+" ar:"+((Item_image)item).aspect_ratio);

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
            if (dbg) logger.log("Icon_manager process_non_iconized_files "+path.toAbsolutePath());
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
        if (dbg) logger.log("Icon_manager process_folders0 ");

        double actual_row_increment;
        if ( Static_application_properties.get_show_icons_for_folders(logger))
        {
            actual_row_increment = row_increment_for_dirs_with_picture;

            for (Path folder_path : paths_manager.folders.keySet())
            {
                if (dbg) logger.log("Icon_manager process_folders1 "+folder_path);
                long start = System.currentTimeMillis();
                if(dbg) logger.log("folder :"+folder_path+" took1 "+(System.currentTimeMillis()-start)+" milliseconds");
                p = process_one_folder_with_picture(the_browser, single_column, column_increment, actual_row_increment, scene_width, p, folder_path, Color.BEIGE);
                if(dbg) logger.log("folder :"+folder_path+" took2 "+(System.currentTimeMillis()-start)+" milliseconds");
            }
        }
        else
        {
            actual_row_increment = row_increment_for_dirs;
            List<Path> keyset = new ArrayList<>(paths_manager.folders.keySet());
            if ( show_total_size_deep_in_each_folder)
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
                Collections.sort(keyset,comp);
            }
            for (Path folder_path : keyset)
            {
                if (dbg) logger.log("Icon_manager process_folders2 "+folder_path);
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

            if ( show_how_many_files_deep_in_each_folder)
            {
                Long how_many_files_deep = folder_file_count_cache.get(folder_path);
                if ( how_many_files_deep == null)
                {
                    logger.log("WARNING: folder_file_count_cache not found for "+folder_path);
                }
                else
                {
                    tmp +=   " (" + how_many_files_deep + " files)";
                }
            }
            else if ( show_total_size_deep_in_each_folder)
            {
                Long bytes = folder_total_sizes_cache.get(folder_path);
                if ( bytes == null)
                {
                    logger.log("WARNING: folder_total_sizes_cache not found for "+folder_path);
                }
                else
                {
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
        if ( paths_manager.image_properties_cache == null) return;
        paths_manager.image_properties_cache.clear_RAM_cache_fx();
    }

    //**********************************************************
    public void geometry_changed_7(Browser b,
                                   Pane pane,
                                   List<Node> mandatory,
                                   String reason,
                                   Change_type change_type,
                                   Path scroll_to,
                                   Hourglass running_man)
    //**********************************************************
    {
        if (scroll_dbg)
            logger.log("geometry_changed reason="+reason+" current_vertical_offset="+current_vertical_offset+" change_type=" +change_type);
        boolean single_column = Static_application_properties.get_single_column(logger);
        if (scroll_dbg) logger.log(Stack_trace_getter.get_stack_trace("geometry_changed single_column="+single_column));
        map_buttons_and_icons_8(b, pane, mandatory, single_column, reason, change_type, scroll_to, running_man);
//        move_absolute(pane, current_vertical_offset, reason);

    }


    //**********************************************************
    public void move_absolute(
            Pane pane,
            double new_vertical_offset,
            String reason)
    //**********************************************************
    {
        if (scroll_dbg)
            logger.log("move_absolute reason= " + reason + " new_vertical_offset=" + new_vertical_offset);
        current_vertical_offset = new_vertical_offset;
        check_visibility_11(pane, "move_absolute");
    }

    // this is on the FX thread
    // and it is called very often for example when scrolling
    //**********************************************************
    void check_visibility_11(Pane pane, String from)
    //**********************************************************
    {
        if ( !items_are_ready.get())
        {
            //logger.log("check_visibility: items are not ready yet ! "+from);
            //has_to_call_check_visibility_latter.set(true);
            return;
        }
        //logger.log("check_visibility: "+ all_items_map.values().size()+" items are ready "+from);
        double h = pane.getHeight();
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
                continue;
            }
            if (item.get_javafx_y()  > h+current_vertical_offset+icon_size)
            {
                if (invisible_dbg) logger.log(item.get_item_path() + " invisible (too far down)");
                item.process_is_invisible(current_vertical_offset);
                continue;
            }
            if (visible_dbg)
                logger.log(item.get_item_path() + " Item is visible at y=" + item.get_javafx_y() + " item height=" + item.get_Height());
            item.process_is_visible(current_vertical_offset);

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
    public double get_landscape_height()
    //**********************************************************
    {
        return landscape_height;
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
        show_how_many_files_deep_in_each_folder = true;
        show_total_size_deep_in_each_folder = false;
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
    }

    //**********************************************************
    public void show_total_size_deep_in_each_folder(Browser browser,
                                                    Pane pane,
                                                    List<Node> mandatory)
    //**********************************************************
    {
        show_total_size_deep_in_each_folder = true;
        show_how_many_files_deep_in_each_folder = false;
        folder_total_sizes_cache = new HashMap<>();
        logger.log("Icon_manager: show_total_size_deep_in_each_folder");
        AtomicInteger count = new AtomicInteger(0);
        Show_running_man_frame_with_abort_button show_running_man_frame = Show_running_man_frame_with_abort_button.show_running_man("Computing folder sizes", 300, logger);
        for ( Item i : all_items_map.values())
        {
            if (i instanceof Item_button item_button)
            {
                if(Files.isDirectory(item_button.get_true_path()))
                {
                    item_button.add_total_size_deep_folder(count, item_button.get_button(), item_button.text, item_button.get_true_path(), folder_total_sizes_cache, show_running_man_frame.aborter, logger);
                }
            }
        }
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
                    Jfx_batch_injector.inject(()-> geometry_changed_7(browser,pane,mandatory,"sort by size",Change_type.layout_changed,null, show_running_man_frame),logger);
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
    public Path get_top_left(Pane pane)
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
        logger.log("\n\nIcon_manager::get_y_offset_of "+target+" NOT FOUND");

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
        double x_max = 0;
        double y_min = Double.MAX_VALUE;
        landscape_height = 0;
        for (Item item : all_items_map.values()) {
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

            if (item.get_javafx_y() + h > landscape_height) landscape_height = item.get_javafx_y() + h;
        }

        if (paths_manager.get_iconized_sorted("compute_bounding_rectangle").isEmpty()) {
            // when there is no iconized items in the folder
            // it may happen that the height of the last row of buttons at the bottom is underestimated
            landscape_height += 100;
        }
        if (scroll_dbg)
            logger.log("landscape_height="+landscape_height);
        if ( landscape_height_listener != null)
        {
            landscape_height_listener.browsed_landscape_height_has_changed(landscape_height,current_vertical_offset);
        }
    }


    //**********************************************************
    public void set_scroll_to_listener(Scroll_to_listener vertical_slider)
    //**********************************************************
    {
        scroll_to_listener = vertical_slider;
    }
}
