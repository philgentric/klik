//SOURCES ../../images/decoding/Fast_rotation_from_exif_metadata_extractor.java
//SOURCES ../../level3/experimental/Multiple_image_window.java

package klik.browser.items;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.actor.Job_termination_reporter;
import klik.browser.Browser;
import klik.browser.Drag_and_drop;
import klik.browser.Image_and_properties;
import klik.browser.icons.animated_gifs.Ffmpeg_utils;
import klik.image_ml.image_similarity.Image_feature_vector_RAM_cache;
import klik.browser.icons.image_properties_cache.Rotation;
import klik.change.Change_gang;
import klik.image_ml.Feature_vector;
import klik.look.my_i18n.My_I18n;
import klik.util.files_and_paths.*;
import klik.images.Image_window;
import klik.images.decoding.Fast_rotation_from_exif_metadata_extractor;
import klik.level3.experimental.Multiple_image_window;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.ui.Hourglass;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.execute.System_open_actor;
import klik.util.ui.Show_running_man_frame_with_abort_button;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;


//**********************************************************
public class Item_image extends Item
//**********************************************************
{
    protected ImageView image_view;
    Pane image_pane;
    public Double aspect_ratio;
    public static Image default_icon;
    double actual_icon_size;

    //**********************************************************
    public Item_image(
            Browser b,
            Path p,
            Double aspect_ratio, Logger logger)
    //**********************************************************
    {
        super(b,p, null, logger);
        this.aspect_ratio = aspect_ratio;
        actual_icon_size = icon_size / 3.0;
        if ( default_icon == null) default_icon = Look_and_feel_manager.get_default_icon(actual_icon_size);


        // first time
        image_view = new ImageView();
        Tooltip.install(image_view,new Tooltip(path.getFileName().toString()));
        image_pane = new StackPane(image_view);

        if ( dbg)
            logger.log("item_image: loading default icon in the image view, w=" +default_icon.getWidth()+", h="+default_icon.getHeight()+" FOR:  "+path);
        image_view.setPreserveRatio(true);
        image_view.setSmooth(true);
        image_view.setFitWidth(actual_icon_size);
        image_view.setFitHeight(actual_icon_size);
        //the_image_view.setManaged(false);
        image_view.setCache(false);
        //image_view.setCacheHint(CacheHint.SPEED);
        Drag_and_drop.init_drag_and_drop_sender_side(get_Node(),browser,path,logger);


        image_view.setOnMouseClicked(event ->
            {
                if (event.getButton() == MouseButton.SECONDARY) {
                    //logger.log("\n\nItem_image isSecondaryButtonDown");
                    ContextMenu context_menu = define_a_menu_to_the_imageview();
                    context_menu.show(image_view, event.getScreenX(), event.getScreenY());
                    return;
                }
                if (event.isMetaDown()) {
                    Optional<Multiple_image_window> option = Multiple_image_window.get_Multiple_image_window(browser.my_Stage.the_Stage, path, false, logger);
                    if (option.isEmpty()) {
                        // let us a bit of checking about why this failed
                        Change_gang.report_anomaly(path);
                    }
                    return;
                }
                if (event.isControlDown()) {
                    if (dbg) logger.log("\n\nItem_image event=" + event + " CTRL is down");
                    set_is_selected();
                } else {
                    if (dbg) logger.log("\n\nItem_image OnMouseClicked " + path);
                    on_mouse_clicked(logger);
                }
            });

    }

    //**********************************************************
    private void on_mouse_clicked(Logger logger)
    //**********************************************************
    {
        browser.selection_handler.reset_selection(); // will clear all selections

        if ( Guess_file_type.is_this_path_an_image(path))
        {
            open_an_image(logger);
        }
        else
        {
            System_open_actor.open_with_system(browser,path,logger);
            //open_with_system(logger);
        }
    }

    //**********************************************************
    @Override // Item
    public void set_is_unselected_internal()
    {
        if ( image_view != null) image_view.setViewport(null);
    }
    //**********************************************************
    @Override // Item
    public void set_is_selected_internal()
    //**********************************************************
    {
       double DELTA = icon_size/2.0;
       Rectangle2D r = new Rectangle2D(-DELTA, -DELTA, image_view.getFitWidth() + DELTA, image_view.getFitHeight() + DELTA);
       image_view.setViewport(r);
    }

    //**********************************************************
    public void open_an_image(Logger logger)
    //**********************************************************
    {
        Image_window s = Image_window.get_Image_window(browser, path, logger);
        if ( dbg) logger.log("\n\nImage_stage opening for path:" + path.toString());
    }


    @Override // Item
    public Path get_path_for_display(boolean try_deep) {
        return path;
    }

    @Override // Icon_destination
    public Path get_path_for_display_icon_destination() {
        return path;
    }

    //**********************************************************
    @Override
    public boolean is_trash()
    //**********************************************************
    {
        return false;
    }

    //**********************************************************
    @Override
    public boolean is_parent()
    //**********************************************************
    {
        return false;
    }



    //**********************************************************
    public ContextMenu define_a_menu_to_the_imageview()
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu);

        {
            MenuItem menu_item = create_open_exif_frame_menu_item(path,logger);
            context_menu.getItems().add(menu_item);
        }
        {
            MenuItem menu_item = create_show_similar_menu_item(path,logger);
            context_menu.getItems().add(menu_item);
        }
        {
            MenuItem menu_item = new MenuItem(My_I18n.get_I18n_string("Rename", logger)+ " "+path.getFileName());
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Item_image: Renaming "+path);

                Path new_path =  Static_files_and_paths_utilities.ask_user_for_new_file_name(browser.my_Stage.the_Stage,path,logger);
                if ( new_path == null) return;

                List<Old_and_new_Path> l = new ArrayList<>();
                Old_and_new_Path oandn = new Old_and_new_Path(path, new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command,false);
                l.add(oandn);
                Moving_files.perform_safe_moves_in_a_thread(browser.my_Stage.the_Stage,l, true, browser_aborter, logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(My_I18n.get_I18n_string("Delete", logger));
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Deleting "+path);
                Static_files_and_paths_utilities.move_to_trash(browser.my_Stage.the_Stage,path, null, browser_aborter, logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(My_I18n.get_I18n_string("Edit", logger));
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Editing "+path);
                System_open_actor.open_with_system(browser,path,logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem(My_I18n.get_I18n_string("Open_With_Registered_Application", logger));
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Opening with registered app: "+path);
                System_open_actor.open_special(browser.my_Stage.the_Stage,path,browser_aborter,logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            context_menu.getItems().add(Item.create_show_file_size_menu_item(browser,path, dbg, logger));
            if (Static_application_properties.get_level3(logger)) context_menu.getItems().add(Item.create_edit_tag_menu_item(path, dbg,logger));
        }

        if ( this.item_type == Iconifiable_item_type.video)
        {
            make_menu_items_for_videos(path,browser,context_menu,dbg, browser_aborter,logger);
        }
        return context_menu;

    }

    static  Map<Path,Map<Path,Double>> similarities = new HashMap<>();

    //**********************************************************
    public MenuItem create_show_similar_menu_item(Path path, Logger logger)
    //**********************************************************
    {
        String txt = "Show 3 similar images";//My_I18n.get_I18n_string("Info_about", logger);
        MenuItem menu_item = new MenuItem(txt);
        menu_item.setOnAction(actionEvent -> {
            if (dbg) logger.log("show similar");
            Runnable r =new Runnable() {
                @Override
                public void run() {

                    Hourglass x = Show_running_man_frame_with_abort_button.show_running_man("wait",20000,logger);
                    Image_feature_vector_RAM_cache image_feature_vector_ram_cache = new Image_feature_vector_RAM_cache(browser.displayed_folder_path,"image_feature_vectors", new Aborter("fv_c",logger),logger);

                    image_feature_vector_ram_cache.reload_cache_from_disk();

                    Path dir = browser.displayed_folder_path;
                    File[] files = dir.toFile().listFiles();
                    List<Path> targets = new ArrayList<>();
                    if ( files == null) return;
                    for (File f : files)
                    {
                        if ( f.isDirectory()) continue;

                        if ( f.getName().startsWith("._"))
                        {
                            continue;
                        }
                        if ( f.getName().equals(path.getFileName().toString())) continue;

                        if ( !Guess_file_type.is_file_an_image(f)) continue;
                        targets.add(f.toPath());
                    }
                    if ( targets.isEmpty()) return;
                    CountDownLatch cdl = new CountDownLatch(targets.size()+1);
                    Job_termination_reporter tr = new Job_termination_reporter() {
                        @Override
                        public void has_ended(String message, Job job) {
                            cdl.countDown();
                            if ( cdl.getCount() % 100 == 0) logger.log(""+cdl.getCount());
                        }
                    };
                    image_feature_vector_ram_cache.get_from_cache(path,tr,false);
                    for ( int i = 0 ; i < targets.size(); i++)
                    {
                        Path p1 = targets.get(i);
                        image_feature_vector_ram_cache.get_from_cache(p1,tr,false);
                    }
                    try {
                        cdl.await();
                    } catch (InterruptedException e) {
                        logger.log(""+e);
                        return;
                    }
                    image_feature_vector_ram_cache.save_whole_cache_to_disk();
                    Feature_vector fv2 = image_feature_vector_ram_cache.get_from_cache(path,tr,true);
                    Path min_p1 = find_min(targets, image_feature_vector_ram_cache, fv2, path);
                    targets.remove(min_p1);
                    Path min_p2 = find_min(targets,image_feature_vector_ram_cache,fv2,path);
                    targets.remove(min_p2);
                    Path min_p3 = find_min(targets,image_feature_vector_ram_cache,fv2,path);
                    Runnable rr = new Runnable() {
                        @Override
                        public void run() {
                            double zz = 10;
                            show_one_at(path,zz,200);
                            zz += 300;
                            if ( min_p1 != null) show_one_at(min_p1,zz,200);
                            zz += 300;
                            if ( min_p2 != null) show_one_at(min_p2,zz,200);
                            zz += 300;
                            if ( min_p3 != null) show_one_at(min_p3,zz,200);
                        }
                    };
                    Jfx_batch_injector.inject(rr,logger);
                    x.close();
                }
            };
            Actor_engine.execute(r,logger);


        });

        return menu_item;
    }

    private Path find_min(List<Path> targets, Image_feature_vector_RAM_cache image_feature_vector_ram_cache, Feature_vector fv2, Path path)
    {
        Path min_p1 = null;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < targets.size(); i++)
        {
            Path p1 = targets.get(i);
            Double similarity = read_similarity(path,p1);
            if ( similarity == null)
            {
                Feature_vector fv1 = image_feature_vector_ram_cache.get_from_cache(p1, null,true);
                if (fv1 == null) continue; // server failure
                similarity = fv1.cosine_similarity(fv2);
                store_similarity(similarity, path, p1);
            }
            if ( similarity < min)
            {
                min = similarity;
                min_p1 = p1;
            }
        }
        return min_p1;
    }

    private void show_one_at(Path target_path,double x, double y)
    {
        Image_window returned = new Image_window(browser, target_path, x, y,300,300, logger);
        returned.the_Stage.setX(x);
        returned.the_Stage.setY(y);
        logger.log("x="+returned.the_Stage.getX());
        logger.log("y="+returned.the_Stage.getY());

    }
    //**********************************************************
    private void store_similarity(Double similarity, Path p1, Path p2)
    //**********************************************************
    {
        Map<Path, Double> m1 = similarities.get(p1);
        if (m1 != null)
        {
            m1.put(p2,similarity);
            return;
        }
        Map<Path, Double> m2 = similarities.get(p2);
        if (m2 != null)
        {
            m2.put(p1, similarity);
            return;
        }

        m1 = new HashMap<>();
        similarities.put(p1,m1);
        m1.put(p2, similarity);
    }

    //**********************************************************
    private Double read_similarity(Path p1, Path p2)
    //**********************************************************
    {
        Map<Path, Double> m1 = similarities.get(p1);
        if (m1 != null)
        {
            Double similarity = m1.get(p2);
            if ( similarity != null) return similarity;
        }
        Map<Path, Double> m2 = similarities.get(p2);
        if (m2 != null)
        {
            Double similarity = m2.get(p1);
            if ( similarity != null) return similarity;
        }
        return null;
    }
    //**********************************************************
    private Double get_similarity_from_RAM_cache(Path p1, Path p2)
    //**********************************************************
    {
        {
            Map<Path, Double> m = similarities.get(p1);
            if ( m != null)
            {
                Double s = m.get(p2);
                if ( s != null) return s;
                else return null;
            }
        }
        {
            Map<Path, Double> m = similarities.get(p2);
            if ( m != null)
            {
                Double s = m.get(p1);
                return s;
            }
            else
            {
                return null;
            }
        }
    }

    //**********************************************************
    public static void make_menu_items_for_videos(Path path, Browser browser, ContextMenu context_menu, boolean dbg, Aborter aborter, Logger logger)
    //**********************************************************
    {
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem("Convert to mp4");
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("convert to mp4");
                AtomicBoolean abort_reported = new AtomicBoolean(false);
                Ffmpeg_utils.video_to_mp4_in_a_thread(browser.my_Stage.the_Stage,path,aborter, abort_reported, logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem("(experimental) generate as many 5s gif animation as 5s in the movie, in a new folder (may take a long time!)");
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Generating animated gifs !");
                Ffmpeg_utils.generate_many_gifs(browser.my_Stage.the_Stage,path,5,5,aborter,logger);
            });
            context_menu.getItems().add(menu_item);
        }
        {
            javafx.scene.control.MenuItem menu_item = new javafx.scene.control.MenuItem("(experimental) generate gif animations from a video, interactively");
            menu_item.setOnAction(event -> {
                if (dbg) logger.log("Generating animated gifs !");
                Ffmpeg_utils.interactive(path,logger);
            });
            context_menu.getItems().add(menu_item);
        }
    }


    @Override
    public double get_Width()
    {
        return icon_size;
    }



    //**********************************************************
    @Override
    public double get_Height()
    //**********************************************************
    {
        return icon_size;
    }

    //**********************************************************
    @Override
    public void receive_icon(Image_and_properties image_and_rotation)
    //**********************************************************
    {
        //logger.log("RECEIVING icon");
        // this is NOT on the FX thread
        if ( image_view == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("the_image_view == null"));
            return;
        }

        if (!visible_in_scene.get())
        {
            // this happen if between the time the icon was request and now,
            // the item is not visible anymore typically because the user scrolled away
            if ( dbg)
                logger.log("!visible_in_scene.get() : calling you_are_invisible");
            Jfx_batch_injector.inject(() -> you_are_invisible(),logger);
            return;
        }
        if ( image_and_rotation == null)
        {
            if ( dbg)
                logger.log("image_and_rotation == null ");
            //Jfx_batch_injector.inject(() -> you_are_invisible(),logger);
            return;
        }
        if ( image_and_rotation.image() == null)
        {
            if ( dbg)
                logger.log("image_and_rotation.image() == null : setting the image to null in the Image_view");
            //Jfx_batch_injector.inject(() -> you_are_invisible(),logger);
            return;
        }

        if ( (image_and_rotation.image().getHeight()  < 1) || (image_and_rotation.image().getWidth() < 1))
        {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: empty image, not set "+path.toAbsolutePath()));
            Jfx_batch_injector.inject(() -> you_are_invisible(),logger);
            return;
        }


        Jfx_batch_injector.inject(() -> receive_icon_in_fx_thread(image_and_rotation),logger);

    }


    //**********************************************************
    @Override
    public boolean has_icon()
    //**********************************************************
    {
        return true;
    }


    //**********************************************************
    public void receive_icon_in_fx_thread(Image_and_properties image_and_properties)
    //**********************************************************
    {
        if ( dbg)
        {
            if ( image_and_properties.properties() ==null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("FATAL receive_icon_in_fx_thread image_and_properties.properties() ==null, for: "+path));
                return;
            }
            logger.log("receive_icon_in_fx_thread," +
                    "\n   w icon=          "+image_and_properties.image().getWidth()+
                    "\n   h icon=          "+image_and_properties.image().getHeight()+
                    "\n   w image=         "+image_and_properties.properties().w()+
                    "\n   h image=         "+image_and_properties.properties().h()+
                    "\n   rot image=       "+image_and_properties.properties().rotation()+
                    "\n   aspect ratio=    "+image_and_properties.properties().get_aspect_ratio()+
                    "\n   for:             "+path);

        }

        double local_rot = 0;
        {
            Rotation rotation = image_and_properties.properties().rotation();
            if (rotation == null)
            {
                if (Files.exists(path))
                {
                    if (
                            (Guess_file_type.is_this_path_a_video(path)) || (Guess_file_type.is_this_path_a_pdf(path))
                    ) {
                        if (dbg) logger.log("PDF or video => rot=0");
                        local_rot = Double.valueOf(0);
                    } else {
                        local_rot = Fast_rotation_from_exif_metadata_extractor.get_rotation(path, true, browser_aborter, logger);
                    }
                }
                else
                {
                    logger.log(Stack_trace_getter.get_stack_trace("WTF"));
                    you_are_invisible();
                    return;
                }

            }
        }
        if ( item_type == Iconifiable_item_type.pdf)
        {
            if (aspect_ratio == null)
            {
                logger.log("SHOULD NOT HAPPEN");
                double local = image_and_properties.image().getWidth()/image_and_properties.image().getHeight();
                if( dbg) logger.log(Stack_trace_getter.get_stack_trace("setting aspect ratio for PDF from icon: "+ local));
                aspect_ratio = local;
            }
        }
        // the above operation can take some time...
        // and in the mean time the situation can change
        if (!visible_in_scene.get())
        {
            you_are_invisible();
            return;
        }

        image_view.setSmooth(true);
        image_view.setImage(image_and_properties.image());

        if (( image_and_properties.image().getHeight() >= icon_size) && (image_and_properties.image().getWidth() >= icon_size))
        {
            // this happens when the icon is PDF as we dont scale PDF icons
            if (dbg) logger.log("icon larger than target HAPPENS1 for: "+path);
            image_view.setFitWidth(icon_size);
            image_view.setFitHeight(icon_size);
            if ((local_rot == 90) || (local_rot == 270))
            {
                // this actually NEVER HAPPENS now since a PDF icon is never rotated
                //if (dbg)
                    logger.log("HAPPENS2 for: "+path);
                image_view.setFitWidth(image_and_properties.image().getHeight());
                image_view.setFitHeight(image_and_properties.image().getWidth());
            }
        }
        else
        {
            if ((local_rot == 90) || (local_rot == 270))
            {

                if ( image_and_properties.image().getHeight() < image_and_properties.image().getWidth())
                {
                    if (dbg)
                        logger.log("HAPPENS3A for: "+path);
                    image_view.setFitWidth(icon_size);
                    image_view.setFitHeight(-1);
                }
                else
                {
                    // this happens rarely as it is an image that is rotated AND wider than high after rotation
                    //(most of the rotated images are portrait shot by turning the camera
                    if (dbg)
                        logger.log("HAPPENS3B for: "+path);
                    image_view.setFitWidth(-1);
                    image_view.setFitHeight(icon_size);
                }
            }
            else
            {
                if (dbg)
                    logger.log("HAPPENS4 for: "+path);
                image_view.setFitWidth(image_and_properties.image().getWidth());
                image_view.setFitHeight(image_and_properties.image().getHeight());
            }
        }
        if ( image_and_properties.properties().rotation() != null) {
            image_pane.setRotate(Rotation.to_angle(image_and_properties.properties().rotation()));
        }
        else
        {
            if ( dbg) logger.log("image_and_rotation.rotation() is null");
        }
    }

    //**********************************************************
    @Override // Item
    public int get_icon_size()
    //**********************************************************
    {
        return icon_size;
    }

    //**********************************************************
    @Override // Item
    public void you_are_visible_specific()
    //**********************************************************
    {
        //logger.log("Item_image::you_are_visible_specific "+get_item_path());
        if ( default_icon == null)
        {
            logger.log("BAD WARNING: item_image: default_icon == null");
            return;
        }

        image_view.setImage(default_icon);

    }
    //**********************************************************
    @Override // Item
    public void you_are_invisible_specific()
    //**********************************************************
    {
        image_view.setImage(null);
    }


    //**********************************************************
    private void log_visibility_state_number(int i)
    //**********************************************************
    {
        get_logger().log(path+" visibility state #" + i);
    }

    //**********************************************************
    @Override
    public Node get_Node()
    //**********************************************************
    {
        return image_pane;
    }

    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        return "is Item_image for : " + path.toAbsolutePath();
    }



}