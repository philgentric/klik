// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.More_settings_stage;
import klik.util.execute.Execute_via_script_in_tmp_file;
import klik.util.execute.Guess_OS;
import klik.util.log.Logger;

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
        HBox hb = More_settings_stage.make_hbox_with_button_and_explanation(
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
    public String get_command_string_to_install(Window owner, Logger logger)
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
        return switch (this) {
            case Ytdlp -> "Install_Youtubedownloader";
            case AcousticID_chromaprint -> "Install_Fpcalc";
            case ImageMagick -> "Install_Imagemagick";
            case FFmpeg -> "Install_Ffmpeg";
            case Vips -> "Install_Vips";
            case GraphicsMagick -> "Install_Graphicsmagick";
            case MediaInfo -> "Install_Mediainfo";
        };
    }
    //**********************************************************
    public String get_macOS_install_command()
    //**********************************************************
    {
        // this is NOT for display: this MUST be th exact required string in:
        // brew install <REQUIRED_STRING>
        return switch (this) {
            case Ytdlp -> "brew install yt-dlp";
            case AcousticID_chromaprint -> "brew install chromaprint";
            case ImageMagick -> "brew install imagemagick";
            case FFmpeg -> "brew install ffmpeg";
            case Vips -> "brew install vips";
            case GraphicsMagick -> "brew install graphicsmagick";
            case MediaInfo -> "brew install mediainfo";
        };
    }

    //**********************************************************
    public String get_Windows_install_command()
    //**********************************************************
    {
        // this is NOT for display: this MUST be th exact required string in:
        // brew install <REQUIRED_STRING>
        return switch (this) {
            case Ytdlp -> "choco install yt-dlp -y";
            case AcousticID_chromaprint -> "choco install chromaprint -y";
            case ImageMagick -> "choco install imagemagick -y";
            case FFmpeg -> "choco install ffmpeg -y";
            case Vips -> "choco install vips -y";
            case GraphicsMagick -> "choco install graphicsmagick -y";
            case MediaInfo -> "choco install mediainfo -y";
        };
    }


    //**********************************************************
    public String get_Linux_install_command(Window owner, Logger logger)
    //**********************************************************
    {
        // this is NOT for display: this MUST be the exact required string
        return switch (this)
        {
            case Ytdlp -> "brew install yt-dlp";
            case AcousticID_chromaprint -> "brew install chromaprint";
            case ImageMagick -> "brew install imagemagick";
            case Vips -> "brew install vips";
            case GraphicsMagick -> "brew install graphicsmagick";
            case MediaInfo -> "brew install mediainfo";
            case FFmpeg -> special(owner,logger);
        };
    }

    private String special(Window owner, Logger logger) {
        {
            // super important: on Linux javaFX audio i.e. the audio player REQUIRES ffmpeg
            TextInputDialog dialog = new TextInputDialog("");
            Look_and_feel_manager.set_dialog_look(dialog,owner,logger);
            dialog.initOwner(owner);
            dialog.setWidth(1200);
            VBox vbox = new VBox();
            //PasswordField pwf = new PasswordField();
            //vbox.getChildren().add(pwf);
            Label l1 = new Label("Installing ffmpeg is required for audio playback (and more features)");
            vbox.getChildren().add(l1);
            Label l2 = new Label("The command is:");
            vbox.getChildren().add(l2);
            Label l3 = new Label("sudo apt install ffmpeg");
            vbox.getChildren().add(l3);
            dialog.getDialogPane().setContent(vbox);

            dialog.showAndWait();
            dialog.close();
        }
        return null;
    }
}
