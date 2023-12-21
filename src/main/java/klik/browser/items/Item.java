package klik.browser.items;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.browser.Drag_and_drop;
import klik.browser.icons.Icon_destination;
import klik.browser.icons.Icon_status;
import klik.files_and_paths.Files_and_Paths;
import klik.look.Font_size;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.metadata.Tag_stage;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public abstract class Item implements Icon_destination
//**********************************************************
{
    protected final int icon_size;
    protected double rotation = 0; // cache
    public Icon_status icon_status = Icon_status.no_icon;

    protected final boolean dbg = false;
    protected Path path;
    protected final Browser browser;
    protected final Logger logger;
    public final Iconifiable_item_type item_type;
    public AtomicBoolean visible_in_scene = new AtomicBoolean(false);
    public final Aborter aborter = new Aborter();

    // virtual coordinates: will change whenever the window geometry changes
    private double x;
    private double y;

    //**********************************************************
    public Item(Browser browser_,
                Path path_,
                Logger logger_)
    //**********************************************************
    {
        browser = browser_;
        path = path_;
        logger = logger_;
        item_type = Iconifiable_item_type.from_extension(path);
        icon_size = Static_application_properties.get_icon_size(logger);
    }

    public final Scene getScene()
    {
        return browser.the_Scene;
    }

    protected final Logger get_logger()
    {
        return logger;
    }


    public void set_visible(boolean b)
    {
        get_Node().setVisible(b);
    }

    public void set_translate_X(double dx)
    {
        if (get_Node() != null) get_Node().setTranslateX(dx);
    }

    public void set_translate_Y(double dy)
    {
        if (get_Node() != null) get_Node().setTranslateY(dy);
    }

    public void set_x(double x_)
    {
        x = x_;
    }

    public void set_y(double y_)
    {
        y = y_;
    }

    public double get_x()
    {
        return x;
    }

    public double get_y()
    {
        return y;
    }

    public abstract Node get_Node();

    public abstract double get_Width();

    public abstract void set_MinHeight(double height);
    //public abstract void set_MinWidth(double height);

    public abstract double get_Height();

    // this is called asynchronously from Icon_factory, when the icon has been made
    public abstract void set_Image(Image i, boolean real);

    @Override
    public Icon_status get_icon_status() {
        return icon_status;
    }

    @Override
    public void set_icon_status(Icon_status s) {
        icon_status = s;
    }

    @Override
    public Iconifiable_item_type get_item_type() {
        return item_type;
    }

    public abstract String get_string();

    public abstract void set_is_selected_internal();

    public abstract void set_is_unselected_internal();

    //**********************************************************
    public void unset_image_is_selected()
    //**********************************************************
    {
        set_is_unselected_internal();
    }

    //**********************************************************
    public void set_is_selected()
    //**********************************************************
    {
        browser.selection_handler.add_to_selected_files(path);
        set_is_selected_internal();
    }

    //**********************************************************
    private void set_background(BackgroundFill background_fill)
    //**********************************************************
    {
        Node n = get_Node();
        if ( n instanceof Button)
        {
            Button button = (Button)get_Node();
            button.setBackground(new Background(background_fill));
            Node node = button.getGraphic();
            if (node instanceof Label)
            {
                Look_and_feel_manager.set_label_look((Label) node);
            }
        }
        else if ( n instanceof VBox)
        {
            ((VBox)get_Node()).setBackground(new Background(background_fill));
        }
    }


    void set_background_for_setOnDragEntered() {
        BackgroundFill background_fill = Look_and_feel_manager.get_drag_fill();
        if (Drag_and_drop.dbg_drag_and_drop) logger.log("Item_folder_with_icon OnDragOver color = "+background_fill);
        set_background(background_fill);
    }

    void set_background_for_setOnDragOver()
    {
        set_background_for_setOnDragEntered();

    }

    void set_background_for_setOnDragExited() {
        Look_and_feel i = Look_and_feel_manager.get_instance();
        BackgroundFill color = i.get_background_fill();
        if (Drag_and_drop.dbg_drag_and_drop) logger.log("Item_folder_with_icon setOnDragExited color = "+color);
        set_background(color);

    }
    //**********************************************************
    public void init_drag_and_drop()
    //**********************************************************
    {

        get_Node().setOnDragDetected(drag_event -> {
            if (dbg) logger.log("Item.init_drag_and_drop() drag detected");
            Dragboard db = get_Node().startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
/*
            if (browser.selection_handler.get_select_all_folders())
            {
                logger.log("Item.init_drag_and_drop() drag detected, adding ALL folders");
                // the browser is in select all mode so this means we dont take just this 1 folder
                List<File> tmp = browser.get_folder_list();
                browser.selection_handler.set_select_all_folders(false);
                browser.selection_handler.reset_selection();
                browser.selection_handler.add_into_selected_files(tmp);
            }
            if (browser.selection_handler.get_select_all_files())
            {
                logger.log("Item.init_drag_and_drop() drag detected, adding ALL files");
                // the browser is in select all mode so this means we dont take just this 1 file
                List<File> tmp = browser.get_file_list();
                browser.selection_handler.set_select_all_files(false);
                browser.selection_handler.reset_selection();
                browser.selection_handler.add_into_selected_files(tmp);
            }
*/
            List<File> ll = browser.selection_handler.get_selected_files();
            // if we are here it is because the user is dragging an item
            if (!ll.contains(path.toFile())) ll.add(path.toFile());
            // this crashes the VM !!?? content.putFiles(ll);
            StringBuilder sb = new StringBuilder();
            for (File f : ll) {
                sb.append("\n").append(f.getAbsolutePath());
            }
            logger.log(" selected files: " + sb);
            content.put(DataFormat.PLAIN_TEXT, sb.toString());
            db.setContent(content);
            drag_event.consume();
        });

        get_Node().setOnDragDone(drag_event -> {
            if (drag_event.getTransferMode() == TransferMode.MOVE)
            {
                if (dbg) logger.log("Item.init_drag_and_drop() : setOnDragDone for " + path.toAbsolutePath());
                /*
                DO NOT report it: it will be reported by the receiver Browser scene
                List<Old_and_new_Path> l = new ArrayList<>();
                Command_old_and_new_Path k = Command_old_and_new_Path.command_move;
                Old_and_new_Path oan = new Old_and_new_Path(null,f,k);
                oan.set_status(Status_old_and_new_Path.status_moved);
                l.add(oan);
                Change_gang.report_event(l);*/

                browser.set_status(browser.selection_handler.get_selected_files_count()+ " files have been dragged out");
                browser.selection_handler.reset_selection();
                browser.selection_handler.nothing_selected();
            }
            drag_event.consume();
        });
    }


    //**********************************************************
    public void open_with_system(Logger logger)
    //**********************************************************
    {
        logger.log("open_with_system for path:" + path.toString());
        // try to open it with the system
        try
        {
            Desktop.getDesktop().open(path.toAbsolutePath().toFile());
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("open failed :" + e));

            if (e.toString().contains("doesn't exist."))
            {
                String error = "Your OS/GUI could not open this file";
                Popups.popup_warning(browser.my_Stage.the_Stage, "Open failed", error+", the error is:\n" + e, true,logger);
                browser.set_status(error);
            }
            else
            {
                Popups.popup_warning(browser.my_Stage.the_Stage,"Open failed", "Your OS/GUI could not open this file, the error is:\n" + e + "\nMaybe it is just not properly configured e.g. most often the file extension has to be registered?", false,logger);
            }
        }
    }

    static double xxx = 200;
    static double yyy = 200;
    //**********************************************************
    public static MenuItem create_show_file_size_menu_item(Browser b_, Path path, boolean dbg, Logger logger)
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Show_file_size", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("File size");
            String file_size = Files_and_Paths.get_2_line_string_with_size(path,logger);
            String message = I18n.get_I18n_string("File_size_for", logger) +"\n"+ path.getFileName().toString();
            //Popups.popup_warning(message, file_size, false,logger);
            Stage local_stage = new Stage();
            local_stage.setHeight(200);
            local_stage.setWidth(600);
            local_stage.setX(xxx);
            local_stage.setY(yyy);
            yyy+= 200;
            if ( yyy > 600)
            {
                yyy = 200;
                xxx += 600;
                if ( xxx > 1000) xxx = 200;
            }
            TextArea textarea1 = new TextArea(message+"\n"+file_size);
            Font_size.set_font_size(textarea1,24,logger);
            VBox vbox = new VBox(textarea1);
            Scene scene = new Scene(vbox, Color.WHITE);
            local_stage.setTitle(path.toAbsolutePath().toString());
            local_stage.setScene(scene);
            local_stage.show();

            b_.set_status(message+"   :   "+file_size.replace("\n","   =   "));
        });
        return menu_item;
    }

    //**********************************************************
    public static MenuItem create_edit_tag_menu_item(Path path, boolean dbg, Logger logger)
    //**********************************************************
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Show_tag", logger));
        menu_item.setOnAction(event -> {
            if (dbg) logger.log("File tag");
            Tag_stage.open_tag_stage(path,true,logger);
        });

        return menu_item;
    }


    public Path get_item_path() {
        return path;
    }

    // path for display takes different form depending on the item type
    // it can be null, a PNG icon, or an animated gif
    abstract public Path get_path_for_display();


    //**********************************************************
    void rotate_and_center(Image i, ImageView the_image_view)
    //**********************************************************
    {
        double x_difference = 0;
        double y_difference = 0;

        if ( rotation == 0)
        {
            if ( i.getHeight() < i.getWidth())
            {
                double actual = icon_size* i.getHeight()/ i.getWidth();
                y_difference = (icon_size-actual)/2;
            }
            else
            {
                double actual = icon_size* i.getWidth()/ i.getHeight();
                x_difference = (icon_size-actual)/2;
            }
        }
        else if ( rotation == 180)
        {
            if ( i.getHeight() < i.getWidth())
            {
                double actual = icon_size* i.getHeight()/ i.getWidth();
                y_difference = -(icon_size-actual)/2;
            }
            else
            {
                double actual = icon_size* i.getWidth()/ i.getHeight();
                x_difference = (icon_size-actual)/2;
            }
        }
        else if ( rotation == 90)
        {
            if ( i.getHeight() < i.getWidth())
            {
                double actual = icon_size* i.getHeight()/ i.getWidth();
                x_difference = -(icon_size-actual)/2;
            }
            else
            {
                double actual = icon_size* i.getWidth()/ i.getHeight();
                y_difference = (icon_size-actual)/2;
            }
        }
        else if ( rotation == 270)
        {
            if ( i.getHeight() < i.getWidth())
            {
                double actual = icon_size* i.getHeight()/ i.getWidth();
                x_difference = (icon_size-actual)/2;
            }
            else
            {
                double actual = icon_size* i.getWidth()/ i.getHeight();
                y_difference = (icon_size-actual)/2;
            }
        }

        the_image_view.getTransforms().clear();
        Translate trans = new Translate();
        trans.setX(x_difference);
        trans.setY(y_difference);
        the_image_view.getTransforms().add(trans);
        {
            Rotate rot = new Rotate();
            rot.setAngle(rotation);
            rot.setPivotX(icon_size / 2.0);
            rot.setPivotY(icon_size / 2.0);
            the_image_view.getTransforms().add(rot);
        }
    }

    //**********************************************************
    public void cancel()
    //**********************************************************
    {
        aborter.abort();
        cancel_custom();
    }
    public abstract void cancel_custom();

}
