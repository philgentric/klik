package klik;

import javafx.scene.control.Button;
import javafx.stage.Window;
import klik.look.Look_and_feel;
import klik.look.my_i18n.My_I18n;
import klik.util.execute.Execute_via_script_in_tmp_file;
import klik.util.log.Logger;
import klik.util.ui.Popups;

//**********************************************************
public enum External_application
//**********************************************************
{
    Ytdlp,
    ImageMagick,
    FFmpeg,
    GraphicsMagick,
    MediaInfo;


    //**********************************************************
    public Button get_button(
            double width, double icon_size,
            //VBox vbox,
            Look_and_feel look_and_feel, Window owner, Logger logger)
    //**********************************************************
    {
        Button b = new Button(My_I18n.get_I18n_string(get_I18n_key(), owner,logger));
        look_and_feel. set_Button_look(b, width, icon_size,null, owner,logger);
        b.setOnAction(e -> Execute_via_script_in_tmp_file.execute(get_command_string_to_install(owner,logger), true, owner, logger));
        return b;
    }


    //**********************************************************
    private String get_command_string_to_install(Window owner, Logger logger)
    //**********************************************************
    {
        if ( System.getProperty("os.name").toLowerCase().contains("mac")) {
            return "brew install "+get_brew_install_name();
        }
        Popups.popup_warning("Warning", "Sorry, this is implemented only for Mac.",
                false, owner, logger);
        return null;
    }


    //**********************************************************
    public String get_I18n_key()
    //**********************************************************
    {
        switch (this)
        {
            case Ytdlp:
                return "Install_Youtubedownloader";
            case ImageMagick:
                return "Install_Imagemagick";
            case FFmpeg:
                return "Install_Ffmpeg";
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
        switch (this)
        {
            case Ytdlp:
                return "yt-dlp";
            case ImageMagick:
                return "imagemagick";
            case FFmpeg:
                return "ffmpeg";
            case GraphicsMagick:
                return "graphicsmagick";
            case MediaInfo:
                return "mediainfo";
            default:
                return null;
        }
    }
}
