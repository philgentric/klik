package klik.browser;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import klik.I18N.I18n;
import klik.I18N.Local_manager;
import klik.change.After_move_handler;
import klik.change.Change_gang;
import klik.change.Old_and_new_Path;
import klik.change.Static_change_utilities;
import klik.images.Image_stage;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.History;
import klik.properties.History_item;
import klik.properties.Properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.Tool_box;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.*;

//**********************************************************
public class Browser implements After_move_handler, Y_max_listener, Exception_recorder, Scan_show_slave
//**********************************************************
{
    public static final boolean dbg = false;
    public static double MIN_DIR_BUTTON_WIDTH = 350;
    private double dir_button_width = MIN_DIR_BUTTON_WIDTH;

    static int ID_generator = 0;
    int ID;


    public final Stage the_stage;
    final Scene the_scene;
    final Pane the_pane;
    final Top_pane top_pane;
    final Icon_manager icon_manager;
    final Logger logger;
    final Path displayed_folder_path;
    private WatchService watcher;
    private final Map<WatchKey,Path> keys;

    List<Node> mandatory = new ArrayList<>();


    final Slider vertical_slider;
    public static final double half_slider_width = 20;
    public double slider_width = 400;
    public static final boolean slider_indicator = false;
    public static final boolean slider_mover = true;


    //**********************************************************
    public static Browser create_browser(
            Browser old_browser,
            boolean same_place,
            Path dir,
            boolean keep_offset,
            Logger logger)
    //**********************************************************
    {


        get_history_of_dirs().add(dir);
        double x = 0;
        double y = 0;

        double offset = 0;
        double width = 1920 / 2;
        double height = 1080 - y;
        if (old_browser != null) {
            Change_gang.deregister(old_browser);
            old_browser.stop_scan();
            old_browser.the_pane.getChildren().clear();
            offset = old_browser.icon_manager.get_y_offset();
            width = old_browser.the_stage.getWidth();
            height = old_browser.the_stage.getHeight();
            old_browser.the_stage.close();
            x = old_browser.the_stage.getX();
            y = old_browser.the_stage.getY();

        }
        if (same_place == false) {
            x += 100;
            y += 100;
        }
        if (dbg) logger.log("NEW browser");
        Stage new_stage = new Stage();

        {
            Image icon = Look_and_feel_manager.get_default_directory_icon(300);
            if ( icon == null)
            {
                logger.log("WARNING: cannot get default icon for directory");
            }
            else
            {
                new_stage.getIcons().add(icon);
            }
        }
        new_stage.show();
        new_stage.setX(x);
        new_stage.setY(y);
        new_stage.setWidth(width);
        new_stage.setHeight(height);
        Browser bs = new Browser(
                new_stage,
                dir,
                logger);
        new_stage.setScene(bs.the_scene);
        if (keep_offset) bs.icon_manager.set_y_offset(offset);
        return bs;
    }


    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE);//, ENTRY_MODIFY);
        {
            Path prev = keys.get(key);
            if (prev == null) {
                logger.log("register:"+ dir);
            } else {
                if (!dir.equals(prev)) {
                    logger.log("update: "+ prev+" => "+dir);
                }
            }
        }
        keys.put(key, dir);
    }
    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    //**********************************************************
    private Browser(
            Stage the_stage_,
            Path dir_,
            Logger logger_)
    //**********************************************************
    {
        displayed_folder_path = dir_;
        logger = logger_;
        
        /* file system watch service */
        
        try
        {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }

        keys = new HashMap<WatchKey,Path>();
        try
        {
            register(displayed_folder_path);
        } catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        /*
        this works but thread must be recovered !!
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                for (;;)
                {

                    // wait for key to be signalled
                    WatchKey key;
                    try {
                        key = watcher.take();
                    } catch (InterruptedException x) {
                        return;
                    }

                    Path dir = keys.get(key);
                    if (dir == null) {
                        logger.log("WatchKey not recognized!!");
                        continue;
                    }

                    for (WatchEvent<?> event: key.pollEvents()) {
                        WatchEvent.Kind kind = event.kind();

                        // TBD - provide example of how OVERFLOW event is handled
                        if (kind == OVERFLOW) {
                            continue;
                        }

                        // Context for directory entry event is the file name of entry
                        WatchEvent<Path> ev = cast(event);
                        Path name = ev.context();
                        Path child = dir.resolve(name);

                        // print out event
                        logger.log("event name = "+ event.kind().name()+" child="+ child);


                    }

                    // reset key and remove from set if directory no longer accessible
                    boolean valid = key.reset();
                    if (!valid) {
                        keys.remove(key);

                        // all directories are inaccessible
                        if (keys.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        };
        Tool_box.execute(r,logger);
        */

        ID = ID_generator;
        ID_generator++;
        Change_gang.register(this);
        the_stage = the_stage_;

        set_title();


        the_pane = new Pane();
        the_scene = new Scene(the_pane);//, W, H);
        set_select_all(false);

        icon_manager = new Icon_manager(this,logger);
        String ret = icon_manager.scan_dir(displayed_folder_path,videos_for_which_giffing_failed);
        if (ret.equals(Icon_manager.ACCESS_DENIED_EXCEPTION)) {
            logger.log("access denied");
        }
        if (ret.equals(Icon_manager.GONE_DIR_EXCEPTION)) {
            logger.log("directory gone");
        }

        the_pane.getChildren().clear();
        top_pane = new Top_pane(
                this,
                the_stage,
                the_scene,
                displayed_folder_path,
                Look_and_feel_manager.get_instance().get_top_height(),
                logger);
        mandatory.add(top_pane.hBox);

        //Button up_button = create_up_button();
        //mandatory.add(up_button);

        //double icon_size = Properties.get_icon_size();

        icon_manager.geometry_changed(
                this,
                the_stage,
                the_scene,
                the_pane,
                mandatory,
                Look_and_feel_manager.get_instance().get_top_height(),
                dir_button_width,
                Look_and_feel_manager.get_instance().get_dir_height(),
                Look_and_feel_manager.get_instance().get_file_height(),
                null);

        if (slider_indicator || slider_mover)
        {
            vertical_slider = create_slider(
                    the_stage, the_scene, the_pane,
                    Look_and_feel_manager.get_instance().get_dir_height(), icon_manager, logger);

            mandatory.add(vertical_slider);
            slider_width = 2*half_slider_width;
        }
        else
        {
            vertical_slider = null;
            slider_width = 0;
        }



        Browser this_Browser_scene = this;
        the_stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {

                Change_gang.deregister(this_Browser_scene);

            }
        });

        the_stage.addEventHandler(KeyEvent.KEY_PRESSED,
                new EventHandler<KeyEvent>() {
                    public void handle(final KeyEvent keyEvent) {
                        if (keyEvent.getCode() == KeyCode.ESCAPE) {
                            logger.log("ESCAPE!");
                            the_stage.close();
                            System.exit(0);
                            return;
                        }
                    }
                });


        the_scene.setOnKeyTyped(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {

                logger.log("" + keyEvent+ "getCharacter->" + keyEvent.getCharacter() + "<- getCode:" + keyEvent.getCode());
                if (keyEvent.isShiftDown()) logger.log("isShiftDown:" + keyEvent.isShiftDown());
                if (keyEvent.isAltDown()) logger.log("isAltDown:" + keyEvent.isAltDown());
                if ( keyEvent.isMetaDown()) logger.log("isMetaDown:" + keyEvent.isMetaDown());

                if (keyEvent.getCharacter().equals("s"))
                {
                    logger.log("character is s");
                    toggle_scan();
                }
                if (keyEvent.getCharacter().equals("w"))
                {
                    logger.log("character is w = slow down scan");
                    slow_down_scan();
                }
                if (keyEvent.getCharacter().equals("x"))
                {
                    logger.log("character is w = speed up scan");
                    speed_up_scan();
                }

                if (keyEvent.isMetaDown()) {
                    if (keyEvent.getCharacter().equals("a")) {
                        set_select_all(true);
                    }
                    if (keyEvent.getCharacter().equals("s"))
                    {
                        logger.log("character is s");
                        toggle_scan();
                    }
                    if (keyEvent.getCharacter().equals("+")) {
                        zoom_plus();
                    }
                    if (keyEvent.getCharacter().equals("=")) {
                        zoom_plus();
                    }
                    if (keyEvent.getCharacter().equals("-")) {
                        zoom_minus();
                    }

                }

                if (keyEvent.getCharacter().equals("n")) {
                    Browser.create_browser(null, false, displayed_folder_path, false, logger);
                }


            }
        });

        the_scene.setOnDragOver(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                Object source = event.getGestureSource();
                if (source == null) {
                    //logger.log("source class is null " + event.toString());
                } else {
                    //logger.log("source class is:" + source.getClass().getName());
                    try {
                        Item item = (Item) source;
                        Scene scene_of_source = item.getScene();

                        // data is dragged over the target
                        // accept it only if it is not dragged from the same node
                        if (scene_of_source == the_scene) {
                            logger.log("drag reception for scene: same scene, giving up<<");
                            return;
                        }
                    } catch (ClassCastException e) {
                        return;
                    }
                }
                if ( dbg) logger.log("Ready to accept MOVE!");
                event.acceptTransferModes(TransferMode.MOVE);

                event.consume();
                //the_stage.requestFocus();

            }
        });

        the_scene.setOnDragDropped(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                Tool_box.accept_drag_dropped_as_a_move_in(event, displayed_folder_path,the_pane, "scene of browser", logger);
            }
        });

        the_stage.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                the_pane.setMaxWidth(newValue.doubleValue());
                if (dbg) logger.log("new browser width ="+newValue.doubleValue());

                // adjust the dir button width, within limits
                double available = newValue.doubleValue() - slider_width;
                double min = MIN_DIR_BUTTON_WIDTH;
                if (icon_manager.max_dir_text_length > min) min = icon_manager.max_dir_text_length;
                for (int i = 100; ; i--) {
                    double local_dir_button_width = available / (double) i;
                    if (local_dir_button_width > min) {
                        dir_button_width = local_dir_button_width;
                        break;
                    }

                }
                scene_geometry_changed("width changed by user");
            }
        });
        the_stage.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                scene_geometry_changed("height changed by user");

            }
        });

        the_scene.setOnScroll(new EventHandler<ScrollEvent>()
        {
            @Override
            public void handle(ScrollEvent event)
            {
                //double dx = event.getDeltaX();
                double dy = event.getDeltaY();
                scroll(dy);
            }
        });

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                the_stage.setWidth(the_stage.getWidth() * 1.001);

            }
        });
    }

    private void scroll(double dy)
    {
        boolean inverted = Tool_box.get_vertical_scroll();
        if (inverted)
        {
            //logger.log("scroll is inverted");
            dy = -dy;
        }
        else
        {
            //logger.log("scroll is not inverted");
        }

        if (dbg) logger.log("scroll dy=" + dy);
        top_pane.hBox.toFront();
        double updated_dy = dy;
        if (slider_mover == false)
        {
            // scroll is in control of the ACTUAL move
            // which will change the dy value
            updated_dy = icon_manager.move_relative_master(the_stage, the_scene, the_pane, "scroll", dy, Look_and_feel_manager.get_instance().get_dir_height());
        }
        if (slider_indicator)
        {
            double new_val = vertical_slider.getValue() - updated_dy;
            //double h = the_scene.getHeight();
            //new_val = icon_manager.compute_offset_absolute(new_val,h,dir_button_width);
            // this will only update the slider
            //logger.log("OnScroll vertical_slider.getValue()="+vertical_slider.getValue()+" dy=" + dy+"  new slider value = "+new_val);
            vertical_slider.setValue(new_val);
        }



        if (slider_mover )
        {
            double new_val = vertical_slider.getValue() - dy;
            //double new_y = slider_to_scene(icon_manager, new_val);

            //double h = the_scene.getHeight();
            //new_y = icon_manager.compute_offset_absolute(new_y,h,dir_button_width);
            // move is delegated to the slider;, because with will cause "valueChanged"
            //logger.log("OnScroll vertical_slider.getValue()="+vertical_slider.getValue()+" dy=" + dy+"  new slider value = "+new_val);
            vertical_slider.setValue(new_val);
        }

    }

    private List<Path> videos_for_which_giffing_failed = new ArrayList<>();

    //**********************************************************
    @Override
    public void  record(Path path)
    //**********************************************************
    {
        logger.log("Recording an exception ! for :"+path);
        if (videos_for_which_giffing_failed.contains(path) == false )
        {
            videos_for_which_giffing_failed.add(path);
            Platform.runLater(new Runnable() {
                @Override
                public void run()
                {
                    scene_geometry_changed("icon fabrication failed");
                }
            });
        }
    }
    //**********************************************************
    @Override
    public void changed(double y_max)
    //**********************************************************
    {
        if ( vertical_slider != null)
        {
            vertical_slider.setMax(y_max);
        }
    }

    //**********************************************************
    private static void adapt_slider_to_scene(Slider slider, Scene scene, double button_height, Icon_manager icon_manager)
    //**********************************************************
    {
        slider.setTranslateX(scene.getWidth() - half_slider_width);//vertical.getWidth());
        slider.setTranslateY(button_height+half_slider_width);//vertical.getWidth());
        slider.setPrefHeight(scene.getHeight() - 2*(button_height+half_slider_width));
    }

    //**********************************************************
    private static Slider create_slider(Stage stage, Scene scene, Pane pane, double dir_button_height,
                                        Icon_manager icon_manager, Logger logger)
    //**********************************************************
    {

        //logger.log("create_slider y_max="+icon_manager.y_max);

        Slider slider = new Slider(0, icon_manager.y_max, icon_manager.y_max);
        pane.getChildren().add(slider);
        slider.setOrientation(Orientation.VERTICAL);


        if ( slider_indicator)
        {
            // if indicator the user cannot grab and move the cursor
            slider.setDisable(true);
        }

        adapt_slider_to_scene(slider, scene, dir_button_height, icon_manager);


        //vertical.setBlockIncrement(1);
        //vertical.setUnitIncrement(1);// amount of change when CLICKING on the buttons at the end of the scrollbar
        slider.toFront();
        slider.setVisible(true);
        slider.setValue(icon_manager.y_max);

        if ( slider_mover)
        {
            slider.valueProperty().addListener(new ChangeListener<Number>()
            {
                public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val)
                {
                    double new_y = slider_to_scene(icon_manager, new_val.doubleValue());

                    if (dbg)
                        logger.log("slider old=" + old_val + " new=" + new_val+" y_max="+icon_manager.y_max+" ===>"+new_y);
                    //double dy = old_val.doubleValue() - new_val.doubleValue();
                    icon_manager.move_absolute(stage, scene, pane, new_y , dir_button_height,"move absolute = slider moved!"+/*Stack_trace_getter.get_stack_trace*/("slider old=" + old_val + " new=" + new_val));
                    //up_button.toFront();
                    //new_dir_button.toFront();

                }
            });
        }
        return slider;
    }

    //**********************************************************
    public static double slider_to_scene(Icon_manager icon_manager,double slider)
    //**********************************************************
    {
        return icon_manager.y_max-slider;
    }

    //**********************************************************
    private void set_title()
    //**********************************************************
    {
        if ( displayed_folder_path == null) return;
        if ( displayed_folder_path.toFile() == null) return;
        if ( displayed_folder_path.toFile().listFiles() == null) return;
        long how_many_files = displayed_folder_path.toFile().listFiles().length;
        the_stage.setTitle(displayed_folder_path.toAbsolutePath().toString()+" :     "+how_many_files+" files & folders"   );
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
        //dir_button_height *= fac;
        //file_button_height *= fac;
        Properties.set_icon_size((int) (Properties.get_icon_size() * fac));
/*
        {
            double s = up_button.getFont().getSize();
            Font f = new Font(s * fac);
            up_button.setFont(f);
            //new_dir_button.setFont(f);

        }
   */
        icon_manager.modify_button_fonts(fac);
        scene_geometry_changed("zoom");

    }


    //**********************************************************
    private void scene_geometry_changed(String from)
    //**********************************************************
    {

        if (dbg) logger.log(true, true, "scene_geometry_changed()" + from);

        if (icon_manager.scan_dir(displayed_folder_path,videos_for_which_giffing_failed).equals(icon_manager.OK) == false)
        {
            logger.log(true, true, "scene_geometry_changed() scan dir failed");

            //Browser.create_browser(this, true, dir.getParent(), false, logger);
        }
        icon_manager.geometry_changed(
                this, the_stage, the_scene, the_pane, mandatory,
                Look_and_feel_manager.get_instance().get_top_height(),
                dir_button_width,
                Look_and_feel_manager.get_instance().get_dir_height(),
                Look_and_feel_manager.get_instance().get_file_height(),
                vertical_slider);
        //icon_manager.set_geometry(pane, icon_size.get_icon_size(), dir_button_height, file_button_height, scene);
        if (slider_indicator || slider_mover)
        {
            adapt_slider_to_scene(vertical_slider, the_scene, Look_and_feel_manager.get_instance().get_dir_height(), icon_manager);
        }


        for (Node n : mandatory) n.toFront();
        set_title();
    }




    //**********************************************************
    public void create_new_directory()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(I18n.get_I18n_string("New_directory",logger));
        dialog.setWidth(the_stage.getWidth());
        dialog.setTitle(I18n.get_I18n_string("New_directory",logger));
        dialog.setHeaderText(I18n.get_I18n_string("Enter_name_of_new_directory",logger));
        dialog.setContentText(I18n.get_I18n_string("New_directory_name",logger));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();

            Path new_dir = displayed_folder_path.resolve(new_name);
            //logger.log("CREATE DIR = "+new_dir.toAbsolutePath().toString());

            try {
                Files.createDirectory(new_dir);
                scene_geometry_changed("created new empty dir");
            } catch (IOException e) {
                logger.log("new directory creation FAILED: " + e);
                e.printStackTrace();
            }

        }
    }


    //**********************************************************
    private void create_menu_item_for_one_icon_size(Menu menu, int target_size)
    //**********************************************************
    {
        Browser this_Browser_scene = this;


        CheckMenuItem item = new CheckMenuItem(I18n.get_I18n_string("Icon_size_",logger) + target_size);
        int actual_size = Properties.get_icon_size();
        if (actual_size == target_size) {
            item.setSelected(true);
        } else {
            item.setSelected(false);
        }
        item.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (((CheckMenuItem) actionEvent.getSource()).isSelected()) {
                    Properties.set_icon_size(target_size);
                    scene_geometry_changed("icon size changed");
                }
            }
        });
        menu.getItems().add(item);

    }



    public Scene get_scene() {
        return the_scene;
    }

    //**********************************************************
    @Override
    public void you_receive_this_because_a_move_occurred_somewhere(List<Old_and_new_Path> l, Logger logger)
    //**********************************************************
    {
        Browser this_local = this;

        if (Change_gang.is_my_directory_impacted(displayed_folder_path, l, logger)) {
            // can be called from a thread which is NOT the FX event thread
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    scene_geometry_changed("change gang");
                    //get_a_browser(this_local,the_stage,dir,true,logger);
                }
            });
        }
    }



    @Override
    public String get_string() {
        return "Browser_scene:" + displayed_folder_path.toAbsolutePath() + " " + ID;
    }

    private boolean select_all = false;
    public boolean get_select_all(){return select_all;};


    //**********************************************************
    List<File> get_file_list()
    //**********************************************************
    {
        return icon_manager.get_file_list();
    }

    //**********************************************************
    public void show_popup_menu(double x, double y)
    //**********************************************************
    {
        ContextMenu context_menu;
        context_menu = new ContextMenu();
        {
            String text = I18n.get_I18n_string("History",logger);// to: " + parent.toAbsolutePath().toString();

            Menu history_menu = new Menu(text);
            create_history_menu(history_menu);
            context_menu.getItems().add(history_menu);

        }
        {
            String text = I18n.get_I18n_string("Select_all_files_for_drag_and_drop",logger);// to: " + parent.toAbsolutePath().toString();

            CheckMenuItem item = new CheckMenuItem(text);
            item.setSelected(select_all);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    set_select_all(((CheckMenuItem) event.getSource()).isSelected());
                }
            });
            context_menu.getItems().add(item);

        }


        {
            String text = I18n.get_I18n_string("Refresh",logger);// to: " + parent.toAbsolutePath().toString();

            MenuItem item = new MenuItem(text);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    scene_geometry_changed("refresh");
                }
            });
            context_menu.getItems().add(item);

        }
        {
            String text = I18n.get_I18n_string("Create_new_empty_directory",logger);// to: " + parent.toAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    create_new_directory();
                }
            });
            context_menu.getItems().add(item);

        }
        {
            String text = I18n.get_I18n_string("Start_stop_slow_scan",logger);// to: " + parent.toAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    toggle_scan();
                }
            });
            context_menu.getItems().add(item);
        }
        {
            String text = I18n.get_I18n_string("Search_images_by_keywords",logger);// to: " + parent.toAbsolutePath().toString();

            MenuItem item = new MenuItem(text);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    search_images_by_keyworks();
                }
            });
            context_menu.getItems().add(item);
        }
        {
            String text = I18n.get_I18n_string("Undo_LAST_move_or_delete",logger);// to: " + parent.toAbsolutePath().toString();

            MenuItem item = new MenuItem(text);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    Static_change_utilities.undo_last_move(logger);
                }
            });
            context_menu.getItems().add(item);
        }

        {
            String text = I18n.get_I18n_string("Show_hidden_file",logger);// to: " + parent.toAbsolutePath().toString();

            CheckMenuItem item = new CheckMenuItem(text);
            item.setSelected(Properties.get_show_hidden_files());
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    Properties.set_show_hidden_files(((CheckMenuItem) actionEvent.getSource()).isSelected());
                    scene_geometry_changed("show hidden file boolean changed");
                }
            });
            context_menu.getItems().add(item);
        }
        {
            String text = I18n.get_I18n_string("Show_gifs_first",logger);// to: " + parent.toAbsolutePath().toString();
            CheckMenuItem item = new CheckMenuItem(text);
            item.setSelected(Properties.get_show_gifs_first());
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    Properties.set_show_gifs_first(((CheckMenuItem) actionEvent.getSource()).isSelected());
                    scene_geometry_changed("show onmy gif boolean changed");
                }
            });
            context_menu.getItems().add(item);
        }
        {
            String text = I18n.get_I18n_string("Sort_files_by_name_vs_decreasing_size",logger);// to: " + parent.toAbsolutePath().toString();
            CheckMenuItem item = new CheckMenuItem(text);
            item.setSelected(Tool_box.get_sort_files_by_name());
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    Tool_box.set_sort_files_by_name(((CheckMenuItem) actionEvent.getSource()).isSelected());
                    scene_geometry_changed("sort file by name");
                }
            });
            context_menu.getItems().add(item);
        }
        {
            String text = I18n.get_I18n_string("Show_icons_for_images_and_videos",logger);// to: " + parent.toAbsolutePath().toString();
            CheckMenuItem item = new CheckMenuItem(text);
            item.setSelected(Tool_box.get_show_icons());
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    Tool_box.set_show_icons(((CheckMenuItem) actionEvent.getSource()).isSelected());
                    scene_geometry_changed("show icons="+((CheckMenuItem) actionEvent.getSource()).isSelected());
                }
            });
            context_menu.getItems().add(item);
        }
        create_menu_item_for_icon_size(context_menu);
        create_menu_item_for_style(context_menu);
        create_menu_item_for_language(context_menu);

        {
            String text = I18n.get_I18n_string("Invert_vertical_scroll_direction",logger);// to: " + parent.toAbsolutePath().toString();

            CheckMenuItem item = new CheckMenuItem(text);
            item.setSelected(Tool_box.get_vertical_scroll());
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    Tool_box.set_vertical_scroll(((CheckMenuItem) actionEvent.getSource()).isSelected());
                }
            });
            context_menu.getItems().add(item);
        }


        {
            String text = I18n.get_I18n_string("Clear_Icon_Cache_Folder",logger);// to: " + parent.toAbsolutePath().toString();

            MenuItem item = new MenuItem(text);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    Tool_box.clear_icon_cache(logger);
                }
            });
            context_menu.getItems().add(item);
        }
       {
            String text = I18n.get_I18n_string("Clear_Trash_Folder",logger);// to: " + parent.toAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    Tool_box.clear_trash(logger);
                }
            });
            context_menu.getItems().add(item);
        }
        {
            MenuItem item = new MenuItem("About Klik");
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    show_about_stage();
                }
            });
            context_menu.getItems().add(item);
        }

        context_menu.show(the_pane, x, y);
    }


    @Override
    public void scroll_a_bit(double dy)
    {
        scroll(dy);
    }

    Scan_show the_scan_show;

    private void start_scan()
    {
        the_scan_show = new Scan_show(this,logger);

    }
    private void stop_scan()
    {
        if (the_scan_show == null) return;
        the_scan_show.stop_the_show();
        the_scan_show = null;

    }
    private void toggle_scan()
    {
        if (the_scan_show == null) start_scan();
        else stop_scan();
    }
    private void slow_down_scan()
    {
        if (the_scan_show == null)
        {
            start_scan();
            return;
        }
        the_scan_show.slow_down();
    }
    private void speed_up_scan()
    {
        if (the_scan_show == null)
        {
            start_scan();
            return;
        }
        the_scan_show.hurry_up();
    }

    private void create_history_menu(Menu history_menu)
    {
        {
            String text = I18n.get_I18n_string("Clear_History",logger);// to: " + parent.toAbsolutePath().toString();
            MenuItem item = new MenuItem(text);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    get_history_of_dirs().clear();
                }
            });
            history_menu.getItems().add(item);
        }

        Browser this_browser = this;
        for (History_item hi : get_history_of_dirs().history)
        {
            if ( hi.string.equals(displayed_folder_path.toAbsolutePath().toString()))
            {
                // no interrest in showing the one we are in !
                continue;
            }
            MenuItem item = new MenuItem(hi.string);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    create_browser(this_browser,false,Path.of(hi.string),false,logger);
                }
            });
            history_menu.getItems().add(item);

        }


    }

    //**********************************************************
    public static History get_history_of_dirs()
    //**********************************************************
    {
        return new History("HISTORY_OF_DIRS");
    }


    //**********************************************************
    private void create_menu_item_for_style(ContextMenu context_menu)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Style",logger);// to: " + parent.toAbsolutePath().toString();

        Menu menu = new Menu(text);
        context_menu.getItems().add(menu);
        for( Look_and_feel s : Look_and_feel_manager.registered)
        {
            create_check_menu_item_for_style(menu, s);
        }
    }
    //**********************************************************
    private void create_check_menu_item_for_style(Menu menu, Look_and_feel style)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem("" + style.name);
        Look_and_feel current = Properties.get_style(logger);
        if (current.name.equals(style.name) )
        {
            item.setSelected(true);
        } else {
            item.setSelected(false);
        }
        Browser this_browser = this;

        item.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (((CheckMenuItem) actionEvent.getSource()).isSelected()) {
                    Look_and_feel_manager.set_look_and_feel(style);

                    Browser.create_browser(this_browser,true, displayed_folder_path,true,logger);
                }
            }
        });
        menu.getItems().add(item);

    }

    //**********************************************************
    private void create_menu_item_for_language(ContextMenu context_menu)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Language",logger);// to: " + parent.toAbsolutePath().toString();

        Menu menu = new Menu(text);
        context_menu.getItems().add(menu);
        for( String s : Local_manager.get_registered_locals())
        {
            create_check_menu_item_for_language(menu, s);
        }
    }
    //**********************************************************
    private void create_check_menu_item_for_language(Menu menu, String s)
    //**********************************************************
    {
        CheckMenuItem item = new CheckMenuItem("" + s);
        String current = Properties.get_language(logger);
        if (current.equals(s) )
        {
            item.setSelected(true);
        } else {
            item.setSelected(false);
        }
        Browser this_browser = this;
        item.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (((CheckMenuItem) actionEvent.getSource()).isSelected())
                {
                    Local_manager.set_instance(s);
                    I18n.reset();
                    Browser.create_browser(this_browser,true, displayed_folder_path,true,logger);
                }
            }
        });
        menu.getItems().add(item);

    }

    //**********************************************************
    private void create_menu_item_for_icon_size(ContextMenu context_menu)
    //**********************************************************
    {
        String text = I18n.get_I18n_string("Icon_Size",logger);// to: " + parent.toAbsolutePath().toString();

        Menu item = new Menu(text);

        context_menu.getItems().add(item);

        create_menu_item_for_one_icon_size(item, 64);
        create_menu_item_for_one_icon_size(item, 128);
        create_menu_item_for_one_icon_size(item, Properties.DEFAULT_ICON_SIZE);
        create_menu_item_for_one_icon_size(item, 400);
        create_menu_item_for_one_icon_size(item, 512);
        create_menu_item_for_one_icon_size(item, 1024);
    }


    //**********************************************************
    public void set_select_all(Boolean b)
    //**********************************************************
    {
        select_all = b;

        if (select_all) {
            the_pane.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));

        } else {
            the_pane.setBackground(new Background(new BackgroundFill(Look_and_feel_manager.get_instance().get_background_color(), CornerRadii.EMPTY, Insets.EMPTY)));
        }
    }

    //**********************************************************
    private void search_images_by_keyworks()
    //**********************************************************
    {

        //List<String> exclusion_list = Tool_box.load_keyword_exclusion_list(logger);
        Set<String> given = new HashSet<>();
        Image_stage.ask_user_and_find(
                displayed_folder_path,
                the_stage,
                the_pane,
                given,
                logger
        );

    }

    //**********************************************************
    public void show_about_stage()
    //**********************************************************
    {
        TextFlow textFlow = new TextFlow();
        textFlow.setLayoutX(40);
        textFlow.setLayoutY(40);

        one_line_bold("Klik",textFlow);
        one_line("    Klik is a file system explorer with a strong focus on images.",textFlow);
        one_line("    The original purpose of Klik was to enable to sort pictures into folders,",textFlow);
        one_line("    but it is a pretty good File Manager ",textFlow);
        one_line_bold("Performance",textFlow);
        one_line("    Klik has been designed for speed",textFlow);
        one_line("    Klik can manipulate folders that contains ten of thousands of images",textFlow);
        one_line("    Klik can display huge images (tested up to 14000x10000 pixels)",textFlow);
        one_line("    Most time consuming operations are performed asynchronously so that the UI remains fluid",textFlow);
        one_line_bold("Intuitive",textFlow);
        one_line("    Klik has been designed to be very intuitive",textFlow);
        one_line("    Play with Drag & Drop, you will see!",textFlow);
        one_line("    Moving files and folders around has never been easier",textFlow);
        one_line_bold("Transparency",textFlow);
        one_line("    Contrarily to a number of other products, Klik does not hide your images",textFlow);
        one_line("    Klik does not use hidden folders or whatever \"Libraries\"!",textFlow);
        one_line("    Klik only uses 100% transparent file system operations",textFlow);
        one_line("    Klik never modifies a file, it only creates folders at your will",textFlow);
        one_line("    and enables you to move files from folder to folder",textFlow);
        one_line_bold("Safety",textFlow);
        one_line("    Klik never deletes a file without asking you for confirmation",textFlow);
        one_line("    In Klik, \"delete\" actually means moving the file into the \"klik_trash\" folder",textFlow);
        one_line("    you can visit the \"klik_trash\" folder and recover any \"deleted\" file or folder",textFlow);
        one_line("    only clearing the \"klik_trash\" folder is final, and you will be asked for confirmation",textFlow);
        one_line_bold("Windows",textFlow);
        one_line("     Klik has 2 types of windows: \"Browser\" and \"Image\" ",textFlow);
        one_line("     You can open has many windows as you want, the limit is your machine's RAM",textFlow);
        one_line_bold("Browser Windows",textFlow);
        one_line("    = displays the content of a folder",textFlow);
        one_line("    Uses icons for images and movies, and buttons for everything else",textFlow);
        one_line("    Clicking on an icon will popup a new \"Image\" window displaying that image ",textFlow);
        one_line("    Clicking on an file-button will open that file (with the default application) ",textFlow);
        one_line("    Clicking on an folder-button will open that folder, replacing the current one ",textFlow);
        one_line("    Has a slideshow mode ",textFlow);
        one_line_bold("Image Windows",textFlow);
        one_line("    = displays one image at a time",textFlow);
        one_line("    Can load images one after the other very fast to explore a folder (using the space bar or the left/right arrows",textFlow);
        one_line("    Has a slideshow mode",textFlow);
        one_line_bold("Top Buttons",textFlow);
        one_line("    Klik \"Browser window\" has 4 top buttons that are always present (even if the folder is empty)",textFlow);
        one_line("        Up button: will open the parent directory",textFlow);
        one_line("        Tools button: will popup a menu (see below)",textFlow);
        one_line("        New Window button: will popup a new \"Browser\" Window",textFlow);
        one_line("        Trash button: will display the content of the \"klik_trash\" folder",textFlow);
        one_line_bold("Drag & drop (D&D)",textFlow);
        one_line("    In Klik, you can Drag-and-Drop (almost) everything!",textFlow);
        one_line("        In a Browser window: ",textFlow);
        one_line("           D&D works for icons representing images in a folder ,",textFlow);
        one_line("           D&D works for buttons representing non-image files in a folder ,",textFlow);
        one_line("           D&D works for buttons folders",textFlow);
        one_line("        In an Image window:  D&D enables to move the image",textFlow);
        one_line("        D&D reception spots (where you can drop something) include:",textFlow);
        one_line("          Browser window: the file will be moved into the corresponding folder",textFlow);
        one_line("          Folder buttons: the file will be moved into the corresponding folder",textFlow);
        one_line("          Trash button: the file will be moved into the trash folder",textFlow);
        one_line("          Up button: the file will be moved into the parent folder",textFlow);
        one_line_bold("The little features that make Klik great",textFlow);
        one_line("     You can easily rename things (folders and files)",textFlow);
        one_line("     Klik remembers all settings (in a human readable file called klik_properties.txt)",textFlow);
        one_line("     Klik tells you how many pictures a folder contains",textFlow);
        one_line("     Klik displays file name and pixel sizes in the title of \"Image\" windows",textFlow);
        one_line("     You can sort folders alphabetically or by file/folder size",textFlow);
        one_line("     You can visualise how much room a folder takes on disk (folder size = everything including all sub-folder's content)",textFlow);
        one_line("     Klik history remembers the folders you visited, so you can shortcut",textFlow);
        one_line("     Klik history can be cleared (and it effectively erases for ever the history...)",textFlow);
        one_line("     Klik uses system defaults to open files: you can play music, open sheets etc",textFlow);
        one_line("     Klik uses system defaults to edit files: you can start the system-configured default editor for anything, from Klik",textFlow);
        one_line("     You can find images by keywords (it assumes keywords compose file names)",textFlow);
        one_line("     You can find duplicated files/images (even if they have different names)",textFlow);
        one_line("     You can see the full EXIF metadata of the pictures (if any)",textFlow);
        one_line("     You can repair animated gifs",textFlow);
        one_line("     You can close Klik windows with a single Escape key stroke",textFlow);


        one_line_bold("Copyrigth",textFlow);
        one_line("Copyrigth 2021 Philippe Gentric",textFlow);
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(1000, 1000);
        sp.setContent(textFlow);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        Stage local_stage = new Stage();
        local_stage.setHeight(600);
        local_stage.setWidth(1000);

        Scene scene = new Scene(sp, 1000, 600, Color.WHITE);
        local_stage.setTitle("About Klik");
        local_stage.setScene(scene);
        local_stage.show();
    }

    private void one_line_bold(String s, TextFlow textFlow)
    {
        Text t = new Text(s);
        t.setFont(Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR,24));
        textFlow.getChildren().add(t);
        textFlow.getChildren().add(new Text(System.lineSeparator()));
    }
    private void one_line(String s, TextFlow textFlow)
    {
        Text t = new Text(s);
        t.setFont(Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR,16));
        textFlow.getChildren().add(t);
        textFlow.getChildren().add(new Text(System.lineSeparator()));
    }


}
