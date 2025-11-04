// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.work_in_progress.video;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import klik.util.log.Logger;

//**********************************************************
public class Video_player
//**********************************************************
{

    private static final boolean dbg =  false;
    public static final int WIDTH = 500;

     Stage stage = null;
     Slider slider;
     Label now_value;
     Label duration_value;
     Logger logger;
     String url;
     private volatile MediaPlayer the_media_player;
     static Video_player instance = null;



    //**********************************************************
    public static void play_movie(String url_, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new Video_player(logger);
        }
        instance.play(url_);

    }

    //**********************************************************
    private void play(String url_)
    //**********************************************************
    {

        clean_up();
        url = "https://youtu.be/c56t7upa8Bk";//url_;
        logger.log("video player play() "+url);

        stage.setTitle(url);

        url = url.replaceAll(" ","%20");
        //UrlEscapers.urlFragmentEscaper().escape("file://"+f.getCanonicalPath());

        Media sound = new Media( url  );
        MediaPlayer local = new MediaPlayer(sound);
        local.setCycleCount(1);
        local.setOnStalled(new Runnable(){
            public void run()
            {
                logger.log("\n\nWARNING player is stalling !!");
            }
        });
        local.setOnReady(new Runnable(){
            @Override
            public void run()
            {
                logger.log("video player READY!");
                on_player_ready(local);
            }
        });

        stage.show();
        //stage.setAlwaysOnTop(true);
    }

    private void on_player_ready(MediaPlayer local_) {

        the_media_player = local_;


        // the player pilots how the slider moves during playback
        the_media_player.currentTimeProperty().addListener((ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) -> {
            if (dbg) logger.log("player changing current time:"+newValue.toSeconds());
            double seconds = newValue.toSeconds();
            slider.setValue(seconds);
            now_value.setText((int)seconds+" seconds");
        });

        //logger.log("song start:"+ player.getStartTime().toSeconds());
        //logger.log("song stop:"+ player.getStopTime().toSeconds());
        double seconds = the_media_player.getStopTime().toSeconds();
        slider.setValue(0);
        slider.setMax(seconds);
        the_media_player.play();
        duration_value.setText((int)seconds+" seconds");


    }

    //**********************************************************
    public Video_player(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        stage = new Stage();
        stage.setMinWidth(WIDTH);
        VBox vbox = new VBox();
        HBox hb1 = new HBox();
        Label duration_text = new Label("Duration: ");
        hb1.getChildren().add(duration_text);
        duration_value = new Label("0.0s");
        hb1.getChildren().add(duration_value);
        vbox.getChildren().add(hb1);

        HBox hb2 = new HBox();
        Label now_text = new Label("Now: ");
        hb2.getChildren().add(now_text);
        now_value = new Label("0.0s");
        hb2.getChildren().add(now_value);
        vbox.getChildren().add(hb2);

        slider = new Slider();
        slider.setMinWidth(WIDTH);
        slider.setPrefWidth(WIDTH);
        vbox.getChildren().add(slider);




        // but the user may click/slide the slider
        slider.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if ( the_media_player == null) return;
                slider.setValueChanging(true);
                double v = event.getX()/slider.getWidth()*slider.getMax();
                slider.setValue(v);
                logger.log("player seeking: slider new value = "+slider.getValue());
                Duration target = Duration.seconds(v);
                logger.log("player seeking:"+target);
                the_media_player.seek(target);
                slider.setValueChanging(false);
            }
        });

        HBox hb3 = new HBox();
        vbox.getChildren().add(hb3);

        Button pause = new Button("Pause");
        hb3.getChildren().add(pause);
        Button restart = new Button("Restart");
        restart.setDisable(true);
        hb3.getChildren().add(restart);
        Button rewind = new Button("Rewind");
        hb3.getChildren().add(rewind);

        {

            pause.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if ( the_media_player == null) return;
                    the_media_player.pause();
                    pause.setDisable(true);
                    restart.setDisable(false);
                }
            });

            restart.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if ( the_media_player == null) return;
                    the_media_player.play();
                    pause.setDisable(false);
                    restart.setDisable(true);
                }
            });

            rewind.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if ( the_media_player == null) return;
                    the_media_player.stop();
                    the_media_player.play();
                    pause.setDisable(false);
                    restart.setDisable(true);
                }
            });
        }

        Button cancel = new Button("Stop & close");
        {
            cancel.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    clean_up();
                    stage.close();
                    instance = null;
                }
            });
        }
        vbox.getChildren().add(cancel);


        // this one is called only on EXTERNAL close requests
        // i.e. hitting the cross in the title
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                logger.log("stage setOnCloseRequest");
                clean_up();
                stage.close();
                instance = null;
            }
        });

        MediaView the_media_view = new MediaView(the_media_player);
        vbox.getChildren().add(the_media_view);
        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();
    }

    //**********************************************************
    private void clean_up()
    //**********************************************************
    {
        if ( the_media_player != null)
        {
            the_media_player.stop();
            the_media_player.dispose();
            the_media_player = null;
        }
    }
}
