package klik.browser;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import klik.look.Look_and_feel_manager;
import klik.properties.Properties;
import klik.util.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

//**********************************************************
public class Icon_manager
//**********************************************************
{
    public static final boolean show_video_as_gif = true;
    public static final boolean dbg = false;
    public static final boolean add_and_remove = true;
    public static final String ACCESS_DENIED_EXCEPTION = "AccessDeniedException";
    public static final String GONE_DIR_EXCEPTION = "GONE_DIR_EXCEPTION";
    public static final String OK = "OK";
    public static final String IOEXCEPTION = "IOException";

    private Logger logger;
    private List<Item> all_items = new ArrayList<>();
    private Map<Path, Item> map = new HashMap<Path, Item>();

    private List<Path> paths_list_dirs = new ArrayList<>();
    private List<Path> paths_list_non_image_files = new ArrayList<>();
    private List<Path> paths_list_images = new ArrayList<>();
    private double x_min = Double.MAX_VALUE;
    private double x_max = -Double.MAX_VALUE;
    private double y_min = Double.MAX_VALUE;
    public double y_max = -Double.MAX_VALUE;

    // state:
    private double y_offset = 0;
    //int n_threads;
    private boolean denied = false;
    public Comparator<? super Path> file_comparator;
    public double max_dir_text_length;

    Y_max_listener y_max_listener;
    Exception_recorder exception_recorder;

    //**********************************************************
    public Icon_manager(Y_max_listener y_max_listener_, Logger logger_)
    //**********************************************************
    {
        y_max_listener = y_max_listener_;
        exception_recorder = (Exception_recorder) y_max_listener_;
        logger = logger_;
    }

    //**********************************************************
    public String scan_dir(Path dir, List<Path> videos_for_which_giffing_failed)
    //**********************************************************
    {

        if (Tool_box.get_sort_files_by_name()) {
            file_comparator = alphabetical_file_name_comparator;
        } else {
            file_comparator = decreasing_file_size_comparator;
        }

        boolean show_hidden_files = Properties.get_show_hidden_files();
        boolean show_only_gifs = Properties.get_show_only_gifs();
        paths_list_dirs.clear();
        max_dir_text_length = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))//, "*.{jpg,JPG,gif,GIF,png,PNG,jpeg,JPEG}"))
        {
            for (Path f : stream) {
                if (show_hidden_files == false) if (f.getFileName().toString().startsWith(".")) continue;
                if (Files.isDirectory(f) == false) continue;
                // directory
                paths_list_dirs.add(f);
                Text t = new Text(f.getFileName().toString());
                double l = t.getLayoutBounds().getWidth();
                if (l > max_dir_text_length) max_dir_text_length = l;
            }
            Collections.sort(paths_list_dirs, file_comparator);
        } catch (AccessDeniedException e) {
            logger.log(Stack_trace_getter.get_stack_trace(ACCESS_DENIED_EXCEPTION + e));
            denied = true;
            return ACCESS_DENIED_EXCEPTION;
        } catch (NoSuchFileException e) {
            logger.log(Stack_trace_getter.get_stack_trace("NoSuchFileException" + e));
            // the DIR is gone !!
            denied = true;
            return GONE_DIR_EXCEPTION;
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            return IOEXCEPTION;
        }

        paths_list_non_image_files.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))//, "*.{jpg,JPG,gif,GIF,png,PNG,jpeg,JPEG}"))
        {
            for (Path f : stream) {
                if (show_hidden_files == false) if (f.getFileName().toString().startsWith(".")) continue;
                if (Files.isDirectory(f)) continue;
                if (show_video_as_gif)
                {
                    if ( Guess_file_type_from_extension.is_this_path_a_video(f))
                    {
                        if (videos_for_which_giffing_failed.contains(f))
                        {
                            paths_list_non_image_files.add(f);
                        }
                        continue;
                    }
                }
                if (Guess_file_type_from_extension.is_this_path_an_image(f)) continue;
                // non-image, non-directory
                paths_list_non_image_files.add(f);
            }
            Collections.sort(paths_list_non_image_files, file_comparator);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            return IOEXCEPTION;
        }

        paths_list_images.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))//, "*.{jpg,JPG,gif,GIF,png,PNG,jpeg,JPEG}"))
        {
            for (Path path : stream) {
                if (show_hidden_files == false) if (path.getFileName().toString().startsWith(".")) continue;
                if (Files.isDirectory(path)) continue;
                if (show_video_as_gif)
                {
                    if ( Guess_file_type_from_extension.is_this_path_a_video(path))
                    {
                        if (videos_for_which_giffing_failed.contains(path))
                        {
                            // is not to be considered as an image
                            continue;
                        }
                        else
                        {
                            paths_list_images.add(path);
                            continue;
                        }
                    }
                }

                if (Guess_file_type_from_extension.is_this_path_an_image(path) == false) continue;
                if (show_only_gifs) if (Guess_file_type_from_extension.is_gif_extension(path) == false) continue;
                paths_list_images.add(path);
                //logger.log(f.toString()+" is image OR video");

            }
            Collections.sort(paths_list_images, file_comparator);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            return IOEXCEPTION;
        }

        return OK;
    }


    //**********************************************************
    public final static Comparator<Path> alphabetical_file_name_comparator = new Comparator<Path>() {
        @Override
        public int compare(Path f1, Path f2) {
            return f1.getFileName().toString().compareTo(f2.getFileName().toString());
        }
    };

    public final static Comparator<Path> decreasing_file_size_comparator = new Comparator<Path>() {
        @Override
        public int compare(Path f1, Path f2) {
            return (Long.valueOf(f2.toFile().length())).compareTo(Long.valueOf(f1.toFile().length()));
        }
    };


    //**********************************************************
    private void map_icons(Browser browser,
                           Stage the_stage,
                           Scene scene,
                           Pane pane,
                           List<Node> mandatory,
                           double top_button_height,
                           double dir_button_width,
                           double dir_button_height,
                           double file_button_height
    )
    //**********************************************************
    {

        double icon_size = Properties.get_icon_size();
        //Look_and_feel_manager.load_default_icon(icon_size, logger);

        double scene_width = scene.getWidth();
        //logger.log("\n\nGET scene_widh="+scene_width);
        pane.getChildren().clear();
        pane.getChildren().addAll(mandatory);
        if (denied) {
            ImageView iv_denied = new ImageView(Look_and_feel_manager.get_denied_icon(icon_size));
            iv_denied.setPreserveRatio(true);
            iv_denied.setSmooth(true);
            iv_denied.setY(top_button_height);
            pane.getChildren().add(iv_denied);
            compute_bounding_rectangle();

            return;
        }
        all_items.clear();
        Point2D p = new Point2D(0, top_button_height);
        for (Path f : paths_list_dirs) {
            Item item = new Item_non_image(browser, the_stage, pane, f, f.getFileName().toString(), scene,
                    dir_button_height, file_button_height, logger);
            map.put(f, item);
            all_items.add(item);
            p = new_Point(p, item, dir_button_width, dir_button_height, scene_width, dir_button_width);

        }

        if (p.getX() != 0) p = new Point2D(0, p.getY() + dir_button_height);

        for (Path f : paths_list_non_image_files) {

            String text = f.getFileName().toString();
            long size = f.toFile().length() / 1000000L;
            if (Guess_file_type_from_extension.is_this_path_a_video(f)) text = size + "MB VIDEO: " + text;
            Item item = new Item_non_image(browser, the_stage, pane, f, text, scene,
                    dir_button_height, file_button_height, logger);
            map.put(f, item);
            all_items.add(item);
            item.get_Node().setVisible(false);
            p = new_Point(p, item, dir_button_width, file_button_height, scene_width, dir_button_width);
        }

        if (p.getX() != 0) p = new Point2D(0, p.getY() + dir_button_height);

        double max_h = 0;
        double item_width = icon_size;
        for (Path f : paths_list_images) {
            Item item = null;

            if (Tool_box.get_show_icons() == false)
            {
                item = new Item_non_image(browser, the_stage, pane, f,
                        f.getFileName().toString() + "\n" + f.toFile().length() + " Bytes",
                        scene,
                        dir_button_height,
                        file_button_height, logger);
            }
            else
            {
                //logger.log(f.toString()+" is image (not a video)");
                item = new Item_image(browser, f, scene, icon_size, logger);
            }
            map.put(f, item);
            all_items.add(item);
            double last_y = p.getY();
            //logger.log("icon height = " + item.get_Height());
            if (item.get_Height() > max_h) max_h = item.get_Height();
            double dh = max_h;
            if (dh == 0) dh = icon_size; // safety?

            if (Tool_box.get_show_icons() == false) {
                p = new_Point(p, item, dir_button_width, file_button_height, scene_width, dir_button_width);
            }
            else
            {
                p = new_Point(p, item, icon_size, dh, scene_width, item_width);
                if (p.getY() != last_y) {
                    // changed row
                    max_h = 0;
                }
            }

        }
        //set_geometry(pane, icon_size, dir_button_height, file_button_height, scene);
        //request_async_icon_load(icon_size);

        compute_bounding_rectangle();


    }

    //**********************************************************
    private Point2D new_Point(Point2D p,
                              Item item,
                              double w,
                              double dh, // row increment, if needed
                              double scene_width,
                              double item_width)
    //**********************************************************
    {
        //item.get_Node().setVisible(true);
        item.set_MinWidth(w);
        item.set_MinHeight(dh); // is not going to do anything on icons
        //double w2 = item.get_Width(); NO!! this is often 0
        //logger.log("item with = "+w2);
        double new_x = p.getX();
        double new_y = p.getY();
        if (p.getX() > 0) {
            if (p.getX() + item_width > scene_width) {
                // new ROW
                new_x = 0;
                double delta_h = dh;
                if (item.get_Node() != null) {
                    delta_h = item.get_Node().getBoundsInLocal().getHeight();
                }
                //logger.log("new row, delta_h = " + delta_h);
                if (delta_h == 0) {
                    delta_h = dh;
                    //logger.log("CHANGED delta_h = " + delta_h);
                }
                new_y += delta_h;
            }
        }
        item.set_x(new_x);
        item.set_y(new_y);
        new_x += w;
        return new Point2D(new_x, new_y);
    }


    //**********************************************************
    private void compute_bounding_rectangle()
    //**********************************************************
    {
        // compute bounding rectangle

        x_min = Double.MAX_VALUE;
        x_max = 0;
        y_min = Double.MAX_VALUE;
        y_max = 0;
        for (Item c : all_items) {
            if (c.get_x() < x_min) x_min = c.get_x();
            if (c.get_x() + c.get_Width() > x_max) x_max = c.get_x() + c.get_Width();
            if (c.get_y() < y_min) y_min = c.get_y();
            if (c.get_y() + c.get_Height() > y_max) y_max = c.get_y() + c.get_Height();
        }

        //logger.log(Stack_trace_getter.get_stack_trace("y_max ="+y_max));
        //logger.log("y_max =" + y_max);

        y_max_listener.changed(y_max);
    }

    //**********************************************************
    public void geometry_changed(Browser browser,
                                 Stage stage,
                                 Scene scene,
                                 Pane pane,
                                 List<Node> mandatory,
                                 double top_button_height,
                                 double dir_button_width,
                                 double dir_button_height,
                                 double file_button_height,
                                 Slider vertical_slider
    )
    //**********************************************************
    {
        map_icons(browser,
                stage,
                scene,
                pane,
                mandatory,
                top_button_height,
                dir_button_width,
                dir_button_height,
                file_button_height
        );

        if (Browser.slider_mover)
        {
            double y = 0;
            if ( vertical_slider !=null)
            {
                y = Browser.slider_to_scene( this,vertical_slider.getValue());
            }
            move_absolute(stage, scene, pane,y, dir_button_height, "geometry changed");
        }
        else
            {
            move_relative(stage, scene, pane, 0, dir_button_height, "geometry changed");
        }

    }

    //**********************************************************
    public double move_relative_master(Stage stage, Scene scene, Pane pane, String tag, double dy, double dir_button_height)
    //**********************************************************
    {
        return move_relative_with_inertia(stage, scene, pane, tag, dy, dir_button_height);
    }

    //**********************************************************
    private double move_relative_with_inertia(Stage stage, Scene scene, Pane pane, String tag, double dy, double dir_button_height)
    //**********************************************************
    {
        double dx = 0;//dx2/2 ;
        //dy = dy / 2;
        if (dbg) logger.log(tag + " enter dx=" + dx + " dy=" + dy);
        long before = System.nanoTime();
        double fac = 0.9;
        long target = 2;
        double actual_vertical_move = 0;
        for (; ; ) {
            if (Exceptions_in_threads_catcher.oops) return actual_vertical_move;
            actual_vertical_move += dy;
            move_relative(stage, scene, pane, dy, dir_button_height, tag);
            dx *= fac;
            dy *= fac;
            fac *= 0.6;

            if ((Math.abs(dx) < 0.1) && (Math.abs(dy) < 0.1)) break;
            try {
                long now = System.nanoTime();
                long ms = target - (now - before) / 1000000L;
                before = now;
                if (dbg) logger.log("        milliseconds=" + ms + " dx=" + dx + " dy=" + dy);
                if (ms >= 1) Thread.sleep(ms);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return actual_vertical_move;
    }


    //**********************************************************
    private void compute_offset_relative(double dy, double h, double dir_button_height)
    //**********************************************************
    {
        double candidate_y_offset = y_offset + dy;
        if (dy > 0) {
            // going down
            if (candidate_y_offset + h > y_max) {
                // too far down
                candidate_y_offset = y_max - h + dir_button_height;
                if (dbg) logger.log("too far downward");
            }
        }
        if (dy < 0) {
            // going up
            if (candidate_y_offset < 0) {
                // too far up
                candidate_y_offset = 0;
                if (dbg) logger.log("too far upward");
            }
        }
        y_offset = candidate_y_offset;
        if (dbg) logger.log("y_offset=" + y_offset);

    }

    //**********************************************************
    public double compute_offset_absolute(double candidate_y_offset, double h, double dir_button_height)
    //**********************************************************
    {
        if (candidate_y_offset + h > y_max) {
            // too far down
            candidate_y_offset = y_max - h + dir_button_height;
            if (dbg) logger.log("too far downward");
        }
        if (candidate_y_offset < 0)
        {
            // too far up
            candidate_y_offset = 0;
            if (dbg) logger.log("too far upward");
        }
        y_offset = candidate_y_offset;
        if (dbg) logger.log("y_offset=" + y_offset);
        return y_offset;
    }

    //**********************************************************
    public void move_relative(
            Stage stage,
            Scene scene,
            Pane pane,
            double dy,
            double dir_button_height,
            String reason)
    //**********************************************************
    {
        //double W = scene.getWidth();
        double h = scene.getHeight();
        //double minimal_y = dir_button_height;
        if (dbg) logger.log("move_all dy=" + dy + " reason:" + reason);

        compute_offset_relative(dy, h, dir_button_height);
        check_visibility(stage,pane,h);
    }

    //**********************************************************
    public void move_absolute(
            Stage stage,
            Scene scene,
            Pane pane,
            double new_y,
            double dir_button_height,
            String reason)
    //**********************************************************
    {
        double h = scene.getHeight();
        if (dbg) logger.log("move_absolute new_y=" + new_y + " reason:" + reason);
        compute_offset_absolute(new_y, h, dir_button_height);
        check_visibility(stage,pane,h);
    }

    //**********************************************************
    void check_visibility(Stage stage, Pane pane, double h)
    //**********************************************************
    {
        double icon_size = Properties.get_icon_size();
        int visible = 0;
        for (Item item : all_items) {
            if (item.get_y() + item.get_Height() - y_offset < 0) {
                process_is_invisible(pane, icon_size, item);
                continue;
            }
            if (item.get_y() - y_offset > h) {
                process_is_invisible(pane, icon_size, item);
                continue;
            }
            process_is_visible(stage, pane, item, icon_size);
            visible++;
        }

        if (dbg) logger.log("visible=" + visible);
    }


    //**********************************************************
    private void process_is_visible(Stage stage, Pane pane, Item item, double icon_size)
    //**********************************************************
    {
        // if (item.visible_in_scene)
        {
            // normally we did it all before
        }
        //  else
        {
            item.visible_in_scene = true;
            if (item instanceof Item_image) {
                Item_image ii = (Item_image) item;

                switch (ii.icon_status) {
                    case no_icon:
                        ii.load_default_icon(stage, logger);
                    case default_icon: {
                        Icon_factory_request ifr = new Icon_factory_request(ii, icon_size, exception_recorder);
                        Tool_box.get_icon_factory(logger).make_icon(ifr);
                        ii.icon_status = Icon_status.true_icon_requested;
                    }
                    break;
                    case true_icon_requested:
                    case true_icon_in_the_making:
                    case true_icon:
                        break;
                }
            }
            if (pane.getChildren().contains(item.get_Node()) == false) {
                if (dbg) logger.log("adding item: " + item.get_string());
                pane.getChildren().add(item.get_Node());
            }
            item.setVisible(true);
        }


        item.setTranslateX(item.get_x());
        item.setTranslateY(item.get_y() - y_offset);
    }

    //**********************************************************
    private void process_is_invisible(Pane pane, double icon_size, Item item)
    //**********************************************************
    {
        if (item.visible_in_scene) {
            item.visible_in_scene = false;
            if (item.get_Node() == null) return;
            item.get_Node().setVisible(false);
            if (add_and_remove) {
                if (dbg) logger.log("removing invisible icon of: " + item.get_string());
                pane.getChildren().remove(item.get_Node());
            }
            if (item instanceof Item_image) {
                // let us hope the GC might save us !
                // i.e. in directories with very large number of images
                // the icon manager can cause an OutOfMemor if we would keep invisible images in memory
                Item_image ii = (Item_image) item;
                ii.set_Image(null, false);//Static_image_utilities.get_default_icon(icon_size, logger), false);
            }
        }
    }



      /*
    // request ALL icons: this is ok for a few images
    private void request_async_icon_load(double icon_size)
    {
        for (Path f : paths_list_images)
        {
            Item item = map.get(f);
            Item_image ii = (Item_image) item;
            ii.async_icon_load(icon_factory,  icon_size,logger);
        }
        logger.log("load_icons() DONE!!");
        // send last "hara-kiri" message to clean shutdown the icon factory
        icon_factory.inject_request(new Icon_factory_request(null, 0, null));

    }
    */

/*
    void set_geometry2(Pane pane, double icon_size, double dir_button_height, double file_button_height, Scene scene) {
        double button_width = icon_size;
        if (icon_size < 300) button_width = 300;

        double x = 0;
        double y = 2 * dir_button_height;
        for (Path f : paths_list_dirs) {
            Item item = map.get(f);
            item.set_MinWidth(button_width);
            item.set_MinHeight(dir_button_height);
            double w = item.get_Width();

            if (x > 0) {
                if (x + w > scene.getWidth()) {
                    x = 0;
                    y += item.get_Height();
                }
            }
            item.set_x(x);
            item.set_y(y);
            x += w;

        }
        if (x != 0) {
            x = 0;
            y += dir_button_height;
        }
        for (Path f : paths_list_non_image_files) {
            // non-image, non-directory
            Item item = map.get(f);
            item.set_MinWidth(button_width);
            item.set_MinHeight(file_button_height);
            double w = item.get_Width();
             if (x > 0) {
                if (x + w > scene.getWidth()) {
                    x = 0;
                    y += item.get_Height();
                }
            }
            item.set_x(x);
            item.set_y(y);
            x += w;

        }
        if (x != 0) {
            x = 0;
            y += file_button_height;
        }

        double current_row_height = 0;
        for (Path f : paths_list_images) {
            Item item = map.get(f);
            double hhh = item.get_Height();
            if (current_row_height < hhh) current_row_height = hhh;

            if (x > 0) {
                if (x + icon_size > scene.getWidth()) {
                    x = 0;
                    y += current_row_height;
                    current_row_height = 0;
                }
            }
            item.set_x(x);
            item.set_y(y);
            x += icon_size;
        }
    }


*/

    //**********************************************************
    public void modify_button_fonts(double v)
    //**********************************************************
    {
        for (Item i : all_items) {
            if (i instanceof Item_non_image) {
                Item_non_image ini = (Item_non_image) i;
                double s = ini.button.getFont().getSize();
                Font f = new Font(s * v);
                ini.button.setFont(f);

            }
        }
    }

    //**********************************************************
    public void icon_size_is_now(double icon_size)
    //**********************************************************
    {
        for (Item i : all_items) {
            if (i instanceof Item_image) {
                Item_image ii = (Item_image) i;
                ii.icon_status = Icon_status.no_icon;
            }
        }
    }

    public double get_y_offset() {
        return y_offset;
    }

    public void set_y_offset(double offset) {
        y_offset = offset;
    }

    //**********************************************************
    public List<File> get_image_file_list()
    //**********************************************************
    {
        List<File> returned = new ArrayList<>();
        for (Path p : paths_list_images) {
            returned.add(p.toFile());
        }
        return returned;
    }

    //**********************************************************
    public List<File> get_file_list()
    //**********************************************************
    {
        List<File> returned = new ArrayList<>();
        for (Path p : paths_list_images) {
            returned.add(p.toFile());
        }
        for (Path p : paths_list_non_image_files) {
            returned.add(p.toFile());
        }


        return returned;
    }
}
