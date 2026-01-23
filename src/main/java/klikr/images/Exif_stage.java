// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.images;

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
import javafx.stage.Window;
import klikr.properties.String_constants;
import klikr.util.External_application;
import klikr.properties.Non_booleans_properties;
import klikr.util.execute.Execute_result;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.properties.boolean_features.Booleans;
import klikr.util.files_and_paths.Extensions;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.image.decoding.Exif_metadata_extractor;
import klikr.experimental.fusk.Fusk_static_core;
import klikr.experimental.fusk.Fusk_strings;
import klikr.look.Look_and_feel_manager;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.execute.Execute_command;
import klikr.util.ui.Jfx_batch_injector;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


//**********************************************************
public class Exif_stage
//**********************************************************
{
    private static final boolean exif_dbg = false;
    private static final double WIDTH = 1000;
    private static final double VERY_WIDE = 3000;

    //**********************************************************
    public static void show_exif_stage(Image image, Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if(Platform.isFxApplicationThread())
        {
            show_exif_stage_(image,path,owner,aborter,logger);
        }
        else
        {
            Runnable r = () -> show_exif_stage_(image,path, owner, aborter,logger);
            Jfx_batch_injector.inject(r,logger);
        }
    }


    //**********************************************************
    private static void show_exif_stage_(Image image, Path path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (image == null) {
            logger.log(Stack_trace_getter.get_stack_trace("FATL: image is null"));
            return;
        }
        Stage local_stage = new Stage();

        TextFlow textFlow = new TextFlow();
        textFlow.setLayoutX(40);
        textFlow.setLayoutY(40);
        if (exif_dbg) logger.log("$$$$$$ EXIF $$$$$$$$$$$");

        //Scrollable_text_field text_field = new Scrollable_text_field(path.toAbsolutePath().toString(),path,null,local_stage,aborter, logger);
        //Look_and_feel_manager.set_region_look(text_field, owner, logger);
        TextField text_field = new TextField(path.toAbsolutePath().toString());
        Look_and_feel_manager.set_TextField_look(text_field, false, owner, logger);
        text_field.setMinWidth(VERY_WIDE);
        text_field.setEditable(false);
        text_field.setFocusTraversable(false);
        textFlow.getChildren().add(text_field);

        textFlow.getChildren().add(new Text(System.lineSeparator()));


        if (exif_dbg) logger.log("$$$$$$$$$$$$$$$$$$$$$$$$");
        ScrollPane sp = new ScrollPane();
        Look_and_feel_manager.set_region_look(sp, owner, logger);
        Look_and_feel_manager.set_region_look(textFlow, owner, logger);
        sp.setPrefSize(WIDTH, 600);
        sp.setContent(textFlow);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        local_stage.initOwner(owner);
        local_stage.setHeight(600);
        local_stage.setWidth(WIDTH);

        Scene scene = new Scene(sp);

        String extension = Extensions.get_extension(path.getFileName().toString());
        if (extension.equalsIgnoreCase(Fusk_static_core.FUSK_EXTENSION)) {
            if (Fusk_static_core.is_fusk(path, logger)) {
                String base = Extensions.get_base_name(path.toAbsolutePath().toString());
                local_stage.setTitle(Fusk_strings.defusk_string(base, logger));
            } else {
                local_stage.setTitle(path.toAbsolutePath() + "(has the extension but IS NOT a fusk!)");
            }
        } else {
            local_stage.setTitle(path.toAbsolutePath().toString());
        }
        local_stage.setScene(scene);
        local_stage.show();
        local_stage.sizeToScene();
        local_stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (key_event.getCode() == KeyCode.ESCAPE) {
                        local_stage.close();
                        key_event.consume();
                    }
                });
        Actor_engine.execute(()->feed_the_beast(path, image, textFlow, owner, logger),"Extract EXIF and display",logger);
    }

    //**********************************************************
    private static void feed_the_beast(Path path, Image image, TextFlow textFlow, Window owner, Logger logger)
    //**********************************************************
    {
        List<TextField> l = new ArrayList<>();
        {
            String file_size = Static_files_and_paths_utilities.get_1_line_string_with_size(path.toAbsolutePath(), owner,logger);
            l.add(new_line(file_size, textFlow,owner,logger));
        }
        Exif_read_result res = load_exif(path, image, owner,new Aborter("EXIF",logger),logger);
        for (String s : res.exif_items())
        {
            if ( exif_dbg) logger.log(s);
            if ( s.startsWith("Colors:"))
            {
                logger.log("warning truncating after Colors in exif output");
                break;
            }
            l.add(new_line(s, textFlow,owner,logger));
            if ( l.size() > 200)
            {
                logger.log("warning truncating exif output");
                break;
            }
        }
        l.add(new_line("========== graphicsmagick info ===============", textFlow,owner,logger));

        {
            StringBuilder sb = get_graphicsmagick_info(path,owner,logger);
            if (sb == null) return;
            // break sb.toString() into lines
            String[] lines = sb.toString().split("\n");
            int i = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if ( line.startsWith("Colors:"))
                {
                    logger.log("warning truncating after Colors in graphicsmagick_info output");
                    break;
                }
                l.add(new_line(line, textFlow,owner,logger));
                if ( i > 100)
                {
                    logger.log("warning truncating graphicsmagick_info output");
                    break;
                }
                i++;
            }
        }
        Platform.runLater(()->
        {
            for ( TextField text_field : l) {
                textFlow.getChildren().add(text_field);
                textFlow.getChildren().add(new Text(System.lineSeparator()));
            }
        });

    }

    //**********************************************************
    private static TextField new_line(String file_size, TextFlow textFlow,Window owner,Logger logger)
    //**********************************************************
    {
        TextField text_field = new TextField(file_size);
        text_field.setEditable(false);
        text_field.setFocusTraversable(false);
        text_field.setMinWidth(VERY_WIDE);
        Look_and_feel_manager.set_TextField_look(text_field,false,owner,logger);
        return text_field;
    }

    //**********************************************************
    public static Exif_read_result load_exif(Path path, Image image, Window owner,Aborter aborter, Logger logger)
    //**********************************************************
    {
        List<String> exifs_tags_list = new ArrayList<>();
        boolean image_is_damaged = false;
        String title = null;
        double rotation = 0;
        try
        {
            Exif_metadata_extractor extractor = new Exif_metadata_extractor(path,owner, logger);
            double how_many_pixels = image.getWidth()*image.getHeight();
            exifs_tags_list = extractor.get_exif_metadata(how_many_pixels,true,aborter,false);
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
    public static StringBuilder get_graphicsmagick_info(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        List<String> graphicsMagick_command_line = new ArrayList<>();
        graphicsMagick_command_line.add(External_application.GraphicsMagick.get_command(owner,logger));
        graphicsMagick_command_line.add("identify");
        graphicsMagick_command_line.add("-verbose");
        graphicsMagick_command_line.add(path.toAbsolutePath().toString());

        StringBuilder sb = new StringBuilder();
        Execute_result res = Execute_command.execute_command_list(graphicsMagick_command_line, path.getParent().toFile(), 2000, sb,logger);
        if ( !res.status())
        {
            List<String> verify = new ArrayList<>();
            verify.add(External_application.GraphicsMagick.get_command(owner,logger));
            verify.add("--version");
            String home = System.getProperty(String_constants.USER_HOME);
            Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, logger);
            if ( !res2.status())
            {
                Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
            }
            return sb;
        }
        //logger.log(sb.toString());
        return sb;
    }


}
