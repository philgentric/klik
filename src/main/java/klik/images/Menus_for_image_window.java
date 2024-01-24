package klik.images;

import javafx.print.PrinterJob;
import javafx.scene.control.*;
import klik.actor.Aborter;
import klik.browser.icons.animated_gifs.Gif_repair;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.change.undo.Undo_engine;
import klik.files_and_paths.Guess_file_type;
import klik.level2.metadata.Tag_stage;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static klik.images.Image_display_handler.build_Image_context;

//**********************************************************
public class Menus_for_image_window
//**********************************************************
{

    /*
    //**********************************************************
    public static MenuBar make_menu_bar(Browser the_browser, Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuBar returned = new MenuBar();
        //returned.setStyle("-fx-foreground-color: white;-fx-background-color: darkgrey;");

        {
            Menu slide_show = new Menu("Slide show");
            returned.getMenus().add(slide_show);
            List<MenuItem> l = manage_slide_show(image_window);
            for ( MenuItem mi : l) slide_show.getItems().add(mi);
        }

        {
            Menu file_menu = new Menu("File");
            returned.getMenus().add(file_menu);
            MenuItem rename = get_rename_menu_item(image_window, image_context_owner);
            file_menu.getItems().add(rename);
            MenuItem copy = get_copy_menu_item(the_browser, image_window, image_context_owner);
            file_menu.getItems().add(copy);
            MenuItem print = get_print_menu_item(image_context_owner);
            file_menu.getItems().add(print);
            MenuItem delete = new MenuItem("Delete (d)");
            delete.setOnAction(actionEvent -> image_window.image_display_handler.delete());
            file_menu.getItems().add(delete);
            MenuItem edit_menu_item = make_edit_menu_item(image_window, image_context_owner);
            file_menu.getItems().add(edit_menu_item);
            MenuItem open = get_open_menu_item(image_window, image_context_owner);
            file_menu.getItems().add(open);
            MenuItem browse = get_browse_menu_item(image_window, image_context_owner);
            file_menu.getItems().add(browse);
            if (Static_application_properties.get_level2(image_context_owner.logger))
            {
                if (Guess_file_type.is_this_path_a_gif(image_context_owner.image_context.path)) {
                    MenuItem gif_repair = get_gif_repair_menu_item(image_window, image_context_owner);
                    file_menu.getItems().add(gif_repair);
                }
            }
        }
        {
            Menu undo = new Menu("Undo & do same");
            returned.getMenus().add(undo);
            MenuItem undo_move = get_undo_menu_item(image_window);
            undo.getItems().add(undo_move);
            MenuItem do_same_move = get_do_same_move_menu_item(image_window);
            undo.getItems().add(do_same_move);
        }

        {
            Menu search = new Menu("Search");
            returned.getMenus().add(search);
            MenuItem search_k = get_search_by_autoextracted_keyword_menu_item(the_browser, image_window, image_context_owner);
            search.getItems().add(search_k);
            MenuItem search_y = get_search_by_user_given_keywords_menu_item(the_browser, image_window, image_context_owner);
            search.getItems().add(search_y);
        }
        {
            Menu info = new Menu("Info");
            returned.getMenus().add(info);
            MenuItem info_menu_item = make_info_menu_item(image_window, image_context_owner);
            info.getItems().add(info_menu_item);
            MenuItem tag_view_menu_item = make_tag_view_menu_item(image_window);
            info.getItems().add(tag_view_menu_item);
            MenuItem tag_edit_menu_item = make_tag_edit_menu_item(image_window);
            info.getItems().add(tag_edit_menu_item);
        }

        {
            Menu display = new Menu("Display");
            returned.getMenus().add(display);
            List<MenuItem> l = manage_full_screen(image_window);
            for ( MenuItem mi : l) display.getItems().add(mi);
            CheckMenuItem quality = get_quality_check_menu_item(image_window, image_context_owner);
            display.getItems().add(quality);
        }
        {
            Menu mode = new Menu("Mode");
            returned.getMenus().add(mode);
            MenuItem click_to_zoom = get_set_zoom_mode_menu_item(image_window);
            mode.getItems().add(click_to_zoom);
            MenuItem drag_and_drop = get_set_drag_and_drop_mode_menu_item(image_window);
            mode.getItems().add(drag_and_drop);
            MenuItem pix_for_pix = get_set_pix_for_pix_menu_item(image_window);
            mode.getItems().add(pix_for_pix);
        }
        {
            Menu RAM_cache = new Menu("Image RAM cache");
            returned.getMenus().add(RAM_cache);
            MenuItem show_cache = get_show_cache_menu_item(image_window);
            RAM_cache.getItems().add(show_cache);
            MenuItem clear_cache = get_clear_cache_menu_item(image_window);
            RAM_cache.getItems().add(clear_cache);
        }

        return returned;
    }
*/
    //**********************************************************
    public static ContextMenu make_context_menu(Browser the_browser, Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        final ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu);
        List<MenuItem> fullscreen = manage_full_screen(image_window);
        for (MenuItem mi : fullscreen) context_menu.getItems().add(mi);

        List<MenuItem> l = manage_slide_show(image_window);
        for ( MenuItem mi : l) context_menu.getItems().add(mi);

        MenuItem info_menu_item = make_info_menu_item(image_window, image_context_owner);
        context_menu.getItems().add(info_menu_item);

        MenuItem edit_menu_item = make_edit_menu_item(image_window, image_context_owner);
        context_menu.getItems().add(edit_menu_item);

        CheckMenuItem quality = get_quality_check_menu_item(image_window, image_context_owner);
        context_menu.getItems().add(quality);

        MenuItem open = get_open_menu_item(image_window, image_context_owner);
        context_menu.getItems().add(open);


        MenuItem browse = get_browse_menu_item(image_window, image_context_owner);
        context_menu.getItems().add(browse);

        MenuItem rename = get_rename_menu_item(image_window, image_context_owner);
        context_menu.getItems().add(rename);

        MenuItem copy = get_copy_menu_item(the_browser, image_window, image_context_owner);
        context_menu.getItems().add(copy);

        MenuItem print = get_print_menu_item(image_context_owner);
        context_menu.getItems().add(print);


        MenuItem search_k = get_search_by_autoextracted_keyword_menu_item(the_browser, image_window, image_context_owner);
        context_menu.getItems().add(search_k);


        MenuItem search_y = get_search_by_user_given_keywords_menu_item(the_browser, image_window, image_context_owner);
        context_menu.getItems().add(search_y);


        MenuItem click_to_zoom = get_set_zoom_mode_menu_item(image_window);
        context_menu.getItems().add(click_to_zoom);

        MenuItem drag_and_drop = get_set_drag_and_drop_mode_menu_item(image_window);
        context_menu.getItems().add(drag_and_drop);

        MenuItem pix_for_pix = get_set_pix_for_pix_menu_item(image_window);
        context_menu.getItems().add(pix_for_pix);


        if ( Static_application_properties.get_level2(image_context_owner.logger))
        {
            if (Guess_file_type.is_this_path_a_gif(image_context_owner.image_context.path)) {
                MenuItem gif_repair = get_gif_repair_menu_item(image_window, image_context_owner);
                context_menu.getItems().add(gif_repair);
            }
        }

        MenuItem undo_move = get_undo_menu_item(image_window);
        context_menu.getItems().add(undo_move);

        return context_menu;
    }

    //**********************************************************
    private static MenuItem get_do_same_move_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem move_again = new MenuItem("Move this image to same destination folder as previous move");//I18n.get_I18n_string("Undo_LAST_move_or_delete", image_window.logger));
        move_again.setOnAction(e -> do_same_move(image_window));
        return move_again;
    }
    //**********************************************************
    public static void do_same_move(Image_window image_window)
    //**********************************************************
    {
        image_window.logger.log("moving image to same folder as previous move");
        Same_move_engine.same_move(image_window.image_display_handler.image_context.path, image_window.the_Stage, image_window.logger);
        image_window.image_display_handler.change_image_relative(1, image_window.ultim_mode);

    }

    //**********************************************************
    private static MenuItem get_undo_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem undo_move = new MenuItem(I18n.get_I18n_string("Undo_LAST_move_or_delete", image_window.logger));
        undo_move.setOnAction(e -> {
            image_window.logger.log("undoing last move");
            Undo_engine.perform_last_undo(image_window.the_Stage, image_window.logger);
        });
        return undo_move;
    }

    //**********************************************************
    private static MenuItem get_gif_repair_menu_item(Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuItem repair1 = new MenuItem(I18n.get_I18n_string("Repair_animated_gif", image_window.logger));
        repair1.setOnAction(e -> {
            image_context_owner.logger.log("GIF repair1");
            Path tmp_dir = Gif_repair.extract_all_frames_in_animated_gif(image_window.the_Stage, image_context_owner.image_context, new Aborter(), image_window.logger);
            if (tmp_dir == null) {
                image_context_owner.logger.log("GIF repair1 failed!");
                return;
            }
            image_context_owner.logger.log("GIF repair1 OK");
            Path local_path = Gif_repair.reassemble_all_frames(image_window.the_Stage, image_context_owner.image_context, tmp_dir, image_window.logger);
            if (local_path == null) {
                image_context_owner.logger.log("GIF repair2 failed!");
                return;
            }
            image_context_owner.logger.log("GIF repair2 OK");
            image_context_owner.image_context = Image_context.get_Image_context(local_path,image_window.aborter, image_window.logger);
            if (image_context_owner.image_context == null) {
                image_context_owner.logger.log("getting a new image context failed after gif repair");
            } else {
                image_window.set_image(image_context_owner.image_context);
            }
        });
        return repair1;
    }



    //**********************************************************
    private static MenuItem get_show_cache_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem clear_ram_cache = new MenuItem("Print image RAM cache info");//I18n.get_I18n_string("qsqsdsdq", image_window.logger));
        clear_ram_cache.setOnAction(event -> image_window.image_display_handler.print_image_cache());
        return clear_ram_cache;
    }

    //**********************************************************
    private static MenuItem get_clear_cache_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem clear_ram_cache = new MenuItem("Clear image RAM cache");//I18n.get_I18n_string("qsqsdsdq", image_window.logger));
        clear_ram_cache.setOnAction(event -> image_window.image_display_handler.clear_all_image_cache());
        return clear_ram_cache;
    }

    //**********************************************************
    private static MenuItem get_set_pix_for_pix_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem pix_for_pix = new MenuItem(I18n.get_I18n_string("Pix_for_pix", image_window.logger));
        pix_for_pix.setOnAction(event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.pix_for_pix));
        return pix_for_pix;
    }

    //**********************************************************
    private static MenuItem get_set_drag_and_drop_mode_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem drag_and_drop = new MenuItem(I18n.get_I18n_string("Drag_and_drop", image_window.logger));
        drag_and_drop.setOnAction(event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.drag_and_drop));
        return drag_and_drop;
    }

    //**********************************************************
    private static MenuItem get_set_zoom_mode_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem click_to_zoom = new MenuItem(I18n.get_I18n_string("Click_to_zoom", image_window.logger));
        click_to_zoom.setOnAction(event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.click_to_zoom));
        return click_to_zoom;
    }

    //**********************************************************
    private static MenuItem get_search_by_user_given_keywords_menu_item(Browser the_browser, Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuItem search_y = new MenuItem(I18n.get_I18n_string("Choose_keywords", image_window.logger));
        search_y.setOnAction(event -> image_context_owner.image_context.search_using_keywords_given_by_the_user(the_browser));
        return search_y;
    }

    //**********************************************************
    private static MenuItem get_search_by_autoextracted_keyword_menu_item(Browser the_browser, Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuItem search_k = new MenuItem(I18n.get_I18n_string("Search_by_keywords_from_this_ones_name", image_window.logger));
        search_k.setOnAction(event -> image_context_owner.image_context.search_using_keywords_from_the_name(the_browser));
        return search_k;
    }

    //**********************************************************
    private static MenuItem get_copy_menu_item(Browser the_browser, Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuItem copy = new MenuItem(I18n.get_I18n_string("Copy", image_window.logger));
        copy.setOnAction(event -> {
            Runnable r = image_context_owner.image_indexer::signal_file_copied;
            image_context_owner.image_context.copy(the_browser, r);
        });
        return copy;
    }
    //**********************************************************
    private static MenuItem get_print_menu_item(Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuItem print = new MenuItem(I18n.get_I18n_string("Print", image_context_owner.logger));
        print.setOnAction(event -> {
            image_context_owner.logger.log("Printing");
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null) {
                System.out.println(job.jobStatusProperty().asString());

                boolean printed = job.printPage(image_context_owner.image_context.the_image_view);
                if (printed) {
                    job.endJob();
                    image_context_owner.logger.log("Printing done");
                } else {
                    image_context_owner.logger.log("Printing failed.");
                }
            } else {
                image_context_owner.logger.log("Could not create a printer job.");
            }
        });
        return print;
    }



    //**********************************************************
    private static MenuItem get_rename_menu_item(Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuItem rename = new MenuItem(I18n.get_I18n_string("Rename_with_shortcut", image_window.logger));
        rename.setOnAction(event -> image_context_owner.image_context.rename_file_for_an_image_window(image_window));
        return rename;
    }

    //**********************************************************
    private static MenuItem get_browse_menu_item(Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuItem browse = new MenuItem(I18n.get_I18n_string("Browse", image_window.logger));

        browse.setOnAction(event -> {
            image_context_owner.logger.log("browse this!");
             Browser_creation_context.additional_no_past(image_context_owner.image_context.path.getParent(), image_window.logger);
        });
        return browse;
    }

    //**********************************************************
    private static MenuItem get_open_menu_item(Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuItem open = new MenuItem(I18n.get_I18n_string("Open", image_window.logger));
        open.setOnAction(event -> image_context_owner.image_context.open());
        return open;
    }

    //**********************************************************
    private static CheckMenuItem get_quality_check_menu_item(Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        CheckMenuItem quality = new CheckMenuItem(I18n.get_I18n_string("Image_quality_high", image_window.logger));
        quality.setSelected(image_context_owner.alternate_rescaler);
        quality.setOnAction(actionEvent -> {
            boolean new_high_quality = ((CheckMenuItem) actionEvent.getSource()).isSelected();
            if (new_high_quality != image_context_owner.alternate_rescaler) {
                Image_context image_context = build_Image_context(new_high_quality, image_context_owner.image_context.path, image_window.aborter, image_window.logger);
                image_window.set_image(image_context);
            }

        });
        return quality;
    }

    //**********************************************************
    private static MenuItem make_edit_menu_item(Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuItem edit = new MenuItem(I18n.get_I18n_string("Edit", image_window.logger));
        edit.setOnAction(event -> image_context_owner.image_context.edit());
        return edit;
    }

    //**********************************************************
    private static MenuItem make_info_menu_item(Image_window image_window, Image_display_handler image_context_owner)
    //**********************************************************
    {
        MenuItem info = new MenuItem(I18n.get_I18n_string("Info_about", image_window.logger)
                + image_context_owner.image_context.path.toAbsolutePath() + I18n.get_I18n_string("Info_about_file_shortcut", image_window.logger));
        info.setOnAction(event -> image_context_owner.image_context.show_exif_stage());
        return info;
    }

    //**********************************************************
    private static MenuItem make_tag_edit_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem info = new MenuItem("Edit image tag properties - experimental (t)");
        info.setOnAction(event -> Tag_stage.open_tag_edit_stage(image_window.image_display_handler.get_image_context().path,image_window.logger));
        return info;
    }
    //**********************************************************
    private static MenuItem make_tag_view_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem info = new MenuItem("Show image tag properties - experimental (t)");
        info.setOnAction(event -> Tag_stage.open_tag_view_stage(image_window.image_display_handler.get_image_context().path,image_window.logger));
        return info;
    }


    //**********************************************************
    private static List<MenuItem> manage_slide_show(Image_window image_window)
    //**********************************************************
    {
        List<MenuItem> returned = new ArrayList<>();

        boolean slide_show_is_running = image_window.is_slide_show_running();

        MenuItem slide_show_start = new MenuItem(I18n.get_I18n_string("Start_slide_show", image_window.logger)+" (s)");
        returned.add(slide_show_start);
        MenuItem slide_show_stop = new MenuItem(I18n.get_I18n_string("Stop_slide_show", image_window.logger)+" (s)");
        returned.add(slide_show_stop);
        MenuItem faster = new MenuItem("Slide show: faster (x)");
        returned.add(faster);
        faster.setOnAction(actionEvent -> {
            if (slide_show_is_running) image_window.hurry_up();
        });
        MenuItem slower = new MenuItem("Slide show: slower (w)");
        returned.add(slower);
        slower.setOnAction(actionEvent -> {
            if (slide_show_is_running) image_window.slow_down();
        });

        if( slide_show_is_running)
        {
            slide_show_start.setDisable(true);
            slide_show_stop.setDisable(false);
            faster.setDisable(false);
            slower.setDisable(false);
        }
        else
        {
            slide_show_start.setDisable(false);
            slide_show_stop.setDisable(true);
            faster.setDisable(true);
            slower.setDisable(true);
        }

        slide_show_start.setOnAction(event -> {
            System.out.println("slide_show_start OnAction");
            image_window.start_slide_show();
            slide_show_start.setDisable(true);
            slide_show_stop.setDisable(false);
            faster.setDisable(false);
            slower.setDisable(false);
        });

        slide_show_stop.setOnAction(event -> {
            image_window.stop_slide_show();
            slide_show_start.setDisable(false);
            slide_show_stop.setDisable(true);
            faster.setDisable(true);
            slower.setDisable(true);
        });

        return returned;
    }


    //**********************************************************
    private static List<MenuItem> manage_full_screen(Image_window image_window)
    //**********************************************************
    {
        MenuItem start_fullscreen = new MenuItem(I18n.get_I18n_string("Go_full_screen", image_window.logger));
        MenuItem stop_fullscreen = new MenuItem(I18n.get_I18n_string("Stop_full_screen", image_window.logger));

        if ( image_window.is_full_screen)
        {
            start_fullscreen.setDisable(true);
            stop_fullscreen.setDisable(false);

        }
        else {
            start_fullscreen.setDisable(false);
            stop_fullscreen.setDisable(true);
        }

        start_fullscreen.setOnAction(event -> {
            image_window.the_Stage.setFullScreen(true);
            image_window.is_full_screen = true;
            stop_fullscreen.setDisable(false);
            start_fullscreen.setDisable(true);
        });
        stop_fullscreen.setOnAction(event -> {
            image_window.the_Stage.setFullScreen(false);
            image_window.is_full_screen = false;
            stop_fullscreen.setDisable(true);
            start_fullscreen.setDisable(false);
        });


        List<MenuItem> returned = new ArrayList<>();
        returned .add(start_fullscreen);
        returned .add(stop_fullscreen);
        return returned;
    }
}
