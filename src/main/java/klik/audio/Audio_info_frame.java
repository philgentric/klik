package klik.audio;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.util.files_and_paths.*;
import klik.util.ui.Hourglass;
import klik.util.log.Logger;
import klik.util.ui.Progress_window;
import klik.util.info_stage.Info_stage;
import klik.util.info_stage.Line_for_info_stage;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Audio_info_frame
//**********************************************************
{


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
        Hourglass progress_window = Progress_window.show(
                false,
                "Calling mediainfo",
                30,
                x,
                y,
                owner,
                logger);
        List<Line_for_info_stage> l = new ArrayList<>();
        l.add(new Line_for_info_stage(true,"Information about this file as reported by mediainfo:"));

        String performer = null;
        String release = null;
        for(String s : MediaInfo.get(path,logger))
        {
            {
                String marker = "Track name";
                if (s.contains(marker))
                {
                    if (!s.contains("Track name/Position"))
                    {
                        if (!s.contains("Track name/Total"))
                        {
                            logger.log("looking for:"+marker+" found:"+s);
                            int i = s.indexOf(marker);
                            if (i >= 0) {
                                release = s.substring(i + marker.length());
                                release = release.replace(":", "");
                                release = release.trim();
                                logger.log("looking for:"+marker+" found:"+release);
                            }
                        }
                    }
                }
            }
            {
                String marker = "Performer";
                if (s.contains(marker)) {
                    if (!s.contains("Album/Performer"))
                    {
                        int i = s.indexOf(marker);
                        if (i >= 0) {
                            performer = s.substring(i + marker.length());
                            performer = performer.replace(":", "");
                            performer = performer.trim();
                        }
                    }
                }
            }
            l.add(new Line_for_info_stage(false,s));
        }

        Image icon;
        if ( (performer != null) && (release != null))
        {
            logger.log("performer:"+performer+ " release:"+release);
            icon = MusicBrainz.get_icon(performer, release, owner,logger);
            if ( icon != null)
            {
                l.add(new Line_for_info_stage(false,"icon found @ MusicBrainz"));
            }
            else
            {
                l.add(new Line_for_info_stage(false,"No icon found @ MusicBrainz"));
            }
            String candidate_name =  MusicBrainz.make_name(performer,release,owner,logger);
            if ( !path.getFileName().toString().equals(candidate_name)) {
                String extension = Static_files_and_paths_utilities.get_extension(path.getFileName().toString());
                Path new_path = null;
                try
                {
                    new_path = path.getParent().resolve(candidate_name + "." + extension);
                }
                catch( InvalidPathException e)
                {
                    logger.log(""+e);
                }
                if (new_path != null)
                {
                    List<Old_and_new_Path> ll = new ArrayList<>();
                    ll.add(new Old_and_new_Path(path, new_path, Command.command_rename, Status_old_and_new_Path.before_command, false));
                    Moving_files.perform_safe_moves_in_a_thread(ll, true, 100, 100, owner, new Aborter("dummy", logger), logger);
                }
            }
        }
        else
        {
            icon = null;
        }


        Runnable r = () -> Info_stage.show_info_stage("INFO:",l,icon, null);
        Platform.runLater(r);
        progress_window.close();

    }
}
