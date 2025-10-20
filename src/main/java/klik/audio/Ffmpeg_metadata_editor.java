package klik.audio;

import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;
import klik.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.util.execute.Execute_command;
import klik.util.files_and_paths.Extensions;
import klik.util.log.Logger;

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
            List<String> cmds = new ArrayList<>();
            cmds.add("ffmpeg");
            cmds.add("-i");
            cmds.add(path.toAbsolutePath().toString());
            cmds.add("-c");
            cmds.add("copy");
            if (new_title != null) {
                cmds.add("-metadata");
                cmds.add("title=" + new_title);
            }
            if (new_performer != null) {
                cmds.add("-metadata");
                cmds.add("artist=" + new_performer);
            }
            cmds.add(output_path);

            logger.log("cmds" + cmds);

            StringBuilder sb = new StringBuilder();
            String out = Execute_command.execute_command_list(cmds, path.getParent().toFile(), 2000, sb, logger);
            logger.log("wtf" + sb);
            logger.log("ffmpeg meta data edit:" + out);
        };
        Actor_engine.execute(r,"Ffmpeg meta data edit",logger);
    }
}
