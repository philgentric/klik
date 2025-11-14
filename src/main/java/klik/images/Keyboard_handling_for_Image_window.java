// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.images;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;
import klik.Window_type;
import klik.Instructions;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Booleans;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.ui.Popups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import static klik.images.Image_window.dbg;

//**********************************************************
public class Keyboard_handling_for_Image_window
//**********************************************************
{
    private static final boolean keyboard_dbg = false;
    //**********************************************************
    static void handle_keyboard(
            Image_window image_window,
            final KeyEvent key_event,
            Logger logger)
    //**********************************************************
    {
        if ( image_window.image_display_handler == null)
        {
            logger.log("Image_window.image_display_handler is null, cannot handle keyboard event");
            return;
        }

        boolean exit_on_escape_preference = Booleans.get_boolean_defaults_to_true(Feature.Use_escape_to_close_windows.name(), image_window.stage);

        Window owner = image_window.stage;
        if (keyboard_dbg) logger.log("Image_stage KeyEvent="+key_event);


        if (key_event.getCode() == KeyCode.ESCAPE)
        {
            key_event.consume();
            if (!exit_on_escape_preference)
            {
                logger.log("Image_stage : ignoring escape by user preference");
                return;
            }
            if (image_window.is_full_screen)
            {
                logger.log("Image_stage : ignoring escape as a way to close the image window, because we were in full screen");
                // normally javafx will exit fullscreen ...
                image_window.is_full_screen = false;
            }
            else
            {
                if ( dbg) logger.log("image_window : closing image window by user escape");
                image_window.stage.close();
                image_window.my_close();

                int i = image_window.stage_group.indexOf(image_window);
                if ( i >=0) image_window.stage_group.remove(image_window);
                if ( !image_window.stage_group.isEmpty())
                {
                    Image_window previous = image_window.stage_group.get(image_window.stage_group.size()-1);
                    if (previous != null)
                    {
                        previous.stage.requestFocus();
                        previous.stage.toFront();
                    }

                }
            }

            return;
        }

        if(
                (key_event.isShiftDown() )
                        &&
                        (key_event.getCode().equals(KeyCode.D)||(key_event.getCode() == KeyCode.BACK_SPACE))
        )
        {
            key_event.consume();
            if (Booleans.get_boolean(Feature.Shift_d_is_sure_delete.name(), image_window.stage))
            {
                // shift d is "sure delete"
                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                Path path = image_window.image_display_handler.get_image_context().get().path;
                try {
                    Files.delete(path);
                } catch (NoSuchFileException x) {
                    logger.log("no such file or directory:" + path);
                    return;
                } catch (IOException e) {
                    logger.log("cannot delete ? " + e);
                    return;
                }
                image_window.image_display_handler.change_image_relative(1, image_window.ultim_mode);
            }
            else {
                Popups.popup_warning("❗ Ahah ❗","Using Shift-D for sure-deleting a file requires to enable it in the preferences", false,owner,logger);
            }
            return;
        }

        switch (key_event.getText())
        {
            case"=" -> {
                if (keyboard_dbg) logger.log("= like pix-for-pix: use mouse to select visible part of large image");
                image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.pix_for_pix);
                return;
            }

            case "b","B" -> {
                if (keyboard_dbg) logger.log("b like browse");


                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;

                Instructions.additional_no_past( Window_type.File_system_2D,
                         new Path_list_provider_for_file_system(image_window.image_display_handler.get_image_context().get().path.getParent(),logger),
                        owner,logger);
                key_event.consume();
                return;
            }
            case "c","C" -> {
                if (keyboard_dbg) logger.log("c like copy");

                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                Runnable after = image_window.image_display_handler.image_indexer::signal_file_copied;
                image_window.image_display_handler.get_image_context().get().copy(
                        image_window.path_list_provider,
                        image_window.path_comparator_source,
                        after,
                        owner);
                key_event.consume();
                return;
            }
            case "d","D" -> {
                if (keyboard_dbg) logger.log("d like delete, move to trash");

                image_window.image_display_handler.delete();
                key_event.consume();
                return;
            }
            case "e","E" -> {
                if (keyboard_dbg) logger.log("e like edit");

                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                image_window.image_display_handler.get_image_context().get().edit(owner);
                key_event.consume();
                return;
            }
            case "f","F" -> {
                //if (keyword_dbg) logger.log("f like Face Recognition");
                //if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                //Menus_for_image_window.face_rec(Face_detection_type.MTCNN,image_window);

                if (keyboard_dbg) logger.log("f like Fullscreen");
                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                Menus_for_image_window.toggle_fullscreen(image_window);

                key_event.consume();
                return;
            }
            case "i","I" -> {
                if (keyboard_dbg) logger.log("i like information");

                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                Image_context image_context = image_window.image_display_handler.get_image_context().get();
                Exif_stage.show_exif_stage(image_context.image, image_context.path, image_window.stage, image_window.aborter, image_context.logger);
                key_event.consume();
                return;
            }
            case "k","K" -> {
                if (keyboard_dbg) logger.log("k like search_using_keywords_from_the_name");

                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                image_window.image_display_handler.get_image_context().get().search_using_keywords_from_the_name(
                        image_window.path_list_provider,
                        image_window.path_comparator_source,
                        image_window.aborter);
                key_event.consume();
                return;
            }
            case "m","M" -> {
                if (keyboard_dbg) logger.log("m like Move (enables drag-and-drop mode)");
                image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.drag_and_drop);
                key_event.consume();
                return;
            }
            case "o","O" -> {
                if (keyboard_dbg) logger.log("o like open ");

                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                image_window.image_display_handler.get_image_context().get().open(image_window);
                key_event.consume();
                return;
            }
            case "r","R" -> {
                if (keyboard_dbg) logger.log("r like rename");

                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                image_window.image_display_handler.get_image_context().get().rename_file_for_an_image_window(image_window);
                key_event.consume();
                return;
            }
            case "s","S" -> {
                if (keyboard_dbg) logger.log("s like slideshow");
                image_window.toggle_slideshow();
                key_event.consume();
                return;
            }
            /*
            case "t","T" -> {
                if (keyboard_dbg) logger.log("t like tag");

                if( Booleans.get_boolean(Feature.Enable_tags.name(), image_window.stage)) {

                    if (image_window.image_display_handler.get_image_context().isEmpty()) return;
                    Tag_stage.open_tag_stage(image_window.image_display_handler.get_image_context().get().path, true, image_window.stage, image_window.aborter,logger);
                }
                key_event.consume();
                return;
            }*/
            case "u" , "U" -> {
                if (keyboard_dbg) logger.log("u like next ultim");

                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                image_window.image_display_handler.get_next_u(image_window.image_display_handler.get_image_context().get().path);
                key_event.consume();
                return;
            }

            case "v","V" -> {
                if (keyboard_dbg) logger.log("v like Vote");

                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                Image_context ic = image_window.image_display_handler.get_image_context().get();
                Optional<Image_context> new_ic = ic.ultim(image_window);
                if (new_ic.isPresent()) image_window.image_display_handler.set_image_context(new_ic.get());
                key_event.consume();
                return;
            }
            case "w","W" -> {
                if (keyboard_dbg) logger.log("w => slow down slide show");
                if (image_window.is_slide_show_running()) image_window.slow_down();
                key_event.consume();
                return;
            }
            case "x","X" -> {
                if (keyboard_dbg) logger.log("x => speed up slide show");
                if (image_window.is_slide_show_running()) image_window.hurry_up();
                key_event.consume();
                return;
            }
            case "y","Y" -> {
                if (keyboard_dbg) logger.log("y => move to same folder as previous move");
                Menus_for_image_window.do_same_move(image_window);
                key_event.consume();
                return;
            }
            case "z","Z" -> {
                if (keyboard_dbg)
                    logger.log("Z like Zoom (enables click-to-zoom mode: use the mouse to select the zoomed area)");
                image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.click_to_zoom);
                key_event.consume();
                return;
            }
        }

        if (keyboard_dbg) logger.log("keyboard : KeyEvent="+key_event.getCode());

        switch (key_event.getCode())
        {
            /*
            case UP:
                if ( keyword_dbg) logger.log("zoom in:");

                if ( image_window.image_display_handler.get_image_context().isEmpty()) return;
                image_window.image_display_handler.get_image_context().get().change_zoom_factor(image_window,1.05);
                break;
                */

            case UP:
                {
                    if (keyboard_dbg) logger.log("UP = previous rescaler");
                    Path p = image_window.image_display_handler.get_image_context().get().path;
                    if (!Guess_file_type.is_this_path_a_gif(p,logger)) {
                        // JAVAFX Image for GIF does not support PixelReader
                        image_window.rescaler = image_window.rescaler.previous();
                        image_window.redisplay(true);
                    }
                }
                break;

            case DOWN:
                {
                    if (keyboard_dbg) logger.log("DOWN = next rescaler");
                    Path p = image_window.image_display_handler.get_image_context().get().path;
                    if (!Guess_file_type.is_this_path_a_gif(p,logger)) {
                        // JAVAFX Image for GIF does not support PixelReader
                        image_window.rescaler = image_window.rescaler.next();
                        image_window.redisplay(true);
                    }
                }
            break;

            case LEFT:
                if (keyboard_dbg) logger.log("left");
                image_window.image_display_handler.change_image_relative(-1, image_window.ultim_mode);
                break;

            case SPACE:
                if (keyboard_dbg) logger.log("space");
            case RIGHT:
                if (keyboard_dbg) logger.log("right");
                image_window.image_display_handler.change_image_relative(1, image_window.ultim_mode);
                break;

            case DELETE:
                if (keyboard_dbg) logger.log("delete, move to trash");
                image_window.image_display_handler.delete();
                key_event.consume();
                break;

            case BACK_SPACE:
                if (keyboard_dbg) logger.log("backspace like delete, move to trash");
                image_window.image_display_handler.delete();
                key_event.consume();
                break;


            default:
                if (keyboard_dbg) logger.log("default");
                break;

        }
        key_event.consume();

    }




}
