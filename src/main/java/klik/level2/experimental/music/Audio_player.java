package klik.level2.experimental.music;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;
import klik.util.Popups;

import javax.swing.*;
import java.io.*;
import java.util.Objects;
import java.util.Optional;

import static klik.properties.Static_application_properties.USER_HOME;

//**********************************************************
public class Audio_player
//**********************************************************
{

    private static final boolean dbg =  false;
    public static final String PLAYLIST_EXTENSION = "playlist";
    public static final int WIDTH = 500;

    private  String playlist_name = null;
     File saving_dir = null;
     Stage stage;
     Button save_this_playlist;
     Button save_new_playlist;
     Button add_to_playlist;
     Button next;
     Button previous;
     Slider slider;
     Label now_value;
     Label duration_value;
     ObservableList<File> observable_playlist = FXCollections.observableArrayList();
     ListView<File> the_playlist_view;
     Logger logger;
     File the_song_file;
     private volatile MediaPlayer the_media_player;
     static Audio_player instance = null;



    //**********************************************************
    public static void play_song(File the_song_file, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new Audio_player(logger);
        }
        instance.play(the_song_file);

    }
    //**********************************************************
    public static void play_playlist(File file, Logger logger)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new Audio_player(logger);
        }
        instance.play_playlist_innernal(file);
    }

    //**********************************************************
    private void play(File the_song_file_)
    //**********************************************************
    {
        logger.log("play() "+the_song_file_.getAbsolutePath());

        clean_up();
        the_song_file = the_song_file_;

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

        Media sound;
        try
        {
            sound = new Media(encoded);
        }
        catch (IllegalArgumentException e)
        {
            Popups.popup_Exception(e,256,"Fatal",logger);
            return;
        }

        MediaPlayer local = new MediaPlayer(sound);
        local.setCycleCount(1);
        local.setOnStalled(() -> logger.log("\n\nWARNING player is stalling !!"));
        local.setOnReady(() -> {
            logger.log("player READY!");
            on_player_ready(local);
        });

        stage.show();
        //stage.setAlwaysOnTop(true);
    }

    private void on_player_ready(MediaPlayer local_) {

        the_media_player = local_;
        the_media_player.setOnEndOfMedia(() -> {
            logger.log("EndOfMedia");
            jump_to_next(the_song_file, logger);
        });

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

        if (observable_playlist.isEmpty())
        {
            add_to_playlist.setDisable(false);
            return;
        }
        for (File file : observable_playlist) {
            if (file.getAbsolutePath().equals(the_song_file.getAbsolutePath())) {
                add_to_playlist.setDisable(true);
                return;
            }
        }
        add_to_playlist.setDisable(false);
    }

    //**********************************************************
    public Audio_player(Logger logger_)
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
        slider.setOnMouseReleased(event -> {
            if ( the_media_player == null) return;
            slider.setValueChanging(true);
            double v = event.getX()/slider.getWidth()*slider.getMax();
            slider.setValue(v);
            logger.log("player seeking: slider new value = "+slider.getValue());
            Duration target = Duration.seconds(v);
            logger.log("player seeking:"+target);
            the_media_player.seek(target);
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

        {
            previous.setOnAction(actionEvent -> jump_to_previous(the_song_file, logger));
            if ( observable_playlist.isEmpty()) previous.setDisable(true);

            pause.setOnAction(actionEvent -> {
                if ( the_media_player == null) return;
                the_media_player.pause();
                pause.setDisable(true);
                restart.setDisable(false);
            });

            restart.setOnAction(actionEvent -> {
                if ( the_media_player == null) return;
                the_media_player.play();
                pause.setDisable(false);
                restart.setDisable(true);
            });

            rewind.setOnAction(actionEvent -> {
                if ( the_media_player == null) return;
                the_media_player.stop();
                the_media_player.play();
                pause.setDisable(false);
                restart.setDisable(true);
            });
            next.setOnAction(actionEvent -> jump_to_next(the_song_file, logger));
            if ( observable_playlist.isEmpty()) next.setDisable(true);
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

        String add_to_playlist_button_text = "Add to play list: "+playlist_name;
        if (playlist_name == null) add_to_playlist_button_text = "Add to play list";
        add_to_playlist = new Button(add_to_playlist_button_text);
        add_to_playlist.setDisable(true);
        {
            add_to_playlist.setOnAction(actionEvent -> add_to_playlist(the_song_file));
        }
        hb4.getChildren().add(add_to_playlist);
        save_this_playlist = new Button("Save current playlist");
        save_this_playlist.setDisable(true);
        {
            save_this_playlist.setOnAction(actionEvent -> save_this_playlist(stage,logger));
        }
        hb4.getChildren().add(save_this_playlist);
        save_new_playlist = new Button("Save as new playlist");
        {
            save_new_playlist.setOnAction(actionEvent -> save_playlist(stage,logger));
        }
        if( observable_playlist.isEmpty()) save_new_playlist.setDisable(true);
        hb4.getChildren().add(save_new_playlist);




        // this one is called only on EXTERNAL close requests
        // i.e. hitting the cross in the title
        stage.setOnCloseRequest(windowEvent -> {
            logger.log("stage setOnCloseRequest");
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
                            logger.log("setting style for " + item.getAbsolutePath());

                            setText(item.getAbsolutePath());
                            setStyle("-fx-control-inner-background: derive(#add8e6,15%)");
                        }
                    }
                };
            }
        });

        the_playlist_view.getSelectionModel().selectedItemProperty().addListener((observableValue, old_file, new_file) -> {
            if ( new_file==null) logger.log("PANIC event +new_file.==null ???");
            logger.log("list item selected: "+ Objects.requireNonNull(new_file).getAbsolutePath());
            play(new_file);
        });

        /*
        the_playlist_view.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                File item = the_playlist_view.getSelectionModel().getSelectedItem();
            }
        });
*/
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
                logger.log("setting selection at "+k+" for "+f.getAbsolutePath());
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
                logger.log("setting selection at "+k+" for "+f.getAbsolutePath());
                the_playlist_view.getSelectionModel().select(k);
            }
        }
    }

    //**********************************************************
    private  void add_to_playlist(File f)
    //**********************************************************
    {
        observable_playlist.add(f);
        if (playlist_name != null) {
            save_this_playlist.setDisable(false);
        }
        save_new_playlist.setDisable(false);
        previous.setDisable(false);
        next.setDisable(false);
    }
    //**********************************************************
    private  void save_playlist(Stage stage, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> run_on_swing_event_thread(stage, logger);
        SwingUtilities.invokeLater(r);
    }
    //**********************************************************
    private  void save_this_playlist(Stage stage, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> save(playlist_name,logger);
        SwingUtilities.invokeLater(r);
    }

    //**********************************************************
     void run_on_swing_event_thread(Stage stage, Logger logger)
    //**********************************************************
    {
        logger.log("save_playlist");

        JFileChooser chooser = new JFileChooser();
        chooser.setFileHidingEnabled(false); // ONLY reason to use SWING !!!
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
        Platform.runLater(() -> run_on_javafx_thread(logger));
    }

    //**********************************************************
    private  void run_on_javafx_thread(Logger logger)
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

        String new_name = result.get();
        save(new_name, logger);
    }

    //**********************************************************
    private  void save(String new_name,Logger logger)
    //**********************************************************
    {
        if ( !new_name.endsWith(PLAYLIST_EXTENSION)) new_name += "."+PLAYLIST_EXTENSION;
        logger.log("Saving playlist as:" + new_name);

        File savior = new File(saving_dir, new_name);
        try
        {
            FileWriter fw = new FileWriter(savior);
            for (File f : observable_playlist)
            {
                fw.write(f.getAbsolutePath() + "\n");
            }
            fw.close();
        }
        catch (IOException e)
        {
            logger.log(e.toString());
        }
    }




    //**********************************************************
    public void play_playlist_innernal(File playlist_file)
    //**********************************************************
    {

        logger.log("Going to play list: "+playlist_file.getAbsolutePath());

        playlist_name = playlist_file.getName();
        try {
            BufferedReader br = new BufferedReader( new FileReader(playlist_file));
            observable_playlist.clear();
            for(;;)
            {
                String song = br.readLine();
                if ( song == null ) break;
                observable_playlist.add(new File(song));
            }
        } catch (FileNotFoundException e) {
            logger.log(e.toString());
        } catch (IOException e) {
            logger.log(e.toString());
        }

        if ( observable_playlist.isEmpty())
        {
            logger.log("WARNING: playlist is empty !?");
            return;
        }
        previous.setDisable(false);
        next.setDisable(false);
        save_new_playlist.setDisable(false);
        File first = observable_playlist.get(0);
        play_song(first,logger);

    }
}
