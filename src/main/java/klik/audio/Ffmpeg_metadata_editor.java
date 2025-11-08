// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.audio;

import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Booleans;
import klik.util.execute.Execute_result;
import klik.util.execute.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.util.execute.Execute_command;
import klik.util.files_and_paths.Extensions;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//**********************************************************
public class Ffmpeg_metadata_editor
//**********************************************************
{
    //**********************************************************
    public static void edit_metadata_of_a_file_in_a_thread(
            Path path, Window owner, Logger logger)
    //**********************************************************
    {
        Actor_engine.execute(()->
        Platform.runLater(()->open_editor(path,owner,logger)),"Edit ffmpeg metadata",logger);
    }

    //**********************************************************
    private static String get_string_from_user(String key, String value,Window owner, Logger logger)
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(value);
        Look_and_feel_manager.set_dialog_look(dialog,owner,logger);
        dialog.initOwner(owner);
        dialog.setWidth(800);
        //dialog.setTitle("Editing "+key);
        dialog.setHeaderText("Editing "+key);
        //dialog.setContentText(value);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String new_name = result.get();
            dialog.close();
            return new_name;
        }
        dialog.close();
        return  null;
    }
    //**********************************************************
    private static void open_editor(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        List<String> lll = MediaInfo.get(path, owner, logger);
        String performer = MediaInfo.extract_performer(lll,logger);
        String release = MediaInfo.extract_release(lll,logger);

        String new_performer= get_string_from_user("performer (artist,group name etc)",performer,owner,logger);
        logger.log ("new_performer="+new_performer);
        String new_title= get_string_from_user("title ('release', song name)",release,owner,logger);

        logger.log ("new_title="+new_title);
        Runnable r = ()->
        {
            String output_path = path.toAbsolutePath().toString();
            String base = Extensions.get_base_name(output_path);
            String extension = Extensions.get_extension(output_path);
            output_path = Extensions.add(base + "_edited", extension);
            List<String> command_line_for_ffmpeg = new ArrayList<>();
            command_line_for_ffmpeg.add("ffmpeg");
            command_line_for_ffmpeg.add("-i");
            command_line_for_ffmpeg.add(path.toAbsolutePath().toString());
            command_line_for_ffmpeg.add("-c");
            command_line_for_ffmpeg.add("copy");
            if (new_title != null) {
                command_line_for_ffmpeg.add("-metadata");
                command_line_for_ffmpeg.add("title=" + new_title);
            }
            if (new_performer != null) {
                command_line_for_ffmpeg.add("-metadata");
                command_line_for_ffmpeg.add("artist=" + new_performer);
            }
            command_line_for_ffmpeg.add(output_path);

            logger.log("command_line_for_ffmpeg:" + command_line_for_ffmpeg);

            StringBuilder sb = new StringBuilder();
            Execute_result res = Execute_command.execute_command_list(command_line_for_ffmpeg, path.getParent().toFile(), 2000, sb, logger);
            if ( !res.status())
            {
                List<String> verify = new ArrayList<>();
                verify.add("ffmpeg");
                verify.add("-version");
                String home = System.getProperty(Non_booleans_properties.USER_HOME);
                Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, logger);
                if ( !res2.status())
                {
                    Booleans.manage_show_ffmpeg_install_warning(owner,logger);
                }
                return;
            }
            logger.log("wtf" + sb);
            logger.log("ffmpeg meta data edit:" + res.output());
        };
        Actor_engine.execute(r,"Ffmpeg meta data edit",logger);
    }
}
