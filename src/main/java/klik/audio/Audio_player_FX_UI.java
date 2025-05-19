package klik.audio;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import klik.actor.Aborter;
import klik.browser.Drag_and_drop;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Popups;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


//**********************************************************
public class Audio_player_FX_UI implements Song_adding_receiver
//**********************************************************
{
    private static final boolean dbg =  false;
    public static final int WIDTH = 500;
    public static final String AUDIO_PLAYER = "AUDIO_PLAYER";
    private static final String PAUSE = "Pause";
    private static final String PLAY = "Play";
    Stage stage;
    VBox the_vertical_box;
    VBox the_equalizer_vbox =  new VBox();
    HBox the_equalizer_hbox = new HBox();
    HBox the_sound_control_hbox = new HBox();
    Button Save_Playlist_Under_A_New_Name;
    Button next;
    Button previous;
    Button play_pause;
    Slider balance_slider;
    Slider the_timeline_slider;
    Label now_value;
    Label the_status_label;
    Label duration_value;
    ScrollPane scroll_pane;
    Logger logger;
    Aborter aborter;
    private volatile Optional<MediaPlayer> the_media_player_option = Optional.empty();
    private volatile Optional<AudioEqualizer> the_equalizer_option = Optional.empty();
    private ObservableList<EqualizerBand> equalizer_bands;

    double volume = 0.5;
    double balance = 0.0;
    Label playlist_name;

    String pause_string;
    String play_string;
    static final boolean keyword_dbg = true;

    // STATE:
    Playlist playlist;

    //**********************************************************
    Audio_player_FX_UI(Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.aborter= aborter;
        this.logger = logger;
        stage = new Stage();
        playlist = new Playlist(this,logger);
        Rectangle2D r = Non_booleans.get_window_bounds(AUDIO_PLAYER);
        stage.setX(r.getMinX());
        stage.setY(r.getMinY());
        stage.setWidth(r.getWidth());
        stage.setHeight(r.getHeight());
        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            //if ( dbg) logger.log("ChangeListener: image window position and/or size changed");
            Rectangle2D b = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Non_booleans.save_window_bounds(stage,AUDIO_PLAYER,logger);
        };
        stage.xProperty().addListener(change_listener);
        stage.yProperty().addListener(change_listener);
        stage.widthProperty().addListener(change_listener);
        stage.heightProperty().addListener(change_listener);
        stage.setMinWidth(WIDTH);
        VBox the_top_vbox = new VBox();
        Look_and_feel_manager.set_region_look(the_top_vbox);

        VBox duration_vbox = define_duration_vbox();
        the_top_vbox.getChildren().add(duration_vbox);
        volume_and_balance(the_top_vbox);

        the_top_vbox.getChildren().add(define_playlist_hbox());


        ScrollPane scroll_pane = define_scrollpane_with_songs();

        pause_string = My_I18n.get_I18n_string(PAUSE,logger);
        play_string = My_I18n.get_I18n_string(PLAY,logger);


        // called only on EXTERNAL close requests i.e. hitting the cross in the title
        stage.setOnCloseRequest(windowEvent -> {
            logger.log("Audio player closing");
            clean_up();
            stage.close();
            Audio_player.instance = null;
        });

        playlist.add_listener();
        BorderPane bottom_border_pane = define_bottom_pane(the_top_vbox, scroll_pane);

        Scene scene = new Scene(bottom_border_pane);
        stage.setScene(scene);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> handle_keyboard( keyEvent, logger));
        stage.show();

        playlist.init();

    }


    //**********************************************************
    public void change_song(File song)
    //**********************************************************
    {
        playlist.change_song(song);
    }

    //**********************************************************
    private BorderPane define_bottom_pane(Pane top_pane, ScrollPane scroll_pane)
    //**********************************************************
    {
        BorderPane returned = new BorderPane();
        returned.setTop(top_pane);
        returned.setCenter(scroll_pane);

        VBox the_status_bar = new VBox();

        the_status_label = new Label("Status: OK");
        the_status_label.prefWidth(1000);
        the_status_label.setWrapText(true);
        Look_and_feel_manager.set_region_look(the_status_label);
        the_status_bar.getChildren().add(the_status_label);
        returned.setBottom(the_status_bar);
        Look_and_feel_manager.set_region_look(returned);
        return returned;
    }

    //**********************************************************
    private VBox define_duration_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();
        Look_and_feel_manager.set_region_look(returned);
        HBox h1 = define_duration_hbox();
        returned.getChildren().add(h1);

        define_timeline_slider();
        returned.getChildren().add(the_timeline_slider);

        Look_and_feel_manager.set_button_look(returned,true);
        return returned;
    }

    //**********************************************************
    private HBox define_playlist_hbox()
    //**********************************************************
    {
        HBox returned = new HBox();
        Look_and_feel_manager.set_region_look(returned);


        {

            Button remove_from_playlist = new Button(My_I18n.get_I18n_string("Remove_playing_song_from_playlist",logger));
            Look_and_feel_manager.set_button_look(remove_from_playlist, true);
            remove_from_playlist.setOnAction(_ -> playlist.remove_from_playlist_and_jump_to_next());
            returned.getChildren().add(remove_from_playlist);
        }
        {

            Button undo_remove = new Button(My_I18n.get_I18n_string("Undo_Remove",logger));
            Look_and_feel_manager.set_button_look(undo_remove, true);
            undo_remove.setOnAction(_ -> playlist.undo_remove());
            returned.getChildren().add(undo_remove);
        }
        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            returned.getChildren().add(spacer);
        }

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            returned.getChildren().add(spacer);
        }

        playlist_name = new Label(My_I18n.get_I18n_string("Name_Of_Playlist",logger)+" : "+playlist.get_playlist_name());
        playlist_name.setMinWidth(200);
        Look_and_feel_manager.set_region_look(playlist_name);
        returned.getChildren().add(playlist_name);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            returned.getChildren().add(spacer);
        }

        Save_Playlist_Under_A_New_Name = new Button(My_I18n.get_I18n_string("Save_Playlist_Under_A_New_Name",logger));
        {
            Save_Playlist_Under_A_New_Name.setOnAction(actionEvent -> save_new_playlist());
        }
        Look_and_feel_manager.set_button_look(Save_Playlist_Under_A_New_Name,true);
        returned.getChildren().add(Save_Playlist_Under_A_New_Name);

        return returned;
    }

    //**********************************************************
    private Button define_landing_zone_button()
    //**********************************************************
    {
        //Button landing_zone = new Button(My_I18n.get_I18n_string("Drop new music files or folders here",logger));
        Button landing_zone = new Button("Drop new music files or folders here");
        Look_and_feel_manager.set_button_look(landing_zone, true);
        landing_zone.setMinHeight(100);
        BackgroundFill background_fill = new BackgroundFill(Color.LIGHTCORAL, CornerRadii.EMPTY, Insets.EMPTY);
        landing_zone.setBackground(new Background(background_fill));

        landing_zone.setOnDragEntered(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragEntered RECEIVER SIDE" );
            Look_and_feel_manager.set_background_for_setOnDragEntered(landing_zone,logger);
            drag_event.consume();
        });
        landing_zone.setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragExited RECEIVER SIDE");
            Look_and_feel_manager.set_background_for_setOnDragExited(landing_zone,logger);
            drag_event.consume();
        });
        landing_zone.setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragOver RECEIVER SIDE");
            drag_event.acceptTransferModes(TransferMode.MOVE);
            Look_and_feel_manager.set_background_for_setOnDragOver(landing_zone,logger);
            drag_event.consume();
        });
        landing_zone.setOnDragDropped(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragDropped RECEIVER SIDE");

            Object source = drag_event.getGestureSource();
            if (source == null)
            {
                logger.log(("WARNING: accept_drag_dropped_as_a_move_in, cannot check for stupid move because the event's source is null: " + drag_event.getSource()));
            }
            else
            {
                if (source == landing_zone) {
                    logger.log("source is excluded: cannot drop onto itself");
                    drag_event.consume();
                    return;
                }
            }

            Dragboard dragboard = drag_event.getDragboard();
            List<File> the_list = new ArrayList<>();
            String s = dragboard.getString();
            if (s == null) {
                logger.log( "dragboard.getString()== null");
            }
            else
            {
                logger.log(" drag ACCEPTED for STRING:->" + s+ "<-");
                for (String ss : s.split("\\r?\\n"))
                {
                    if (ss.isBlank()) continue;
                    logger.log(" drag ACCEPTED for additional file: " + ss);
                    the_list.add(new File(ss));
                }
                if (the_list.isEmpty())
                {
                    logger.log(" drag list is empty ? " + s);
                }
            }
            {
                List<File> l = dragboard.getFiles();
                for (File fff : l)
                {
                    logger.log("... drag ACCEPTED for file= " + fff.getAbsolutePath());
                    if ( !the_list.contains(fff) )  the_list.add(fff);
                }
            }

            playlist.add(the_list, stage, stage.getX()+100, stage.getY()+100);

            // tell the source
            drag_event.setDropCompleted(true);
            drag_event.consume();
        });
        return landing_zone;
    }


    //**********************************************************
    private void volume_and_balance(VBox the_big_vbox)
    //**********************************************************
    {
        the_sound_control_hbox.getChildren().add(define_volume_and_balance_vbox());
        the_sound_control_hbox.getChildren().add(the_equalizer_vbox);
        the_sound_control_hbox.getChildren().add(define_jump_vbox());
        the_sound_control_hbox.getChildren().add(define_the_landing_zone_vbox());
        the_big_vbox.getChildren().add(the_sound_control_hbox);

    }

    //**********************************************************
    private VBox define_the_landing_zone_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();
        Button landing_zone = define_landing_zone_button();
        returned.getChildren().add(landing_zone);
        return returned;
    }

    //**********************************************************
    private VBox define_volume_and_balance_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();

        VBox volume_vbox = define_volume_vbox();
        returned.getChildren().add(volume_vbox);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer);
            VBox.setVgrow(spacer, Priority.ALWAYS);
            returned.getChildren().add(spacer);
        }

        VBox balance_vbox = define_balance_vbox();
        returned.getChildren().add(balance_vbox);


        return returned;
    }

    //**********************************************************
    private VBox define_balance_vbox()
    //**********************************************************
    {
        VBox balance_vbox= new VBox();
        HBox h1 = define_balance_hbox();
        balance_vbox.getChildren().add(h1);
        Button b = define_reset_balance_button();
        balance_vbox.getChildren().add(b);

        Look_and_feel_manager.set_button_look(balance_vbox,true);
        return balance_vbox;

    }
    //**********************************************************
    private HBox define_balance_hbox()
    //**********************************************************
    {
        // balance hbox
        HBox balance_hbox = new HBox();
        Label label = new Label("Balance");
        balance_hbox.getChildren().add(label);

        balance_slider = new Slider(-1.0, 1.0, 0.0);
        balance_slider.setMinWidth(30);
        balance_hbox.getChildren().add(balance_slider);
        balance_slider.valueProperty().addListener((observableValue, number, t1) -> {
            if (the_media_player_option.isEmpty()) return;
            balance = balance_slider.getValue();
            the_media_player_option.get().setBalance(balance);
        });

        return balance_hbox;
    }


    //**********************************************************
    private Button define_reset_balance_button()
    //**********************************************************
    {
        Button reset_balance = new Button(My_I18n.get_I18n_string("Reset_Balance",logger));
        Look_and_feel_manager.set_button_look(reset_balance,true);
        reset_balance.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                balance_slider.setValue(0);
                the_media_player_option.get().setBalance(0);
            }
        });
        return reset_balance;
    }

    //**********************************************************
    private VBox define_volume_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();
        HBox hbox = define_volume_hbox();
        returned.getChildren().add(hbox);

        Button mute = new Button(My_I18n.get_I18n_string("Mute",logger));
        Look_and_feel_manager.set_button_look(mute,true);
        mute.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                MediaPlayer mp = the_media_player_option.get();
                if ( mp.isMute())
                {
                    mp.setMute(false);
                    mute.setText("Mute");
                }
                else
                {
                    mp.setMute(true);
                    mute.setText(My_I18n.get_I18n_string("Unmute",logger)); //"Unmute");
                }
            }
        });
        returned.getChildren().add(mute);
        Look_and_feel_manager.set_button_look(returned,true);

        return returned;
    }

    //**********************************************************
    private HBox define_volume_hbox()
    //**********************************************************
    {
        HBox volume_hbox = new HBox();

        ImageView iv = new ImageView(Look_and_feel_manager.get_speaker_icon());
        iv.setFitHeight(20);
        iv.setFitWidth(20);
        volume_hbox.getChildren().add(iv);

        Slider volume_slider = new Slider(0, 1, 0.5);
        volume_slider.setMinWidth(30);
        volume_hbox.getChildren().add(volume_slider);
        volume_slider.valueProperty().addListener((observableValue, number, t1) -> {
            if (the_media_player_option.isEmpty()) return;
            volume = volume_slider.getValue();
            the_media_player_option.get().setVolume(volume);
        });
        return volume_hbox;
    }

    //**********************************************************
    private VBox define_jump_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();
        Look_and_feel_manager.set_region_look(returned);

        previous = new Button(My_I18n.get_I18n_string("Jump_To_Previous_Song",logger));
        Look_and_feel_manager.set_button_look(previous, true);
        previous.setOnAction(_ -> playlist.jump_to_previous());
        returned.getChildren().add(previous);


        next = new Button(My_I18n.get_I18n_string("Jump_To_Next_Song",logger));
        Look_and_feel_manager.set_button_look(next, true);
        next.setOnAction(_ -> playlist.jump_to_next());
        returned.getChildren().add(next);


        Button shuffle = new Button(My_I18n.get_I18n_string("Shuffle",logger));
        Look_and_feel_manager.set_button_look(shuffle, true);
        shuffle.setOnAction(_->FXCollections.shuffle(playlist.observable_playlist));
        returned.getChildren().add(shuffle);


        return returned;
    }




    //**********************************************************
    private ScrollPane define_scrollpane_with_songs()
    //**********************************************************
    {
        scroll_pane = new ScrollPane();
        Look_and_feel_manager.set_region_look(scroll_pane);
        scroll_pane.addEventFilter(KeyEvent.KEY_PRESSED, key_event -> {
            logger.log("trapping event "+key_event);
            switch (key_event.getCode())
            {
            case UP:
                logger.log("handle event: UP");
                playlist.jump_to_previous();
                key_event.consume(); // prevent default key handling
                break;
            case DOWN:
                logger.log("handle event: DOWN");
                playlist.jump_to_next();
                key_event.consume(); // prevent default key handling
                break;
            }
        });

        Look_and_feel_manager.set_region_look(scroll_pane);
        scroll_pane.setPrefSize(WIDTH, 600);
        the_vertical_box = new VBox();
        scroll_pane.setContent(the_vertical_box);

        scroll_pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll_pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll_pane.setPrefHeight(3000);

        return scroll_pane;
    }

    //**********************************************************
    private void define_timeline_slider()
    //**********************************************************
    {
        the_timeline_slider = new Slider();
        //image_viewerLook_and_feel_manager.set_region_look(the_timeline_slider);
        the_timeline_slider.setMinWidth(WIDTH);
        the_timeline_slider.setPrefWidth(WIDTH);
        // but the user may click/slide the slider
        the_timeline_slider.setOnMouseReleased(event -> {
            if (the_media_player_option.isEmpty()) return;
            the_timeline_slider.setValueChanging(true);
            double v = event.getX() / the_timeline_slider.getWidth() * the_timeline_slider.getMax();
            the_timeline_slider.setValue(v);
            //logger.log("player seeking: slider new value = "+slider.getValue());
            Duration target = Duration.seconds(v);
            //logger.log("player seeking:"+target);
            the_media_player_option.get().seek(target);
            the_timeline_slider.setValueChanging(false);
        });
    }

    //**********************************************************
    private HBox define_duration_hbox()
    //**********************************************************
    {
        HBox hbox = new HBox();
        Look_and_feel_manager.set_region_look(hbox);
        Label duration_text = new Label(My_I18n.get_I18n_string("Duration",logger)+" : ");
        Look_and_feel_manager.set_region_look(duration_text);
        hbox.getChildren().add(duration_text);
        duration_value = new Label("0.0s");
        Look_and_feel_manager.set_region_look(duration_value);
        hbox.getChildren().add(duration_value);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        Button rewind = new Button(My_I18n.get_I18n_string("Rewind",logger));
        Look_and_feel_manager.set_button_look(rewind, true);
        rewind.setOnAction(_ -> {
            rewind();
        });
        hbox.getChildren().add(rewind);


        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        play_pause = new Button(pause_string);
        Look_and_feel_manager.set_button_look(play_pause, true);
        play_pause.setOnAction(_ -> {
            toggle_play_stop();
        });
        hbox.getChildren().add(play_pause);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        Label now_text = new Label(My_I18n.get_I18n_string("Now",logger)+" : ");
        Look_and_feel_manager.set_region_look(now_text);
        hbox.getChildren().add(now_text);
        now_value = new Label("0.0s");
        Look_and_feel_manager.set_region_look(now_value);

        hbox.getChildren().add(now_value);
        return hbox;
    }

    @Override
    public void play_song_with_new_media_player(File new_song, Integer current_time_s)
    {
        String encoded;
        try
        {
            encoded = "file://" + new_song.getCanonicalPath();
        }
        catch (IOException e)
        {
            logger.log("\n\nFATAL: " + e);
            return;
        }
        encoded = encoded.replaceAll(" ", "%20");

        try
        {
            Media sound = new Media(encoded);
            MediaPlayer tmp_media_player = new MediaPlayer(sound);



            tmp_media_player.setCycleCount(1);
            tmp_media_player.setOnStalled(() -> logger.log("\n\nWARNING player is stalling !!"));
            tmp_media_player.setOnReady(() -> on_player_ready(tmp_media_player));
            tmp_media_player.setOnPlaying(() -> {
                if ( current_time_s != null)
                {
                    //logger.log(" OnPlaying, jumping: "+ current_time_s);
                    Duration target = Duration.seconds(current_time_s);
                    the_media_player_option.get().seek(target);
                }});

            play_pause.setText(pause_string);

        }
        catch (MediaException me)
        {
            logger.log(Stack_trace_getter.get_stack_trace(me.toString()));
            //logger.log((me.toString()));
            playlist.remove_from_playlist_and_jump_to_next();
        }
        catch (IllegalArgumentException e)
        {
            Popups.popup_Exception(e, 256, "Fatal", logger);
            playlist.remove_from_playlist_and_jump_to_next();
        }
        stage.show();
    }

    @Override
    public void set_playlist_name_display(String new_playlist_name)
    {
        playlist_name.setText(new_playlist_name);
    }

    //**********************************************************
    private void on_player_ready(MediaPlayer new_media_player)
    //**********************************************************
    {
        if ( the_media_player_option.isPresent())
        {
            the_media_player_option.get().stop();
            the_media_player_option.get().dispose();
        }
        the_media_player_option = Optional.of(new_media_player);
        the_equalizer_option = Optional.of(new_media_player.getAudioEqualizer());
        equalizer_bands = the_equalizer_option.get().getBands();
        //logger.log("\n\nequalizer init, bands= "+ equalizer_bands.size());

        define_equalizer();

        the_media_player_option.get().setOnEndOfMedia(() -> playlist.jump_to_next());

        // the player pilots how the slider moves during playback
        the_media_player_option.get().currentTimeProperty().addListener((ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) -> {
            if (dbg) logger.log("player changing current time:"+newValue.toSeconds());
            double seconds = newValue.toSeconds();
            the_timeline_slider.setValue(seconds);
            now_value.setText((int)seconds+" seconds");
            if ( seconds > 20) Non_booleans.save_curent_time_in_song((int)seconds);
        });

        //logger.log("song start:"+ player.getStartTime().toSeconds());
        //logger.log("song stop:"+ player.getStopTime().toSeconds());
        double seconds = the_media_player_option.get().getStopTime().toSeconds();
        the_timeline_slider.setValue(0);
        the_timeline_slider.setMax(seconds);
        the_media_player_option.get().setVolume(volume);
        the_media_player_option.get().setBalance(balance);
        the_media_player_option.get().play();
        String s = (int)seconds+" seconds";
        if ( seconds > 3600)
        {
            int h = (int)seconds/3600;
            int m = ((int)seconds - h*3600)/60;
            double ss = seconds - h*3600 - m*60;
            ss *= 10;
            ss = (double)((int)ss)/10.0;
            s = h+ " hours, "+m+" minutes, "+ ss+ " seconds";
        }
        else if ( seconds > 60)
        {
            int m = (int)seconds/60;
            double ss = seconds - m*60;
            ss *= 10;
            ss = (double)((int)ss)/10.0;
            s = m+" minutes, "+ ss+ " seconds";
        }
        duration_value.setText(s);


    }

    boolean equalizer_created = false;
    List<Slider> sliders = new ArrayList<>();
    List<ChangeListener<? super Number> > listeners = new ArrayList<>();
    //**********************************************************
    private void define_equalizer()
    //**********************************************************
    {
        if ( equalizer_created)
        {
            for ( int i = 0; i < sliders.size(); i++)
            {
                Slider s = sliders.get(i);
                equalizer_bands.get(i).setGain(s.getValue());
                int finalI = i;
                ChangeListener<? super Number>  listener = listeners.get(i);
                s.valueProperty().removeListener((ChangeListener<? super Number>) listener);

                listener = (ov, old_val_, new_val_) -> {
                    double slider = new_val_.doubleValue();
                    equalizer_bands.get(finalI).setGain(slider);
                };
                s.valueProperty().addListener(listener);
                listeners.add(listener);
            }
        }
        else
        {
            double MIN = -24;
            double MAX = 12;
            double DEFAULT = 0;
            double LEFT= 20;
            double y= 100;
            int how_many_rectangles = equalizer_bands.size();
            double w = 10;
            double x = LEFT;
            for (int i = 0; i < how_many_rectangles; i++)
            {
                Slider s = new Slider(MIN, MAX, DEFAULT);
                s.setMinHeight(100);
                s.setMinWidth(30);
                s.setOrientation(Orientation.VERTICAL);
                x += w;
                the_equalizer_hbox.getChildren().add(s);
                int finalI = i;
                ChangeListener<? super Number>  listener = (ov, old_val_, new_val_) -> {
                    double slider = new_val_.doubleValue();
                    equalizer_bands.get(finalI).setGain(slider);
                };
                listeners.add(listener);
                s.valueProperty().addListener(listener);
                sliders.add(s);
            }
            equalizer_created = true;
            the_equalizer_vbox.getChildren().add(the_equalizer_hbox);
            Button reset_equalizer_button = make_reset_equalizer_button();
            the_equalizer_vbox.getChildren().add(reset_equalizer_button);
            Look_and_feel_manager.set_button_look(the_equalizer_vbox,true);
        }
    }

    //**********************************************************
    private Button make_reset_equalizer_button()
    //**********************************************************
    {
        Button reset_button = new Button(My_I18n.get_I18n_string("Reset_Equalizer",logger));
        Look_and_feel_manager.set_button_look(reset_button,true);
        reset_button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent)
            {
                for (int i = 0; i < equalizer_bands.size(); i++)
                {
                    equalizer_bands.get(i).setGain(0.0);
                    sliders.get(i).setValue(0.0);
                }
            }
        });
        return reset_button;
    }


    //**********************************************************
    @Override
    public void clean_up()
    //**********************************************************
    {
        if (the_media_player_option.isPresent())
        {
            the_media_player_option.get().stop();
            the_media_player_option.get().dispose();
            the_media_player_option = Optional.empty();
        }

    }

    @Override
    public void set_title(String s)
    {
        stage.setTitle(s);
    }


    //**********************************************************
    private void save_new_playlist()
    //**********************************************************
    {
        // need to do it on swing event thread
        // as we use fileChooser to be able to display hidden folders
        Runnable r = () -> playlist.choose_playlist_file_name(stage);
        SwingUtilities.invokeLater(r);
    }



    //**********************************************************
    public void play_playlist_internal(File playlist_file_)
    //**********************************************************
    {
        if ( Non_booleans.get_main_properties_manager().get(Playlist.PLAYLIST_FILE_NAME) == null)
        {
            Non_booleans.get_main_properties_manager().add(Playlist.PLAYLIST_FILE_NAME, playlist_file_.getAbsolutePath());
        }
        //logger.log("Going to play list: "+playlist_file.getAbsolutePath());
        playlist.load_playlist(playlist_file_);
        if ( playlist.is_empty())
        {
            previous.setDisable(true);
            next.setDisable(true);

            logger.log("WARNING: playlist is empty !?");
            return;
        }
        previous.setDisable(false);
        next.setDisable(false);

        playlist.play_fist_song();

    }

    //**********************************************************
    void handle_keyboard(final KeyEvent key_event, Logger logger)
    //**********************************************************
    {

        if ( keyword_dbg) logger.log("Image_stage KeyEvent="+key_event);

        logger.log("Image_stage KeyEvent.code"+key_event.getCode());
        switch (key_event.getCode())
        {
            case F7:
                if ( keyword_dbg) logger.log("F7");
                rewind();
                break;
            case F8:
                if ( keyword_dbg) logger.log("F8");
                toggle_play_stop();
                break;
            case F9:
                if ( keyword_dbg) logger.log("F9");
                playlist.jump_to_next();
                break;

            case UP:
                if ( keyword_dbg) logger.log("UP");
                break;

            case DOWN:
                if ( keyword_dbg) logger.log("zoom down/out:");
                break;

            case LEFT:
                if ( keyword_dbg) logger.log("left");
                playlist.jump_to_previous();
                break;

            case SPACE:
                if ( keyword_dbg) logger.log("space");
            case RIGHT:
                if ( keyword_dbg) logger.log("right");
                playlist.jump_to_next();
                break;

            default:
                if ( keyword_dbg) logger.log("default");
                break;

        }

        if (key_event.getCode() == KeyCode.ESCAPE)
        {
            key_event.consume();
            return;
        }


        switch (key_event.getText())
        {
            case"=" -> {
                if (keyword_dbg) logger.log("=");
                return;
            }
        }

        key_event.consume();

    }

    //**********************************************************
    private void toggle_play_stop()
    //**********************************************************
    {
        if (the_media_player_option.isEmpty()) return;
        MediaPlayer mp = the_media_player_option.get();
        MediaPlayer.Status status = mp.getStatus();
        if ( status== MediaPlayer.Status.PLAYING) set_is_paused();
        else set_is_playing();
    }


    //**********************************************************
    private void rewind()
    //**********************************************************
    {
        if (the_media_player_option.isEmpty()) return;
        the_media_player_option.get().stop();
        set_is_playing();
    }


    //**********************************************************
    private void set_is_playing()
    //**********************************************************
    {
        the_media_player_option.get().play();
        play_pause.setText(pause_string);

    }

    //**********************************************************
    private void set_is_paused()
    //**********************************************************
    {
        the_media_player_option.get().pause();
        play_pause.setText(play_string);
    }

    @Override
    public void add(Button song)
    {
        Runnable r = () -> the_vertical_box.getChildren().add(song);
        Platform.runLater(r);
    }

    @Override
    public void remove(Button song)
    {
        the_vertical_box.getChildren().remove(song);
    }

    @Override
    public void scroll_to(File target)
    {
        double v = playlist.file_to_scroll(target);
        scroll_pane.setVvalue(v);
    }

    @Override
    public void set_status(String s)
    {
        the_status_label.setText(s);

    }
}
