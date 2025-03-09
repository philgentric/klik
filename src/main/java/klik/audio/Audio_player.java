package klik.audio;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import klik.browser.icons.animated_gifs.Ffmpeg_utils;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.log.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static klik.properties.Static_application_properties.USER_HOME;

//**********************************************************
public class Audio_player
//**********************************************************
{
    private static final boolean dbg =  false;
    private static final String PLAYLIST_FILE_NAME = "PLAYLIST_FILE_NAME";
    public static final String PLAYLIST_EXTENSION = "klik_playlist";
    public static final int WIDTH = 500;
    public static final String AUDIO_PLAYER = "AUDIO_PLAYER";
    private static final String PAUSE = "Pause";
    private static final String PLAY = "Play";
    private static File playlist_file = null;
    File saving_dir = null;
    Stage stage;
    VBox the_equalizer_vbox =  new VBox();
    HBox the_equalizer_hbox = new HBox();
    HBox the_sound_control_hbox = new HBox();
    Button save_as_new_playlist;
    Button next;
    Button previous;
    Button play_pause;
    Slider balance_slider;
    boolean is_playing;
    Slider the_timeline_slider;
    Label now_value;
    Label duration_value;
    ObservableList<File> observable_playlist = FXCollections.observableArrayList();
    Map<File, Button> file_to_button;
    Button selected = null;
    ScrollPane scroll_pane;
    Logger logger;
    File the_song_file;
    private volatile Optional<MediaPlayer> the_media_player_option = Optional.empty();
    private volatile Optional<AudioEqualizer> the_equalizer_option = Optional.empty();
    private ObservableList<EqualizerBand> equalizer_bands;

    double volume = 0.5;
    double balance = 0.0;
    static Audio_player instance = null;
    Label play_list_name;

    //**********************************************************
    private Audio_player(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        stage = new Stage();
        Rectangle2D r = Static_application_properties.get_window_bounds(AUDIO_PLAYER,logger);
        stage.setX(r.getMinX());
        stage.setY(r.getMinY());
        stage.setWidth(r.getWidth());
        stage.setHeight(r.getHeight());
        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            if ( dbg) logger.log("ChangeListener: image window position and/or size changed");
            Rectangle2D b = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Static_application_properties.save_window_bounds(stage,AUDIO_PLAYER,logger);
        };
        stage.xProperty().addListener(change_listener);
        stage.yProperty().addListener(change_listener);
        stage.widthProperty().addListener(change_listener);
        stage.heightProperty().addListener(change_listener);
        stage.setMinWidth(WIDTH);
        VBox the_big_vbox = new VBox();

        VBox duration_vbox = define_duration_vbox();
        the_big_vbox.getChildren().add(duration_vbox);
        volume_and_balance(the_big_vbox);
        the_big_vbox.getChildren().add(define_playlist_hbox());
        define_scrollpane_with_songs(the_big_vbox);




        // called only on EXTERNAL close requests i.e. hitting the cross in the title
        stage.setOnCloseRequest(windowEvent -> {
            logger.log("Audio player closing");
            clean_up();
            stage.close();
            instance = null;
        });

        Scene scene = new Scene(the_big_vbox);
        stage.setScene(scene);
        stage.show();

    }

    //**********************************************************
    private VBox define_duration_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();
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


        {

            Button remove_from_playlist = new Button("Remove active song from playlist ");
            Look_and_feel_manager.set_button_look(remove_from_playlist, true);
            remove_from_playlist.setOnAction(actionEvent -> remove_from_playlist());
            returned.getChildren().add(remove_from_playlist);
        }
        {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            returned.getChildren().add(spacer);
        }
        playlist_file = get_playlist_file(logger);
        //logger.log("playlist_file="+playlist_file.getAbsolutePath());
        String play_list_name_s = extract_playlist_name();
        //logger.log("playlist_name="+play_list_name_s);

        play_list_name = new Label(play_list_name_s);
        play_list_name.setMinWidth(200);
        Look_and_feel_manager.set_region_look(play_list_name);
        returned.getChildren().add(play_list_name);

        save_as_new_playlist = new Button("Save as new playlist");
        {
            save_as_new_playlist.setOnAction(actionEvent -> save_new_playlist());
        }
        Look_and_feel_manager.set_button_look(save_as_new_playlist,true);
        returned.getChildren().add(save_as_new_playlist);

        return returned;
    }

    //**********************************************************
    private void line_with_stop_and_close(VBox vbox)
    //**********************************************************
    {
        Button cancel = new Button("Stop & close");
        Look_and_feel_manager.set_button_look(cancel,true);
        {
            cancel.setOnAction(actionEvent -> {
                clean_up();
                stage.close();
                instance = null;
            });
        }
        vbox.getChildren().add(cancel);
    }




    //**********************************************************
    private void volume_and_balance(VBox the_big_vbox)
    //**********************************************************
    {
        the_sound_control_hbox.getChildren().add(define_volume_and_balance_vbox());
        the_sound_control_hbox.getChildren().add(the_equalizer_vbox);
        the_sound_control_hbox.getChildren().add(define_jump_vbox());
        the_big_vbox.getChildren().add(the_sound_control_hbox);

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
        Button reset_balance = new Button("Reset balance");
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

        Button mute = new Button("Mute");
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
                    mute.setText("Unmute");
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

        previous = new Button("Jump to previous");
        Look_and_feel_manager.set_button_look(previous, true);
        previous.setOnAction(actionEvent -> jump_to_previous(the_song_file));
        returned.getChildren().add(previous);


        next = new Button("Jump to next ");
        Look_and_feel_manager.set_button_look(next, true);
        next.setOnAction(actionEvent -> jump_to_next(the_song_file));
        returned.getChildren().add(next);

        return returned;
    }




    //**********************************************************
    private void define_scrollpane_with_songs(VBox the_big_vbox)
    //**********************************************************
    {
        file_to_button = new HashMap<>();
        scroll_pane = new ScrollPane();
        scroll_pane.addEventFilter(KeyEvent.KEY_PRESSED, key_event -> {
            logger.log("trapping event "+key_event);
            key_event.consume(); // prevent default key handling
            switch (key_event.getCode())
            {
            case UP:
                logger.log("handle event: UP");
                jump_to_previous(the_song_file);
                break;
            case DOWN:
                logger.log("handle event: DOWN");
                jump_to_next(the_song_file);
                break;
            }
        });

        the_big_vbox.getChildren().add(scroll_pane);
        Look_and_feel_manager.set_region_look(scroll_pane);
        scroll_pane.setPrefSize(WIDTH, 600);
        VBox vb = new VBox();
        scroll_pane.setContent(vb);

        scroll_pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll_pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll_pane.setPrefHeight(3000);
        observable_playlist.addListener(new ListChangeListener<File>() {
            @Override
            public void onChanged(Change<? extends File> change) {
                while ( change.next())
                {
                    for (File f : change.getRemoved())
                    {
                        Button b = file_to_button.get(f);
                        if ( b != null) vb.getChildren().remove(b);
                    }
                    for (File f : change.getAddedSubList())
                    {
                        Button b = new Button(f.getName());
                        b.setPrefWidth(2000);
                        vb.getChildren().add(b);
                        Look_and_feel_manager.set_button_look(b,false);
                        file_to_button.put(f,b);
                        b.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent actionEvent) {
                                play_song(f);
                                set_selected(f);
                            }
                        });
                    }
                }
            }
        });
    }

    //**********************************************************
    private void define_timeline_slider()
    //**********************************************************
    {
        the_timeline_slider = new Slider();
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
        Label duration_text = new Label("Duration: ");
        Look_and_feel_manager.set_region_look(duration_text);
        hbox.getChildren().add(duration_text);
        duration_value = new Label("0.0s");
        Look_and_feel_manager.set_region_look(duration_value);
        hbox.getChildren().add(duration_value);

        {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        Button rewind = new Button("Rewind");
        Look_and_feel_manager.set_button_look(rewind, true);
        rewind.setOnAction(actionEvent -> {
            if (the_media_player_option.isEmpty()) return;
            the_media_player_option.get().stop();
            the_media_player_option.get().play();
            set_is_playing();
        });
        hbox.getChildren().add(rewind);


        {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        play_pause = new Button(PAUSE);
        is_playing = false;
        Look_and_feel_manager.set_button_look(play_pause, true);
        play_pause.setOnAction(actionEvent -> {
            if (the_media_player_option.isEmpty()) return;
            if ( is_playing) set_is_paused();
            else set_is_playing();
        });
        hbox.getChildren().add(play_pause);

        {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        Label now_text = new Label("Now: ");
        Look_and_feel_manager.set_region_look(now_text);
        hbox.getChildren().add(now_text);
        now_value = new Label("0.0s");
        Look_and_feel_manager.set_region_look(now_value);

        hbox.getChildren().add(now_value);
        return hbox;
    }


    // entry #1
    //**********************************************************
    public static void play_song_new_process(File the_song_file, Logger logger)
    //**********************************************************
    {
        List<String> cmds = new ArrayList<>();
        logger.log("play_song_new_process()");

        cmds.add("gradle");// --args= "+the_song_file.getAbsolutePath());
        cmds.add("audio_player");// --args= "+the_song_file.getAbsolutePath());

        String path =  "--args=\""+the_song_file.getAbsolutePath()+"\"";
        cmds.add(path);


        //cmds.add("--args=\""+the_song_file.getAbsolutePath().replaceAll(" ", "\\ ")+"\"");
        StringBuilder sb = new StringBuilder();
        Execute_command.execute_command_list_no_wait(cmds,new File("."),20*1000,sb,logger);
        logger.log(sb.toString());

    }

    //**********************************************************
    public static void play_song_same_process(File the_song_file, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new Audio_player(logger);
        }
        if ( playlist_file == null)
        {
            playlist_file = get_playlist_file(logger);
        }
        instance.load_playlist(playlist_file);
        instance.play_song(the_song_file);
    }
    //**********************************************************
    public static void play_playlist(File file, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new Audio_player(logger);
        }
        playlist_file = file;
        set_playlist_file_name(file.getAbsolutePath(),logger);

        instance.play_playlist_internal(file);
    }

    //**********************************************************
    private void play_song(File the_song_file_)
    //**********************************************************
    {
        if ( the_song_file_ == null)
        {
            logger.log("FATAL: the_song_file_ is null");
            return;
        }
        double bitrate = Ffmpeg_utils.get_audio_bitrate(null,the_song_file_.toPath(),logger);
        logger.log("bitrate= "+bitrate);
        clean_up();
        the_song_file = the_song_file_;
        add_and_save_if_needed();
        stage.setTitle(the_song_file.getName() +"       bitrate= "+bitrate+" kb/s");

        String encoded;
        try {
            encoded = "file://"+the_song_file.getCanonicalPath();
        } catch (IOException e) {
            logger.log("\n\nFATAL: "+e);
            return;
        }
        encoded = encoded.replaceAll(" ","%20");

        try
        {
            Media sound = new Media(encoded);
            MediaPlayer tmp_media_player = new MediaPlayer(sound);
            tmp_media_player.setCycleCount(1);
            tmp_media_player.setOnStalled(() -> logger.log("\n\nWARNING player is stalling !!"));
            tmp_media_player.setOnReady(() -> on_player_ready(tmp_media_player));
        }
        catch (MediaException me)
        {
            //logger.log(Stack_trace_getter.get_stack_trace(me.toString()));
            logger.log((me.toString()));
            remove_from_playlist();
        }
        catch (IllegalArgumentException e)
        {
            Popups.popup_Exception(e,256,"Fatal",logger);
            remove_from_playlist();
        }
        stage.show();
    }

    //**********************************************************
    private void set_is_playing()
    //**********************************************************
    {
        is_playing = true;
        the_media_player_option.get().play();
        play_pause.setText(PAUSE);

    }

    //**********************************************************
    private void set_is_paused()
    //**********************************************************
    {
        is_playing = false;
        the_media_player_option.get().pause();
        play_pause.setText(PLAY);
    }

    //**********************************************************
    private void add_and_save_if_needed()
    //**********************************************************
    {
        for (File file : observable_playlist)
        {
            if (file.getAbsolutePath().equals(the_song_file.getAbsolutePath()))
            {
                set_selected(the_song_file);
                return;
            }
        }
        observable_playlist.add(the_song_file);
        set_selected(the_song_file);
        scroll_pane.setVvalue(1.0);           //1.0 means 100% at the bottom
        save();
    }

    //**********************************************************
    private void set_selected(File f)
    //**********************************************************
    {

        logger.log("set_selected "+f);
        Button b = file_to_button.get(f);
        if ( selected == b) return;
        if ( b!=null) b.setStyle("-fx-background-color: #90D5FF");
        else logger.log("wtf1");
        if ( selected != null)
        {
            logger.log("resetting background for previously selected");
            selected.setStyle("-fx-background-color: #ffffff");
            //selected.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        }
        selected = b;

        //b.setBackground(new Background(new BackgroundFill(Color.SKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));

    }

    //**********************************************************
    private void on_player_ready(MediaPlayer local_)
    //**********************************************************
    {
        if ( the_media_player_option.isPresent())
        {
            the_media_player_option.get().stop();
            the_media_player_option.get().dispose();
        }
        the_media_player_option = Optional.of(local_);
        the_equalizer_option = Optional.of(local_.getAudioEqualizer());
        equalizer_bands = the_equalizer_option.get().getBands();
        logger.log("\n\nequalizer init, bands= "+ equalizer_bands.size());

        define_equalizer();

        the_media_player_option.get().setOnEndOfMedia(() -> jump_to_next(the_song_file));

        // the player pilots how the slider moves during playback
        the_media_player_option.get().currentTimeProperty().addListener((ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) -> {
            if (dbg) logger.log("player changing current time:"+newValue.toSeconds());
            double seconds = newValue.toSeconds();
            the_timeline_slider.setValue(seconds);
            now_value.setText((int)seconds+" seconds");
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
            int m = (int)seconds- h*3600;
            double ss = seconds- h*3600 - m*60;
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
        Button reset_button = new Button("Reset equalizer");
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
    private void remove_from_playlist()
    //**********************************************************
    {
        observable_playlist.remove(the_song_file);
        Button b = file_to_button.get(the_song_file);
        ((VBox)(scroll_pane.getContent())).getChildren().remove(b);
        jump_to_next(the_song_file);
        save();
    }

    //**********************************************************
    private String extract_playlist_name()
    //**********************************************************
    {
        return FilenameUtils.getBaseName(playlist_file.getName());
    }

    //**********************************************************
    private void clean_up()
    //**********************************************************
    {
        if (the_media_player_option.isPresent())
        {
            the_media_player_option.get().stop();
            the_media_player_option.get().dispose();
            the_media_player_option = Optional.empty();
        }

    }

    //**********************************************************
    private  int find_index(File ff)
    //**********************************************************
    {
        for (int k = 0; k < observable_playlist.size();k++)
        {
            if ( observable_playlist.get(k).getAbsolutePath().equals(ff.getAbsolutePath())) return k;
        }
        return -1;
    }

    //**********************************************************
    private  void jump_to_next(File f)
    //**********************************************************
    {
        if ( observable_playlist.isEmpty()) return;

        for (int i = 0; i < observable_playlist.size(); i++)
        {
            File file = observable_playlist.get(i);
            if ( file.getAbsolutePath().equals(f.getAbsolutePath()))
            {
                int k = i+1;
                if (k >= observable_playlist.size()) k = 0;
                File target = observable_playlist.get(k);
                set_selected(target);
                play_song(target);
                scroll_to(target);
                return;
            }
        }

    }
    //**********************************************************
    private  void jump_to_previous(File f)
    //**********************************************************
    {
        if ( observable_playlist.isEmpty()) return;
        for (int i = 0; i < observable_playlist.size(); i++)
        {
            File file = observable_playlist.get(i);
            if ( file.getAbsolutePath().equals(f.getAbsolutePath()))
            {
                int k = i-1;
                if (k < 0 ) k = observable_playlist.size()-1;
                File target = observable_playlist.get(k);
                set_selected(target);
                play_song(target);
                scroll_to(target);
                return;
            }
        }
    }

    //**********************************************************
    private void scroll_to(File target)
    //**********************************************************
    {
        double v = file_to_scroll(target);
        scroll_pane.setVvalue(v);
    }

    //**********************************************************
    double file_to_scroll(File f)
    //**********************************************************
    {
        for (int i = 0; i < observable_playlist.size(); i++)
        {
            File file = observable_playlist.get(i);
            if (file == f)
            {
                double returned = (double)i/(double)(observable_playlist.size()-1);
                logger.log(" scroll to "+i+" => "+returned);
                return  returned;
            }
        }
        return 1.0;
    }

    //**********************************************************
    private void save_new_playlist()
    //**********************************************************
    {
        // need to do it on swing event thread
        // as we use fileChooser to be able to display hidden folders
        Runnable r = () -> choose_playlist_file_name();
        SwingUtilities.invokeLater(r);
    }

    //**********************************************************
    private void choose_playlist_file_name()
    //**********************************************************
    {
        //logger.log("save_playlist");

        JFileChooser chooser = new JFileChooser();
        chooser.setFileHidingEnabled(false); // reason to use SWING !!!
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if ( saving_dir == null)
        {
            String home = System.getProperty(USER_HOME);
            saving_dir = new File(home,"playlists");
            if ( !saving_dir.exists())
            {
                if (!saving_dir.mkdir())
                {
                    logger.log("WARNING: creating directory failed for: "+saving_dir.getAbsolutePath());
                }
            }
        }
        chooser.setCurrentDirectory(saving_dir.getParentFile());
        chooser.setSelectedFile(saving_dir);
        int status = chooser.showOpenDialog(null);
        if (status != JFileChooser.APPROVE_OPTION) return;
        saving_dir = chooser.getSelectedFile();
        Platform.runLater(() -> choose_playlist_name());
    }

    //**********************************************************
    private  void choose_playlist_name()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog("playlistname");
        Look_and_feel_manager.set_dialog_look(dialog);
        dialog.initOwner(stage);
        dialog.setTitle("Choose a name for the playlist");
        dialog.setContentText("playlistname");

        // The Java 8 way to get the response value (with lambda expression).
        //result.ifPresent(name -> logger.log("Your name: " + name));
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            logger.log("playlist not saved");
            Popups.popup_warning(stage, "Not saved ", "plylist not saved", true, logger);
            return;
        }

        String playlist_name = result.get();
        play_list_name.setText(playlist_name);

        if ( !playlist_name.endsWith(PLAYLIST_EXTENSION)) playlist_name += "."+PLAYLIST_EXTENSION;

        playlist_file = new File(saving_dir,playlist_name);
        save();
    }

    //**********************************************************
    private void save()
    //**********************************************************
    {
        if ( playlist_file == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return;
        }
        //logger.log("Saving playlist as:" + playlist_file.getAbsolutePath());
        try
        {
            FileWriter fw = new FileWriter(playlist_file);
            for (File f : observable_playlist)
            {
                fw.write(f.getAbsolutePath() + "\n");
            }
            fw.close();
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("not saved"+e.toString()));
        }
    }




    //**********************************************************
    public void play_playlist_internal(File playlist_file_)
    //**********************************************************
    {
        //logger.log("Going to play list: "+playlist_file.getAbsolutePath());
        load_playlist(playlist_file_);
        if ( observable_playlist.isEmpty())
        {
            logger.log("WARNING: playlist is empty !?");
            return;
        }
        previous.setDisable(false);
        next.setDisable(false);
        File first = observable_playlist.get(0);
        play_song_same_process(first,logger);

    }

    //**********************************************************
    private void load_playlist(File playlist_file_)
    //**********************************************************
    {
        if ( playlist_file_ == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return;
        }
        try {
            BufferedReader br = new BufferedReader( new FileReader(playlist_file_));
            observable_playlist.clear();
            for(;;)
            {
                String song = br.readLine();
                if ( song == null ) break;
                observable_playlist.add(new File(song));
            }
            playlist_file = playlist_file_;
        } catch (FileNotFoundException e) {
            try {
                playlist_file.createNewFile();
            } catch (IOException ex) {
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            }
            if (playlist_file.canWrite())
            {
                play_list_name.setText(extract_playlist_name());
                return;
            }
            playlist_file = null;
            logger.log(Stack_trace_getter.get_stack_trace("cannot write"+e.toString()));
        } catch (IOException e) {
            playlist_file = null;
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
    }

    //**********************************************************
    public static File get_playlist_file(Logger logger)
    //**********************************************************
    {
        String playlist_file_name = Static_application_properties.get_main_properties_manager(logger).get(PLAYLIST_FILE_NAME);
        if ( playlist_file_name == null)
        {
            playlist_file_name = "playlist."+ Audio_player.PLAYLIST_EXTENSION;
            Static_application_properties.get_main_properties_manager(logger).add_and_save(PLAYLIST_FILE_NAME,playlist_file_name);
        }
        else
        {
            Path p = Path.of(playlist_file_name);
            if (p.isAbsolute()) return p.toFile();
        }
        String home = System.getProperty(USER_HOME);
        Path p = Paths.get(home, Static_application_properties.CONF_DIR, playlist_file_name);
        return p.toFile();
    }
    //**********************************************************
    public static void set_playlist_file_name(String playlist_file_name, Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_main_properties_manager(logger).add_and_save(PLAYLIST_FILE_NAME,playlist_file_name);
    }

}
