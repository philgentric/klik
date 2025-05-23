package klik.images;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.images.decoding.Exif_metadata_extractor;
import klik.experimental.fusk.Fusk_static_core;
import klik.experimental.fusk.Fusk_strings;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.execute.Execute_command;
import klik.util.ui.Jfx_batch_injector;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static klik.browser.icons.animated_gifs.Animated_gif_from_folder.warning_GraphicsMagick;

//**********************************************************
public class Exif_stage
//**********************************************************
{
    private static final boolean exif_dbg = false;
    private static final double WIDTH = 1000;

    //**********************************************************
    public static void show_exif_stage(Image image, Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if(Platform.isFxApplicationThread())
        {
            show_exif_stage_(image,path,aborter,logger);
        }
        else
        {
            Runnable r = () -> show_exif_stage_(image,path,aborter,logger);
            Jfx_batch_injector.inject(r,logger);
        }
    }


    //**********************************************************
    private static void show_exif_stage_(Image image, Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( image == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("FATL: image is null"));
            return;
        }
        TextFlow textFlow = new TextFlow();
        textFlow.setLayoutX(40);
        textFlow.setLayoutY(40);
        if ( exif_dbg) logger.log("$$$$$$ EXIF $$$$$$$$$$$");

        TextField tf = new TextField(path.toAbsolutePath().toString());
        Look_and_feel_manager.set_TextField_look(tf);
        tf.setMinWidth(WIDTH);
        textFlow.getChildren().add(tf);

        textFlow.getChildren().add(new Text(System.lineSeparator()));

        {
            String file_size = Static_files_and_paths_utilities.get_1_line_string_with_size(path.toAbsolutePath(), logger);
            new_line(file_size, textFlow);
        }
        Exif_read_result res = load_exif(path, image, new Aborter("EXIF",logger),logger);
        for (String s : res.exif_items())
        {
            if ( exif_dbg) logger.log(s);
            new_line(s, textFlow);
        }
        {
            StringBuilder sb = get_GraphicsMagick_info(path,logger);
            if (sb == null) return;
            // break sb.toString() into lines
            String[] lines = sb.toString().split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                new_line(line, textFlow);
            }
        }

        if ( exif_dbg) logger.log("$$$$$$$$$$$$$$$$$$$$$$$$");
        ScrollPane sp = new ScrollPane();
        Look_and_feel_manager.set_region_look(sp);
        Look_and_feel_manager.set_region_look(textFlow);
        sp.setPrefSize(WIDTH, 600);
        sp.setContent(textFlow);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        Stage local_stage = new Stage();
        local_stage.setHeight(600);
        local_stage.setWidth(WIDTH);

        Scene scene = new Scene(sp, WIDTH, 600);

        String extension = Static_files_and_paths_utilities.get_extension(path.getFileName().toString());
        if ( extension.equalsIgnoreCase(Fusk_static_core.FUSK_EXTENSION))
        {
            if (Fusk_static_core.is_fusk(path, logger))
            {
                String base = Static_files_and_paths_utilities.get_base_name(path.toAbsolutePath().toString());
                local_stage.setTitle(Fusk_strings.defusk_string(base, logger));
            }
            else
            {
                local_stage.setTitle(path.toAbsolutePath()+"(has the extension but IS NOT a fusk!)");
            }
        }
        else
        {
            local_stage.setTitle(path.toAbsolutePath().toString());
        }
        local_stage.setScene(scene);
        local_stage.show();
        local_stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (key_event.getCode() == KeyCode.ESCAPE) {
                        local_stage.close();
                        key_event.consume();
                    }
                });
    }

    private static void new_line(String file_size, TextFlow textFlow) {
        TextField text_field = new TextField(file_size);
        text_field.setEditable(false);
        text_field.setMinWidth(WIDTH);
        Look_and_feel_manager.set_TextField_look(text_field);
        textFlow.getChildren().add(text_field);
        textFlow.getChildren().add(new Text(System.lineSeparator()));
    }

    //**********************************************************
    public static Exif_read_result load_exif(Path path, Image image, Aborter aborter, Logger logger)
    //**********************************************************
    {
        List<String> exifs_tags_list = new ArrayList<>();
        boolean image_is_damaged = false;
        String title = null;
        double rotation = 0;
        try
        {
            Exif_metadata_extractor extractor = new Exif_metadata_extractor(path,logger);
            double how_many_pixels = image.getWidth()*image.getHeight();
            exifs_tags_list = extractor.get_exif_metadata(how_many_pixels,true,aborter,true);
            rotation = extractor.get_rotation(true,aborter);
            if ( exif_dbg) logger.log(path+" rotation="+rotation);
            image_is_damaged = extractor.is_image_damaged();
            title = extractor.title;
        }
        catch (OutOfMemoryError e)
        {
            logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
        }
        return new Exif_read_result(title,exifs_tags_list,rotation,image_is_damaged);
    }

    //**********************************************************
    public static StringBuilder get_GraphicsMagick_info(Path path, Logger logger)
    //**********************************************************
    {
        List<String> graphicsMagick_command_line = new ArrayList<>();
        graphicsMagick_command_line.add("gm");
        graphicsMagick_command_line.add("identify");
        graphicsMagick_command_line.add("-verbose");
        graphicsMagick_command_line.add(path.toAbsolutePath().toString());

        StringBuilder sb = new StringBuilder();
        if ( Execute_command.execute_command_list(graphicsMagick_command_line, path.getParent().toFile(), 2000, sb,logger) == null)
        {
            logger.log(warning_GraphicsMagick);
            return null;
        }
        logger.log(sb.toString());
        return sb;
    }


}
