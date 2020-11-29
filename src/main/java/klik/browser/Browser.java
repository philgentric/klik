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
import javafx.scene.control.MenuItem;
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
import klik.change.After_move_handler;
import klik.change.Change_gang;
import klik.change.Old_and_new_Path;

import klik.change.Static_change_utilities;
import klik.images.Image_context;
import klik.images.Image_stage;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.History;
import klik.properties.History_item;
import klik.properties.Properties;
import klik.util.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

//**********************************************************
public class Browser implements After_move_handler, Y_max_listener, Exception_recorder
//**********************************************************
{
    public static final boolean dbg = false;
    public static double MIN_DIR_BUTTON_WIDTH = 350;
    private double dir_button_width = MIN_DIR_BUTTON_WIDTH;
    //public static double top_button_height = 50;
    //private double dir_button_height = 40;
    //private double file_button_height = 30;


    static int ID_generator = 0;
    int ID;


    public final Stage the_stage;
    final Scene the_scene;
    final Pane the_pane;
    final Icon_manager icon_manager;
    final Logger logger;
    final Path dir;
    //final private Button up_button;
    //private Button new_dir_button;
    List<Node> mandatory = new ArrayList<>();


    final Slider vertical_slider;
    public static final double half_slider_width = 20;
    public double slider_width = 400;
    public static final boolean slider_indicator = false;
    public static final boolean slider_mover = true;

    // state


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

    //**********************************************************
    private Browser(
            Stage the_stage_,
            Path dir_,
            Logger logger_)
    //**********************************************************
    {
        dir = dir_;
        logger = logger_;
        ID = ID_generator;
        ID_generator++;
        Change_gang.register(this);
        the_stage = the_stage_;

        set_title();


        the_pane = new Pane();
        the_scene = new Scene(the_pane);//, W, H);


        icon_manager = new Icon_manager(this,logger);
        String ret = icon_manager.scan_dir(dir,videos_for_which_giffing_failed);
        if (ret.equals(Icon_manager.ACCESS_DENIED_EXCEPTION)) {
            logger.log("access denied");
        }
        if (ret.equals(Icon_manager.GONE_DIR_EXCEPTION)) {
            logger.log("directory gone");
        }

        the_pane.getChildren().clear();
        Top_pane tp = new Top_pane(
                this,
                the_stage,
                the_scene,
                dir,
                Look_and_feel.get_top_button_height(),
                logger);
        mandatory.add(tp.hBox);

        //Button up_button = create_up_button();
        //mandatory.add(up_button);

        //double icon_size = Properties.get_icon_size();

        icon_manager.geometry_changed(
                this,
                the_stage,
                the_scene,
                the_pane,
                mandatory,
                Look_and_feel.get_top_button_height(),
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

                if (keyEvent.isMetaDown()) {
                    if (keyEvent.getCharacter().equals("a")) {
                        set_select_all(true);
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
                    Browser.create_browser(null, false, dir, false, logger);
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
                Tool_box.accept_drag_dropped_as_a_move_in(event, dir, the_scene, "scene of browser", logger);
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
                //up_button.toFront();
                tp.hBox.toFront();
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
        });

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                the_stage.setWidth(the_stage.getWidth() * 1.001);

            }
        });
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
                public void run() {
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

    public static double slider_to_scene(Icon_manager icon_manager,double slider)
    {
        return icon_manager.y_max-slider;
    }

    //**********************************************************
    private void set_title()
    //**********************************************************
    {
        if ( dir == null) return;
        if ( dir.toFile() == null) return;
        if ( dir.toFile().listFiles() == null) return;
        long how_many_files = dir.toFile().listFiles().length;
        the_stage.setTitle(dir.toAbsolutePath().toString()+" :     "+how_many_files+" files & folders"   );
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

        if (icon_manager.scan_dir(dir,videos_for_which_giffing_failed).equals(icon_manager.OK) == false)
        {
            logger.log(true, true, "scene_geometry_changed() scan dir failed");

            //Browser.create_browser(this, true, dir.getParent(), false, logger);
        }
        icon_manager.geometry_changed(
                this, the_stage, the_scene, the_pane, mandatory,
                Look_and_feel.get_top_button_height(),
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
        TextInputDialog dialog = new TextInputDialog(dir.toAbsolutePath().toString());
        dialog.setWidth(the_stage.getWidth());
        dialog.setTitle("New directory");
        dialog.setHeaderText("             Enter the name of the new directory                 ");
        dialog.setContentText("New directory name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();

            Path new_dir = dir.resolve(new_name);
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


        CheckMenuItem item = new CheckMenuItem("Icon size = " + target_size);
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

        if (Change_gang.is_my_directory_impacted(dir, l, logger)) {
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
        return "Browser_scene:" + dir.toAbsolutePath() + " " + ID;
    }

    public boolean select_all = false;

    List<File> get_image_file_list() {
        return icon_manager.get_image_file_list();
    }

    List<File> get_file_list() {
        return icon_manager.get_file_list();
    }

    //**********************************************************
    public void show_popup_menu(double x, double y)
    //**********************************************************
    {
        ContextMenu context_menu;
        context_menu = new ContextMenu();
        {
            Menu history_menu = new Menu("History");
            create_history_menu(history_menu);
            context_menu.getItems().add(history_menu);

        }
        {
            CheckMenuItem item = new CheckMenuItem("Select all files (not dirs) for drag and drop");
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
            MenuItem item = new MenuItem("Refresh");
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    scene_geometry_changed("refresh");
                }
            });
            context_menu.getItems().add(item);

        }
        {
            MenuItem item = new MenuItem("Create new empty directory");
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    create_new_directory();
                }
            });
            context_menu.getItems().add(item);

        }
        {
            MenuItem item = new MenuItem("Search images by keywords");
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    search_images_by_keyworks();
                }
            });
            context_menu.getItems().add(item);
        }
        {
            MenuItem item = new MenuItem("Undo LAST move or delete");
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    Static_change_utilities.undo_last_move(logger);
                }
            });
            context_menu.getItems().add(item);
        }
        {
            CheckMenuItem item = new CheckMenuItem("Invert vertical scroll direction");
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
            CheckMenuItem item = new CheckMenuItem("Show hidden files");
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
            CheckMenuItem item = new CheckMenuItem("Show only gif");
            item.setSelected(Properties.get_show_only_gifs());
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    Properties.set_show_only_gifs(((CheckMenuItem) actionEvent.getSource()).isSelected());
                    scene_geometry_changed("show onmy gif boolean changed");
                }
            });
            context_menu.getItems().add(item);
        }
        {
            CheckMenuItem item = new CheckMenuItem("Sort files by name (vs decreasing size)");
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
            CheckMenuItem item = new CheckMenuItem("Show icons for images and videos");
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



        {
            MenuItem item = new MenuItem("Clear icon cache folder");
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    Tool_box.clear_icon_cache(logger);
                }
            });
            context_menu.getItems().add(item);
        }
        {
            MenuItem item = new MenuItem("Clear trash folder");
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

    private void create_history_menu(Menu history_menu)
    {
        {
            MenuItem item = new MenuItem("Clear history");
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
            MenuItem item = new MenuItem(hi.path);
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    create_browser(this_browser,false,Path.of(hi.path),false,logger);
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
        Menu menu = new Menu("Style");
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

                    Browser.create_browser(this_browser,true, dir,true,logger);
                }
            }
        });
        menu.getItems().add(item);

    }
    //**********************************************************
    private void create_menu_item_for_icon_size(ContextMenu context_menu)
    //**********************************************************
    {
        Menu item = new Menu("Icon size");

        context_menu.getItems().add(item);

        create_menu_item_for_one_icon_size(item, 32);
        create_menu_item_for_one_icon_size(item, 64);
        create_menu_item_for_one_icon_size(item, 96);
        create_menu_item_for_one_icon_size(item, 128);
        create_menu_item_for_one_icon_size(item, 256);
        create_menu_item_for_one_icon_size(item, 300);
        create_menu_item_for_one_icon_size(item, 400);
        create_menu_item_for_one_icon_size(item, 500);
    }


    //**********************************************************
    private void set_select_all(Boolean b)
    //**********************************************************
    {
        select_all = b;

        if (select_all) {
            the_pane.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));

        } else {
            the_pane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        }
    }

    //List<Old_and_new_Path> last_moves = new ArrayList<>();

    //**********************************************************
    private void search_images_by_keyworks()
    //**********************************************************
    {

        //List<String> exclusion_list = Tool_box.load_keyword_exclusion_list(logger);
        Set<String> given = new HashSet<>();
        Image_stage.ask_user_and_find(
                dir,
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
        one_line("    The purpose of Klik is to enable to sort pictures into folders ",textFlow);
        one_line_bold("Performance",textFlow);
        one_line("    Klik has been designed for speed",textFlow);
        one_line("    Klik can manipulate folders that contains ten of thousands of images",textFlow);
        one_line("    Klik can display huge images",textFlow);
        one_line("    Display is always updated extremely fast: much faster than other products",textFlow);
        one_line("   Operations are performed asynchronously so that the UI remains fluid at all times",textFlow);
        one_line_bold("Intuitive",textFlow);
        one_line("    Klik has been designed to be very intuitive",textFlow);
        one_line("    Play with Drag & Drop, you will see!",textFlow);
        one_line("    Moving files and folders around has never been easier",textFlow);
        one_line_bold("Transparency",textFlow);
        one_line("    Contrarily to a number of other products, Klik does not hide your images",textFlow);
        one_line("    Klik does not use hidden folders or whatever \"Libraries\"!",textFlow);
        one_line("    Klik only uses 100% transparent file system operations",textFlow);
        one_line("    Klik never modifies a file, it only creates folders at our will",textFlow);
        one_line("    and enables you to move files from folder to folder",textFlow);
        one_line_bold("Safety",textFlow);
        one_line("    Klik never deletes a file without asking you for confirmation",textFlow);
        one_line("    In Klik, \"delete\" means moving the file into the \"klik_trash\" folder",textFlow);
        one_line("    you can visit the \"klik_trash\" folder and recover any \"deleted\" file or folder",textFlow);
        one_line("    only clearing the \"klik_trash\" folder is final, and you will be asked for confirmation",textFlow);
        one_line_bold("Windows",textFlow);
        one_line("     Klik has 2 types of windows: \"Browser\" and \"Image\" ",textFlow);
        one_line("     You can open has many windows as you want, the limit is your machine's RAM",textFlow);
        one_line("    \"Browser\" window = displays the content of a folder",textFlow);
        one_line("         Uses icons for images and movies, and buttons for everything else",textFlow);
        one_line("         Clicking on an icon will popup a new \"Image\" window displaying that image ",textFlow);
        one_line("         Clicking on an file-button will open that file (with the default application) ",textFlow);
        one_line("         Clicking on an folder-button will open that folder, replacing the current one ",textFlow);
        one_line("    \"Image\" window = displays one image at a time",textFlow);
        one_line("         Can load images one after the other very fast to explore a folder",textFlow);
        one_line("         Has a slideshow mode",textFlow);
        one_line_bold("Top Buttons",textFlow);
        one_line("    Klik \"Browser window\" has 4 top buttons that are always present (even if the folder is empty)",textFlow);
        one_line("        Up button: will open the parent directory",textFlow);
        one_line("        Tools button: will popup a menu (see below)",textFlow);
        one_line("        New Window button: will popup a new \"Browser\" Window",textFlow);
        one_line("        Trash button: will display the content of the \"klik_trash\" folder",textFlow);
        one_line_bold("Drag & drop (D&D)",textFlow);
        one_line("    In Klik, you can D&D (almost) everything!",textFlow);
        one_line("        In a Browser window: ",textFlow);
        one_line("           D&D works for icons representing images in a folder ,",textFlow);
        one_line("           D&D works for buttons representing non-image files in a folder ,",textFlow);
        one_line("           D&D works for buttons folders",textFlow);
        one_line("        In an Image window:  D&D enables to move the image",textFlow);
        one_line("        D&D places where you can drop something include:",textFlow);
        one_line("          Browser window: the file will be moved into the corresponding folder",textFlow);
        one_line("          Folder buttons: the file will be moved into the corresponding folder",textFlow);
        one_line("          Trash button: the file will be moved into the trash folder",textFlow);
        one_line("          Up button: the file will be moved into the trash folder",textFlow);
        one_line_bold("The little features that make Klik great",textFlow);
        one_line("     You can easily rename things (folders and files)",textFlow);
        one_line("     Klik tells you how many pictures a folder contains",textFlow);
        one_line("     Klik displays file name and pixel sizes in the title of \"Image\" windows",textFlow);
        one_line("     You can sort folders alphabetically or by file/folder size",textFlow);
        one_line("     You can visualise how much room something takes on disk, files and folders",textFlow);
        one_line("     Klik history remembers the folders you visited, so you can shortcut",textFlow);
        one_line("     Klik uses system defaults to open files: you can play music, open sheets etc",textFlow);
        one_line("     Klik uses system defaults to edit files: you can edit anything from Klik",textFlow);
        one_line("     You can find images by keywords (it assumes keywords compose file names)",textFlow);
        one_line("     You can find duplicated files/images (even if they have different names)",textFlow);
        one_line("     You can see the full EXIF metadata of the pictures (if any)",textFlow);
        one_line("     You can repair animated gifs",textFlow);
        one_line("     You can close Klik windows with a single Escape key stroke",textFlow);


        one_line_bold("Copyrigth",textFlow);
        one_line("Copyrigth 2020 Philippe Gentric",textFlow);
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
