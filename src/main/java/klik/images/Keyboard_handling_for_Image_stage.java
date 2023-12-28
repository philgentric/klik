package klik.images;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.change.Change_gang;
import klik.level2.metadata.Tag_stage;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

//**********************************************************
public class Keyboard_handling_for_Image_stage
//**********************************************************
{
    private static final boolean keyword_dbg = false;
    //**********************************************************
    static void handle_keyboard(Browser the_browser, Image_window image_stage, final KeyEvent key_event, Logger logger)
    //**********************************************************
    {

        if ( keyword_dbg) logger.log("Image_stage KeyEvent="+key_event);
        if (key_event.getCode() == KeyCode.ESCAPE)
        {
            key_event.consume();
            if (!the_browser.get_escape_preference())
            {
                logger.log("Image_stage : ignoring escape by user preference");
                return;
            }

            if (image_stage.exit_on_escape) {
                image_stage.the_Stage.close();
                logger.log("Image_stage closing on escape");
                Change_gang.deregister(image_stage.image_display_handler);
            }
            else
            {
                logger.log("Image_stage : ignoring escape because we are in full screen");
                image_stage.exit_on_escape = true;
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
            if (Static_application_properties.get_level2(logger))
            {
                // shift d is "sure delete"
                Path path = image_stage.image_display_handler.get_image_context().path;
                try {
                    Files.delete(path);
                } catch (NoSuchFileException x) {
                    logger.log("no such file or directory:" + path);
                    return;
                } catch (IOException e) {
                    logger.log("cannot delete ? " + e);
                    return;
                }
                image_stage.image_display_handler.change_image_relative(1, image_stage.ultim_mode);
            }
            else {
                Popups.popup_warning(the_browser.my_Stage.the_Stage,"Ahah!","Using Shift-D for sure-deleting a file requires to be on level2", false,logger);
            }
            return;
        }

        switch (key_event.getText())
        {
            default -> {
                if (keyword_dbg) logger.log("= like pix-for-pix: use mouse to select visible part of large image");
                image_stage.mouse_handling_for_image_stage.set_mouse_mode(image_stage, Mouse_mode.pix_for_pix);
                break;
            }
            case "b" -> {
                if (keyword_dbg) logger.log("b like browse");
                Browser_creation_context.additional_different_folder(image_stage.image_display_handler.get_image_context().path.getParent(), the_browser, logger);
                key_event.consume();
                return;
            }
            case "c" -> {
                if (keyword_dbg) logger.log("c like copy");
                Runnable after = image_stage.image_display_handler.image_indexer::signal_file_copied;
                image_stage.image_display_handler.get_image_context().copy(the_browser, after);
                key_event.consume();
                return;
            }
            case "d" -> {
                if (keyword_dbg) logger.log("d like delete, move to trash");
                image_stage.image_display_handler.delete();
                key_event.consume();
                return;
            }
            case "e" -> {
                if (keyword_dbg) logger.log("e like edit");
                image_stage.image_display_handler.get_image_context().edit();
                key_event.consume();
                return;
            }
            case "f" -> {
                if (keyword_dbg) logger.log("f like find = search_using_keywords_given_by_the_user");
                image_stage.image_display_handler.get_image_context().search_using_keywords_given_by_the_user(the_browser);
                key_event.consume();
                return;
            }
            case "i" -> {
                if (keyword_dbg) logger.log("i like information");
                image_stage.image_display_handler.get_image_context().show_exif_stage();
                key_event.consume();
                return;
            }
            case "k" -> {
                if (keyword_dbg) logger.log("k like search_using_keywords_from_the_name");
                image_stage.image_display_handler.get_image_context().search_using_keywords_from_the_name(the_browser);
                key_event.consume();
                return;
            }
            case "m" -> {
                if (keyword_dbg) logger.log("m like Move (enables drag-and-drop mode)");
                image_stage.mouse_handling_for_image_stage.set_mouse_mode(image_stage, Mouse_mode.drag_and_drop);
                key_event.consume();
                return;
            }
            case "o" -> {
                if (keyword_dbg) logger.log("o like open ");
                image_stage.image_display_handler.get_image_context().open();
                key_event.consume();
                return;
            }
            case "r" -> {
                if (keyword_dbg) logger.log("r like rename");
                image_stage.image_display_handler.get_image_context().rename_file_for_an_image_stage(image_stage);
                key_event.consume();
                return;
            }
            case "s" -> {
                if (keyword_dbg) logger.log("s like slideshow");
                image_stage.toggle_slideshow();
                key_event.consume();
                return;
            }
            case "t" -> {
                if (keyword_dbg) logger.log("t like tag");
                Tag_stage.open_tag_stage(image_stage.image_display_handler.get_image_context().path, true, logger);
                key_event.consume();
                return;
            }
            case "u" -> {
                if (keyword_dbg) logger.log("u like next ultim");
                image_stage.image_display_handler.get_next_u(image_stage.image_display_handler.get_image_context().path);
                key_event.consume();
                return;
            }
            case "U" -> {
                if (keyword_dbg) logger.log("U like ultim MODE");
                image_stage.ultim_mode = !image_stage.ultim_mode;
                key_event.consume();
                return;
            }
            case "v" -> {
                if (keyword_dbg) logger.log("v like up Vote");
                Image_context ic = image_stage.image_display_handler.get_image_context();
                Image_context new_ic = ic.ultim(image_stage);
                if (new_ic != null) image_stage.image_display_handler.set_image_context(new_ic);
                key_event.consume();
                return;
            }
            case "w" -> {
                if (keyword_dbg) logger.log("w => slow down slide show");
                if (image_stage.slide_show != null) image_stage.slide_show.slow_down();
                key_event.consume();
                return;
            }
            case "x" -> {
                if (keyword_dbg) logger.log("x => speed up slide show");
                if (image_stage.slide_show != null) image_stage.slide_show.hurry_up();
                key_event.consume();
                return;
            }
            case "y" -> {
                if (keyword_dbg) logger.log("y => move to same folder as previous move");
                Menu_for_image_stage.do_same_move(image_stage);
                key_event.consume();
                return;
            }
            case "z" -> {
                if (keyword_dbg)
                    logger.log("Z like Zoom (enables click-to-zoom mode: use the mouse to select the zoomed area)");
                image_stage.mouse_handling_for_image_stage.set_mouse_mode(image_stage, Mouse_mode.click_to_zoom);
                key_event.consume();
                return;
            }
        }

        switch (key_event.getCode())
        {
            case UP:
                if ( keyword_dbg) logger.log("zoom up/in:");
                image_stage.image_display_handler.get_image_context().change_zoom_factor(image_stage,1.05);
                break;

            case DOWN:
                if ( keyword_dbg) logger.log("zoom down/out:");
                image_stage.image_display_handler.get_image_context().change_zoom_factor(image_stage,0.95);
                break;

            case LEFT:
                if ( keyword_dbg) logger.log("left");
                image_stage.image_display_handler.change_image_relative(-1, image_stage.ultim_mode);
                break;

            case SPACE:
                if ( keyword_dbg) logger.log("space");
            case RIGHT:
                if ( keyword_dbg) logger.log("right");
                image_stage.image_display_handler.change_image_relative(1, image_stage.ultim_mode);
                break;

            default:
                break;

        }
        key_event.consume();

    }




}
