// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import klik.look.Look_and_feel;
import klik.properties.boolean_features.Preferences_stage;
import klik.util.execute.Execute_via_script_in_tmp_file;
import klik.util.log.Logger;
import klik.util.ui.Popups;

//**********************************************************
public enum External_application
//**********************************************************
{
    FFmpeg,
    GraphicsMagick,
    MediaInfo,
    Ytdlp,
    AcousticID_chromaprint,
    ImageMagick,
    Vips;


    //**********************************************************
    public HBox get_button(double width, double icon_size, Look_and_feel look_and_feel, Window owner, Logger logger)
    //**********************************************************
    {
        EventHandler<ActionEvent> handler =  e ->
        {
            String cmd = get_command_string_to_install(owner,logger);
            if ( cmd == null) return;
            Execute_via_script_in_tmp_file.execute(cmd, true,true,owner,logger);
        };
        HBox hb = Preferences_stage.make_hbox_with_button_and_explanation(
                get_I18n_key(),
                handler,
                width,
                icon_size,
                look_and_feel,
                owner,
                logger);

        return hb;
    }



    //**********************************************************
    String get_command_string_to_install(Window owner, Logger logger)
    //**********************************************************
    {
        if (( System.getProperty("os.name").toLowerCase().contains("mac"))
        || ( System.getProperty("os.name").toLowerCase().contains("inux")))
        {
            return "brew install "+get_brew_install_name();
        }
        Popups.popup_warning("❗Warning", "Sorry, this is implemented only for Mac and Linux. Your OS: "+System.getProperty("os.name"),
                false, owner, logger);
        return null;
    }



    //**********************************************************
    public String get_I18n_key()
    //**********************************************************
    {
        // this is NOT for display: this MUST be the exact string
        // as found in the ressource bundles
        switch (this)
        {
            case Ytdlp:
                return "Install_Youtubedownloader";
            case AcousticID_chromaprint:
                return "Install_Fpcalc";
            case ImageMagick:
                return "Install_Imagemagick";
            case FFmpeg:
                return "Install_Ffmpeg";
            case Vips:
                return "Install_Vips";
            case GraphicsMagick:
                return "Install_Graphicsmagick";
            case MediaInfo:
                return "Install_Mediainfo";
            default:
                return null;
        }
    }
    //**********************************************************
    public String get_brew_install_name()
    //**********************************************************
    {
        // this is NOT for display: this MUST be th exact required string in:
        // brew install <REQUIRED_STRING>
        switch (this)
        {
            case Ytdlp:
                return "yt-dlp";
            case AcousticID_chromaprint:
                return "chromaprint";
            case ImageMagick:
                return "imagemagick";
            case FFmpeg:
                return "ffmpeg";
            case Vips:
                return "vips";
            case GraphicsMagick:
                return "graphicsmagick";
            case MediaInfo:
                return "mediainfo";
            default:
                return null;
        }
    }
}
