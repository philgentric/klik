package klik.audio;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.log.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

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
    private static File playlist_file = null;
    File saving_dir = null;
    Stage stage;
    Button save_as_new_playlist;
    Button next;
    Button previous;
    Slider slider;
    Label now_value;
    Label duration_value;
    ObservableList<File> observable_playlist = FXCollections.observableArrayList();
    ListView<File> the_playlist_view;
    Logger logger;
    File the_song_file;
    private volatile Optional<MediaPlayer> the_media_player_option = Optional.empty();
    double volume = 0.5;
    static Audio_player instance = null;
    Label play_list_name;
    //**********************************************************
    public static void play_song(File the_song_file, Logger logger)
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
        //logger.log("play() "+the_song_file_.getAbsolutePath());

        clean_up();
        the_song_file = the_song_file_;
        add_and_save_if_needed();

        stage.setTitle(the_song_file.getName());

        String encoded;
        try {
            encoded = "file://"+the_song_file.getCanonicalPath();
        } catch (IOException e) {
            logger.log("\n\nFATAL: "+e);
            return;
        }
        encoded = encoded.replaceAll(" ","%20");
        //UrlEscapers.urlFragmentEscaper().escape("file://"+f.getCanonicalPath());

        try
        {
            Media sound = new Media(encoded);

            MediaPlayer tmp_media_player = new MediaPlayer(sound);
            tmp_media_player.setCycleCount(1);
            tmp_media_player.setOnStalled(() -> logger.log("\n\nWARNING player is stalling !!"));
            tmp_media_player.setOnReady(() -> {
                //logger.log("player READY!");
                on_player_ready(tmp_media_player);
            });

        }
        catch (MediaException me)
        {
            logger.log(Stack_trace_getter.get_stack_trace(me.toString()));
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
    private void add_and_save_if_needed()
    //**********************************************************
    {
        for (File file : observable_playlist)
        {
            if (file.getAbsolutePath().equals(the_song_file.getAbsolutePath()))
            {
                the_playlist_view.getSelectionModel().select(the_song_file);
                return;
            }
        }
        observable_playlist.add(the_song_file);
        the_playlist_view.getSelectionModel().select(the_song_file);
        save();
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
        the_media_player_option.get().setOnEndOfMedia(() -> {
            //logger.log("EndOfMedia");
            jump_to_next(the_song_file, logger);
        });

        // the player pilots how the slider moves during playback
        the_media_player_option.get().currentTimeProperty().addListener((ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) -> {
            if (dbg) logger.log("player changing current time:"+newValue.toSeconds());
            double seconds = newValue.toSeconds();
            slider.setValue(seconds);
            now_value.setText((int)seconds+" seconds");
        });

        //logger.log("song start:"+ player.getStartTime().toSeconds());
        //logger.log("song stop:"+ player.getStopTime().toSeconds());
        double seconds = the_media_player_option.get().getStopTime().toSeconds();
        slider.setValue(0);
        slider.setMax(seconds);
        the_media_player_option.get().setVolume(volume);
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
        VBox vbox = new VBox();
        HBox hb1 = new HBox();
        Label duration_text = new Label("Duration: ");
        hb1.getChildren().add(duration_text);
        duration_value = new Label("0.0s");
        hb1.getChildren().add(duration_value);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hb1.getChildren().add(spacer);

        Label now_text = new Label("Now: ");
        hb1.getChildren().add(now_text);
        now_value = new Label("0.0s");
        hb1.getChildren().add(now_value);
        vbox.getChildren().add(hb1);

        slider = new Slider();
        slider.setMinWidth(WIDTH);
        slider.setPrefWidth(WIDTH);
        vbox.getChildren().add(slider);




        // but the user may click/slide the slider
        slider.setOnMouseReleased(event -> {
            if ( the_media_player_option.isEmpty()) return;
            slider.setValueChanging(true);
            double v = event.getX()/slider.getWidth()*slider.getMax();
            slider.setValue(v);
            //logger.log("player seeking: slider new value = "+slider.getValue());
            Duration target = Duration.seconds(v);
            //logger.log("player seeking:"+target);
            the_media_player_option.get().seek(target);
            slider.setValueChanging(false);
        });

        HBox hb3 = new HBox();
        vbox.getChildren().add(hb3);
        previous = new Button("Jump to previous");
        hb3.getChildren().add(previous);
        Button pause = new Button("Pause");
        hb3.getChildren().add(pause);
        Button restart = new Button("Restart");
        restart.setDisable(true);
        hb3.getChildren().add(restart);
        Button rewind = new Button("Rewind");
        hb3.getChildren().add(rewind);
        next = new Button("Jump to next ");
        hb3.getChildren().add(next);
        Button delete = new Button("Remove from playlist ");
        hb3.getChildren().add(delete);
        {
            ImageView iv = new ImageView(Look_and_feel_manager.get_speaker_icon());
            iv.setFitHeight(20);
            iv.setFitWidth(20);
            hb3.getChildren().add(iv);
        }

        Slider volume_slider = new Slider(0,1, 0.5);
        hb3.getChildren().add(volume_slider);

        volume_slider.valueProperty().addListener((observableValue, number, t1) -> {
            if ( the_media_player_option.isEmpty()) return;
            volume = volume_slider.getValue();
            the_media_player_option.get().setVolume(volume);
        });

        {
            previous.setOnAction(actionEvent -> jump_to_previous(the_song_file, logger));
            if ( observable_playlist.isEmpty()) previous.setDisable(true);

            pause.setOnAction(actionEvent -> {
                if ( the_media_player_option.isEmpty()) return;
                the_media_player_option.get().pause();
                pause.setDisable(true);
                restart.setDisable(false);
            });

            restart.setOnAction(actionEvent -> {
                if (the_media_player_option.isEmpty()) return;
                the_media_player_option.get().play();
                pause.setDisable(false);
                restart.setDisable(true);
            });

            rewind.setOnAction(actionEvent -> {
                if (the_media_player_option.isEmpty()) return;
                the_media_player_option.get().stop();
                the_media_player_option.get().play();
                pause.setDisable(false);
                restart.setDisable(true);
            });
            next.setOnAction(actionEvent -> jump_to_next(the_song_file, logger));
            if ( observable_playlist.isEmpty()) next.setDisable(true);

            delete.setOnAction(actionEvent -> remove_from_playlist());
        }

        Button cancel = new Button("Stop & close");
        {
            cancel.setOnAction(actionEvent -> {
                clean_up();
                stage.close();
                instance = null;
            });
        }
        vbox.getChildren().add(cancel);

        HBox hb4 = new HBox();
        vbox.getChildren().add(hb4);

        playlist_file = get_playlist_file(logger);
        //logger.log("playlist_file="+playlist_file.getAbsolutePath());
        String play_list_name_s = extract_playlist_name();
        //logger.log("playlist_name="+play_list_name_s);

        play_list_name = new Label(play_list_name_s);
        hb4.getChildren().add(play_list_name);


        save_as_new_playlist = new Button("Save as new playlist");
        {
            save_as_new_playlist.setOnAction(actionEvent -> save_new_playlist());
        }
        hb4.getChildren().add(save_as_new_playlist);



        // this one is called only on EXTERNAL close requests
        // i.e. hitting the cross in the title
        stage.setOnCloseRequest(windowEvent -> {
            logger.log("Audio player closing");
            clean_up();
            stage.close();
            instance = null;
        });

        the_playlist_view = new ListView<>();
        vbox.getChildren().add(the_playlist_view);
        the_playlist_view.setItems(observable_playlist);
        the_playlist_view.setCellFactory(new Callback<>() {
            @Override
            public ListCell<File> call(ListView<File> fileListView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(File item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null) {
                            //logger.log("shit happens 1");
                            setText(null);
                        } else if (empty) {
                            logger.log("shit happens 2");
                            setText(null);
                        } else {
                            //logger.log("setting style for " + item.getAbsolutePath());
                            setText(item.getAbsolutePath());
                            setStyle("-fx-control-inner-background: derive(#add8e6,15%)");
                        }
                    }
                };
            }
        });

        the_playlist_view.getSelectionModel().selectedItemProperty().addListener((observableValue, old_file, new_file) -> {
            if ( new_file==null)
            {
                logger.log("PANIC event +new_file.==null ???");
                return;
            }
            //logger.log("list item selected: "+ Objects.requireNonNull(new_file).getAbsolutePath());
            play_song(new_file);
        });

        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();

    }

    //**********************************************************
    private void remove_from_playlist()
    //**********************************************************
    {
        observable_playlist.remove(the_song_file);
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
    private  void jump_to_next(File f, Logger logger)
    //**********************************************************
    {
        if ( observable_playlist.isEmpty()) return;

        for (int i = 0; i < observable_playlist.size(); i++)
        {
            if ( observable_playlist.get(i).getAbsolutePath().equals(f.getAbsolutePath()))
            {
                int k = i+1;
                if (k >= observable_playlist.size()) k = 0;
                //play(observable_playlist.get(k), logger);
                the_playlist_view.scrollTo(k);
                //logger.log("setting selection at "+k+" for "+f.getAbsolutePath());
                the_playlist_view.getSelectionModel().select(k);
            }
        }

    }
    //**********************************************************
    private  void jump_to_previous(File f, Logger logger)
    //**********************************************************
    {
        if ( observable_playlist.isEmpty()) return;
        for (int i = 0; i < observable_playlist.size(); i++)
        {
            if ( observable_playlist.get(i).getAbsolutePath().equals(f.getAbsolutePath()))
            {
                int k = i-1;
                if (k < 0 ) k = observable_playlist.size()-1;
                //play(observable_playlist.get(k), logger);
                the_playlist_view.scrollTo(k);
                //logger.log("setting selection at "+k+" for "+f.getAbsolutePath());
                the_playlist_view.getSelectionModel().select(k);
            }
        }
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
        play_song(first,logger);

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
