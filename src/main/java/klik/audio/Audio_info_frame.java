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

        List<String> lll = MediaInfo.get(path, logger);
        for(String s : lll)
        {
            l.add(new Line_for_info_stage(false,s));
        }
        String performer = MediaInfo.extract_performer(lll,logger);
        String release = MediaInfo.extract_release(lll,logger);

        Image icon = get_icon_from_MusicBrainz(path, performer, release, owner, logger);
        if ( icon != null)
        {
            l.add(new Line_for_info_stage(false,"icon found @ MusicBrainz"));
        }
        else
        {
            l.add(new Line_for_info_stage(false,"No icon found @ MusicBrainz"));
        }

        Runnable r = () -> Info_stage.show_info_stage("INFO:",l,icon, null);
        Platform.runLater(r);
        progress_window.close();

    }

    private static Image get_icon_from_MusicBrainz(
            Path path,
            String performer,
            String release,
            Window owner, Logger logger) {
        Image icon;
        if ( (performer != null) && (release != null))
        {
            logger.log("performer:"+ performer + " release:"+ release);
            Path icon_folder = path.getParent();
            icon = MusicBrainz.get_icon(performer, release, icon_folder, owner, logger);

            String candidate_name =  MusicBrainz.make_name(performer, release, owner, logger);
            String extension = Extensions.get_extension(path.getFileName().toString());
            candidate_name = Extensions.add(candidate_name,extension);
            if ( !path.getFileName().toString().equals(candidate_name))
            {
                logger.log("name ->"+ path.getFileName()+"<- could be improved ->"+candidate_name+"<-");
                Path new_path = null;
                try
                {
                    new_path = path.getParent().resolve(candidate_name);
                }
                catch( InvalidPathException e)
                {
                    logger.log(""+e);
                }
                if (new_path != null)
                {
                    List<Old_and_new_Path> ll = new ArrayList<>();
                    ll.add(new Old_and_new_Path(path, new_path, Command.command_rename, Status.before_command, false));
                    Moving_files.perform_safe_moves_in_a_thread(ll, true, 100, 100, owner, new Aborter("dummy", logger), logger);
                }
            }
        }
        else
        {
            icon = null;
        }
        return icon;
    }
}
