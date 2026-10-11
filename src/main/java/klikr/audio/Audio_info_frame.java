// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.audio;

import javafx.application.Platform;
import javafx.scene.image.Image;
import klikr.util.Kontext;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.info_stage.Info_stage;
import klikr.util.info_stage.Line_for_info_stage;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Audio_info_frame
//**********************************************************
{


    //**********************************************************
    public static void show(Path path, Kontext context)
    //**********************************************************
    {
        Runnable r = () -> show_(path,context);
        Actor_engine.execute(r,"Show audio info frame", context.logger());
    }

    //**********************************************************
    private static void show_(Path path, Kontext context)
    //**********************************************************
    {
        Hourglass progress_window = Progress_window.show(
                "Calling mediainfo",
                30,
                context).orElse(null);
        List<Line_for_info_stage> l = new ArrayList<>();
        l.add(new Line_for_info_stage(true,"Information about this file as reported by mediainfo:"));

        List<String> lll = MediaInfo.get(path, context);
        for(String s : lll)
        {
            l.add(new Line_for_info_stage(false,s));
        }
        String performer = MediaInfo.extract_performer(lll,context);
        String release = MediaInfo.extract_release(lll,context);
        context.log("performer:"+ performer + " release:"+ release);

        Image icon = MusicBrainz.get_icon(path, performer, release, context);
        if ( icon != null)
        {
            l.add(new Line_for_info_stage(false,"icon found @ MusicBrainz"));
            if (MusicBrainz.improve_file_name(path, performer, release, context))
            {
                l.add(new Line_for_info_stage(false,"File name improved as 'artist'+'title' "));
            }
        }
        else
        {
            l.add(new Line_for_info_stage(false,"No icon found @ MusicBrainz"));
        }

        Runnable r = () -> Info_stage.show_info_stage("INFO:",l,icon,context);
        Platform.runLater(r);
        if ( progress_window != null) progress_window.close();

    }


}
