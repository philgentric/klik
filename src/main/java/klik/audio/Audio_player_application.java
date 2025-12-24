// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.audio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import klik.*;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.tcp.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

//**********************************************************
public class Audio_player_application extends Application
//**********************************************************
{
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

        if (  Audio_player.start_server(Shared_services.aborter(), stage_,logger))
        {
            Audio_player.init(true,context.extract_path(),stage_,logger);
        }
        else
        {
            logger.log("AUDIO PLAYER: not starting!\n" +
                    "(reason: failed to start server)\n" +
                    "This is normal if the audio player is already running\n" +
                    "Since in general having 2 player playing is just cacophony :-)");
            // send not_started to unblock the launcher server
            Integer reply_port = Audio_player.extract_reply_port(logger);
            if ( reply_port == null)
            {
                logger.log("AUDIO PLAYER: failed to get reply port");
                return;
            }
            // blocking call otherwise exit will prevent the reply from flying out
            TCP_client.send("127.0.0.1", reply_port, Launcher.NOT_STARTED, logger);

            stage_.close();
            Platform.exit();
            System.exit(0);

        }
    }


}
