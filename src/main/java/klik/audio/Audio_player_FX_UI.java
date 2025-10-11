package klik.audio;
//SOURCES ./Playlist.java
//SOURCES ./Song.java



import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.Drag_and_drop;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans_properties;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.ui.Progress_window;

import java.io.File;
import java.io.IOException;
import java.util.*;


//**********************************************************
public class Audio_player_FX_UI implements Media_callbacks
//**********************************************************
{
    private static final boolean dbg =  true;
    private static final boolean ultra_dbg =  false;
    private static final boolean scroll_dbg =  false;

    public static final int WIDTH = 500;
    public static final String AUDIO_PLAYER = "AUDIO_PLAYER";
    private static final String PAUSE = "Pause";
    private static final String PLAY = "Play";
    Stage stage;
    VBox the_equalizer_vbox =  new VBox();
    HBox the_equalizer_hbox = new HBox();
    HBox the_sound_control_hbox = new HBox();
    Button save_playlist_under_a_new_name;
    Button next;
    Button previous;
    Button play_pause_button;
    Slider balance_slider;
    Slider the_timeline_slider;
    VBox the_vertical_box;
    ImageView speaker_image_view;

    Label now_value_label;
    Label duration_value_label;
    Label playlist_name_label;
    Label the_status_label;
    Label total_duration;

    ScrollPane scroll_pane;
    Logger logger;
    Aborter aborter;
    //private volatile Optional<MediaPlayer> the_media_player_option = Optional.empty();
    //private volatile Optional<AudioEqualizer> the_equalizer_option = Optional.empty();
    private ObservableList<EqualizerBand> equalizer_bands;

    double volume = 0.5;
    double balance = 0.0;

    String pause_string;
    String play_string;
    static final boolean keyword_dbg = true;

    // STATE:

    private final Playlist playlist;

    //**********************************************************
    Audio_player_FX_UI(Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.aborter = aborter;
        this.logger = logger;
        stage = new Stage();
        this.playlist = new Playlist(this, stage, logger);


        pause_string = My_I18n.get_I18n_string(PAUSE, stage,logger);
        play_string = My_I18n.get_I18n_string(PLAY, stage,logger);
        logger.log("play_string = " + play_string);

        Rectangle2D r = Non_booleans_properties.get_window_bounds(AUDIO_PLAYER,stage);
        stage.setX(r.getMinX());
        stage.setY(r.getMinY());
        stage.setWidth(r.getWidth());
        stage.setHeight(r.getHeight());
        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            //if ( dbg) logger.log("ChangeListener: image window position and/or size changed");
            Rectangle2D b = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Non_booleans_properties.save_window_bounds(stage, AUDIO_PLAYER,logger);
        };
        stage.xProperty().addListener(change_listener);
        stage.yProperty().addListener(change_listener);
        stage.widthProperty().addListener(change_listener);
        stage.heightProperty().addListener(change_listener);
        stage.setMinWidth(WIDTH);
        stage.sizeToScene();

    }


    //**********************************************************
    public void define_ui()
    //**********************************************************
    {

        the_equalizer_vbox.getChildren().clear();
        the_equalizer_hbox.getChildren().clear();
        the_sound_control_hbox.getChildren().clear();


        VBox the_top_vbox = new VBox();
        Look_and_feel_manager.set_region_look(the_top_vbox,stage,logger);

        VBox duration_vbox = define_duration_vbox();
        the_top_vbox.getChildren().add(duration_vbox);
        volume_and_balance(the_top_vbox);

        the_top_vbox.getChildren().add(define_playlist_hbox());

        ScrollPane scroll_pane = define_scrollpane_with_songs();


        // called only on EXTERNAL close requests i.e. hitting the cross in the title
        stage.setOnCloseRequest(windowEvent -> {
            logger.log("Audio player closing");
            stop_current_media();
            stage.close();
            UI_instance_holder.set_null();
        });

        BorderPane bottom_border_pane = define_bottom_pane(the_top_vbox, scroll_pane);

        Scene scene = new Scene(bottom_border_pane);
        stage.setScene(scene);
        // this keyboqard event handler is trapped in define_scrollpane_with_songs
        scene.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> handle_keyboard(keyEvent, logger));
        stage.show();

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
        Look_and_feel_manager.set_region_look(the_status_label,stage,logger);
        the_status_bar.getChildren().add(the_status_label);
        returned.setBottom(the_status_bar);
        Look_and_feel_manager.set_region_look(returned,stage,logger);
        return returned;
    }

    //**********************************************************
    private VBox define_duration_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();
        Look_and_feel_manager.set_region_look(returned,stage,logger);
        HBox h1 = define_duration_hbox();
        returned.getChildren().add(h1);

        define_timeline_slider();
        returned.getChildren().add(the_timeline_slider);

        Look_and_feel_manager.set_button_look(returned,true,stage,logger);
        return returned;
    }

    //**********************************************************
    private HBox define_playlist_hbox()
    //**********************************************************
    {
        HBox returned = new HBox();
        Look_and_feel_manager.set_region_look(returned,stage,logger);


        {

            Button remove_from_playlist = new Button(My_I18n.get_I18n_string("Remove_playing_song_from_playlist",stage,logger));
            Look_and_feel_manager.set_button_look(remove_from_playlist, true,stage,logger);
            remove_from_playlist.setOnAction(actionEvent -> playlist.remove_from_playlist_and_jump_to_next());
            returned.getChildren().add(remove_from_playlist);
        }

        {

            Button undo_remove_button = new Button(My_I18n.get_I18n_string("Undo_Remove",stage,logger));
            Look_and_feel_manager.set_button_look(undo_remove_button, true,stage,logger);
            undo_remove_button.setOnAction(actionEvent -> playlist.undo_remove());
            returned.getChildren().add(undo_remove_button);
        }
        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer,stage,logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            returned.getChildren().add(spacer);
        }
        {

            Button search = new Button(My_I18n.get_I18n_string("Search",stage,logger));
            Look_and_feel_manager.set_button_look(search,true,stage,logger);
            returned.getChildren().add(search);
            search.setOnAction(actionEvent -> playlist.search());
        }
        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer,stage,logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            returned.getChildren().add(spacer);
        }

        playlist_name_label = new Label(My_I18n.get_I18n_string("Name_Of_Playlist",stage,logger)+" : "+playlist.get_playlist_name());
        playlist_name_label.setMinWidth(200);
        Look_and_feel_manager.set_region_look(playlist_name_label,stage,logger);
        returned.getChildren().add(playlist_name_label);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer,stage,logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            returned.getChildren().add(spacer);
        }

        save_playlist_under_a_new_name = new Button(My_I18n.get_I18n_string("Save_Playlist_Under_A_New_Name",stage,logger));
        {
            save_playlist_under_a_new_name.setOnAction(actionEvent -> save_new_playlist());
        }
        Look_and_feel_manager.set_button_look(save_playlist_under_a_new_name,true,stage,logger);
        returned.getChildren().add(save_playlist_under_a_new_name);

        return returned;
    }


    //**********************************************************
    private Button define_landing_zone_button()
    //**********************************************************
    {
        String drop = My_I18n.get_I18n_string("Drop_Here",stage,logger);
        Button landing_zone = new Button(drop);
        Look_and_feel_manager.set_button_look(landing_zone, true,stage,logger);
        landing_zone.setMinHeight(100);
        BackgroundFill background_fill = new BackgroundFill(Color.LIGHTCORAL, CornerRadii.EMPTY, Insets.EMPTY);
        landing_zone.setBackground(new Background(background_fill));

        landing_zone.setOnDragEntered(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragEntered RECEIVER SIDE" );
            Look_and_feel_manager.set_background_for_setOnDragEntered(landing_zone,stage,logger);
            drag_event.consume();
        });
        landing_zone.setOnDragExited(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragExited RECEIVER SIDE");
            Look_and_feel_manager.set_background_for_setOnDragExited(landing_zone,stage,logger);
            drag_event.consume();
        });
        landing_zone.setOnDragOver(drag_event -> {
            if (Drag_and_drop.drag_and_drop_dbg) logger.log("OnDragOver RECEIVER SIDE");
            drag_event.acceptTransferModes(TransferMode.MOVE);
            Look_and_feel_manager.set_background_for_setOnDragOver(landing_zone,stage,logger);
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
            List<String> the_list = new ArrayList<>();
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
                    the_list.add(ss);
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
                    if ( !the_list.contains(fff.getAbsolutePath()) )  the_list.add(fff.getAbsolutePath());
                }
            }

            playlist.user_wants_to_add_songs(the_list);

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
        total_duration = new Label();
        returned.getChildren().add(total_duration);
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
            Look_and_feel_manager.set_region_look(spacer,stage,logger);
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

        Look_and_feel_manager.set_button_look(balance_vbox,true,stage,logger);
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
        Button reset_balance = new Button(My_I18n.get_I18n_string("Reset_Balance",stage,logger));
        Look_and_feel_manager.set_button_look(reset_balance,true,stage,logger);
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

        String mute_string = My_I18n.get_I18n_string("Mute",stage,logger);
        Button mute = new Button(mute_string);
        Look_and_feel_manager.set_button_look(mute,true,stage,logger);
        mute.setOnAction((ActionEvent e) -> {
            if ( Media_instance_statics.toggle_mute())
            {
                mute.setText(mute_string);
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_on_icon(stage,logger));
            }
            else
            {
                mute.setText(My_I18n.get_I18n_string("Unmute",stage,logger)); //"Unmute");
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_off_icon(stage,logger));
            }
        });
        returned.getChildren().add(mute);
        Look_and_feel_manager.set_button_look(returned,true,stage,logger);

        return returned;
    }

    //**********************************************************
    private HBox define_volume_hbox()
    //**********************************************************
    {
        HBox volume_hbox = new HBox();

        speaker_image_view = new ImageView(Look_and_feel_manager.get_speaker_on_icon(stage,logger));
        speaker_image_view.setFitHeight(60);
        speaker_image_view.setFitWidth(60);
        volume_hbox.getChildren().add(speaker_image_view);

        volume = Non_booleans_properties.get_audio_volume(stage,logger);
        Slider volume_slider = new Slider(0, 1, volume);
        volume_slider.setMinWidth(30);
        volume_hbox.getChildren().add(volume_slider);
        volume_slider.valueProperty().addListener((observableValue, number, t1) -> {
            volume = volume_slider.getValue();
            Media_instance_statics.set_volume(volume);
            Non_booleans_properties.save_audio_volume(volume,stage);
            if ( volume >=0.01)
            {
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_on_icon(stage,logger));
            }
            else
            {
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_off_icon(stage,logger));
            }
        });
        return volume_hbox;
    }

    //**********************************************************
    private VBox define_jump_vbox()
    //**********************************************************
    {
        VBox returned = new VBox();
        Look_and_feel_manager.set_region_look(returned,stage,logger);

        previous = new Button(My_I18n.get_I18n_string("Jump_To_Previous_Song",stage,logger));
        Look_and_feel_manager.set_button_look(previous, true,stage,logger);
        previous.setOnAction((ActionEvent e) -> playlist.jump_to_previous_from_user());
        returned.getChildren().add(previous);


        next = new Button(My_I18n.get_I18n_string("Jump_To_Next_Song",stage,logger));
        Look_and_feel_manager.set_button_look(next, true,stage,logger);
        next.setOnAction((ActionEvent e) -> playlist.jump_to_next_from_user());
        returned.getChildren().add(next);


        Button shuffle = new Button(My_I18n.get_I18n_string("Shuffle",stage,logger));
        Look_and_feel_manager.set_button_look(shuffle, true,stage,logger);
        shuffle.setOnAction((ActionEvent e)->playlist.shuffle());
        returned.getChildren().add(shuffle);


        return returned;
    }


    //**********************************************************
    private ScrollPane define_scrollpane_with_songs()
    //**********************************************************
    {
        scroll_pane = new ScrollPane();
        Look_and_feel_manager.set_region_look(scroll_pane,stage,logger);
        scroll_pane.addEventFilter(KeyEvent.KEY_PRESSED, key_event -> {
            if ( dbg)logger.log("trapping event "+key_event);
            switch (key_event.getCode())
            {
            case LEFT:
                if ( dbg)logger.log("handle event: LEFT");
            case UP:
                if ( dbg)logger.log("handle event: UP");
                playlist.jump_to_previous_from_user();
                key_event.consume(); // prevent default key handling
                break;
            case RIGHT:
                if ( dbg)logger.log("handle event: RIGHT");
            case SPACE:
                if ( dbg)logger.log("handle event: SPACE");
            case DOWN:
                if ( dbg)logger.log("handle event: DOWN");
                playlist.jump_to_next_from_user();
                key_event.consume(); // prevent default key handling
                break;
            }
        });

        Look_and_feel_manager.set_region_look(scroll_pane,stage,logger);
        scroll_pane.setPrefHeight(3000);
        //scroll_pane.setPrefWidth(1000);
        the_vertical_box = new VBox(0);
        the_vertical_box.setSpacing(0);
        the_vertical_box.setPadding(new Insets(0)); // Remove padding
        the_vertical_box.setFillWidth(true);
        VBox.setVgrow(the_vertical_box, Priority.ALWAYS);





        scroll_pane.setContent(the_vertical_box);

        scroll_pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll_pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
/*
        scroll_pane.setOnScroll( e -> {
            if ( scroll_dbg) logger.log("setOnScroll: " + e);
            process_scroll(e);
        });
*/
        /*


        scroll_pane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            if ( scroll_dbg) logger.log("scroll pane vvalue changed: " + newValue);
            process_scroll(null);
        });
        */
        return scroll_pane;
    }
/*
    //**********************************************************
    private void process_scroll(ScrollEvent e)
    //**********************************************************
    {
        Platform.runLater(()-> process_scroll_internal(e));
    }
    //**********************************************************
    private void process_scroll_internal(ScrollEvent e)
    //**********************************************************
    {
        Bounds scrollPaneBounds = scroll_pane.getContent().getLayoutBounds();
        logger.log("\n\nscrollPane minY " + scrollPaneBounds.getMinY()); // typically 0
        logger.log("scrollPane maxY " + scrollPaneBounds.getMaxY());

        double scroll = scroll_pane.vvalueProperty().get();
        if ( scroll_dbg) logger.log("scroll = " + scroll);
        double viewportHeight = scroll_pane.getViewportBounds().getHeight();
        if ( scroll_dbg) logger.log("viewportHeight = " + viewportHeight);
        if ( scroll_dbg) logger.log("scrollPaneBounds.getMinY() = " + scrollPaneBounds.getMinY());
        if ( scroll_dbg) logger.log("scrollPaneBounds.getMaxY() = " + scrollPaneBounds.getMaxY());


        Bounds contentBounds = the_vertical_box.getLayoutBounds();
        if ( scroll_dbg) logger.log("Content bounds: " + contentBounds);

        double contentViewStartY = scrollPaneBounds.getMinY() + scroll * (scrollPaneBounds.getMaxY() - viewportHeight);
        if ( scroll_dbg) logger.log("contentViewStartY = " + contentViewStartY);


        double contentViewEndY = contentViewStartY + viewportHeight;
        if ( scroll_dbg) logger.log("contentViewEndY = " + contentViewEndY);
        int count_visible = 0;
        //StringBuilder sb =null;
        //if ( scroll_dbg ) sb = new StringBuilder();

        List<Song> local = playlist.get_a_copy_of_all_songs();
        Comparator<? super Song> compa = (Comparator<Song>) (o1, o2) -> {
            Double d1 = Double.valueOf(o1.node.getBoundsInParent().getMinY());
            Double d2 = Double.valueOf(o2.node.getBoundsInParent().getMinY());
            return d1.compareTo(d2);
        };
        Collections.sort(local,compa);
        for ( Song s : local)
        {
            Bounds node_Bounds = s.node.getBoundsInParent();
            if ( scroll_dbg) logger.log("node minY " + node_Bounds.getMinY()+" for "+s.path);
            if ( scroll_dbg) logger.log("node maxY " + node_Bounds.getMaxY());

            if(node_Bounds.getMinY() > contentViewEndY )
            {
                //if ( sb !=null) sb.append("not ok1 is invisible : ").append(node_Bounds.getMinY()).append(">").append(contentViewEndY).append("\n");
                s.process_invisible(logger);
                continue;
            }
            //if ( sb !=null) sb.append("ok1 : " + node_Bounds.getMinY() +"<="+ contentViewEndY).append("\n");
            if(node_Bounds.getMaxY() < contentViewStartY )
            {
                //if ( sb !=null) sb.append("not ok2 is invisible : ").append(node_Bounds.getMaxY()).append("<").append( contentViewStartY).append("\n");
                s.process_invisible(logger);
                continue;
            }
            //if ( sb !=null) sb.append("ok2 is visible : " + node_Bounds.getMaxY() +">="+ contentViewStartY).append("\n");
            s.process_visible(playlist,stage,logger);
            count_visible++;
        }
        //if ( sb !=null) logger.log(sb.toString());
        if ( scroll_dbg) logger.log("visible songs: " + count_visible + " out of " + local.size());
        if ( e !=null) e.consume();
    }
*/
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
            the_timeline_slider.setValueChanging(true);
            double v = event.getX() / the_timeline_slider.getWidth() * the_timeline_slider.getMax();
            the_timeline_slider.setValue(v);
            //logger.log("player seeking: slider new value = "+slider.getValue());
            Duration target = Duration.seconds(v);
            //logger.log("player seeking:"+target);
            Media_instance_statics.seek(target);
            the_timeline_slider.setValueChanging(false);
        });
    }

    //**********************************************************
    private HBox define_duration_hbox()
    //**********************************************************
    {
        HBox hbox = new HBox();
        Look_and_feel_manager.set_region_look(hbox,stage,logger);
        Label duration_text = new Label(My_I18n.get_I18n_string("Duration",stage,logger)+" : ");
        Look_and_feel_manager.set_region_look(duration_text,stage,logger);
        hbox.getChildren().add(duration_text);
        duration_value_label = new Label("0.0s");
        Look_and_feel_manager.set_region_look(duration_value_label,stage,logger);
        hbox.getChildren().add(duration_value_label);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer,stage,logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        Button rewind = new Button(My_I18n.get_I18n_string("Rewind",stage,logger));
        Look_and_feel_manager.set_button_look(rewind, true,stage,logger);
        rewind.setOnAction((ActionEvent e) -> {
            rewind();
        });
        hbox.getChildren().add(rewind);


        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer,stage,logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        play_pause_button = new Button(pause_string);
        Look_and_feel_manager.set_button_look(play_pause_button, true,stage,logger);
        play_pause_button.setOnAction((ActionEvent e) -> toggle_play_stop());
        hbox.getChildren().add(play_pause_button);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer,stage,logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        Label now_text = new Label(My_I18n.get_I18n_string("Now",stage,logger)+" : ");
        Look_and_feel_manager.set_region_look(now_text,stage,logger);
        hbox.getChildren().add(now_text);
        now_value_label = new Label("0.0s");
        Look_and_feel_manager.set_region_look(now_value_label,stage,logger);

        hbox.getChildren().add(now_value_label);
        return hbox;
    }
    //**********************************************************
    @Override // Media_callbacks
    public void on_end_of_media()
    //**********************************************************
    {
        Non_booleans_properties.save_current_time_in_song(0, null);
        playlist.jump_to_next_on_end_of_media();
    }

    //**********************************************************
    @Override // Media_callbacks
    public void on_player_ready()
    //**********************************************************
    {

        equalizer_bands = Media_instance_statics.get_bands();
        //logger.log("\n\nequalizer init, bands= "+ equalizer_bands.size());

        define_equalizer();

        ChangeListener<Duration> xx = (observable, oldValue, newValue) -> {
            if (ultra_dbg)
                logger.log("song current time:" + newValue.toSeconds());
            double seconds = newValue.toSeconds();
            the_timeline_slider.setValue(seconds);
            now_value_label.setText((int) seconds + " seconds");
            Non_booleans_properties.save_current_time_in_song((int) seconds, stage);

        };

        // the player pilots how the slider moves during playback
        Media_instance_statics.add_current_time_listener(xx);

        //logger.log("song start:"+ player.getStartTime().toSeconds());
        //logger.log("song stop:"+ player.getStopTime().toSeconds());
        double seconds = Media_instance_statics.get_stop_time().toSeconds();
        the_timeline_slider.setValue(0);
        the_timeline_slider.setMax(seconds);
        Media_instance_statics.set_volume(volume);
        Media_instance_statics.set_balance(balance);
        Media_instance_statics.play();
        String s = get_nice_string_for_duration(seconds,stage,logger);
        duration_value_label.setText(s);


    }

    //**********************************************************
    public static String get_nice_string_for_duration(double seconds_in, Window owner, Logger logger)
    //**********************************************************
    {
        int d = 0;
        int h = 0;
        int m = 0;
        int seconds = (int)seconds_in;

        h = seconds /3600;
        if ( h > 24)
        {
            d = h/24;
            h = h - d*24;
            seconds = seconds - d*24*3600 - h*3600;
        }
        if ( seconds > 60)
        {
            m = seconds /60;
            seconds = seconds - m*60;
        }
        String abbreviation_for_second =My_I18n.get_I18n_string("Abbreviation_For_Second",owner,logger);
        String abbreviation_for_minute = My_I18n.get_I18n_string("Abbreviation_For_Minute",owner,logger);
        String abbreviation_for_hour = My_I18n.get_I18n_string("Abbreviation_For_Hour",owner,logger);
        String abbreviation_for_day = My_I18n.get_I18n_string("Abbreviation_For_Day",owner,logger);
        String returned = seconds+abbreviation_for_second;
        if( m>0) returned = m+abbreviation_for_minute+" "+returned;
        if( h>0) returned = h+abbreviation_for_hour+" "+returned;
        if( d>0) returned = d+abbreviation_for_day+" "+returned;
        return returned;
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
                    double slider_value = new_val_.doubleValue();
                    equalizer_bands.get(finalI).setGain(slider_value);
                    Non_booleans_properties.save_equalizer_value_for_band(finalI, slider_value,stage);
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
                double value = Non_booleans_properties.get_equalizer_value_for_band(i,stage,logger);
                Slider s = new Slider(MIN, MAX, value);
                s.setMinHeight(100);
                s.setMinWidth(30);
                s.setOrientation(Orientation.VERTICAL);
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
            Look_and_feel_manager.set_button_look(the_equalizer_vbox,true,stage,logger);
        }
    }

    //**********************************************************
    private Button make_reset_equalizer_button()
    //**********************************************************
    {
        Button reset_button = new Button(My_I18n.get_I18n_string("Reset_Equalizer",stage,logger));
        Look_and_feel_manager.set_button_look(reset_button,true,stage,logger);
        reset_button.setOnAction(actionEvent -> {
            for (int i = 0; i < equalizer_bands.size(); i++) {
                equalizer_bands.get(i).setGain(0.0);
                sliders.get(i).setValue(0.0);
                Non_booleans_properties.save_equalizer_value_for_band(i, 0.0,stage);
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
    public void set_total_duration(String s)
    //**********************************************************
    {
        Runnable r = () -> total_duration.setText(s);
        Platform.runLater(r);
    }


    //**********************************************************
    public void play_song_with_new_media_player(String new_song, boolean first_time)
    //**********************************************************
    {
        String encoded;
        try
        {
            encoded = "file://"+(new File(new_song)).getCanonicalPath();
        }
        catch (IOException e)
        {
            logger.log("ERROR "+e);
            return;
        }
        encoded = encoded.replaceAll(" ","%20");

        Media_instance_statics.play_this(encoded,this, first_time,stage,logger);

        Platform.runLater(() -> play_pause_button.setText(pause_string));
    }

    //**********************************************************
    public void set_playlist_name_display(String new_play_list_name)
    //**********************************************************
    {
        Runnable r = () -> playlist_name_label.setText(new_play_list_name);
        Platform.runLater(r);
    }




    //**********************************************************
    public void add_all_songs(List<Song> songs)
    //**********************************************************
    {
        Runnable r = () ->
        {
            the_vertical_box.getChildren().clear();
            for ( Song s : songs)
            {
                the_vertical_box.getChildren().add(s.node);
            }
            the_vertical_box.requestLayout();
            the_vertical_box.layout();
            //process_scroll(null); // Calculate visibility after layout
        };
        Platform.runLater(r);
    }
/*
    //**********************************************************
    public void add_one_song(Song song)
    //**********************************************************
    {
        Runnable r = () ->
        {
            the_vertical_box.getChildren().add(song.node);
            the_vertical_box.requestLayout();
            //the_vertical_box.layout();
            //process_scroll(null); // Calculate visibility after layout
        };
        Platform.runLater(r);
    }
*/
    //**********************************************************
    public void remove_song(Song song)
    //**********************************************************
    {
        Runnable r = () -> the_vertical_box.getChildren().remove(song.node);
        Platform.runLater(r);
    }


    //**********************************************************
    public void remove_all_songs()
    //**********************************************************
    {
        Runnable r = () -> the_vertical_box.getChildren().clear();
        Platform.runLater(r);
    }


    //**********************************************************
    public void scroll_to(String target)
    //**********************************************************
    {
        double x = playlist.get_scroll_for(target);
        double h = scroll_pane.getViewportBounds().getHeight();
        double max = scroll_pane.getContent().getLayoutBounds().getMaxY();
        double y = x * (1 + h/max)- h/2/max;
        if ( y < 0 ) y = 0;
        if ( y > 1 ) y = 1;
        double finalY = y;
        Platform.runLater(() -> scroll_pane.setVvalue(finalY));
    }

    //**********************************************************
    public void set_status(String s)
    //**********************************************************
    {
        Runnable r = () -> the_status_label.setText(s);
        Platform.runLater(r);
    }


    //**********************************************************
    private void save_new_playlist()
    //**********************************************************
    {

        // need to do it on swing event thread
        // as we use fileChooser to be able to display hidden folders
        Runnable r = () -> playlist.choose_playlist_file_name();
        Platform.runLater(r);
    }

    //**********************************************************
    public void play_playlist_internal(File playlist_file_)
    //**********************************************************
    {

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
        if ( keyword_dbg) logger.log("Audio_player_FX_UI KeyEvent="+key_event);

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
                        Progress_window x = Progress_window.show(
                                true,
                                "Importing audio tracks",
                                30*60,
                                stage.getX()+100,
                                stage.getY()+100,
                                stage,
                                logger);

                        List<String> file_names = import_from_youtube(content);
                        x.close();
                        if ( !file_names.isEmpty())
                        {
                            Platform.runLater(() -> playlist.user_wants_to_add_songs(file_names));
                        }
                    };
                    Actor_engine.execute(r,logger);
                } else {
                    System.out.println("No text content in clipboard");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error accessing clipboard");
            }
        }


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
            case DOWN:
                if ( keyword_dbg) logger.log("DOWN");
            case SPACE:
                if ( keyword_dbg) logger.log("space");
            case RIGHT:
                if ( keyword_dbg) logger.log("right");
                playlist.jump_to_next_from_user();
                break;

            case UP:
                if ( keyword_dbg) logger.log("UP");
            case LEFT:
                if ( keyword_dbg) logger.log("left");
                playlist.jump_to_previous_from_user();
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
    private List<String> import_from_youtube(String youtube_url)
    //**********************************************************
    {
        //yt-dlp -x --audio-format aac --audio-quality 0 https://youtu.be/3DB-uJ0TxKQ

        logger.log("going to extract audio tracks from URl:"+youtube_url);

        List<String> cmds = new ArrayList<>();
        cmds.add("yt-dlp");
        cmds.add("-4");
        cmds.add("-x");
        cmds.add("--audio-format");
        cmds.add("aac");
        cmds.add("--audio-quality");
        cmds.add("0");
        cmds.add(youtube_url);

        StringBuilder sb = new StringBuilder();
        String home = System.getProperty(Non_booleans_properties.USER_HOME);
        Execute_command.execute_command_list(cmds, new File(home), 20 * 1000, sb, logger);
        logger.log(sb.toString());

        List<String> returned = new ArrayList<>();
        String detector = "[ExtractAudio] Destination:";
        for ( String l : sb.toString().split("\n"))
        {
            if (l.startsWith(detector))
            {
                returned.add(home+ File.separator+l.substring(detector.length()).trim());
            }
        }
        return returned;
    }

    //**********************************************************
    private void toggle_play_stop()
    //**********************************************************
    {
        MediaPlayer.Status status = Media_instance_statics.get_status();
        if ( status== MediaPlayer.Status.PLAYING) set_is_paused();
        else set_is_playing();
    }


    //**********************************************************
    private void rewind()
    //**********************************************************
    {

        Media_instance_statics.stop();
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

    //**********************************************************
    public void change_song(String song, long start, boolean first_time)
    //**********************************************************
    {
        playlist.change_song(song, start, first_time);
    }

    //**********************************************************
    public void playlist_init()
    //**********************************************************
    {
        playlist.init();
        //Platform.runLater(()->process_scroll(null));
    }

    //**********************************************************
    public void set_selected()
    //**********************************************************
    {
        playlist.set_selected(System.currentTimeMillis());
    }
}
