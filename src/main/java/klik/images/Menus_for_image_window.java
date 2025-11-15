// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../browser/icons/animated_gifs/Gif_repair.java
package klik.images;

import javafx.print.PrinterJob;
import javafx.scene.control.*;
import klik.Window_type;
import klik.Instructions;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.animated_gifs.Gif_repair;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.browser.items.Item_file_with_icon;
import klik.change.Redo_same_move_engine;
import klik.change.undo.Undo_for_moves;
import klik.machine_learning.face_recognition.Face_detection_type;
import klik.machine_learning.face_recognition.Face_recognition_actor;
import klik.machine_learning.face_recognition.Face_recognition_message;
import klik.machine_learning.face_recognition.Face_recognition_service;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.look.my_i18n.My_I18n;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Booleans;
import klik.util.files_and_paths.Guess_file_type;
import klik.look.Look_and_feel_manager;
import klik.util.image.rescaling.Image_rescaling_filter;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Menu_items;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
        return Menu_items.make_menu_item("Undo_LAST_move_or_delete",
                    e -> {
            image_window.logger.log("undoing last move");
            double x = image_window.stage.getX()+100;
            double y = image_window.stage.getY()+100;
            Undo_for_moves.perform_last_undo_fx(image_window.stage, x, y, image_window.logger);
        }, image_window.stage, image_window.logger);
    }

    //**********************************************************
    private static MenuItem get_gif_repair_menu_item(boolean faster, Image_window image_window, Image_display_handler image_display_handler, Aborter aborter)
    //**********************************************************
    {
        String tag = " slower";
        if ( faster) tag = " faster";
        MenuItem repair = new MenuItem(My_I18n.get_I18n_string("Repair_animated_gif", image_window.stage,image_window.logger)+ tag);
        Look_and_feel_manager.set_menu_item_look(repair,image_window.stage,image_window.logger);

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
        Optional<Image_context> option = Image_context.build_Image_context(local_path, image_window, image_window.aborter, image_window.logger);
        if (option.isEmpty()) {
            image_display_handler.logger.log("getting a new image context failed after gif repair");
        } else {
            image_display_handler.set_image_context( option.get());
            image_window.redisplay(true);
        }
    }





    //**********************************************************
    private static MenuItem get_search_by_user_given_keywords_menu_item(
            Image_window image_window)
    //**********************************************************
    {
        MenuItem search_y = new MenuItem(My_I18n.get_I18n_string("Choose_keywords", image_window.stage,image_window.logger));
        Look_and_feel_manager.set_menu_item_look(search_y,image_window.stage,image_window.logger);

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
        return Menu_items.make_menu_item("Search_by_keywords_from_this_ones_name",
                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().search_using_keywords_from_the_name(
                    image_window.path_list_provider,
                    image_window.path_comparator_source,
                    
                    image_window.aborter);
        }, image_window.stage, image_window.logger);
    }

    //**********************************************************
    private static MenuItem get_copy_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item("Copy",
                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            Runnable r = image_window.image_display_handler.image_indexer::signal_file_copied;
            image_window.image_display_handler.get_image_context().get().copy(image_window.path_list_provider, image_window.path_comparator_source, r,image_window.stage);
        }, image_window.stage, image_window.logger);
    }
    //**********************************************************
    private static MenuItem get_print_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item("Print", event -> print(image_window), image_window.stage, image_window.logger);
    }

    private static void print(Image_window image_window) {
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
    }


    //**********************************************************
    private static MenuItem get_rename_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item("Rename_with_shortcut",
                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().rename_file_for_an_image_window(image_window);
        }, image_window.stage, image_window.logger);
    }




    //**********************************************************
    private static MenuItem get_delete_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item("Delete_with_shortcut",
                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().rename_file_for_an_image_window(image_window);
        }, image_window.stage, image_window.logger);
    }

    //**********************************************************
    private static MenuItem get_browse_menu_item(Image_window image_window)
    //**********************************************************
    {
        return get_browse_menu_item_ejective("Browse_folder",image_window, Window_type.File_system_2D);
    }

    //**********************************************************
    private static MenuItem get_browse_3D_menu_item(Image_window image_window)
    //**********************************************************
    {
        return get_browse_menu_item_ejective("Browse_folder_3D", image_window, Window_type.File_system_3D);
    }
    //**********************************************************
    private static MenuItem get_browse_menu_item_ejective(String button_text_key,Image_window image_window, Window_type context_type)
    //**********************************************************
    {
        return Menu_items.make_menu_item(button_text_key,
                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
             Instructions.additional_no_past(
                     context_type,
                      new Path_list_provider_for_file_system(image_window.image_display_handler.get_image_context().get().path.getParent(), image_window.stage,image_window.logger),
                     image_window.stage,
                     image_window.logger);
        }, image_window.stage, image_window.logger);
    }



    //**********************************************************
    private static MenuItem get_open_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item("Open",
                event ->
        {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().open(image_window);
        }, image_window.stage, image_window.logger);
    }


    /*
    Face recognition
     */
    //**********************************************************
    public static MenuItem get_perform_face_recognition_service_no_face_detection_menu_item(
            Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item(
                "Perform_face_recognition_service_DIRECTLY",
                event -> perform_face_reco_directly(image_window),
                image_window.stage, image_window.logger);
    }

    //**********************************************************
    private static void perform_face_reco_directly(Image_window image_window)
    //**********************************************************
    {
        if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
        Face_recognition_service recognition_services = Face_recognition_service.get_instance(image_window.stage, image_window.logger);
        if ( recognition_services == null) return;

        //AtomicInteger count_for_label = new AtomicInteger(0);// not used
        boolean do_face_detection = false;
        Path image_path = image_window.image_display_handler.get_image_context().get().path;

        if ( Mouse_handling_for_Image_window.cropped_image_path != null) image_path = Mouse_handling_for_Image_window.cropped_image_path;
        Face_recognition_message msg = new Face_recognition_message(
                image_path.toFile(),
                Face_detection_type.haars_high_precision,// ignored
                do_face_detection,
                null, // this recognition ONLY i.e. no training will happen
                true,
                image_window.aborter, null);

        Face_recognition_actor actor = new Face_recognition_actor(recognition_services);
        Actor_engine.run(actor,msg,null, image_window.logger);
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






    //**********************************************************
    private static MenuItem make_edit_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item("Edit",
                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().edit(image_window.stage);
        }, image_window.stage, image_window.logger);
    }

    //**********************************************************
    private static MenuItem make_edit2_menu_item(Image_window image_window, Logger logger)
    //**********************************************************
    {
        return Menu_items.make_menu_item("Open_With_Registered_Application",
                event -> {

            if ( image_window.image_display_handler.get_image_context().isEmpty())
            {
                logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL no context"));
                return;
            }
            image_window.image_display_handler.get_image_context().get().edit2(image_window.stage, image_window.stage, image_window.aborter);
        }, image_window.stage, image_window.logger);
    }


    //**********************************************************
    public static MenuItem make_info_menu_item(Image_window image_window)
    //**********************************************************
    {
        String txt = My_I18n.get_I18n_string("Info_about", image_window.stage,image_window.logger);

        MenuItem info = new MenuItem(txt
                + image_window.image_display_handler.get_image_context().get().path.getFileName() + My_I18n.get_I18n_string("Info_about_file_shortcut", image_window.stage,image_window.logger));
        Look_and_feel_manager.set_menu_item_look(info,image_window.stage,image_window.logger);
        info.setOnAction(event ->
                {

                    if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                    Image_context image_context = image_window.image_display_handler.get_image_context().get();
                    Exif_stage.show_exif_stage(image_context.image, image_context.path, image_window.stage,image_window.aborter, image_context.logger);
                });
        Look_and_feel_manager.set_menu_item_look(info,image_window.stage,image_window.logger);
        return info;
    }



    //**********************************************************
    private static List<MenuItem> manage_slide_show(Image_window image_window)
    //**********************************************************
    {
        List<MenuItem> returned = new ArrayList<>();

        boolean slide_show_is_running = image_window.is_slide_show_running();

        MenuItem slide_show = new MenuItem(My_I18n.get_I18n_string("Start_slide_show", image_window.stage,image_window.logger)+" (s)");
        Look_and_feel_manager.set_menu_item_look(slide_show,image_window.stage,image_window.logger);
        returned.add(slide_show);

        MenuItem faster = new MenuItem("Slide show: faster (x)");
        Look_and_feel_manager.set_menu_item_look(faster,image_window.stage,image_window.logger);

        returned.add(faster);
        faster.setOnAction(actionEvent -> {
            if (slide_show_is_running) image_window.hurry_up();
        });
        MenuItem slower = new MenuItem("Slide show: slower (w)");
        Look_and_feel_manager.set_menu_item_look(slower,image_window.stage,image_window.logger);

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
        Look_and_feel_manager.set_menu_item_look(fullscreen,image_window.stage,image_window.logger);
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
                                                Supplier<Feature_vector_cache> fv_cache_supplier,
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


        if (Booleans.get_boolean(Feature.Enable_alternate_image_scaling.name(),image_window.stage))
        {
            Path p = image_window.image_display_handler.get_image_context().get().path;
            if(!Guess_file_type.is_this_path_a_gif(p, image_window.stage, logger))
            {
                // javafx Image for GIF does not support pixelReader
                context_menu.getItems().add(get_rescaler_menu(image_window));
            }
        }

        if ( Booleans.get_boolean(Feature.Enable_image_similarity.name(), image_window.stage))
        {
            context_menu.getItems().add(Item_file_with_icon.create_show_similar_menu_item(
                    image_window.image_display_handler.get_image_context().get().path,
                    //image_properties_cache,
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
            Look_and_feel_manager.set_menu_item_look(fr_context_menu, image_window.stage, logger);

            context_menu.getItems().add(fr_context_menu);
            fr_context_menu.getItems().add(get_perform_face_recognition_service_no_face_detection_menu_item(image_window));

            fr_context_menu.getItems().add(Menu_items.make_menu_item_with_explanation(
                    "Perform_face_recognition_service_with_high_precision_face_detector",
                    event -> face_rec(Face_detection_type.MTCNN, image_window),image_window.stage,logger));
            fr_context_menu.getItems().add(Menu_items.make_menu_item_with_explanation(
                    "Perform_face_recognition_service_with_optimistic_face_detector",
                    event -> face_rec(Face_detection_type.haars_false_positioves, image_window),image_window.stage,logger));
            fr_context_menu.getItems().add(Menu_items.make_menu_item_with_explanation(
                    "Perform_face_recognition_service_with_ALT1_face_detector",
                    event -> face_rec(Face_detection_type.haars_alt1, image_window),image_window.stage,logger));
            fr_context_menu.getItems().add(Menu_items.make_menu_item_with_explanation(
                    "Perform_face_recognition_service_with_ALT2_face_detector",
                    event -> face_rec(Face_detection_type.haars_alt2, image_window),image_window.stage,logger));

        }
        context_menu.getItems().add(get_open_menu_item(image_window));
        context_menu.getItems().add(get_browse_menu_item(image_window));
        context_menu.getItems().add(get_browse_3D_menu_item(image_window));
        context_menu.getItems().add(get_rename_menu_item(image_window));
        context_menu.getItems().add(get_delete_menu_item(image_window));
        context_menu.getItems().add(get_copy_menu_item(image_window));
        context_menu.getItems().add(get_print_menu_item(image_window));
        context_menu.getItems().add(get_search_by_autoextracted_keyword_menu_item(image_window));
        context_menu.getItems().add(get_search_by_user_given_keywords_menu_item(image_window));

        context_menu.getItems().add(Menu_items.make_menu_item_with_explanation(
                "Click_to_zoom",
                event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.click_to_zoom),image_window.stage,logger));

        context_menu.getItems().add(Menu_items.make_menu_item_with_explanation(
                "Drag_and_drop",
                event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.drag_and_drop),image_window.stage,logger));

        context_menu.getItems().add(Menu_items.make_menu_item_with_explanation(
                "Pix_for_pix",
                event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.pix_for_pix), image_window.stage,logger));


        if (image_window.image_display_handler.get_image_context().isPresent())
        {
            if (Guess_file_type.is_this_path_a_gif(image_window.image_display_handler.get_image_context().get().path, image_window.stage, logger))
            {
                context_menu.getItems().add(get_gif_repair_menu_item(true,image_window, image_window.image_display_handler, image_window.aborter));
                context_menu.getItems().add(get_gif_repair_menu_item(false,image_window, image_window.image_display_handler, image_window.aborter));
            }
        }

        context_menu.getItems().add(get_undo_menu_item(image_window));

        return context_menu;
    }

    //**********************************************************
    private static Menu get_rescaler_menu(Image_window imageWindow)
    //**********************************************************
    {
        Menu returned =  new Menu("Image Rescaler");
        ToggleGroup group = new ToggleGroup();
        for ( Image_rescaling_filter filter : Image_rescaling_filter.values())
        {
            RadioMenuItem rmi = get_rescaler_radio_menu_item(imageWindow,filter);
            rmi.setToggleGroup(group);
            returned.getItems().add(rmi);
        }
        return returned;
    }
    //**********************************************************
    private static RadioMenuItem get_rescaler_radio_menu_item(Image_window image_window, Image_rescaling_filter filter)
    //**********************************************************
    {
        RadioMenuItem item = new RadioMenuItem(filter.name());
        Look_and_feel_manager.set_menu_item_look(item, image_window.stage, image_window.logger);
        if ( image_window.rescaler == filter) item.setSelected(true);
        item.setOnAction(actionEvent -> {
            image_window.rescaler = filter;
            image_window.redisplay(true);
        });
        return item;
    }

}
