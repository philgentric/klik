package klik.audio;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import klik.actor.Aborter;
import klik.browser.Drag_and_drop;
import klik.browser.icons.animated_gifs.Ffmpeg_utils;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.log.Stack_trace_getter;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static klik.audio.Audio_player.sanitize_file_name;
import static klik.properties.Non_booleans.USER_HOME;

//**********************************************************
public class Audio_player_frame
//**********************************************************
{
    private static final boolean dbg =  false;
    private static final String PLAYLIST_FILE_NAME = "PLAYLIST_FILE_NAME";
    public static final String KLIK_AUDIO_PLAYLIST_EXTENSION = "klik_audio_playlist";
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
    Button Save_Playlist_Under_A_New_Name;
    Button next;
    Button previous;
    Button play_pause;
    Slider balance_slider;
    Slider the_timeline_slider;
    Label now_value;
    Label duration_value;
    ObservableList<File> observable_playlist = FXCollections.observableArrayList();
    Map<File, Button> file_to_button;
    Button selected = null;
    ScrollPane scroll_pane;
    Logger logger;
    Aborter aborter;
    private volatile Optional<MediaPlayer> the_media_player_option = Optional.empty();
    private volatile Optional<AudioEqualizer> the_equalizer_option = Optional.empty();
    private ObservableList<EqualizerBand> equalizer_bands;

    double volume = 0.5;
    double balance = 0.0;
    Label playlist_name;
    //Browser browser = null;

    String pause_string;
    String play_string;
    static final boolean keyword_dbg = true;

    //main STATE:
    File the_song_file;
    Label the_status_label;

    //**********************************************************
    Audio_player_frame(Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.aborter= aborter;
        this.logger = logger;
        stage = new Stage();
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

        BorderPane bottom_border_pane = define_bottom_pane(the_top_vbox, scroll_pane);

        Scene scene = new Scene(bottom_border_pane);
        stage.setScene(scene);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> handle_keyboard( keyEvent, logger));
        stage.show();

        if (playlist_file == null)
        {
            playlist_file = get_playlist_file(aborter,logger);
        }
        load_playlist(playlist_file);
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
            remove_from_playlist.setOnAction(actionEvent -> remove_from_playlist_and_jump_to_next());
            returned.getChildren().add(remove_from_playlist);
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
        playlist_file = get_playlist_file(aborter,logger);
        //logger.log("playlist_file="+playlist_file.getAbsolutePath());
        String playlist_name_s = extract_playlist_name();
        logger.log("playlist_name="+playlist_name_s);

        playlist_name = new Label(My_I18n.get_I18n_string("Name_Of_Playlist",logger)+" : "+playlist_name_s);
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
        Button landing_zone = new Button(My_I18n.get_I18n_string("Drop new music files or folders here",logger));
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

            for ( File f : the_list)
            {
                if ( f.isDirectory())
                {
                    load_folder(f);
                }
                else
                {
                    f = sanitize_file_name(f,aborter,logger);
                    add_and_save_if_needed(f);
                }

            }
            save_observable_playlist();

            // tell the source
            drag_event.setDropCompleted(true);
            drag_event.consume();
        });
        return landing_zone;
    }

    //**********************************************************
    private void load_folder(File f)
    //**********************************************************
    {
        File[] files = f.listFiles();
        if ( files == null) return ;
        for (File ff : files)
        {
            if ( ff.isDirectory()) load_folder(ff);
            else
            {
                ff = sanitize_file_name(ff,aborter,logger);
                add_and_save_if_needed(ff);
            }
        }
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
        previous.setOnAction(_ -> jump_to_previous());
        returned.getChildren().add(previous);


        next = new Button(My_I18n.get_I18n_string("Jump_To_Next_Song",logger));
        Look_and_feel_manager.set_button_look(next, true);
        next.setOnAction(_ -> jump_to_next());
        returned.getChildren().add(next);


        Button shuffle = new Button(My_I18n.get_I18n_string("Shuffle",logger));
        Look_and_feel_manager.set_button_look(shuffle, true);
        shuffle.setOnAction(_->FXCollections.shuffle(observable_playlist));
        returned.getChildren().add(shuffle);


        return returned;
    }




    //**********************************************************
    private ScrollPane define_scrollpane_with_songs()
    //**********************************************************
    {
        file_to_button = new HashMap<>();
        scroll_pane = new ScrollPane();
        Look_and_feel_manager.set_region_look(scroll_pane);
        scroll_pane.addEventFilter(KeyEvent.KEY_PRESSED, key_event -> {
            logger.log("trapping event "+key_event);
            switch (key_event.getCode())
            {
            case UP:
                logger.log("handle event: UP");
                jump_to_previous();
                key_event.consume(); // prevent default key handling
                break;
            case DOWN:
                logger.log("handle event: DOWN");
                jump_to_next();
                key_event.consume(); // prevent default key handling
                break;
            }
        });

        Look_and_feel_manager.set_region_look(scroll_pane);
        scroll_pane.setPrefSize(WIDTH, 600);
        VBox the_vertical_box = new VBox();
        scroll_pane.setContent(the_vertical_box);

        scroll_pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll_pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll_pane.setPrefHeight(3000);
        observable_playlist.addListener(new ListChangeListener<File>() {
            @Override
            public void onChanged(Change<? extends File> change) {
                while ( change.next())
                {
                    for (File f : change.getRemoved())
                    {
                        Button b = file_to_button.get(f);
                        if ( b != null)
                        {
                            the_vertical_box.getChildren().remove(b);
                            file_to_button.remove(f);
                            save_observable_playlist();
                        }
                    }
                    for (File f : change.getAddedSubList())
                    {
                        Button b = new Button(f.getName());
                        b.setMnemonicParsing(false);
                        Look_and_feel_manager.set_button_look(b,false);
                        {
                            ContextMenu the_context_menu = new ContextMenu();
                            Look_and_feel_manager.set_context_menu_look(the_context_menu);
                            MenuItem the_menu_item = new MenuItem("Browse folder");
                            the_menu_item.setOnAction(event -> {
                                Path parent = f.toPath().getParent();
                                Audio_player.start_new_process_to_browse(parent,logger);
                            });
                            the_context_menu.getItems().add(the_menu_item);
                            b.setOnContextMenuRequested((ContextMenuEvent event) -> {
                                //if ( dbg)
                                    logger.log("show context menu of button:"+ f.toPath().toAbsolutePath());
                                the_context_menu.show(b, event.getScreenX(), event.getScreenY());
                            });

                        }
                        b.setPrefWidth(2000);
                        the_vertical_box.getChildren().add(b);
                        Look_and_feel_manager.set_button_look(b,false);
                        file_to_button.put(f,b);
                        b.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent actionEvent) {
                                change_song(f);
                                set_selected(f);
                            }
                        });
                    }
                }
            }
        });
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

    //**********************************************************
    void change_song(File new_song)
    //**********************************************************
    {
        Integer current_time_s;
        if (new_song == null) {
            String path = Non_booleans.get_current_song();
            if (path == null) {
                current_time_s = null;
                if ( observable_playlist == null) return;
                if ( observable_playlist.isEmpty()) return;
                new_song = observable_playlist.get(0);
            } else {
                new_song = new File(path);
                current_time_s = Non_booleans.get_current_time_in_song();
            }
            if (new_song == null) {

                logger.log("FATAL: cannot cope with new_song is null");
                return;
            }
        } else {
            if (new_song.exists() == false)
            {
                logger.log(("FATAL: " + new_song.getAbsolutePath() + " does not exist"));
                the_status_label.setText("File not found: "+new_song.getAbsolutePath());
                remove_from_playlist_thats_all(new_song);
                return;
            }
            current_time_s = 0;
        }

        change_song_real(new_song, current_time_s);
    }
    //**********************************************************
    private void change_song_real(File new_song, Integer current_time_s)
    //**********************************************************
    {
        Non_booleans.save_current_song(new_song);

        double bitrate = Ffmpeg_utils.get_audio_bitrate(null,new_song.toPath(),logger);
        //logger.log("bitrate= "+bitrate);
        logger.log(new_song.getName()+" (bitrate= "+bitrate+" kb/s)");
        the_status_label.setText("Status: OK for:"+new_song.getName()+" (bitrate= "+bitrate+" kb/s)");

        clean_up();
        the_song_file = new_song;
        add_and_save_if_needed(the_song_file);

        stage.setTitle(the_song_file.getName() +"       bitrate= "+bitrate+" kb/s");

        String encoded;
        try
        {
            encoded = "file://"+the_song_file.getCanonicalPath();
        }
        catch (IOException e)
        {
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
            remove_from_playlist_thats_all(new_song);
        }
        catch (IllegalArgumentException e)
        {
            Popups.popup_Exception(e,256,"Fatal",logger);
            remove_from_playlist_thats_all(new_song);
        }
        stage.show();
    }


    //**********************************************************
    private void add_and_save_if_needed(File added_song)
    //**********************************************************
    {
        for (File file : observable_playlist)
        {
            if (file.getAbsolutePath().equals(added_song.getAbsolutePath()))
            {
                // that song is ALREADY in the list
                set_selected(added_song);
                return;
            }
        }
        observable_playlist.add(added_song);
        set_selected(added_song);
        scroll_pane.setVvalue(1.0);           //1.0 means 100% at the bottom
        save_observable_playlist();
    }

    //**********************************************************
    private void set_selected(File f)
    //**********************************************************
    {

        if ( dbg) logger.log("set_selected "+f);
        Button future = file_to_button.get(f);
        if ( selected == future)
        {
            // already selected
            if ( dbg) logger.log("already selected "+f);
            return;
        }
        if ( future!=null)
        {
            String s = future.getStyle();
            if ( dbg) logger.log("style before = "+s);
            s = change_background_color(s,"#90D5FF");
            if ( dbg) logger.log("style after = "+s);
            future.setStyle(s);
        }
        else logger.log(Stack_trace_getter.get_stack_trace("should not happen"));
        if ( selected != null)
        {
            if ( dbg) logger.log("resetting background for previously selected");
            String s = selected.getStyle();
            if ( dbg) logger.log("style before = "+s);
            s = change_background_color(s,"#ffffff");
            if ( dbg) logger.log("style after = "+s);
            selected.setStyle(s);

            //selected.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        }
        selected = future;

        //b.setBackground(new Background(new BackgroundFill(Color.SKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));

    }

    //**********************************************************
    private String change_background_color(String style, String new_color)
    //**********************************************************
    {
        // assume style is a string with ';' separated items
        // like this: "-fx-background-color: <<<<some color value>>>>>>>"
        // parse the string to replace the current value of -fx-background-color
        // with the new one
        String returned = "";
        String[] items = style.split(";");
        boolean found = false;
        for ( String item : items)
        {
            String[] parts = item.split(":");
            if ( parts[0].trim().equals("-fx-background-color"))
            {
                found = true;
                returned += parts[0]+":"+new_color+";";
            }
            else{
                returned += item+";";
            }
        }
        if ( found == false)
        {
            returned += "-fx-background-color:"+new_color+";";

        }
        //if ( returned.endsWith(";")) returned = returned.substring(0,returned.length()-1);
        return returned;
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
        //logger.log("\n\nequalizer init, bands= "+ equalizer_bands.size());

        define_equalizer();

        the_media_player_option.get().setOnEndOfMedia(() -> jump_to_next());

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
    private void remove_from_playlist_and_jump_to_next()
    //**********************************************************
    {
        File to_be_removed = the_song_file;
        jump_to_next();
        logger.log("removing from playlist: "+to_be_removed);
        observable_playlist.remove(to_be_removed); // will also update file_to_button in the event handler
    }

    //**********************************************************
    private void remove_from_playlist_thats_all(File to_be_removed)
    //**********************************************************
    {
        logger.log("removing from playlist: "+to_be_removed);
        observable_playlist.remove(to_be_removed); // will also update file_to_button in the event handler
    }

    //**********************************************************
    private String extract_playlist_name()
    //**********************************************************
    {
        return Static_files_and_paths_utilities.get_base_name(playlist_file.getName());
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
    private  void jump_to_next()
    //**********************************************************
    {
        logger.log("jumping to next song");

        if ( observable_playlist.isEmpty())
        {
            logger.log("empty playlist");
            return;
        }

        for (int i = 0; i < observable_playlist.size(); i++)
        {
            File file = observable_playlist.get(i);
            if ( file.getAbsolutePath().equals(the_song_file.getAbsolutePath()))
            {
                logger.log("found current song in playlist = "+i);

                int k = i+1;
                if (k >= observable_playlist.size()) k = 0;
                File target = observable_playlist.get(k);
                set_selected(target);
                change_song(target);
                scroll_to(target);
                return;
            }
        }
        logger.log("jumping to next song ... ??? current song not found");

    }
    //**********************************************************
    private void jump_to_previous()
    //**********************************************************
    {
        if ( observable_playlist.isEmpty()) return;
        for (int i = 0; i < observable_playlist.size(); i++)
        {
            File file = observable_playlist.get(i);
            if ( file.getAbsolutePath().equals(the_song_file.getAbsolutePath()))
            {
                int k = i-1;
                if (k < 0 ) k = observable_playlist.size()-1;
                File target = observable_playlist.get(k);
                set_selected(target);
                change_song(target);
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
        this.playlist_name.setText(playlist_name);

        if ( !playlist_name.endsWith(KLIK_AUDIO_PLAYLIST_EXTENSION)) playlist_name += "."+ KLIK_AUDIO_PLAYLIST_EXTENSION;

        playlist_file = new File(saving_dir,playlist_name);
        save_observable_playlist();
    }

    //**********************************************************
    private void save_observable_playlist()
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
        if ( Non_booleans.get_main_properties_manager().get(PLAYLIST_FILE_NAME) == null)
        {
            Non_booleans.get_main_properties_manager().add(PLAYLIST_FILE_NAME, playlist_file_.getAbsolutePath());
        }
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
        change_song(first);

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
                String song_path = br.readLine();
                if ( song_path == null ) break;
                File song = new File(song_path);
                song = sanitize_file_name(song, aborter, logger);
                add_and_save_if_needed(song);
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
                playlist_name.setText(extract_playlist_name());
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
    public static File get_playlist_file(Aborter aborter, Logger logger)
    //**********************************************************
    {
        String playlist_file_name = Non_booleans.get_main_properties_manager().get(PLAYLIST_FILE_NAME);
        if ( playlist_file_name == null)
        {
            playlist_file_name = "playlist."+ Audio_player_frame.KLIK_AUDIO_PLAYLIST_EXTENSION;
            Non_booleans.get_main_properties_manager().add(PLAYLIST_FILE_NAME,playlist_file_name);
        }
        else
        {
            Path p = Path.of(playlist_file_name);
            if (p.isAbsolute()) return p.toFile();
        }
        String home = System.getProperty(USER_HOME);
        Path p = Paths.get(home, Non_booleans.CONF_DIR, playlist_file_name);
        return p.toFile();
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
                jump_to_next();
                break;

            case UP:
                if ( keyword_dbg) logger.log("UP");
                break;

            case DOWN:
                if ( keyword_dbg) logger.log("zoom down/out:");
                break;

            case LEFT:
                if ( keyword_dbg) logger.log("left");
                jump_to_previous();
                break;

            case SPACE:
                if ( keyword_dbg) logger.log("space");
            case RIGHT:
                if ( keyword_dbg) logger.log("right");
                jump_to_next();
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

}
