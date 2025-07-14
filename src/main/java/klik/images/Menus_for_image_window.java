//SOURCES ../browser/icons/animated_gifs/Gif_repair.java
package klik.images;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.New_window_context;
import klik.browser.icons.animated_gifs.Gif_repair;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.browser.items.Item_file_with_icon;
import klik.change.Redo_same_move_engine;
import klik.change.undo.Undo_for_moves;
import klik.image_ml.face_recognition.Face_detection_type;
import klik.image_ml.face_recognition.Face_recognition_actor;
import klik.image_ml.face_recognition.Face_recognition_message;
import klik.image_ml.face_recognition.Face_recognition_service;
import klik.image_ml.image_similarity.Image_feature_vector_cache;
import klik.look.my_i18n.My_I18n;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Preferences_stage;
import klik.util.files_and_paths.Guess_file_type;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

//**********************************************************
public class Menus_for_image_window
//**********************************************************
{
    static MenuItem fullscreen;

    //**********************************************************
    public static void do_same_move(Image_window image_window)
    //**********************************************************
    {
        if ( image_window.image_display_handler.get_image_context().isEmpty()) return;

        image_window.logger.log("moving image to same folder as previous move");
        Redo_same_move_engine.same_move(image_window.image_display_handler.get_image_context().get().path, image_window.stage, image_window.logger);
        image_window.image_display_handler.change_image_relative(1, image_window.ultim_mode);

    }

    //**********************************************************
    private static MenuItem get_undo_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem undo_move = new MenuItem(My_I18n.get_I18n_string("Undo_LAST_move_or_delete", image_window.stage,image_window.logger));
        undo_move.setOnAction(e -> {
            image_window.logger.log("undoing last move");
            double x = image_window.stage.getX()+100;
            double y = image_window.stage.getY()+100;
            Undo_for_moves.perform_last_undo_fx(image_window.stage, x, y, image_window.logger);
        });
        return undo_move;
    }

    //**********************************************************
    private static MenuItem get_gif_repair_menu_item(boolean faster, Image_window image_window, Image_display_handler image_display_handler, Aborter aborter)
    //**********************************************************
    {
        String tag = " slower";
        if ( faster) tag = " faster";
        MenuItem repair = new MenuItem(My_I18n.get_I18n_string("Repair_animated_gif", image_window.stage,image_window.logger)+ tag);
        repair.setOnAction(e -> repair(faster, image_window, image_display_handler, aborter));
        return repair;
    }



    //**********************************************************
    private static void repair(boolean faster, Image_window image_window, Image_display_handler image_display_handler, Aborter aborter)
    //**********************************************************
    {
        if (image_display_handler.get_image_context().isEmpty()) return;

        String uuid = UUID.randomUUID().toString();

        double current_delay = image_display_handler.get_image_context().get().get_animated_gif_delay(image_window.stage);
        double fac = 1.2;
        if ( faster) fac = 0.8;
        double new_delay = current_delay*fac;
        if ( !faster)
        {
            // fix strange bug that GraphicsMagic does apply delay change like 1 ==> 1.2
            if ( (int)new_delay == (int)current_delay) new_delay = (int)current_delay+1.1;
        }
        image_display_handler.logger.log("GIF repair, faster="+faster+" old_delay="+current_delay+" new_delay="+new_delay);

        image_display_handler.logger.log("GIF repair1");
        Path tmp_dir = Gif_repair.extract_all_frames_in_animated_gif(image_display_handler.get_image_context().get(), uuid, image_window.stage, aborter, image_window.logger);
        if (tmp_dir == null) {
            image_display_handler.logger.log("GIF repair1 failed!");
            return;
        }
        image_display_handler.logger.log("GIF repair1 OK");

        Path p = image_display_handler.get_image_context().get().path;

        // for debug:
        //Path final_dest = Path.of(p.getParent().toAbsolutePath().toString(),"mod_"+p.getFileName().toString());
        Path final_dest = p;

        Path local_path = Gif_repair.reassemble_all_frames(
                new_delay,
                image_window.stage, image_display_handler.get_image_context().get(), tmp_dir, final_dest, uuid,  image_window.logger);
        if (local_path == null) {
            image_display_handler.logger.log("GIF repair2 failed!");
            return;
        }

        image_display_handler.logger.log("GIF repair2 OK");
        Optional<Image_context> option = Image_context.get_Image_context(local_path,  image_window.stage, image_window.aborter, image_window.logger);
        if (option.isEmpty()) {
            image_display_handler.logger.log("getting a new image context failed after gif repair");
        } else {
            image_display_handler.set_image_context( option.get());
            image_window.set_image(image_display_handler.get_image_context().get());
        }
    }


    //**********************************************************
    private static MenuItem make_menu_item(Image_window image_window,String text, EventHandler<ActionEvent> ev)
    //**********************************************************
    {
        MenuItem pix_for_pix = new MenuItem(My_I18n.get_I18n_string(text, image_window.stage,image_window.logger));
        pix_for_pix.setOnAction(ev);
        Button explanation = Preferences_stage.make_explanation_button(text,image_window.stage, image_window.logger);
        pix_for_pix.setGraphic(explanation);
        return pix_for_pix;
    }


    //**********************************************************
    private static MenuItem get_search_by_user_given_keywords_menu_item(
            Image_window image_window)
    //**********************************************************
    {
        MenuItem search_y = new MenuItem(My_I18n.get_I18n_string("Choose_keywords", image_window.stage,image_window.logger));

        search_y.setOnAction(event -> {

            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().search_using_keywords_given_by_the_user(
                    image_window.stage,
                    image_window.path_list_provider,
                    image_window.path_comparator_source,
                    false,
                    
                    image_window.aborter);
        });
        return search_y;
    }

    //**********************************************************
    private static MenuItem get_search_by_autoextracted_keyword_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem search_k = new MenuItem(My_I18n.get_I18n_string("Search_by_keywords_from_this_ones_name", image_window.stage,image_window.logger));
        search_k.setOnAction(event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().search_using_keywords_from_the_name(
                    image_window.path_list_provider,
                    image_window.path_comparator_source,
                    
                    image_window.aborter);
        });
        return search_k;
    }

    //**********************************************************
    private static MenuItem get_copy_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem copy = new MenuItem(My_I18n.get_I18n_string("Copy", image_window.stage,image_window.logger));
        copy.setOnAction(event -> {

            Runnable r = image_window.image_display_handler.image_indexer.get()::signal_file_copied;
            image_window.image_display_handler.get_image_context().get().copy(image_window.path_list_provider, image_window.path_comparator_source, r,image_window.stage);
        });
        return copy;
    }
    //**********************************************************
    private static MenuItem get_print_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem print = new MenuItem(My_I18n.get_I18n_string("Print", image_window.stage,image_window.logger));
        print.setOnAction(event -> {
            image_window.logger.log("Printing");

            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;

            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null) {
                boolean success = job.showPrintDialog(image_window.stage);
                if (success) {
                    System.out.println(job.jobStatusProperty().asString());
                    boolean printed = job.printPage(image_window.image_display_handler.get_image_context().get().the_image_view);
                    if (printed) {
                        job.endJob();
                        image_window.logger.log("Printing done");
                    } else {
                        image_window.logger.log("Printing failed.");
                    }
                } else {
                    image_window.logger.log("Could not create a printer job.");
                }
            }
        });
        return print;
    }



    //**********************************************************
    private static MenuItem get_rename_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem rename = new MenuItem(My_I18n.get_I18n_string("Rename_with_shortcut", image_window.stage,image_window.logger));
        rename.setOnAction(event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().rename_file_for_an_image_window(image_window);
        });
        return rename;
    }




    //**********************************************************
    private static MenuItem get_delete_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem rename = new MenuItem(My_I18n.get_I18n_string("Delete_with_shortcut", image_window.stage,image_window.logger));
        rename.setOnAction(event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().rename_file_for_an_image_window(image_window);
        });
        return rename;
    }


    //**********************************************************
    private static MenuItem get_browse_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem browse = new MenuItem(My_I18n.get_I18n_string("Browse", image_window.stage,image_window.logger));
        browse.setOnAction(event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.logger.log("browse this!");
             New_window_context.additional_no_past(
                      image_window.image_display_handler.get_image_context().get().path.getParent(),
                     image_window.stage,
                     image_window.logger);
        });
        return browse;
    }

    //**********************************************************
    private static MenuItem get_open_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem open = new MenuItem(My_I18n.get_I18n_string("Open", image_window.stage,image_window.logger));
        open.setOnAction(event ->
        {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().open();
        });
        return open;
    }


    /*
    Face recognition
     */
    //**********************************************************
    public static MenuItem get_perform_face_recognition_service_no_face_detection_menu_item(
            Image_window image_window)
    //**********************************************************
    {
        MenuItem mi = new MenuItem("Perform_face_recognition_service_DIRECTLY (debug)");//My_I18n.get_I18n_string("Open", image_window.logger));
        mi.setOnAction(event ->
        {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            Face_recognition_service recognition_services = Face_recognition_service.get_instance(image_window.stage, image_window.logger);
            if ( recognition_services == null) return;

            AtomicInteger count_for_label = new AtomicInteger(0);// not used
            boolean do_face_detection = false;
            Face_recognition_message msg = new Face_recognition_message(
                    image_window.image_display_handler.get_image_context().get().path.toFile(),
                    Face_detection_type.haars_high_precision,// ignored
                    do_face_detection,
                    null, // this recognition ONLY i.e. no training will happen
                    true,
                    image_window.aborter, null);

            Face_recognition_actor actor = new Face_recognition_actor(recognition_services);
            Actor_engine.run(actor,msg,null,image_window.logger);        });
        return mi;
    }
    //**********************************************************
    static void face_rec(Face_detection_type face_detection_type, Image_window image_window)
    //**********************************************************
    {
        if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
        Face_recognition_service recognition_services = Face_recognition_service.get_instance(image_window.stage, image_window.logger);
        if (recognition_services == null) return;

        Face_recognition_actor actor = new Face_recognition_actor(recognition_services);
        boolean do_face_detection = true;
        Face_recognition_message msg = new Face_recognition_message(
                image_window.image_display_handler.get_image_context().get().path.toFile(),
                face_detection_type,
                do_face_detection,
                null,
                true,
                image_window.aborter, null);
        Actor_engine.run(actor,msg,null, image_window.logger);
    }

/*
    //**********************************************************
    private static CheckMenuItem get_quality_check_menu_item(Image_window image_window)
    //**********************************************************
    {
        CheckMenuItem quality = new CheckMenuItem(My_I18n.get_I18n_string("Image_quality_high", image_window.logger));

        quality.setSelected(image_window.image_display_handler.alternate_rescaler);
        quality.setOnAction(actionEvent -> {

            boolean new_high_quality = ((CheckMenuItem) actionEvent.getSource()).isSelected();
            if (new_high_quality != image_window.image_display_handler.alternate_rescaler)
            {
                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                Optional<Image_context> option = build_Image_context(new_high_quality, image_window.image_display_handler.get_image_context().get().path,  image_window.aborter, image_window.logger);
                option.ifPresent(image_window::set_image);
            }

        });
        return quality;
    }
    */


    //**********************************************************
    private static MenuItem make_edit_menu_item(Image_window image_window)
    //**********************************************************
    {
        MenuItem edit = new MenuItem(My_I18n.get_I18n_string("Edit", image_window.stage,image_window.logger));
        edit.setOnAction(event -> {

            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().edit(image_window.stage);
        });
        return edit;
    }

    //**********************************************************
    private static MenuItem make_edit2_menu_item(Image_window image_window, Logger logger)
    //**********************************************************
    {
        MenuItem edit = new MenuItem(My_I18n.get_I18n_string("Open_With_Registered_Application", image_window.stage,image_window.logger));
        edit.setOnAction(event -> {

            if ( image_window.image_display_handler.get_image_context().isEmpty())
            {
                logger.log(Stack_trace_getter.get_stack_trace("FATAL no context"));
                return;
            }
            image_window.image_display_handler.get_image_context().get().edit2(image_window.stage, image_window.stage, image_window.aborter);
        });
        return edit;
    }


    //**********************************************************
    public static MenuItem make_info_menu_item(Image_window image_window)
    //**********************************************************
    {
        String txt = My_I18n.get_I18n_string("Info_about", image_window.stage,image_window.logger);
        if ( image_window.image_display_handler.get_image_context().isEmpty()) new MenuItem(txt+" bug?");

        MenuItem info = new MenuItem(txt
                + image_window.image_display_handler.get_image_context().get().path.getFileName() + My_I18n.get_I18n_string("Info_about_file_shortcut", image_window.stage,image_window.logger));
        info.setOnAction(event ->
                {

                    if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                    Image_context image_context = image_window.image_display_handler.get_image_context().get();
                    Exif_stage.show_exif_stage(image_context.image, image_context.path, image_window.stage,image_window.aborter, image_context.logger);
                });
        return info;
    }



    //**********************************************************
    private static List<MenuItem> manage_slide_show(Image_window image_window)
    //**********************************************************
    {
        List<MenuItem> returned = new ArrayList<>();

        boolean slide_show_is_running = image_window.is_slide_show_running();

        MenuItem slide_show = new MenuItem(My_I18n.get_I18n_string("Start_slide_show", image_window.stage,image_window.logger)+" (s)");
        returned.add(slide_show);
        //MenuItem slide_show_stop = new MenuItem(My_I18n.get_I18n_string("Stop_slide_show", image_window.logger)+" (s)");
        //returned.add(slide_show_stop);
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
            slide_show.setText(My_I18n.get_I18n_string("Stop_slide_show", image_window.stage,image_window.logger));
            faster.setDisable(false);
            slower.setDisable(false);
        }
        else
        {
            slide_show.setText(My_I18n.get_I18n_string("Start_slide_show", image_window.stage,image_window.logger));
            faster.setDisable(true);
            slower.setDisable(true);
        }

        slide_show.setOnAction(event -> {
            System.out.println("slide_show OnAction");
            if( slide_show_is_running)
            {
                image_window.stop_slide_show();
                faster.setDisable(true);
                slower.setDisable(true);
            }
            else
            {
                image_window.start_slide_show();
                faster.setDisable(false);
                slower.setDisable(false);
            }
        });


        return returned;
    }


    //**********************************************************
    private static MenuItem manage_full_screen(Image_window image_window)
    //**********************************************************
    {
        fullscreen = new MenuItem(My_I18n.get_I18n_string("Go_full_screen", image_window.stage,image_window.logger));
        if (image_window.is_full_screen )
        {
            fullscreen.setText(My_I18n.get_I18n_string("Stop_full_screen",image_window.stage,image_window.logger));
        }

        fullscreen.setOnAction(event -> {
            toggle_fullscreen(image_window);
        });

        return fullscreen;
    }


    //**********************************************************
    static void toggle_fullscreen(Image_window image_window)
    //**********************************************************
    {
        if (image_window.is_full_screen )
        {
            image_window.stage.setFullScreen(false);
            image_window.is_full_screen = false;
            fullscreen.setText(My_I18n.get_I18n_string("Go_full_screen", image_window.stage,image_window.logger));
        }
        else
        {
            image_window.stage.setFullScreen(true);
            image_window.is_full_screen = true;
            fullscreen.setText(My_I18n.get_I18n_string("Stop_full_screen", image_window.stage,image_window.logger));
        }
    }

    //**********************************************************
    public static ContextMenu make_context_menu(Image_window image_window,
                                                Image_properties_RAM_cache image_properties_cache,
                                                Supplier<Image_feature_vector_cache> fv_cache_supplier,
                                                Logger logger)
    //**********************************************************
    {
        final ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu,image_window.stage,logger);
        context_menu.getItems().add(manage_full_screen(image_window));

        List<MenuItem> l = manage_slide_show(image_window);
        for (MenuItem mi : l) context_menu.getItems().add(mi);

        context_menu.getItems().add(make_info_menu_item(image_window));
        context_menu.getItems().add(make_edit_menu_item(image_window));
        context_menu.getItems().add(make_edit2_menu_item(image_window,logger));


        /*if (Booleans.get_boolean(Feature.Enable_different_image_scaling.name()))
        {
            context_menu.getItems().add(get_quality_check_menu_item(image_window));
        }*/

        if ( Booleans.get_boolean(Feature.Enable_image_similarity.name(), image_window.stage))
        {
            context_menu.getItems().add(Item_file_with_icon.create_show_similar_menu_item(
                    image_window.image_display_handler.get_image_context().get().path,
                    image_properties_cache,
                    fv_cache_supplier,
                    image_window.path_comparator_source,
                    
                    image_window.stage,
                    image_window.aborter,
                    image_window.logger));
        }

        if ( Booleans.get_boolean(Feature.Enable_face_recognition.name(), image_window.stage))
        {
            String s = My_I18n.get_I18n_string("Face_recognition_service", image_window.stage,logger);
            Menu fr_context_menu = new Menu(s);
            context_menu.getItems().add(fr_context_menu);
            fr_context_menu.getItems().add(make_menu_item(
                    image_window,
                    "Perform_face_recognition_service_with_high_precision_face_detector",
                    event -> face_rec(Face_detection_type.MTCNN, image_window)));
            fr_context_menu.getItems().add(make_menu_item(
                    image_window,
                    "Perform_face_recognition_service_with_optimistic_face_detector",
                    event -> face_rec(Face_detection_type.haars_false_positioves, image_window)));
            fr_context_menu.getItems().add(make_menu_item(
                    image_window,
                    "Perform_face_recognition_service_with_ALT1_face_detector",
                    event -> face_rec(Face_detection_type.haars_alt1, image_window)));
            fr_context_menu.getItems().add(make_menu_item(
                    image_window,
                    "Perform_face_recognition_service_with_ALT2_face_detector",
                    event -> face_rec(Face_detection_type.haars_alt2, image_window)));

            fr_context_menu.getItems().add(get_perform_face_recognition_service_no_face_detection_menu_item(image_window));
        }
        context_menu.getItems().add(get_open_menu_item(image_window));
        context_menu.getItems().add(get_browse_menu_item(image_window));
        context_menu.getItems().add(get_rename_menu_item(image_window));
        context_menu.getItems().add(get_delete_menu_item(image_window));
        context_menu.getItems().add(get_copy_menu_item(image_window));
        context_menu.getItems().add(get_print_menu_item(image_window));
        context_menu.getItems().add(get_search_by_autoextracted_keyword_menu_item(image_window));
        context_menu.getItems().add(get_search_by_user_given_keywords_menu_item(image_window));

        MenuItem click_to_zoom = make_menu_item(
                image_window,
                "Click_to_zoom",
                event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.click_to_zoom));
        context_menu.getItems().add(click_to_zoom);

        context_menu.getItems().add(make_menu_item(
                image_window,
                "Drag_and_drop",
                event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.drag_and_drop)));

        context_menu.getItems().add(make_menu_item(
                image_window,
                "Pix_for_pix",
                event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.pix_for_pix)));


        if (image_window.image_display_handler.get_image_context().isPresent())
        {
            if (Guess_file_type.is_this_path_a_gif(image_window.image_display_handler.get_image_context().get().path))
            {
                context_menu.getItems().add(get_gif_repair_menu_item(true,image_window, image_window.image_display_handler, image_window.aborter));
                context_menu.getItems().add(get_gif_repair_menu_item(false,image_window, image_window.image_display_handler, image_window.aborter));
            }
        }

        context_menu.getItems().add(get_undo_menu_item(image_window));

        return context_menu;
    }




}
