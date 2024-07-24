package klik.audio;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.ui.Hourglass;
import klik.util.log.Logger;
import klik.util.ui.Show_running_man_frame;
import klik.util.execute.Execute_command;
import klik.util.info_stage.Info_stage;
import klik.util.info_stage.Line_for_info_stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Audio_info_frame {

    public static final String warning_mediainfo = "This feature requires mediainfo\n . for mac: brew install mediainfo\n . for any system, download from mediaarea.net\n";

    public static void show(Path path, Logger logger) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                show_(path,logger);
            }
        };
        Actor_engine.execute(r,logger);
    }

    private static void show_(Path path, Logger logger)
    {

        Hourglass running_man = Show_running_man_frame.show_running_man("Calling mediainfo", 30, new Aborter("mediainfo", logger), logger);
        List<Line_for_info_stage> l = new ArrayList<>();
        l.add(new Line_for_info_stage(true,"Information about this file as reported by mediainfo:"));

        List<String> graphicsMagick_command_line = new ArrayList<>();
        graphicsMagick_command_line.add("mediainfo");
        graphicsMagick_command_line.add(path.toAbsolutePath().toString());

        StringBuilder sb = new StringBuilder();
        if ( ! Execute_command.execute_command_list(graphicsMagick_command_line, path.getParent().toFile(), 2000, sb,logger))
        {
            logger.log(warning_mediainfo);
            return;
        }
        String[] lines = sb.toString().split("\\R");
        for(String s : lines)
        {
            if ( s.contains(Execute_command.GOING_TO_SHOOT_THIS)) continue;
            if ( s.contains(Execute_command.IN_WORKING_DIR)) continue;
            if ( s.contains(Execute_command.EXECUTE_COMMAND_END_OF_WAIT_OK)) continue;
            l.add(new Line_for_info_stage(false,s));
        }

        Runnable r = () -> Info_stage.show_info_stage("INFO:",l, null);
        Platform.runLater(r);
        running_man.close();

    }
}
