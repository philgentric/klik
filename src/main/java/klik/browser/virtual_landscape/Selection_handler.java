// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Selection_state.java
package klik.browser.virtual_landscape;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import klik.browser.Drag_and_drop;
import klik.browser.items.Item;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.path_lists.Path_list_provider;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.log.Logger;
import klik.util.ui.Popups;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Selection_handler
//**********************************************************
{

    private final Pane the_pane;
    private final Virtual_landscape virtual_landscape;
    private final Logger logger;
    private final Selection_reporter selection_reporter;

    // state:
    private final List<File> selected_files = new ArrayList<>();
    double x0;
    double y0;
    double x1;
    double y1;
    Rectangle rect = new Rectangle(0, 0, 1, 1);
    private static final boolean multiple_selections = true;

    Selection_state selection_state = Selection_state.nothing_selected;

    //**********************************************************
    public Selection_handler(Pane the_pane_, Virtual_landscape virtual_landscape, Selection_reporter selection_reporter_, Logger logger_)
    //**********************************************************
    {
        the_pane = the_pane_;
        this.virtual_landscape = virtual_landscape;
        selection_reporter = selection_reporter_;
        logger = logger_;
    }

    //**********************************************************
    public void on_drag_over()
    //**********************************************************
    {
        if (Drag_and_drop.drag_and_drop_ultra_dbg) logger.log("on_drag_over()");
        selection_state = Selection_state.selection_started;
    }

    //**********************************************************
    public void on_drag_exited()
    //**********************************************************
    {
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("on_drag_exited()");
        selection_state = Selection_state.nothing_selected;
        nothing_selected();
    }


    //**********************************************************
    public void on_drop()
    //**********************************************************
    {
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("on_drop()");

    }


    //**********************************************************
    public void on_drag_done()
    //**********************************************************
    {
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("on_drag_done()");

    }


    //**********************************************************
    public boolean add_to_selected_files(Path path)
    //**********************************************************
    {
        if ( Files.isDirectory(path))
        {
            String s = "✅ WARNING: directories in a multiple selection is disabled, ignoring: "+path;
            logger.log(s);
            Popups.simple_alert(s,virtual_landscape.owner,logger);
            return false;
        }
        if ( Files.isSymbolicLink(path))
        {
            String s = "✅ WARNING: symbolic link in multiple selection is disabled, ignoring: "+path;
            logger.log(s);
            Popups.simple_alert(s,virtual_landscape.owner,logger);
            return false;
        }
        selected_files.add(path.toFile());
        if (Drag_and_drop.drag_and_drop_dbg)  {
            logger.log("✅ 1 file added to selection = " + path.getFileName());
            logger.log("✅ all selected files =");
            for (File p : selected_files) {
                logger.log("        " + p.getName());
            }
        }
        return true;
    }

    //**********************************************************
    public List<File> get_selected_files()
    //**********************************************************
    {
        return selected_files;
    }

    //**********************************************************
    public void set_select_all_files(boolean b)
    //**********************************************************
    {
        Look_and_feel i = Look_and_feel_manager.get_instance(virtual_landscape.owner,logger);
        if (b)
        {
            the_pane.setBackground(new Background(i.get_all_files_fill()));
        }
        else
        {
            the_pane.setBackground(new Background(i.get_background_fill()));
        }
    }

    //**********************************************************
    public void set_select_all_folders(boolean b)
    //**********************************************************
    {
        Look_and_feel i = Look_and_feel_manager.get_instance(virtual_landscape.owner,logger);
        if (b) {
            the_pane.setBackground(new Background(i.get_all_dirs_fill()));

        } else {
            the_pane.setBackground(new Background(i.get_background_fill()));
        }
    }


    //**********************************************************
    public void handle_mouse_pressed(final MouseEvent mouse_event)
    //**********************************************************
    {
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("handle_mouse_pressed()");

        if (!mouse_event.isPrimaryButtonDown())
        {
            // dont consume event
            return;
        }
        if (!multiple_selections)
        {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("handle_mouse_pressed() giving up, multiple selection disabled by config");
            return;
        }
        if ( selection_state != Selection_state.nothing_selected)
        {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("handle_mouse_pressed() giving up, selection_state != Selection_state.nothing_selected");
            return;
        }

        selection_state = Selection_state.selection_started;

        rect.setArcWidth(5);
        rect.setArcHeight(5);

        Color col = Look_and_feel_manager.get_instance(virtual_landscape.owner,logger).get_selection_box_color();

        rect.setStroke(col);
        rect.setFill(Color.TRANSPARENT);

        final Point2D in_parent = the_pane.sceneToLocal(mouse_event.getSceneX(), mouse_event.getSceneY());
        x0 = in_parent.getX();
        y0 = in_parent.getY();
        rect.setX(x0);
        rect.setY(y0);
        rect.setWidth(1);
        rect.setHeight(1);
        if ( !the_pane.getChildren().contains(rect))
        {
            the_pane.getChildren().add(rect);
        }
        mouse_event.consume();
    }

    //**********************************************************
    public void handle_mouse_dragged(final MouseEvent mouse_event)
    //**********************************************************
    {
        if (!multiple_selections) return;

        if ( selection_state != Selection_state.selection_started)
        {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("handle_mouse_dragged: GIVING UP since selection_state != Selection_state.selection_started");
            mouse_event.consume();
            return;
        }
        if (Drag_and_drop.drag_and_drop_ultra_dbg) logger.log("handle_drag_selection()");

        final Point2D in_parent = the_pane.sceneToLocal(mouse_event.getSceneX(), mouse_event.getSceneY());
        x1 = in_parent.getX();
        y1 = in_parent.getY();
        rect.setX(Math.min(x0, x1));
        rect.setY(Math.min(y0, y1));
        rect.setWidth(Math.abs(x1 - x0));
        rect.setHeight(Math.abs(y1 - y0));
        mouse_event.consume();
    }

    //**********************************************************
    void handle_mouse_released(final MouseEvent mouse_event)
    //**********************************************************
    {
        if (!multiple_selections) return;
        if ( selection_state != Selection_state.selection_started)
        {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("handle_mouse_released: GIVING UP since selection_state != Selection_state.selection_started");
            mouse_event.consume();
            return;
        }
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("handle_mouse_released: selection is defined");
        selection_state = Selection_state.selection_defined;

        final Point2D in_parent = the_pane.sceneToLocal(mouse_event.getSceneX(), mouse_event.getSceneY());
        x1 = in_parent.getX();
        y1 = in_parent.getY();
        the_pane.getChildren().remove(rect);
        // multiple_selection_active = false;

        double x = Math.min(x0, x1);
        double y = Math.min(y0, y1);
        double w = Math.abs(x1 - x0);
        double h = Math.abs(y1 - y0);

        // empty selection is a reset
        if ((w == 0) && (h == 0)) {
            /*
            if (open_item_at(x, y, browser) == true)
            {
                if ( Drag_and_drop.drag_and_drop_dbg) logger.log("selection area is EMPTY means click to open");
            }
            */

            reset_selection();
            nothing_selected();
        }
        else
        {
            // this is dragged click, if its empty it resets the  selection
            extract_selected_files(x, y, w, h);
        }

        mouse_event.consume();
    }

    //**********************************************************
    public void nothing_selected()
    //**********************************************************
    {
        set_select_all_files(false);
        set_select_all_folders(false);
    }

    //**********************************************************
    private void extract_selected_files(double x, double y, double w, double h)
    //**********************************************************
    {
        if ( Drag_and_drop.drag_and_drop_dbg) logger.log("selection area is: x=" + x + ", y=" + y + ", w= " + w + ", h= " + h);

        List<Item> items = virtual_landscape.get_items_in(the_pane, x, y, w, h);
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("=============selection=============");
        for (Item i : items)
        {
            i.set_is_selected(); // show visible feedback to user AND add to list
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("extract_selected_files: "+i.get_item_path().toAbsolutePath());
        }
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("===================================");

        if (items.isEmpty())
        {
            reset_selection();
        }
        else
        {
            selection_reporter.report(selected_files.size() + " files selected");
        }
    }

    //**********************************************************
    public void reset_selection()
    //**********************************************************
    {
        selected_files.clear();
        virtual_landscape.clear_all_selected_images();
    }

    //**********************************************************
    public int get_selected_files_count()
    //**********************************************************
    {
        return selected_files.size();
    }

    //**********************************************************
    public void add_into_selected_files(List<File> ll)
    //**********************************************************
    {
        selected_files.addAll(ll);
    }

    //**********************************************************
    public void select_all_files_in_folder(Path_list_provider path_list_provider)
    //**********************************************************
    {
        reset_selection();
        add_into_selected_files(path_list_provider.only_files(Feature_cache.get(Feature.Show_hidden_files)));
        set_select_all_files(true);
    }
}
