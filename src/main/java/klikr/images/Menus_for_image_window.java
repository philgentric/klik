// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../browser/icons/animated_gifs/Gif_repair.java
package klikr.images;

import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import klikr.Klikr_application;
import klikr.Window_type;
import klikr.Window_builder;
import klikr.machine_learning.ML_server_type;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.Check_remaining_RAM;
import klikr.util.execute.System_open_actor;
import klikr.util.execute.actor.Aborter;
import klikr.util.animated_gifs.Gif_repair;
import klikr.browser_core.items.Item_file_with_icon;
import klikr.change.Redo_same_move_engine;
import klikr.change.undo.Undo_for_moves;
import klikr.machine_learning.face_recognition.Face_recognition_message;
import klikr.machine_learning.face_recognition.Face_recognition_service;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.look.my_i18n.My_I18n;
import klikr.settings.boolean_features.Feature;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.look.Look_and_feel_manager;
import klikr.util.files_and_paths.Moving_files;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.change.old_and_new.Command;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.old_and_new.Status;
import klikr.util.image.rescaling.Image_rescaling_filter;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Items_with_explanation;
import klikr.util.ui.Menu_items;

import java.nio.file.Path;
import java.nio.file.Paths;
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
        return Menu_items.make_menu_item_i18n("Undo_LAST_move_or_delete",
                image_window.undo.getDisplayText(),
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
                    image_window.path_list_provider,
                    image_window.path_comparator_source,
                    false,
                    image_window.aborter,
                    image_window.stage);
        });
        return search_y;
    }

    //**********************************************************
    private static MenuItem get_search_by_autoextracted_keyword_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item_i18n("Search_by_keywords_from_this_ones_name",
                image_window.find.getDisplayText(),
                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().search_using_keywords_from_the_name(
                    image_window.path_list_provider,
                    image_window.path_comparator_source,
                    image_window.aborter,
                    image_window.stage);
        }, image_window.stage, image_window.logger);
    }

    //**********************************************************
    private static MenuItem get_copy_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item_i18n("Copy",
                image_window.copy.getDisplayText(),
                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            Runnable r = () -> image_window.image_display_handler.image_indexer.signal_file_copied(image_window.aborter);
            image_window.image_display_handler.get_image_context().get().copy(image_window.path_list_provider, image_window.path_comparator_source, r,image_window.stage);
        }, image_window.stage, image_window.logger);
    }
    //**********************************************************
    private static MenuItem get_print_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item_i18n("Print", null, event -> print(image_window), image_window.stage, image_window.logger);
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
        return Menu_items.make_menu_item_i18n("Rename",
                image_window.rename.getDisplayText(),

                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().rename_file_for_an_image_window(image_window);
        }, image_window.stage, image_window.logger);
    }




    //**********************************************************
    private static MenuItem get_delete_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item_i18n("Delete",
                image_window.delete.getDisplayText(),

                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;

            Image_context ic = image_window.image_display_handler.get_image_context().get();
            Path to_be_deleted = ic.path;

            Path trash_dir = Static_files_and_paths_utilities.get_trash_dir_of(to_be_deleted, image_window.owner, image_window.logger);
            Path new_Path = (Paths.get(trash_dir.toString(), to_be_deleted.getFileName().toString()));
            List<Old_and_new_Path> l = new ArrayList<>();

            l.add(new Old_and_new_Path(to_be_deleted, new_Path, Command.command_move_to_trash, Status.before_command,false));
            double x = image_window.stage.getX()+100;
            double y = image_window.stage.getY()+100;
            Moving_files.safe_delete_files(l, x,y,image_window.stage,image_window.aborter,image_window.logger);
            image_window.image_display_handler.change_image_relative(1,false);
            }, image_window.stage, image_window.logger);
    }

    //**********************************************************
    private static MenuItem make_browse_2D_menu_item(Image_window image_window)
    //**********************************************************
    {
        return get_browse_menu_item_effective("Browse_folder",image_window, Window_type.File_system_2D);
    }

    //**********************************************************
    private static MenuItem make_browse_3D_menu_item(Image_window image_window)
    //**********************************************************
    {
        return get_browse_menu_item_effective("Browse_folder_3D", image_window, Window_type.File_system_3D);
    }
    //**********************************************************
    private static MenuItem get_browse_menu_item_effective(String button_text_key,Image_window image_window, Window_type window_type)
    //**********************************************************
    {
        String addendum;
        return Menu_items.make_menu_item_i18n(button_text_key,
                image_window.browse.getDisplayText(),
                event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
             Window_builder.additional_no_past(
                     Klikr_application.application,
                     window_type,
                      new Path_list_provider_for_file_system(image_window.image_display_handler.get_image_context().get().path.getParent(), image_window.stage,image_window.logger),
                     image_window.stage,
                     image_window.logger);
        }, image_window.stage, image_window.logger);
    }



    //**********************************************************
    private static MenuItem make_open_with_system_file_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item_i18n("Open_File",null,
                event ->
        {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            Path path = image_window.image_display_handler.get_image_context().get().path;
            System_open_actor.open_with_system(
                    Klikr_application.application,
                    path, image_window.stage,image_window.aborter, image_window.logger);
        }, image_window.stage, image_window.logger);
    }


    /*
    Face recognition
     */

    //**********************************************************
    public static MenuItem get_perform_face_recognition_service_multi_face_detection_menu_item(
            Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item_i18n(
                "Perform_face_recognition",null,
                event -> perform_face_reco(image_window),
                image_window.stage, image_window.logger);
    }

    //**********************************************************
    public static MenuItem get_perform_face_recognition_service_no_face_detection_menu_item(
            Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item_i18n(
                "Perform_face_recognition_service_DIRECTLY",null,
                event -> perform_face_reco_directly(image_window),
                image_window.stage, image_window.logger);
    }



    //**********************************************************
    private static void perform_face_reco(Image_window image_window)
    //**********************************************************
    {
        if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
        Face_recognition_service recognition_services = Face_recognition_service.get_instance(Klikr_application.application,image_window.stage, image_window.logger);
        if ( recognition_services == null) return;

        //AtomicInteger count_for_label = new AtomicInteger(0);// not used
        Path image_path = image_window.image_display_handler.get_image_context().get().path;

        if ( Mouse_handling_for_Image_window.cropped_image_path != null) image_path = Mouse_handling_for_Image_window.cropped_image_path;
        ML_server_type[] types = {ML_server_type.Haar_alt1,ML_server_type.Haar_alt2,ML_server_type.Haar_default,ML_server_type.Haar_tree,ML_server_type.MTCNN};
        for ( ML_server_type type : types )
        {
            Face_recognition_message msg = new Face_recognition_message(
                    image_path.toFile(),
                    type,
                    null, // this is recognition ONLY i.e. no training will happen
                    true,
                    image_window.aborter, null);

            Face_recognition_service.recognize(msg);
        }
    }
    //**********************************************************
    private static void perform_face_reco_directly(Image_window image_window)
    //**********************************************************
    {
        if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
        Face_recognition_service recognition_services = Face_recognition_service.get_instance(Klikr_application.application,image_window.stage, image_window.logger);
        if ( recognition_services == null) return;

        //AtomicInteger count_for_label = new AtomicInteger(0);// not used
        Path image_path = image_window.image_display_handler.get_image_context().get().path;

        if ( Mouse_handling_for_Image_window.cropped_image_path != null) image_path = Mouse_handling_for_Image_window.cropped_image_path;
        Face_recognition_message msg = new Face_recognition_message(
                image_path.toFile(),
                null,
                null, // this is recognition ONLY i.e. no training will happen
                true,
                image_window.aborter, null);

        Face_recognition_service.recognize(msg);
    }

    //**********************************************************
    static void face_rec(ML_server_type face_detection_type, Image_window image_window)
    //**********************************************************
    {
        if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
        Face_recognition_service recognition_services = Face_recognition_service.get_instance(Klikr_application.application,image_window.stage, image_window.logger);
        if (recognition_services == null) return;

        Face_recognition_message msg = new Face_recognition_message(
                image_window.image_display_handler.get_image_context().get().path.toFile(),
                face_detection_type,
                null,
                true,
                image_window.aborter, null);
        recognition_services.run_face_recognition_actor(msg);
    }






    //**********************************************************
    private static MenuItem make_edit_menu_item(Image_window image_window)
    //**********************************************************
    {
        return Menu_items.make_menu_item_i18n("Edit_File",
                image_window.open.getDisplayText(),
        event -> {
            if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
            image_window.image_display_handler.get_image_context().get().open(image_window.stage, image_window.aborter);
        }, image_window.stage, image_window.logger);
    }

    //**********************************************************
    private static MenuItem make_open_with_registered_application_menu_item(Image_window image_window, Logger logger)
    //**********************************************************
    {
        return Menu_items.make_menu_item_i18n("Open_With_Registered_Application",
                null,
                event -> {

            if ( image_window.image_display_handler.get_image_context().isEmpty())
            {
                logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL no context"));
                return;
            }
            image_window.image_display_handler.get_image_context().get().open_with_registered_application(image_window.stage, image_window.stage, image_window.aborter);
        }, image_window.stage, image_window.logger);
    }


    //**********************************************************
    public static MenuItem make_info_menu_item(Image_window image_window)
    //**********************************************************
    {
        String txt = My_I18n.get_I18n_string("Info_about", image_window.stage,image_window.logger);

        MenuItem info = new MenuItem(txt
                + image_window.image_display_handler.get_image_context().get().path.getFileName());
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
    private static String get_start_text(Image_window image_window, KeyCombination onoff)
    //**********************************************************
    {
        return My_I18n.get_I18n_string("Start_slide_show", image_window.stage, image_window.logger)
                + " (" + onoff.getDisplayText() + ")";
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
                                                Supplier<Feature_vector_cache> fv_cache_supplier,
                                                Logger logger)
    //**********************************************************
    {
        final ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu,image_window.stage,logger);
        context_menu.getItems().add(manage_full_screen(image_window));

        context_menu.getItems().add(get_slideshow_menu(image_window));
        context_menu.getItems().add(get_mouse_mode_menu(image_window));

        context_menu.getItems().add(make_info_menu_item(image_window));



        if ( Feature_cache.get(Feature.Enable_image_similarity))
        {
            if (!Check_remaining_RAM.low_memory.get())
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
        }

        if ( Feature_cache.get(Feature.Enable_face_recognition))
        {
            context_menu.getItems().add(get_face_recognition_context_menu(image_window));
        }



        context_menu.getItems().add(get_open_menu_item(image_window));




        context_menu.getItems().add(get_rename_menu_item(image_window));
        context_menu.getItems().add(get_delete_menu_item(image_window));
        context_menu.getItems().add(get_undo_menu_item(image_window));
        context_menu.getItems().add(get_copy_menu_item(image_window));
        context_menu.getItems().add(get_print_menu_item(image_window));
        context_menu.getItems().add(get_search_by_autoextracted_keyword_menu_item(image_window));
        context_menu.getItems().add(get_search_by_user_given_keywords_menu_item(image_window));

        if (image_window.image_display_handler.get_image_context().isPresent())
        {
            if (Guess_file_type.is_this_path_extension_a_gif(image_window.image_display_handler.get_image_context().get().path, logger))
            {
                context_menu.getItems().add(get_gif_repair_menu_item(true,image_window, image_window.image_display_handler, image_window.aborter));
                context_menu.getItems().add(get_gif_repair_menu_item(false,image_window, image_window.image_display_handler, image_window.aborter));
            }
        }

        if (Feature_cache.get(Feature.Enable_alternate_image_scaling))
        {
            Path p = image_window.image_display_handler.get_image_context().get().path;
            if(!Guess_file_type.is_this_path_extension_a_gif(p, logger))
            {
                // javafx Image for GIF does not support pixelReader
                context_menu.getItems().add(get_rescaler_menu(image_window,logger));
            }
        }
        return context_menu;
    }

    //**********************************************************
    public static Menu get_open_menu_item(Image_window image_window)
    //**********************************************************
    {
        String s = My_I18n.get_I18n_string("Open", image_window.stage, image_window.logger);
        Menu returned = new Menu(s);
        Look_and_feel_manager.set_menu_item_look(returned, image_window.stage, image_window.logger);
        returned.getItems().add(make_open_with_registered_application_menu_item(image_window,image_window.logger));
        returned.getItems().add(make_open_with_system_file_menu_item(image_window));
        returned.getItems().add(make_edit_menu_item(image_window));
        returned.getItems().add(make_browse_2D_menu_item(image_window));
        if (Feature_cache.get(Feature.Enable_3D))
        {
            returned.getItems().add(make_browse_3D_menu_item(image_window));
        }
        return returned;
    }

    //**********************************************************
    private static Menu get_face_recognition_context_menu(Image_window image_window)
    //**********************************************************
    {
        String s = My_I18n.get_I18n_string("Face_recognition_service", image_window.stage, image_window.logger);
        Menu fr_context_menu = new Menu(s);
        Look_and_feel_manager.set_menu_item_look(fr_context_menu, image_window.stage, image_window.logger);


        fr_context_menu.getItems().add(get_perform_face_recognition_service_multi_face_detection_menu_item(image_window));



        fr_context_menu.getItems().add(get_perform_face_recognition_service_no_face_detection_menu_item(image_window));


        fr_context_menu.getItems().add(Items_with_explanation.make_menu_item_with_explanation(
                "Perform_face_recognition_service_with_optimistic_face_detector",
                null,
                event -> face_rec(ML_server_type.Haar_default, image_window), image_window.stage, image_window.logger));

        fr_context_menu.getItems().add(Items_with_explanation.make_menu_item_with_explanation(
                "Perform_face_recognition_service_with_tree_face_detector",
                null,
                event -> face_rec(ML_server_type.Haar_tree, image_window), image_window.stage, image_window.logger));

        fr_context_menu.getItems().add(Items_with_explanation.make_menu_item_with_explanation(
                "Perform_face_recognition_service_with_ALT1_face_detector",
                null,
                event -> face_rec(ML_server_type.Haar_alt1, image_window), image_window.stage, image_window.logger));

        fr_context_menu.getItems().add(Items_with_explanation.make_menu_item_with_explanation(
                "Perform_face_recognition_service_with_ALT2_face_detector",
                null,
                event -> face_rec(ML_server_type.Haar_alt2, image_window), image_window.stage, image_window.logger));

        fr_context_menu.getItems().add(Items_with_explanation.make_menu_item_with_explanation(
                "Perform_face_recognition_service_with_high_precision_face_detector",
                null,
                event -> face_rec(ML_server_type.MTCNN, image_window), image_window.stage, image_window.logger));


        return fr_context_menu;
    }
    //**********************************************************
    private static Menu get_mouse_mode_menu(Image_window image_window)
    //**********************************************************
    {
        String s = My_I18n.get_I18n_string("Mouse_Mode", image_window.stage,image_window.logger);
        Menu mouse_mode_context_menu = new Menu(s);
        Look_and_feel_manager.set_menu_item_look(mouse_mode_context_menu, image_window.stage, image_window.logger);


        mouse_mode_context_menu.getItems().add(Items_with_explanation.make_menu_item_with_explanation(
                "Click_to_zoom",
                image_window.click_to_zoom,
                event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.click_to_zoom), image_window.stage, image_window.logger));

        mouse_mode_context_menu.getItems().add(Items_with_explanation.make_menu_item_with_explanation(
                "Drag_and_drop",
                image_window.drag_and_drop,
                event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.drag_and_drop), image_window.stage, image_window.logger));

        mouse_mode_context_menu.getItems().add(Items_with_explanation.make_menu_item_with_explanation(
                "Pix_for_pix",
                image_window.pix_for_pix,
                event -> image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.pix_for_pix), image_window.stage, image_window.logger));
        return mouse_mode_context_menu;
    }

    //**********************************************************
    private static Menu get_slideshow_menu(Image_window image_window)
    //**********************************************************
    {
        String s = My_I18n.get_I18n_string("Slide_Show", image_window.stage,image_window.logger);

        Menu returned = new Menu(s);
        Look_and_feel_manager.set_menu_item_look(returned,image_window.stage,image_window.logger);

        boolean slide_show_is_running = image_window.is_slide_show_running();


        MenuItem slide_show = new MenuItem(get_start_text(image_window, image_window.slideshow_start_stop));
        Look_and_feel_manager.set_menu_item_look(slide_show,image_window.stage,image_window.logger);
        returned.getItems().add(slide_show);

        MenuItem faster = new MenuItem(
                My_I18n.get_I18n_string("Speed_up_scan",image_window.stage,image_window.logger)
                        +" ("+image_window.speed_up_scan.getDisplayText()+")"
        );
        Look_and_feel_manager.set_menu_item_look(faster,image_window.stage,image_window.logger);

        returned.getItems().add(faster);
        faster.setOnAction(actionEvent -> {
            if (slide_show_is_running) image_window.speed_up();
        });
        MenuItem slower = new MenuItem(
                My_I18n.get_I18n_string("Slow_down_scan",image_window.stage,image_window.logger)
                        +" ("+image_window.slow_down_scan.getDisplayText()+")"
        );
        Look_and_feel_manager.set_menu_item_look(slower,image_window.stage,image_window.logger);

        returned.getItems().add(slower);
        slower.setOnAction(actionEvent -> {
            if (slide_show_is_running) image_window.slow_down();
        });

        if( slide_show_is_running)
        {
            slide_show.setText(
                    My_I18n.get_I18n_string("Stop_slide_show", image_window.stage,image_window.logger)
                            +" ("+image_window.slideshow_start_stop.getDisplayText()+")");

            faster.setDisable(false);
            slower.setDisable(false);
        }
        else
        {
            slide_show.setText(get_start_text(image_window, image_window.slideshow_start_stop));

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
    private static Menu get_rescaler_menu(Image_window image_window,Logger logger)
    //**********************************************************
    {
        String s = My_I18n.get_I18n_string("Image_Rescaler", image_window.stage,logger);

        Menu returned =  new Menu(s);
        ToggleGroup group = new ToggleGroup();
        for ( Image_rescaling_filter filter : Image_rescaling_filter.values())
        {
            RadioMenuItem rmi = get_rescaler_radio_menu_item(image_window,filter);
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
