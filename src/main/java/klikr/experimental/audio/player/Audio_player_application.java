// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.experimental.audio.player;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import klikr.Start_context;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Aborter;
import klikr.util.http.Klikr_communicator;
import klikr.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

//**********************************************************
public class Audio_player_application extends Application
//**********************************************************
{
    Klikr_communicator klikr_communicator;
    private final static String name = "Audio_player_application";
    Logger logger;
    Path current;

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
        klikr_communicator = new Klikr_communicator("Audio_player_app",stage_,logger);
        if (klikr_communicator.start_as_singleton())
        {

            Aborter aborter = new Aborter("new audio",logger);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac")
                    );
            File selectedFile = fileChooser.showOpenDialog(stage_);
            if (selectedFile != null) {
                current = selectedFile.toPath();
                The_audio_player.play_song_in_folder(this,current, stage_,logger);
            } else {
                current = null;
            }

            Application local_app = this;
            if ( reply_port != null) klikr_communicator.send_request(reply_port,"/started","POST","started");
            Consumer<String> on_appearance_changed = new Consumer<String>() {
                @Override
                public void accept(String s) {
                    The_audio_player.play_song_in_folder(local_app,Path.of(s), stage_,logger);
                    // path_list_provider;
                    //Window_builder.additional_no_past(this, Window_type.Song_playlist,path_list_provider, stage_, logger);
                    //Old_Audio_player_with_playlist.on_ui_changed(s,Shared_services.aborter(),stage_,logger);
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
