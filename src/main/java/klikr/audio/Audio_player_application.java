// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.audio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import klikr.*;
import klikr.util.http.Klikr_communicator;
import klikr.util.log.Logger;
import klikr.util.tcp.*;

import java.util.function.Consumer;

//**********************************************************
public class Audio_player_application extends Application
//**********************************************************
{
    Klikr_communicator klikr_communicator;
    private final static String name = "Audio_player_application";
    Logger logger;
    //**********************************************************
    public static void main(String[] args) {launch(args);}
    //**********************************************************

    //**********************************************************
    @Override
    public void start(Stage stage_) throws Exception
    //**********************************************************
    {
        Shared_services.init(name, stage_);
        logger = Shared_services.logger();
        Start_context context = Start_context.get_context_and_args(this);

        Integer reply_port = context.extract_reply_port();
        klikr_communicator = new Klikr_communicator("Audio_player",stage_,logger);
        if (klikr_communicator.start_as_singleton())
        {
            Audio_player.init(true,context.extract_path(),stage_,logger);
            if ( reply_port != null) klikr_communicator.send_request(reply_port,"/started","POST","started");
            Consumer<String> on_appearance_changed = new Consumer<String>() {
                @Override
                public void accept(String s) {
                    Audio_player.on_ui_changed(s,Shared_services.aborter(),stage_,logger);
                }
            };
            klikr_communicator.set_on_appearance_changed(on_appearance_changed);
        }
        else
        {
            logger.log("AUDIO PLAYER: not starting!\n" +
                    "(reason: failed to start server)\n" +
                    "This is normal if the audio player is already running\n" +
                    "Since in general having 2 player playing is just cacophony :-)");
            if ( reply_port != null) klikr_communicator.send_request(reply_port,"/started","POST","not started");
            stage_.close();
            Platform.exit();
            System.exit(0);

        }
    }


}
