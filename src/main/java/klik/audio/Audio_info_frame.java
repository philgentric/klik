package klik.audio;

import javafx.application.Platform;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.ui.Hourglass;
import klik.util.log.Logger;
import klik.util.ui.Show_running_film_frame;
import klik.util.execute.Execute_command;
import klik.util.info_stage.Info_stage;
import klik.util.info_stage.Line_for_info_stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Audio_info_frame
//**********************************************************
{

    public static final String warning_mediainfo = "This feature requires mediainfo\n . for mac: brew install mediainfo\n . for any system, download from mediaarea.net\n";

    //**********************************************************
    public static void show(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> show_(path,owner,logger);
        Actor_engine.execute(r,logger);
    }

    //**********************************************************
    private static void show_(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Hourglass running_film = Show_running_film_frame.show_running_film(owner,x,y,"Calling mediainfo", 30, new Aborter("mediainfo", logger), logger);
        List<Line_for_info_stage> l = new ArrayList<>();
        l.add(new Line_for_info_stage(true,"Information about this file as reported by mediainfo:"));

        List<String> graphicsMagick_command_line = new ArrayList<>();
        graphicsMagick_command_line.add("mediainfo");
        graphicsMagick_command_line.add(path.toAbsolutePath().toString());

        StringBuilder sb = new StringBuilder();
        if ( Execute_command.execute_command_list(graphicsMagick_command_line, path.getParent().toFile(), 2000, sb,logger) == null)
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
        running_film.close();

    }
}
