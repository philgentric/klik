package klik.browser.icons;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.browser.Landscape_height_listener;
import klik.browser.items.Item;
import klik.browser.items.Item_button;
import klik.browser.items.Item_folder_with_icon;
import klik.browser.items.Item_image;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.File_sorter;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//**********************************************************
public class Icon_manager
//**********************************************************
{
    public static final boolean dbg = false;
    public static final boolean dbg_visible = false;
    public static final boolean dbg_scroll = false;
    public static final int MIN_PARENT_AND_TRASH_BUTTON_WIDTH = 200;
    public static final int MIN_BUTTON_WIDTH = 300;
    public static final boolean add_and_remove = true;

    private final Logger logger;
    private Landscape_height_listener landscape_height_listener;
    private final Stage owner;
    public Paths_manager paths_manager;
    Aborter aborter = new Aborter();

    // state:
    private final Map<Path, Item> all_items_map = new HashMap<>();
    private double landscape_height = -Double.MAX_VALUE;
    private double current_vertical_offset = 0;
    private int how_many_rows;
    private Path top_left;
    public final double icon_height;

    //**********************************************************
    public Icon_manager(Stage owner_, Refresh_target refreshTarget, Logger logger_)
    //**********************************************************
    {
        owner = owner_;
        logger = logger_;
        //logger.log("WARNING Icon_manager::top_bar="+top_bar);
        paths_manager = new Paths_manager(refreshTarget, aborter, logger);
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
        for (Item i : all_items_map.values()) {
            //if (i instanceof Item_image)
            {
                //((Item_image) i).unset_image_is_selected();
                i.unset_image_is_selected();
            }
        }
    }


    //**********************************************************
    private void map_buttons_and_icons(Browser the_browser,
                                       Pane pane,
                                       List<Node> mandatory,
                                       //Error_type error_type,
                                       boolean single_column,
                                       boolean rebuild_all_items)
    //**********************************************************
    {
        double row_increment_for_dirs = 2 * Static_application_properties.get_font_size(logger);
        double icon_size = Static_application_properties.get_icon_size(logger);
        double column_increment = icon_size;
        int min_button_width = Static_application_properties.get_button_width(logger);
        if ( column_increment < min_button_width) column_increment = min_button_width;
        if ( single_column) column_increment = pane.getWidth();
        double row_increment_for_dirs_with_picture = row_increment_for_dirs + icon_size;
        double scene_width = the_browser.the_Scene.getWidth();
        pane.getChildren().clear();
        pane.getChildren().addAll(mandatory);
        double top_delta_y = 2 * Static_application_properties.get_font_size(logger);
        if (the_browser.error_type == Error_type.DENIED) {
            ImageView iv_denied = new ImageView(Look_and_feel_manager.get_denied_icon(icon_size));
            iv_denied.setPreserveRatio(true);
            iv_denied.setSmooth(true);
            iv_denied.setY(top_delta_y);
            pane.getChildren().add(iv_denied);
            compute_bounding_rectangle("Error_type.denied");
            return;
        }
        if (the_browser.error_type == Error_type.NOT_FOUND) {
            ImageView iv_denied = new ImageView(Look_and_feel_manager.get_not_found_icon(icon_size));
            iv_denied.setPreserveRatio(true);
            iv_denied.setSmooth(true);
            iv_denied.setY(top_delta_y);
            pane.getChildren().add(iv_denied);
            compute_bounding_rectangle("Error_type.not_found");
            return;
        }
        if (the_browser.error_type == Error_type.ERROR) {
            ImageView iv_denied = new ImageView(Look_and_feel_manager.get_unknown_error_icon(icon_size));
            iv_denied.setPreserveRatio(true);
            iv_denied.setSmooth(true);
            iv_denied.setY(top_delta_y);
            pane.getChildren().add(iv_denied);
            compute_bounding_rectangle("Error_type.unknown_error");
            return;
        }


        //if ( rebuild_all_items)
            all_items_map.clear();
        how_many_rows = 0;
        Point2D p = new Point2D(0, 0);
        p = process_folders(the_browser, single_column, row_increment_for_dirs, column_increment, row_increment_for_dirs_with_picture, scene_width, p);
        p = process_non_iconized_files(the_browser, single_column, column_increment, scene_width, p);
        process_iconified_items(the_browser, single_column, icon_size, column_increment, scene_width, p);
        compute_bounding_rectangle("map_buttons_and_icons() OK");
    }

    //**********************************************************
    private void process_iconified_items(Browser the_browser, boolean single_column, double icon_size, double column_increment, double scene_width, Point2D p)
    //**********************************************************
    {
        double file_button_height = 2 * Static_application_properties.get_font_size(logger);

        boolean show_icons_instead_of_text = Static_application_properties.get_show_icons(logger);
        boolean use_aspect_ratio = false;
        if ( (Static_application_properties.get_sort_files_by(logger) == File_sorter.ASPECT_RATIO) ||
            (Static_application_properties.get_sort_files_by(logger) == File_sorter.RANDOM_ASPECT_RATIO))
        {
            use_aspect_ratio = true;
        }
/*

        if (use_aspect_ratio)
        {
            // if the first image aspect_ratio < 1 the x in the Point2D must be NEGATIVE
            //so that the image left border is at screen_X=0
            if ( !paths_manager.get_iconized().isEmpty())
            {
                Path path = paths_manager.get_iconized().get(0);
                double aspect_ratio = Paths_manager.get_aspect_ratio(path);
                if ( aspect_ratio < 1)
                {
                    double neg_x = (aspect_ratio-1)*icon_size/2.0;
                    p = new Point2D(neg_x,p.getY());
                }
            }
        }
        */

        double max_y_in_row[] = new double[1];
        max_y_in_row[0] = -1;
        boolean[] done_shift_up = new boolean[1];
        done_shift_up[0] = false;
        // manage iconized items
        for (Path path : paths_manager.get_iconized())
        {
            Item item;
            if (show_icons_instead_of_text)
            {
                item = all_items_map.get(path);
                if ( item == null)
                {
                    double aspect_ratio = 1.0;
                    if ( use_aspect_ratio) aspect_ratio = paths_manager.aspect_ratio_cache.get_aspect_ratio(path);
                    item = new Item_image(the_browser,path, aspect_ratio, logger);
                    all_items_map.put(path,item);
                }
            }
            else
            {
                String size = Files_and_Paths.get_1_line_string_for_byte_data_size(path.toFile().length());
                item = all_items_map.get(path);
                if ( item == null)
                {
                    item = new Item_button(the_browser, path, path.getFileName().toString()+"("+size+")",
                            // TODO
                            icon_size/2,false,false, logger);
                    all_items_map.put(path,item);
                }
            }

            if (show_icons_instead_of_text)
            {

                p = compute_next_Point2D_for_icons(p, item,
                        icon_size, icon_size,
                        scene_width, single_column, use_aspect_ratio,max_y_in_row, done_shift_up);
            }
            else
            {
                p = new_Point_for_files_and_dirs(p, item,
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
        for (Path path : paths_manager.non_iconized)
        {
            //logger.log("process_non_iconized_files "+path.toAbsolutePath());
            String text = path.getFileName().toString();
            long size = path.toFile().length() / 1000_000L;
            if (Guess_file_type.is_this_path_a_video(path)) text = size + "MB VIDEO: " + text;
            Item item = all_items_map.get(path);
            if ( item == null)
            {
                item = new Item_button(the_browser,path, text,
                        // TODO MAGIC
                        icon_height, false, false, logger);
                all_items_map.put(path,item);
            }
            item.get_Node().setVisible(false);
            p = new_Point_for_files_and_dirs(p, item,
                    column_increment,
                    row_increment_for_files, scene_width, single_column);

            Item_button ini = (Item_button) item;
            ini.get_button().setPrefWidth(column_increment);
            ini.get_button().setMinWidth(column_increment);
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

    //**********************************************************
    private Point2D process_folders(Browser the_browser, boolean single_column, double row_increment_for_dirs, double column_increment, double row_increment_for_dirs_with_picture, double scene_width, Point2D p)
    //**********************************************************
    {
        double actual_row_increment;
        if ( Static_application_properties.get_show_icons_for_folders(logger))
        {
            actual_row_increment = row_increment_for_dirs_with_picture;
            for (Path folder_path : paths_manager.folders)
            {
                long start = System.currentTimeMillis();
                boolean has_picture = false;
                if (Item_folder_with_icon.would_produce_an_image_down_in_the_tree_files(folder_path, logger)) has_picture = true;
                if(dbg) logger.log("folder :"+folder_path+" took1 "+(System.currentTimeMillis()-start)+" milliseconds, has_picture="+has_picture);
                p = process_one_folder_with_picture(has_picture, the_browser, single_column, column_increment, actual_row_increment, scene_width, p, folder_path);
                if(dbg) logger.log("folder :"+folder_path+" took2 "+(System.currentTimeMillis()-start)+" milliseconds");
            }
        }
        else
        {
            actual_row_increment = row_increment_for_dirs;
            for (Path folder_path : paths_manager.folders)
            {
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
            boolean has_picture,
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
             folder_item = new Item_folder_with_icon(the_browser, folder_path, folder_path.getFileName().toString(), false, false, has_picture, logger);
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
            if ( Static_application_properties.get_show_icons_for_folders(logger))
            {
                folder_item = new Item_folder_with_icon(the_browser, folder_path, folder_path.getFileName().toString(), false, false, false, logger);
            }
            else
            {
                folder_item = new Item_button(the_browser,folder_path, folder_path.getFileName().toString(), icon_height, false, false, logger);
            }
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
    private Point2D compute_next_Point2D_for_icons(Point2D p,
                                                   Item item,
                                                   double column_increment,
                                                   double row_increment,
                                                   double scene_width,
                                                   boolean single_column,
                                                   boolean use_aspect_ratio,
                                                   double[] max_y_in_row,
                                                   boolean[] done_shift_up)
    //**********************************************************
    {
        double width_of_this = column_increment;
        double height_of_this = row_increment;
        double neg_x = 0;
        double neg_y = 0;
        if ( use_aspect_ratio)
        {
            //logger.log("aspect_ratio: "+((Item_image)item).aspect_ratio);
            if (((Item_image)item).aspect_ratio < 1.0)
            {
                width_of_this = ((Item_image)item).aspect_ratio * column_increment;
                //logger.log("width_of_this: "+width_of_this);
                neg_x = (width_of_this-column_increment)/2.0;
            }
            else
            {
                height_of_this = row_increment/((Item_image)item).aspect_ratio;
                neg_y = (height_of_this-row_increment)/2.0;
            }
        }

        double current_x = p.getX();
        double current_y = p.getY();
        if ( current_x == 0)
        {
            current_x += neg_x; // first image in row is shifted LEFT to get screen_x = 0;
            if ( neg_y < 0)
            {
                if ( !done_shift_up[0] )
                {
                    current_y += neg_y; // ONCE, first image in row shifted UP to stick to the previous row bottom
                    done_shift_up[0] = true;
                }
            }
        }
        // position the ImageView at the requested position
        item.set_x(current_x);
        item.set_y(current_y);
        if ( max_y_in_row[0] < current_y+height_of_this) max_y_in_row[0] = current_y+height_of_this;
        /// then compute position of NEXT item
        if ( single_column)
        {
            how_many_rows++;
            double future_x = 0;
            double future_y = current_y + row_increment;
            //logger.log("new row "+row_increment);
            return new Point2D(future_x, future_y);
        }



        double real_future_x = current_x+ width_of_this;
        //logger.log("new_x: "+new_x);
        if (real_future_x + column_increment > scene_width)
        {
            // new ROW
            how_many_rows++;
            Point2D returned =  new Point2D(0, max_y_in_row[0]);
            max_y_in_row[0] = -1;
            return returned;
        }

        // continued row
        return new Point2D(current_x+width_of_this, current_y);
    }

    //**********************************************************
    public void clear_aspect_ratio_cache()
    //**********************************************************
    {
        if ( paths_manager.aspect_ratio_cache == null) return;
        paths_manager.aspect_ratio_cache.clear_aspect_ratio_RAM_cache();
    }


    //**********************************************************
    private Point2D new_Point_for_files_and_dirs(Point2D point,
                                                 Item last_item,
                                                 double column_increment,
                                                 double row_increment,
                                                 double scene_width,
                                                 boolean single_column)
    //**********************************************************
    {
        double old_x = point.getX();
        double old_y = point.getY();
        last_item.set_x(old_x);
        last_item.set_y(old_y);

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
            // too far right, need to create a new row for THIS item
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
        if ( dbg_scroll) logger.log("compute_bounding_rectangle() "+reason);
        // compute bounding rectangle

        double x_min = Double.MAX_VALUE;
        double x_max = 0;
        double y_min = Double.MAX_VALUE;
        landscape_height = 0;
        for (Item item : all_items_map.values()) {
            if (item.get_x() < x_min) x_min = item.get_x();
            if (item.get_x() + item.get_Width() > x_max)
            {
                x_max = item.get_x() + item.get_Width();
            }
            if (item.get_y() < y_min)
            {
                y_min = item.get_y();
            }
            double h = item.get_Height();
            if ( dbg) logger.log("h="+h+" for "+item.get_string());

            if (item.get_y() + h > landscape_height) landscape_height = item.get_y() + h;
        }

        if (paths_manager.get_iconized().isEmpty()) {
            // when there is no iconized items in the folder
            // it may happen that the height of the last row of buttons at the bottom is underestimated
            landscape_height += 100;
        }
        if ( dbg_scroll)
            logger.log("landscape_height="+landscape_height);
        if ( landscape_height_listener != null)
        {
            landscape_height_listener.browsed_landscape_height_has_changed(landscape_height,current_vertical_offset);
        }
    }

    //**********************************************************
    public void geometry_changed(Browser b,
                                 Pane pane,
                                 List<Node> mandatory,
                                 String reason,
                                 //Error_type error_type,
                                 boolean rebuild_all_items
    )
    //**********************************************************
    {
        if ( dbg_scroll)
            logger.log("geometry_changed reason="+reason+" current_vertical_offset="+current_vertical_offset+" rebuild_all_items="+rebuild_all_items);
        boolean single_column = Static_application_properties.get_single_column(logger);
        if ( dbg_scroll) logger.log(Stack_trace_getter.get_stack_trace("geometry_changed single_column="+single_column));
        map_buttons_and_icons(b, pane, mandatory,
                //error_type,
                single_column, rebuild_all_items);
        move_absolute(pane, current_vertical_offset, reason);
        b.update_slider(current_vertical_offset);
    }

    //**********************************************************
    public void move_absolute(
            Pane pane,
            double new_vertical_offset,
            String reason)
    //**********************************************************
    {
        if (dbg_scroll)
            logger.log("move_absolute reason= " + reason + " new_vertical_offset=" + new_vertical_offset);
        current_vertical_offset = new_vertical_offset;
        check_visibility(pane);
    }

    //**********************************************************
    void check_visibility(Pane pane)
    //**********************************************************
    {
        double h = pane.getHeight();
        int icon_size = Static_application_properties.get_icon_size(logger);
        double min_y = Double.MAX_VALUE;
        for (Item item : all_items_map.values()) {
            //if (item.get_y() + item.get_Height() - current_vertical_offset < 0)
            if (item.get_y() + item.get_Height() < current_vertical_offset -icon_size)
            {
                if (dbg_visible)
                    logger.log(item.get_item_path() + " invisible (too far up) y=" + item.get_y() + " item height=" + item.get_Height());
                process_is_invisible(pane, item);
                continue;
            }
            if (item.get_y()  > h+current_vertical_offset+icon_size) {
                if (dbg_visible) logger.log(item.get_item_path() + " invisible (too far down)");
                process_is_invisible(pane, item);
                continue;
            }
            if (dbg_visible)
                logger.log(item.get_item_path() + " visible  y=" + item.get_y() + " item height=" + item.get_Height());
            process_is_visible(pane, item);
            if ( item.get_x() > 0) continue;
            if ( item.get_y() < min_y)
            {
                min_y = item.get_y();
                top_left = item.get_item_path();
            }
        }

    }


    //**********************************************************
    private void process_is_visible(Pane pane, Item item)
    //**********************************************************
    {
        //if (show_icons_instead_of_text == false) return;

        {
            item.visible_in_scene = true;
            if (item instanceof Item_image ii) {

                switch (ii.icon_status) {
                    case no_icon:
                        ii.load_default_icon();
                    case default_icon: {
                        if(dbg) logger.log("process_is_visible: making icon factory request for: "+ii.get_item_path());
                        ii.request_icon_to_factory(owner);
                    }
                    break;
                    case true_icon_requested:
                    case true_icon_in_the_making:
                    case true_icon:
                        break;
                }
            }
            //if (item.get_Node() == null) logger.log("item.get_Node() == null");
            if (!pane.getChildren().contains(item.get_Node()))
            {
                if (dbg_visible) logger.log("adding item: " + item.get_string());
                pane.getChildren().add(item.get_Node());
            }
            item.set_visible(true);
        }


        item.set_translate_X(item.get_x());
        item.set_translate_Y(item.get_y() - current_vertical_offset);
    }

    //**********************************************************
    private void process_is_invisible(Pane pane, Item item)
    //**********************************************************
    {
        if (item.visible_in_scene) {
            item.visible_in_scene = false;
            item.cancel();
            if (item.get_Node() == null) return;
            item.get_Node().setVisible(false);
            if (add_and_remove) {
                if (dbg_visible) logger.log("removing from pane invisible icon of: " + item.get_string());
                pane.getChildren().remove(item.get_Node());

            }
            if (item instanceof Item_image ii) {
                // let us hope the GC might save us !
                // i.e. in directories with very large number of images
                // the icon manager can cause an OutOfMemory if we would keep invisible images in memory
                ii.set_Image(null, false);//Static_image_utilities.get_default_icon(icon_size, logger), false);
            }
        }
    }




    //**********************************************************
    public void modify_button_fonts(double v)
    //**********************************************************
    {
        for (Item i : all_items_map.values()) {
            if (i instanceof Item_button) {
                Item_button ini = (Item_button) i;
                double s = ini.get_button().getFont().getSize();
                Font f = new Font(s * v);
                ini.get_button().setFont(f);

            }
        }
    }


    //**********************************************************
    public Item get_item_under(Pane pane, double x, double y)
    //**********************************************************
    {

        for (Item item : all_items_map.values()) {
            Node node = item.get_Node();
            if (!pane.getChildren().contains(node)) continue;
            Bounds b = node.getBoundsInParent();
            if (b.contains(x, y)) {
                //logger.log("2YES ! for " + item.get_icon_path() + " we have bounds X= " + b.getMinX() + " " + b.getMaxX() + " Y= " + b.getMinY() + " " + b.getMaxY());
                return item;
            } else {
                //logger.log("2NO ? for " + item.get_icon_path() + " we have bounds X= " + b.getMinX() + " " + b.getMaxX() + " Y= " + b.getMinY() + " " + b.getMaxY());
            }
        }
        return null;
    }

    //**********************************************************
    public List<Item> get_items_in(Pane pane, double x, double y, double w, double h)
    //**********************************************************
    {
        Bounds bounds = new BoundingBox(x, y, w, h);
        //logger.log("selection  X= " + bounds.getMinX() + " " + bounds.getMaxX() + " Y= " + bounds.getMinY() + " " + bounds.getMaxY());
        List<Item> returned = new ArrayList<>();

        for (Item item : all_items_map.values()) {
            Node node = item.get_Node();
            if (!pane.getChildren().contains(node)) continue;
            Bounds b = node.getBoundsInParent();
            //if (b.intersects(bounds))
            if (bounds.contains(b)) {
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
    public void cancel_all()
    //**********************************************************
    {
        //logger.log(("Icon_manager cancel all!"));
        aborter.abort();
        Icon_factory_actor.get_icon_factory(owner, logger).cancel_all();
        for ( Item i : all_items_map.values())
        {
            i.cancel();
        }
        Icon_factory_actor.reset_videos_for_which_giffing_failed();
    }

    //**********************************************************
    public void show_how_many_files_in_each_folder()
    //**********************************************************
    {
        for ( Item i : all_items_map.values())
        {
            if (i instanceof Item_button ini)
            {
                if(Files.isDirectory(ini.get_true_path()))
                {
                    Item_button.show_how_many_files_folder(ini.get_button(), ini.text, ini.get_true_path(), aborter, logger);
                }
            }
        }
    }

    //**********************************************************
    public Path get_top_left(Pane pane)
    //**********************************************************
    {
        check_visibility(pane);
        //logger.log("Icon_manager::get_top_left"+top_left);
        return top_left;
    }

    //**********************************************************
    public double get_y_offset_of(Path target)
    //**********************************************************
    {
        //logger.log("\n\nIcon_manager::get_y_offset_of "+target);
        String t2 = target.toAbsolutePath().toString();
        for ( Item i : all_items_map.values())
        {
            //logger.log("\n\nIcon_manager::get_y_offset_of ... looking at "+i.get_item_path());

            if ( i.get_item_path().toAbsolutePath().toString().equals(t2))
            {
                //logger.log("\n\nIcon_manager::get_y_offset_of "+target+ " FOUND offset = "+i.get_y());
                return i.get_y();
            }
        }
        //logger.log("\n\nIcon_manager::get_y_offset_of "+target+" NOT FOUND");

        return 0;
    }
}
