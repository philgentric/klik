// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.audio.player;
//SOURCES ./Playlist.java

//SOURCES ./Song.java

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import klikr.Window_builder;
import klikr.Window_type;
import klikr.browsers.Browser_for_song_playlist;
import klikr.browser_core.Drag_and_drop;
import klikr.path_lists.Move_provider;
import klikr.browser_core.Window_manager;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.path_lists.navigator.General_navigator;
import klikr.path_lists.navigator.Navigation_type;
import klikr.path_lists.navigator.Navigator;
import klikr.path_lists.Path_list_provider;
import klikr.settings.File_storage;
import klikr.settings.Non_booleans_properties;
import klikr.settings.String_constants;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

//**********************************************************
public class The_audio_player implements Media_callbacks
//**********************************************************
{
    private static The_audio_player instance;

    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    static final boolean keyword_dbg = false;


    static final String AUDIO_PLAYER_EQUALIZER_BAND_ = "AUDIO_PLAYER_EQUALIZER_BAND_";
    static final String AUDIO_PLAYER_VOLUME = "AUDIO_PLAYER_VOLUME";

    public static final String AUDIO_PLAYER = "AUDIO_PLAYER";
    private static final String PAUSE = "Pause";
    private static final String PLAY = "Play";
    Stage stage;
    VBox the_equalizer_vbox = new VBox();
    HBox the_equalizer_hbox = new HBox();
    HBox the_sound_control_hbox = new HBox();
    Button play_pause_button;
    Slider balance_slider;
    Slider the_timeline_slider;
    ImageView speaker_image_view;

    Label now_value_label;
    Label duration_value_label;
    Label the_status_label;
    Button previous;
    Button next;
    Button browse_source;
    CheckBox auto_next_cb;
    private volatile boolean auto_next = true;

    public final Application application;
    public final Window owner;
    Logger logger;
    Aborter aborter;
    private ObservableList<EqualizerBand> equalizer_bands;

    double volume = 0.5;
    double balance = 0.0;

    String pause_string;
    String play_string;
    Navigator navigator;
    Path current;
    private Browser_for_song_playlist browser;
    Label total_duration;

    //**********************************************************
    public static The_audio_player play_song_in_folder(Application application,
                                                       Path song,
                                                       Window owner,
                                                       Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            Path_list_provider path_list_provider = new Path_list_provider_for_file_system(song.getParent(), owner, logger);
            make_instance(application, path_list_provider,
                    "play_song_in_folder "+song,owner,
                    logger);

        }
        instance.play_song_internal(song);
        return instance;
    }



    //**********************************************************
    public static void play_playlist(Application application,
                                                 Path_list_provider path_list_provider, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            make_instance(application, path_list_provider,
                    "play_playlist",owner, logger);
        }
        String song_s = String_constants.get_current_song(owner);
        Path path = null;
        if ( song_s == null)
        {
            path = path_list_provider.only_song_paths(true,true,aborter).getFirst();
        }
        else
        {
            path = Path.of(song_s);
            if (!path.toFile().exists())
            {
                String_constants.save_current_song("",owner);
                return;
            }
        }
        instance.play_song_internal(path);
    }

    //**********************************************************
    private synchronized static void make_instance(
            Application application,
            Path_list_provider path_list_provider,
            String msg,
            Window owner,  Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            logger.log("New The_audio_player "+msg+" : "+ path_list_provider.get_key());
            instance = new The_audio_player(application,
                    path_list_provider, owner, logger);
            instance.define_ui();
        }
        else
        {
            logger.log("Same The_audio_player "+msg+": "+ path_list_provider.get_key());
        }
    }

    //**********************************************************
    private The_audio_player(Application application,
                             Path_list_provider path_list_provider,
                             Window owner, Logger logger)
    //**********************************************************
    {
        int ID = Window_manager.register();
        this.application = application;
        this.owner = owner;
        Consumer<Path> consumer = this::play_song_internal;
        this.navigator = new General_navigator(Navigation_type.songs,path_list_provider,()->current,consumer,owner,logger);
        this.aborter = new Aborter("audio player",logger);
        this.logger = logger;
        stage = new Stage();

        pause_string = My_I18n.get_I18n_string(PAUSE, stage, logger);
        play_string = My_I18n.get_I18n_string(PLAY, stage, logger);
        // logger.log("play_string = " + play_string);

        Rectangle2D r = Non_booleans_properties.get_window_bounds(AUDIO_PLAYER, stage);
        if ( r != null)
        {
            stage.setX(r.getMinX());
            stage.setY(r.getMinY());
            stage.setWidth(r.getWidth());
            stage.setHeight(r.getHeight());
        }
        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            //logger.log("ChangeListener: image window position and/or lengthchanged");
            Non_booleans_properties.save_window_bounds(stage, AUDIO_PLAYER, logger);
        };
        stage.xProperty().addListener(change_listener);
        stage.yProperty().addListener(change_listener);
        stage.widthProperty().addListener(change_listener);
        stage.heightProperty().addListener(change_listener);
        //stage.sizeToScene();

        stage.setOnCloseRequest(event -> {
            logger.log("Audio player NEW closing");
            stop_current_media();
            The_audio_player.kill_instance();
            if ( browser != null)
            {
                ((Stage)(browser.get_owner())).close();
                browser = null;
            }
            Window_manager.unregister(ID,logger);
            stage.close();
        });
    }

    //**********************************************************
    public static void kill_instance()
    //**********************************************************
    {
        if ( instance == null) return;
        instance.die();
        instance = null;
    }

    //**********************************************************
    public static void set_browser_is_null()
    //**********************************************************
    {
        if ( instance == null) return;
        instance.browser_is_null();
    }

    //**********************************************************
    private void browser_is_null()
    //**********************************************************
    {
        browser = null;
    }

    //**********************************************************
    private void die()
    //**********************************************************
    {
        stage.close();
        stop_current_media();
    }

    //**********************************************************
    public static boolean is_running()
    //**********************************************************
    {
        return instance != null;
    }


    //**********************************************************
    private void play_song_internal(Path song)
    //**********************************************************
    {
        if ( song == null )
        {
            logger.log(Stack_trace_getter.get_stack_trace("FATAL: song == null"));
            return;
        }

        current = song;
        save(0);
        if ( browser !=null) browser.set_unique_selected_item(current);
        String_constants.save_current_song(current.toAbsolutePath().toString(),owner);

        logger.log("The_audio_player ->"+current+"<-");
        Media_instance_statics.stop();

        Media_instance_statics.play_this(current, this, stage, logger);

        Platform.runLater(() ->
                {
                    play_pause_button.setText(pause_string);
                    stage.setTitle(current.getFileName().toString());
                });
    }


    //**********************************************************
    private void define_ui()
    //**********************************************************
    {

        the_equalizer_vbox.getChildren().clear();
        the_equalizer_hbox.getChildren().clear();
        the_sound_control_hbox.getChildren().clear();

        VBox the_top_vbox = new VBox();
        Look_and_feel_manager.set_region_look(the_top_vbox, stage, logger);

        VBox duration_vbox = define_duration_vbox();
        the_top_vbox.getChildren().add(duration_vbox);
        volume_and_balance(the_top_vbox);


        BorderPane bottom_border_pane = define_bottom_pane(the_top_vbox);//, scroll_pane);

        Scene scene = new Scene(bottom_border_pane);
        stage.setScene(scene);
        // this keyboqard event handler is trapped in define_scrollpane_with_songs
        scene.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> handle_keyboard(keyEvent, logger));
        stage.show();

    }

    //**********************************************************
    private VBox define_jump_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();
        Look_and_feel_manager.set_region_look(returned, stage, logger);

        previous = new Button(My_I18n.get_I18n_string("Jump_To_Previous_Song", stage, logger));
        Look_and_feel_manager.set_button_look(previous, true, stage, logger);
        previous.setOnAction((ActionEvent e) ->{
            if ( navigator!=null) navigator.previous(aborter,browser);
        } );
        returned.getChildren().add(previous);

        next = new Button(My_I18n.get_I18n_string("Jump_To_Next_Song", stage, logger));
        Look_and_feel_manager.set_button_look(next, true, stage, logger);
        next.setOnAction((ActionEvent e) ->{
            if ( navigator!=null) navigator.next(aborter,browser);
        } );
        returned.getChildren().add(next);

        auto_next_cb = new CheckBox("Auto next");
        auto_next_cb.setSelected(auto_next);
        Look_and_feel_manager.set_region_look(auto_next_cb, stage, logger);
        auto_next_cb.setOnAction((ActionEvent e) -> {
            auto_next = auto_next_cb.isSelected();
        });
        returned.getChildren().add(auto_next_cb);

        browse_source = new Button(My_I18n.get_I18n_string("Browse_Playlist", stage, logger));
        Look_and_feel_manager.set_button_look(browse_source, true, stage, logger);
        browse_source.setOnAction((ActionEvent e) ->{
            if (browser != null)
            {
                Stage stage = (Stage)(browser.get_owner());
                stage.toFront();
                logger.log("showing song browser");
                return;
            }
            Path_list_provider path_list_provider = navigator.get_path_list_provider();

            if(path_list_provider instanceof Path_list_provider_for_file_system)
            {
                Window_builder.additional_no_past(application, Window_type.File_system_2D, path_list_provider, owner, logger);
            }
            else if(path_list_provider instanceof Path_list_provider_for_playlist)
            {
                logger.log("creating new song playlist browser");
                browser = (Browser_for_song_playlist) Window_builder.additional_no_past(application, Window_type.Song_playlist, path_list_provider, owner, logger);
                browser.set_unique_selected_item(current);
            }

        } );
        returned.getChildren().add(browse_source);

        return returned;
    }

    // **********************************************************
    private VBox define_the_landing_zone_vbox()
    // **********************************************************
    {
        VBox returned = new VBox();
        Button landing_zone = define_landing_zone_button();
        returned.getChildren().add(landing_zone);
        total_duration = new Label();
        returned.getChildren().add(total_duration);
        return returned;
    }

    // **********************************************************
    private Button define_landing_zone_button()
    // **********************************************************
    {
        String drop = My_I18n.get_I18n_string("Drop_Here", stage, logger);
        Button landing_zone = new Button(drop);
        Look_and_feel_manager.set_button_look(landing_zone, true, stage, logger);
        landing_zone.setMinHeight(100);
        BackgroundFill background_fill = new BackgroundFill(Color.LIGHTCORAL, CornerRadii.EMPTY, Insets.EMPTY);
        landing_zone.setBackground(new Background(background_fill));

        landing_zone.setOnDragEntered(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg)
                logger.log("The_audio_player OnDragEntered RECEIVER SIDE");
            Look_and_feel_manager.set_background_for_setOnDragEntered(landing_zone, stage, logger);
            drag_event.consume();
        });
        landing_zone.setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg)
                logger.log("The_audio_player OnDragExited RECEIVER SIDE");
            Look_and_feel_manager.set_background_for_setOnDragExited(landing_zone, stage, logger);
            drag_event.consume();
        });
        landing_zone.setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg)
                logger.log("The_audio_player OnDragOver RECEIVER SIDE");
            drag_event.acceptTransferModes(TransferMode.MOVE);
            Look_and_feel_manager.set_background_for_setOnDragOver(landing_zone, stage, logger);
            drag_event.consume();
        });
        landing_zone.setOnDragDropped(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg)
                logger.log("The_audio_player OnDragDropped RECEIVER SIDE");

            Object source = drag_event.getGestureSource();
            if (source == null) {
                logger.log(
                        ("❗ WARNING: The_audio_player accept_drag_dropped_as_a_move_in, cannot check for stupid move because the event's source is null: "
                                + drag_event));
            } else {
                if (source == landing_zone) {
                    logger.log("❗ The_audio_player source is excluded: cannot drop onto itself");
                    drag_event.consume();
                    return;
                }
            }

            Dragboard dragboard = drag_event.getDragboard();
            List<String> the_list = new ArrayList<>();
            String s = dragboard.getString();
            if (s == null) {
                logger.log("The_audio_player  dragboard.getString()== null");
            } else {
                logger.log("The_audio_player drag ACCEPTED for STRING:->" + s + "<-");
                for (String ss : s.split("\\r?\\n")) {
                    if (ss.isBlank())
                        continue;
                    logger.log("The_audio_player drag ACCEPTED for additional file: " + ss);
                    the_list.add(ss);
                }
                if (the_list.isEmpty()) {
                    logger.log("The_audio_player drag list is empty ? " + s);
                }
            }
            {
                List<File> l = dragboard.getFiles();
                for (File fff : l) {
                    logger.log("The_audio_player ... drag ACCEPTED for file= " + fff.getAbsolutePath());
                    if (!the_list.contains(fff.getAbsolutePath()))
                        the_list.add(fff.getAbsolutePath());
                }
            }


            List<File> the_list2 = new ArrayList<>();
            for ( String ss : the_list )
            {
                logger.log("adding ->"+ss+"<-");
                the_list2.add(new File(ss));
            }
            Path_list_provider path_list_provider = navigator.get_path_list_provider();
            if( path_list_provider instanceof Path_list_provider_for_playlist path_list_provider_for_playlist)
            {
                logger.log("path_list_provider is a Path_list_provider_for_playlist");
                Move_provider move_provider = path_list_provider_for_playlist.get_move_in_provider();
                move_provider.move(null,false,the_list2,owner,100,100,aborter,logger);
            }
            else if ( path_list_provider instanceof Path_list_provider_for_file_system path_list_provider_for_file_system)
            {
                Move_provider move_provider = path_list_provider_for_file_system.get_move_in_provider();
                move_provider.move(path_list_provider_for_file_system.get_folder_path().get(),false,the_list2,owner,100,100,aborter,logger);
            }

            // tell the source
            drag_event.setDropCompleted(true);
            drag_event.consume();
        });
        return landing_zone;
    }

    //**********************************************************
    private BorderPane define_bottom_pane(Pane top_pane)
    //**********************************************************
    {
        BorderPane returned = new BorderPane();
        returned.setTop(top_pane);

        VBox the_status_bar = new VBox();

        the_status_label = new Label("Status: OK");
        the_status_label.prefWidth(1000);
        the_status_label.setWrapText(true);
        Look_and_feel_manager.set_region_look(the_status_label, stage, logger);
        the_status_bar.getChildren().add(the_status_label);
        returned.setBottom(the_status_bar);
        Look_and_feel_manager.set_region_look(returned, stage, logger);
        return returned;
    }

    //**********************************************************
    private VBox define_duration_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();
        Look_and_feel_manager.set_region_look(returned, stage, logger);
        HBox h1 = define_duration_hbox();
        returned.getChildren().add(h1);

        define_timeline_slider();
        returned.getChildren().add(the_timeline_slider);

        Look_and_feel_manager.set_button_look(returned, true, stage, logger);
        return returned;
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
    private VBox define_volume_and_balance_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();

        VBox volume_vbox = define_volume_vbox();
        returned.getChildren().add(volume_vbox);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
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
        VBox balance_vbox = new VBox();
        HBox h1 = define_balance_hbox();
        balance_vbox.getChildren().add(h1);
        Button b = define_reset_balance_button();
        balance_vbox.getChildren().add(b);

        Look_and_feel_manager.set_button_look(balance_vbox, true, stage, logger);
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
            balance = balance_slider.getValue();
            Media_instance_statics.set_balance(balance);
        });

        return balance_hbox;
    }

    //**********************************************************
    private Button define_reset_balance_button()
    //**********************************************************
    {
        Button reset_balance = new Button(My_I18n.get_I18n_string("Reset_Balance", stage, logger));
        Look_and_feel_manager.set_button_look(reset_balance, true, stage, logger);
        reset_balance.setOnAction((ActionEvent e) -> {
            balance_slider.setValue(0);
            Media_instance_statics.set_balance(0);
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

        String mute_string = My_I18n.get_I18n_string("Mute", stage, logger);
        Button mute = new Button(mute_string);
        Look_and_feel_manager.set_button_look(mute, true, stage, logger);
        mute.setOnAction((ActionEvent e) -> {
            if (Media_instance_statics.toggle_mute()) {
                mute.setText(mute_string);
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_on_icon(stage, logger));
            } else {
                mute.setText(My_I18n.get_I18n_string("Unmute", stage, logger)); // "Unmute");
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_off_icon(stage, logger));
            }
        });
        returned.getChildren().add(mute);
        Look_and_feel_manager.set_button_look(returned, true, stage, logger);

        return returned;
    }

    //**********************************************************
    private HBox define_volume_hbox()
    //**********************************************************
    {
        HBox volume_hbox = new HBox();

        speaker_image_view = new ImageView(Look_and_feel_manager.get_speaker_on_icon(stage, logger));
        speaker_image_view.setFitHeight(60);
        speaker_image_view.setFitWidth(60);
        volume_hbox.getChildren().add(speaker_image_view);

        volume = get_audio_volume(stage, logger);
        Slider volume_slider = new Slider(0, 1, volume);
        volume_slider.setMinWidth(100);
        volume_hbox.getChildren().add(volume_slider);
        volume_slider.valueProperty().addListener((observableValue, number, t1) -> {
            volume = volume_slider.getValue();
            Media_instance_statics.set_volume(volume);
            save_audio_volume(volume, stage);
            if (volume >= 0.01) {
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_on_icon(stage, logger));
            } else {
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_off_icon(stage, logger));
            }
        });
        return volume_hbox;
    }


    //**********************************************************
    private void define_timeline_slider()
    //**********************************************************
    {
        the_timeline_slider = new Slider();
        // image_viewerLook_and_feel_manager.set_region_look(the_timeline_slider);
        //the_timeline_slider.setMinWidth(WIDTH);
        //the_timeline_slider.setPrefWidth(WIDTH);
        // but the user may click/slide the slider
        the_timeline_slider.setOnMouseReleased(event -> {
            the_timeline_slider.setValueChanging(true);
            double v = event.getX() / the_timeline_slider.getWidth() * the_timeline_slider.getMax();
            the_timeline_slider.setValue(v);
            // logger.log("player seeking: slider new value = "+slider.getValue());
            Duration target = Duration.seconds(v);
            // logger.log("player seeking:"+target);
            Media_instance_statics.seek(target);
            the_timeline_slider.setValueChanging(false);
        });
    }

    //**********************************************************
    private HBox define_duration_hbox()
    //**********************************************************
    {
        HBox hbox = new HBox();
        Look_and_feel_manager.set_region_look(hbox, stage, logger);
        Label duration_text = new Label(My_I18n.get_I18n_string("Duration", stage, logger) + " : ");
        Look_and_feel_manager.set_region_look(duration_text, stage, logger);
        hbox.getChildren().add(duration_text);
        duration_value_label = new Label("0.0s");
        Look_and_feel_manager.set_region_look(duration_value_label, stage, logger);
        hbox.getChildren().add(duration_value_label);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        Button rewind = new Button(My_I18n.get_I18n_string("Rewind", stage, logger));
        Look_and_feel_manager.set_button_look(rewind, true, stage, logger);
        rewind.setOnAction((ActionEvent e) -> {
            rewind();
        });
        hbox.getChildren().add(rewind);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        play_pause_button = new Button(pause_string);
        Look_and_feel_manager.set_button_look(play_pause_button, true, stage, logger);
        play_pause_button.setOnAction((ActionEvent e) -> toggle_play_stop());
        hbox.getChildren().add(play_pause_button);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        Label now_text = new Label(My_I18n.get_I18n_string("Now", stage, logger) + " : ");
        Look_and_feel_manager.set_region_look(now_text, stage, logger);
        hbox.getChildren().add(now_text);
        now_value_label = new Label("0.0s");
        Look_and_feel_manager.set_region_look(now_value_label, stage, logger);

        hbox.getChildren().add(now_value_label);
        return hbox;
    }

    //**********************************************************
    @Override // Media_callbacks
    public void on_end_of_media()
    //**********************************************************
    {
        save_current_time_in_song(0, null);
        navigator.next(aborter, browser);
    }

    //**********************************************************
    @Override // Media_callbacks
    public void on_player_ready()
    //**********************************************************
    {
        //logger.log("Audio_player_FX_UI on_player_ready()");
        equalizer_bands = Media_instance_statics.get_bands();
        define_equalizer();

        ChangeListener<Duration> xx = (observable, oldValue, newValue) -> {
            if (ultra_dbg)
                logger.log("song current time:" + newValue.toSeconds());
            double seconds = newValue.toSeconds();
            the_timeline_slider.setValue(seconds);
            now_value_label.setText((int) seconds + " seconds");
            save_current_time_in_song((int) seconds, stage);

        };

        // the player pilots how the slider moves during playback
        Media_instance_statics.add_current_time_listener(xx);

        // logger.log("song start:"+ player.getStartTime().toSeconds());
        // logger.log("song stop:"+ player.getStopTime().toSeconds());
        double seconds = Media_instance_statics.get_stop_time().toSeconds();
        the_timeline_slider.setValue(0);
        the_timeline_slider.setMax(seconds);
        Media_instance_statics.set_volume(volume);
        Media_instance_statics.set_balance(balance);
        Media_instance_statics.play();
        String s = get_nice_string_for_duration(seconds, stage, logger);
        duration_value_label.setText(s);

    }

    //**********************************************************
    public static String get_nice_string_for_duration(double seconds_in, Window owner, Logger logger)
    //**********************************************************
    {
        int d = 0;
        int h = 0;
        int m = 0;
        int seconds = (int) seconds_in;

        h = seconds / 3600;
        if (h > 24) {
            d = h / 24;
            h = h - d * 24;
            seconds = seconds - d * 24 * 3600 - h * 3600;
        }
        if (seconds > 60) {
            m = seconds / 60;
            seconds = seconds - m * 60;
        }
        String abbreviation_for_second = My_I18n.get_I18n_string("Abbreviation_For_Second", owner, logger);
        String abbreviation_for_minute = My_I18n.get_I18n_string("Abbreviation_For_Minute", owner, logger);
        String abbreviation_for_hour = My_I18n.get_I18n_string("Abbreviation_For_Hour", owner, logger);
        String abbreviation_for_day = My_I18n.get_I18n_string("Abbreviation_For_Day", owner, logger);
        String returned = seconds + abbreviation_for_second;
        if (m > 0)
            returned = m + abbreviation_for_minute + " " + returned;
        if (h > 0)
            returned = h + abbreviation_for_hour + " " + returned;
        if (d > 0)
            returned = d + abbreviation_for_day + " " + returned;
        return returned;
    }

    boolean equalizer_created = false;
    List<Slider> sliders = new ArrayList<>();
    List<ChangeListener<? super Number>> listeners = new ArrayList<>();

    //**********************************************************
    private void define_equalizer()
    //**********************************************************
    {
        if (equalizer_created)
        {
            for (int i = 0; i < sliders.size(); i++)
            {
                Slider s = sliders.get(i);
                double value = get_equalizer_value_for_band(i, stage, logger);
                s.setValue(value);
                equalizer_bands.get(i).setGain(value);

                int finalI = i;
                ChangeListener<? super Number> listener = listeners.get(i);
                s.valueProperty().removeListener((ChangeListener<? super Number>) listener);

                listener = (ov, old_val_, new_val_) -> {
                    double slider_value = new_val_.doubleValue();
                    logger.log("slider_value="+ slider_value);
                    equalizer_bands.get(finalI).setGain(slider_value);
                    save_equalizer_value_for_band(finalI, slider_value, stage);
                };
                s.valueProperty().addListener(listener);
                listeners.add(listener);
            }
        }
        else
        {

            double MIN = -24;
            double MAX = 12;
            int how_many_rectangles = equalizer_bands.size();
            for (int i = 0; i < how_many_rectangles; i++)
            {
                double value = get_equalizer_value_for_band(i, stage, logger);
                logger.log("Equalizer band#"+i+" ="+ value);
                equalizer_bands.get(i).setGain(value);

                Slider s = new Slider(MIN, MAX, value);
                s.setMinHeight(100);
                s.setMinWidth(30);
                s.setOrientation(Orientation.VERTICAL);
                the_equalizer_hbox.getChildren().add(s);
                int finalI = i;
                ChangeListener<? super Number> listener = (ov, old_val_, new_val_) -> {
                    double slider = new_val_.doubleValue();
                    logger.log("slider="+ slider);
                    equalizer_bands.get(finalI).setGain(slider);
                    save_equalizer_value_for_band(finalI, slider, stage);
                };
                listeners.add(listener);
                s.valueProperty().addListener(listener);
                sliders.add(s);
            }
            equalizer_created = true;
            the_equalizer_vbox.getChildren().add(the_equalizer_hbox);
            Button reset_equalizer_button = make_reset_equalizer_button();
            the_equalizer_vbox.getChildren().add(reset_equalizer_button);
            Look_and_feel_manager.set_button_look(the_equalizer_vbox, true, stage, logger);
        }
    }

    //**********************************************************
    private Button make_reset_equalizer_button()
    //**********************************************************
    {
        Button reset_button = new Button(My_I18n.get_I18n_string("Reset_Equalizer", stage, logger));
        Look_and_feel_manager.set_button_look(reset_button, true, stage, logger);
        reset_button.setOnAction(actionEvent -> {
            for (int i = 0; i < equalizer_bands.size(); i++)
            {
                equalizer_bands.get(i).setGain(0.0);
                sliders.get(i).setValue(0.0);
                save_equalizer_value_for_band(i, 0.0, stage);
            }
        });
        return reset_button;
    }

    //**********************************************************
    public void stop_current_media()
    //**********************************************************
    {
        Media_instance_statics.stop();
        Media_instance_statics.dispose();
    }

    //**********************************************************
    public void set_title(String s)
    //**********************************************************
    {
        Runnable r = () -> stage.setTitle(s);
        Platform.runLater(r);
    }

    //**********************************************************
    void handle_keyboard(final KeyEvent key_event, Logger logger)
    //**********************************************************
    {
        if (keyword_dbg)
            logger.log("Audio_player_FX_UI KeyEvent=" + key_event);

        if (key_event.isMetaDown() && key_event.getCode() == KeyCode.V) {
            // user is pasting some text, typically youtube URL
            System.out.println("Meta + V pressed");
            try {
                // Get the system clipboard
                Clipboard clipboard = Clipboard.getSystemClipboard();

                if (clipboard.hasContent(DataFormat.PLAIN_TEXT)) {
                    String content = clipboard.getString();
                    System.out.println("Clipboard Content: " + content);
                    Runnable r = () -> {
                        Aborter youtube_abort = new Aborter("you tube abort",logger);
                        Optional<Hourglass> hourglass = Progress_window.show_with_abort_button(
                                youtube_abort,
                                "Importing audio tracks",
                                30 * 60,
                                stage.getX() + 100,
                                stage.getY() + 100,
                                stage,
                                logger);

                        Path_list_provider path_list_provider = navigator.get_path_list_provider();
                        if( path_list_provider instanceof Path_list_provider_for_playlist path_list_provider_for_playlist)
                        {
                            logger.log("path_list_provider is a Path_list_provider_for_playlist");
                            path_list_provider_for_playlist.import_from_youtube(content);
                        }
                        hourglass.ifPresent(Hourglass::close);

                    };
                    Actor_engine.execute(r, "Importing audio track(s)", logger);
                } else {
                    System.out.println("No text content in clipboard");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error accessing clipboard");
            }
        }

        switch (key_event.getCode()) {
            case F7:
                if (keyword_dbg)
                    logger.log("F7");
                rewind();
                break;
            case F8:
                if (keyword_dbg)
                    logger.log("F8");
                toggle_play_stop();
                break;
            case F9:
                if (keyword_dbg)
                    logger.log("F9");
            case DOWN:
                if (keyword_dbg)
                    logger.log("DOWN");
            case SPACE:
                if (keyword_dbg)
                    logger.log("space");
            case RIGHT:
                if (keyword_dbg)
                    logger.log("right");
                navigator.next(aborter, browser);
                break;

            case UP:
                if (keyword_dbg)
                    logger.log("UP");
            case LEFT:
                if (keyword_dbg)
                    logger.log("left");
                navigator.previous(aborter, browser);
                break;

            default:
                if (keyword_dbg)
                    logger.log("default");
                break;

        }

        if (key_event.getCode() == KeyCode.ESCAPE) {
            key_event.consume();
            return;
        }



        key_event.consume();

    }


    //**********************************************************
    private void toggle_play_stop()
    //**********************************************************
    {
        MediaPlayer.Status status = Media_instance_statics.get_status();
        if (status == MediaPlayer.Status.PLAYING)
            set_is_paused();
        else
            set_is_playing();
    }

    //**********************************************************
    private void rewind()
    //**********************************************************
    {
        Media_instance_statics.seek(Duration.ZERO);
        set_is_playing();
    }

    //**********************************************************
    private void set_is_playing()
    //**********************************************************
    {
        Media_instance_statics.play();
        play_pause_button.setText(pause_string);

    }

    //**********************************************************
    private void set_is_paused()
    //**********************************************************
    {
        Media_instance_statics.pause();
        play_pause_button.setText(play_string);
    }




    static int previous_current_time_in_song = -1;

    //**********************************************************
    public static void save_current_time_in_song(int time, Window owner)
    //**********************************************************
    {

        if (previous_current_time_in_song > 0)
        {
            if (previous_current_time_in_song / 10 == time / 10)
            {
                // do not save more often than each 0.1s
                return;
            }
        }
        save(time);
    }

    //**********************************************************
    private static void save(int i)
    //**********************************************************
    {
        //logger.log("save_current_time_in_song "+i);
        previous_current_time_in_song = i;
        File_storage pm = Shared_services.main_properties();
        pm.set_and_save(Media_instance.AUDIO_PLAYER_CURRENT_TIME, "" + i);

    }


    //**********************************************************
    public static double get_equalizer_value_for_band(int i, Window owner,Logger logger)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        String s = pm.get(AUDIO_PLAYER_EQUALIZER_BAND_ + i);
        if (s == null) return 0;

        double value = 0;
        try {
            value = Double.valueOf(s);
        } catch (NumberFormatException e) {
            logger.log("WARNING: cannot parse equalizer value for band " + i + "->" + s + "<-");
        }
        return value;
    }

    //**********************************************************
    public static void save_equalizer_value_for_band(int i, double value, Window owner)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        pm.set_and_save(AUDIO_PLAYER_EQUALIZER_BAND_ + i, "" + value);
    }

    //**********************************************************
    public static void save_audio_volume(double value, Window owner)
    //**********************************************************
    {
        Non_booleans_properties.set_double(value,AUDIO_PLAYER_VOLUME,owner);
    }

    //**********************************************************
    public static double get_audio_volume(Window owner,Logger logger)
    //**********************************************************
    {
        return Non_booleans_properties.get_double(AUDIO_PLAYER_VOLUME,0.5,owner);
    }

}
