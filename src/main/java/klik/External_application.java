// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.boolean_features.Preferences_stage;
import klik.util.execute.Execute_via_script_in_tmp_file;
import klik.util.execute.Guess_OS;
import klik.util.log.Logger;
import klik.util.ui.Popups;

import java.util.Optional;

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
            Execute_via_script_in_tmp_file.execute(cmd, true,false,owner,logger);
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
        switch(Guess_OS.guess(owner, logger))
        {
            case MacOS -> {return get_macOS_install_command();}
            case Linux -> {return get_Linux_install_command(owner,logger);}
            case Windows -> {return get_Windows_install_command();}
            case Unknown -> {return "";}
        }
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
    public String get_macOS_install_command()
    //**********************************************************
    {
        // this is NOT for display: this MUST be th exact required string in:
        // brew install <REQUIRED_STRING>
        switch (this)
        {
            case Ytdlp:
                return "brew install yt-dlp";
            case AcousticID_chromaprint:
                return "brew install chromaprint";
            case ImageMagick:
                return "brew install imagemagick";
            case FFmpeg:
                return "brew install ffmpeg";
            case Vips:
                return "brew install vips";
            case GraphicsMagick:
                return "brew install graphicsmagick";
            case MediaInfo:
                return "brew install mediainfo";
            default:
                return null;
        }
    }

    //**********************************************************
    public String get_Windows_install_command()
    //**********************************************************
    {
        // this is NOT for display: this MUST be th exact required string in:
        // brew install <REQUIRED_STRING>
        switch (this)
        {
            case Ytdlp:
                return "choco install yt-dlp";
            case AcousticID_chromaprint:
                return "choco install chromaprint";
            case ImageMagick:
                return "choco install imagemagick";
            case FFmpeg:
                return "choco install ffmpeg";
            case Vips:
                return "choco install vips";
            case GraphicsMagick:
                return "choco install graphicsmagick";
            case MediaInfo:
                return "choco install mediainfo";
            default:
                return null;
        }
    }


    //**********************************************************
    public String get_Linux_install_command(Window owner, Logger logger)
    //**********************************************************
    {
        // this is NOT for display: this MUST be the exact required string
        switch (this)
        {
            case Ytdlp:
                return "brew install yt-dlp";
            case AcousticID_chromaprint:
                return "brew install chromaprint";
            case ImageMagick:
                return "brew install imagemagick";
            case FFmpeg:
                // super important: javafx audio i.e. the audio player REQUIRES ffmpeg
            {
                TextInputDialog dialog = new TextInputDialog("");
                Look_and_feel_manager.set_dialog_look(dialog,owner,logger);
                dialog.initOwner(owner);
                dialog.setWidth(800);
                PasswordField pwf = new PasswordField();
                dialog.getDialogPane().getChildren().add(pwf);
                dialog.setHeaderText("Sudo password required to install ffmpeg ");
                dialog.setContentText("the command to be executed is: 'echo 'password' | sudo -S apt install ffmpeg");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    String password = pwf.getText();
                    dialog.close();
                    return "echo "+password+"sudo -S apt install ffmpeg";
                }
                dialog.close();
            }
            case Vips:
                return "brew install vips";
            case GraphicsMagick:
                return "brew install graphicsmagick";
            case MediaInfo:
                return "brew install mediainfo";
            default:
                return null;
        }
    }
}
