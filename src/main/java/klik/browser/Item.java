package klik.browser;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import klik.I18N.I18n;
import klik.util.Logger;
import klik.util.Tool_box;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public abstract class Item
//**********************************************************
{
    protected final boolean dbg = true;
    protected Path path;
    protected final Browser the_browser;
    protected final Scene scene;
    protected final Logger logger;
    public boolean visible_in_scene = false;

    // virtual coordinates: will change whenever the window geometry changes
    private double x;
    private double y;

    //**********************************************************
    public Item(Browser the_browser_, Path f_, Scene scene_,Logger logger_)
    //**********************************************************
    {
        the_browser = the_browser_;
        path = f_;
        scene = scene_;
        logger = logger_;
    }

    public final Scene getScene() {
        return scene;
    }
    protected final Logger get_logger() {
        return logger;
    }
    public final Path get_Path()
    {
        return path;
    }

    void setVisible(boolean b)
    {
        get_Node().setVisible(b);
    }
    void setTranslateX(double dx)
    {
        if ( get_Node()!= null) get_Node().setTranslateX(dx);
    }
    void setTranslateY(double dy)
    {
        if ( get_Node()!= null) get_Node().setTranslateY(dy);
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
    public abstract void set_MinWidth(double width);
    public abstract double get_Width();
    public abstract void set_MinHeight(double height);
    public abstract double get_Height();
    public abstract void set_Image(Image i, boolean real);
    public abstract String get_string();


    //**********************************************************
    public void init_drag_and_drop()
    //**********************************************************
    {
        get_Node().setOnDragDetected(new EventHandler<MouseEvent>()
        {
            public void handle(MouseEvent event)
            {
                if (dbg) logger.log("Item.init_drag_and_drop() drag detected");
                Dragboard db = get_Node().startDragAndDrop(TransferMode.MOVE);

                ClipboardContent content = new ClipboardContent();
                List<File> l = new ArrayList<>();
                l.add(path.toFile());
                content.putFiles(l);
                if ( the_browser.get_select_all())
                {

                    String s = "";
                    for ( File f: the_browser.get_file_list())
                    {
                        s += "\n"+f.getAbsolutePath();
                    }
                    logger.log("Selected files: "+s);
                    content.put(DataFormat.PLAIN_TEXT,s);

                    // this crashes the VM !!
                    //content.putFiles(the_browser.get_file_list());

                    the_browser.set_select_all(false);
                }
                db.setContent(content);
                event.consume();
            }
        });

        get_Node().setOnDragDone(new EventHandler<DragEvent>()
        {
            public void handle(DragEvent event) {
                if (event.getTransferMode() == TransferMode.MOVE)
                {
                    if (dbg) logger.log("Item.init_drag_and_drop() : setOnDragDone for "+ path.toAbsolutePath());
                    /*
                    DO NOT report it: it will be reported by the receiver Browser scene

                    List<Old_and_new_Path> l = new ArrayList<>();
                    Command_old_and_new_Path k = Command_old_and_new_Path.command_move;
                    Old_and_new_Path oan = new Old_and_new_Path(null,f,k);
                    oan.set_status(Status_old_and_new_Path.status_moved);
                    l.add(oan);
                    Change_gang.report_event(l);*/
                }
                event.consume();
            }
        });
    }

    public static MenuItem create_show_file_size_menu_item(Path path, boolean dbg, Logger logger)
    {
        MenuItem menu_item = new MenuItem(I18n.get_I18n_string("Show_file_size",logger));
        menu_item.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (dbg) logger.log("File size");

                String file_size = Tool_box.get_2_line_string_with_size(path);


                Tool_box.popup_text(I18n.get_I18n_string("File_size_for",logger)+path.getFileName().toString(), file_size);
            }
        });
        return menu_item;
    }

}
