// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.audio;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Window;
import klik.util.execute.actor.Actor_engine;
import klik.util.info_stage.Info_stage;
import klik.util.info_stage.Line_for_info_stage;
import klik.util.log.Logger;
import klik.util.ui.progress.Hourglass;
import klik.util.ui.progress.Progress_window;

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
        Actor_engine.execute(r,"Show audio info frame",logger);
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

        List<String> lll = MediaInfo.get(path, owner, logger);
        for(String s : lll)
        {
            l.add(new Line_for_info_stage(false,s));
        }
        String performer = MediaInfo.extract_performer(lll,logger);
        String release = MediaInfo.extract_release(lll,logger);
        logger.log("performer:"+ performer + " release:"+ release);

        Image icon = MusicBrainz.get_icon(path, performer, release, owner, logger);
        if ( icon != null)
        {
            l.add(new Line_for_info_stage(false,"icon found @ MusicBrainz"));
            if (MusicBrainz.improve_file_name(path, performer, release, owner, logger))
            {
                l.add(new Line_for_info_stage(false,"File name improved as 'artist'+'title' "));
            }
        }
        else
        {
            l.add(new Line_for_info_stage(false,"No icon found @ MusicBrainz"));
        }

        Runnable r = () -> Info_stage.show_info_stage("INFO:",l,icon, null);
        Platform.runLater(r);
        progress_window.close();

    }


}
