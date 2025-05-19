package klik.audio;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.icons.animated_gifs.Ffmpeg_utils;
import klik.change.undo.Undo_core;
import klik.change.undo.Undo_item;
import klik.look.Look_and_feel_manager;
import klik.properties.Non_booleans;
import klik.util.files_and_paths.*;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Hourglass;
import klik.util.ui.Popups;
import klik.util.ui.Show_running_film_frame_with_abort_button;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;


//**********************************************************
public class Playlist
//**********************************************************
{
    private final static boolean dbg = true;
    static final String PLAYLIST_FILE_NAME = "PLAYLIST_FILE_NAME";
    ObservableList<File> observable_playlist = FXCollections.observableArrayList();
    Map<File, Button> file_to_button = new HashMap<>();
    Button selected = null;
    private static File playlist_file = null;
    File the_song_file;
    private final Logger    logger;
    private final Song_adding_receiver the_song_adding_receiver;
    File saving_dir = null;
    private final Undo_core undo_core;


    //**********************************************************
    public Playlist(Song_adding_receiver song_adding_receiver, Logger logger)
    //**********************************************************
    {
        this.the_song_adding_receiver = song_adding_receiver;
        this.logger = logger;
        this.undo_core = new Undo_core("undos_for_music_playlist.properties", logger);
    }

    //**********************************************************
    public void add_listener()
    //**********************************************************
    {
        observable_playlist.addListener((ListChangeListener<File>) change ->
        {
            while (change.next())
            {
                for (File f : change.getRemoved())
                {
                    remove_from_playlist_actual(f);
                }
                for (File f : change.getAddedSubList())
                {
                    define_button_for_a_song(f);
                }
            }
            save_observable_playlist();
        });
    }

    //**********************************************************
    private Button define_button_for_a_song(File f)
    //**********************************************************
    {
        Button local_button = new Button(f.getName());
        local_button.setMnemonicParsing(false);
        Look_and_feel_manager.set_button_look(local_button, false);
        {
            ContextMenu the_context_menu = new ContextMenu();
            Look_and_feel_manager.set_context_menu_look(the_context_menu);
            {
                MenuItem the_menu_item = new MenuItem("Browse folder");
                the_menu_item.setOnAction(_ -> Audio_player.start_new_process_to_browse(f.toPath().getParent(),
                                                                                        logger));
                the_context_menu.getItems().add(the_menu_item);
            }
            {
                MenuItem the_menu_item = new MenuItem("Remove from list");
                the_menu_item.setOnAction(_ -> observable_playlist.remove(f));
                the_context_menu.getItems().add(the_menu_item);
            }
            local_button.setOnContextMenuRequested((ContextMenuEvent event) ->
                                                   {
                                                       //set_background_to(local_button,"#90d5ff");
                                                       //if ( dbg)
                                                       logger.log("show context menu of button:" + f.toPath()
                                                                                                    .toAbsolutePath());
                                                       the_context_menu.show(local_button,
                                                                             event.getScreenX(),
                                                                             event.getScreenY());
                                                       //reset_background_to_default(local_button);
                                                   });


        }
        local_button.setPrefWidth(2000);
        Look_and_feel_manager.set_button_look(local_button, false);
        file_to_button.put(f, local_button);
        local_button.setOnAction(_ -> change_song(f));
        the_song_adding_receiver.add(local_button);

        return local_button;
    }


    //**********************************************************
    private void save_observable_playlist()
    //**********************************************************
    {
        if (playlist_file == null)
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
            logger.log(Stack_trace_getter.get_stack_trace("not saved" + e.toString()));
        }
    }

    public void add(List<File> the_list, Window owner, double x, double y)
    {
        Runnable r = () ->
        {
            Aborter local = new Aborter("adding songs", logger);
            Hourglass hourglass = Show_running_film_frame_with_abort_button.show_running_film("Loading songs", 20 * 60, x, y, logger);
            for (File f : the_list)
            {
                if (f.isDirectory())
                {
                    load_folder(f, owner, local);
                }
                else
                {
                    f = sanitize(f, owner, local, logger);
                    if (f != null) add_if_not_already_there(f);
                }
            }

            for (File f : the_list)
            {
                if (f.isDirectory())
                {
                    load_folder(f, owner, local);
                }
                else
                {
                    f = sanitize(f, owner, local, logger);
                    if (f != null) add_if_not_already_there(f);
                }

            }
            save_observable_playlist();
            hourglass.close();
        };
        Actor_engine.execute(r, logger);

    }


    //**********************************************************
    private void load_folder(File f, Window owner, Aborter aborter)
    //**********************************************************
    {
        File[] files = f.listFiles();
        if (files == null) return;
        for (File ff : files)
        {
            if (ff.isDirectory()) load_folder(ff, owner, aborter);
            else
            {
                ff = sanitize(ff, owner, aborter, logger);
                if (ff != null) add_if_not_already_there(ff);
            }
        }
    }



    //**********************************************************
    private void set_selected(File f)
    //**********************************************************
    {

        if (dbg) logger.log("set_selected " + f);
        Button future = file_to_button.get(f);
        if (selected == future)
        {
            // already selected
            if (dbg) logger.log("already selected " + f);
            return;
        }
        if (future != null)
        {
            set_background_to(future, "#90D5FF");
        }
        else
        {
            logger.log("this file is remove: " + f.getAbsolutePath());
            return;
        }
        if (selected != null)
        {
            reset_background_to_default(selected);
        }
        selected = future;
        the_song_adding_receiver.scroll_to(f);

    }

    //**********************************************************
    private void reset_background_to_default(Button button)
    //**********************************************************
    {
        if (dbg) logger.log("resetting background for previously selected");
        String s = button.getStyle();
        if (dbg) logger.log("style before = " + s);
        s = change_background_color(s, "#ffffff");
        if (dbg) logger.log("style after = " + s);
        button.setStyle(s);
    }

    //**********************************************************
    private void set_background_to(Button future, String color)
    //**********************************************************
    {
        String s = future.getStyle();
        if (dbg) logger.log("style before = " + s);
        s = change_background_color(s, color);
        if (dbg) logger.log("style after = " + s);
        future.setStyle(s);
    }


    //**********************************************************
    void change_song(File new_song)
    //**********************************************************
    {
        Integer current_time_s;
        if (new_song == null)
        {
            String path = Non_booleans.get_current_song();
            if (path == null)
            {
                current_time_s = null;
                if (observable_playlist == null) return;
                if (observable_playlist.isEmpty()) return;
                new_song = observable_playlist.get(0);
            }
            else
            {
                new_song = new File(path);
                current_time_s = Non_booleans.get_current_time_in_song();
            }
            if (new_song == null)
            {
                logger.log("FATAL: cannot cope with new_song is null");
                return;
            }
        }
        else
        {
            if (new_song.exists() == false)
            {
                logger.log(("FATAL: " + new_song.getAbsolutePath() + " does not exist"));
                the_song_adding_receiver.set_status("File not found: " + new_song.getAbsolutePath());
                observable_playlist.remove(new_song);
                return;
            }
            current_time_s = 0;
        }

        Non_booleans.save_current_song(new_song);

        double bitrate = Ffmpeg_utils.get_audio_bitrate(null, new_song.toPath(), logger);
        //logger.log("bitrate= "+bitrate);
        logger.log(new_song.getName() + " (bitrate= " + bitrate + " kb/s)");
        the_song_adding_receiver.set_status("Status: OK for:" + new_song.getName() + " (bitrate= " + bitrate + " kb/s)");

        the_song_adding_receiver.clean_up();
        the_song_file = new_song;
        add_if_not_already_there(the_song_file);

        the_song_adding_receiver.set_title(the_song_file.getName() + "       bitrate= " + bitrate + " kb/s");


        the_song_adding_receiver.play_song_with_new_media_player(new_song, current_time_s);
        set_selected(the_song_file);

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
        for (String item : items)
        {
            String[] parts = item.split(":");
            if (parts[0].trim().equals("-fx-background-color"))
            {
                found = true;
                returned += parts[0] + ":" + new_color + ";";
            }
            else
            {
                returned += item + ";";
            }
        }
        if (found == false)
        {
            returned += "-fx-background-color:" + new_color + ";";

        }
        //if ( returned.endsWith(";")) returned = returned.substring(0,returned.length()-1);
        return returned;
    }


    //**********************************************************
    static File sanitize(File song, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (!Guess_file_type.is_this_extension_an_audio(Static_files_and_paths_utilities.get_extension(song.getName())))
        {
            return null;
        }
        String parent = song.getParent();
        String file_name = song.getName();
        String new_name = Static_files_and_paths_utilities.get_base_name(file_name);
        new_name = new_name.replaceAll("\\[", "_");
        new_name = new_name.replaceAll("]", "_");
        new_name = new_name.replaceAll("\\(", "_");
        new_name = new_name.replaceAll("\\)", "_");
        new_name = new_name.replaceAll(" & ", "_and_");
        new_name = new_name.replaceAll("&", "_and_");
        new_name = new_name.replaceAll("-", "_");
        new_name = new_name.replaceAll("=", "_");
        new_name = new_name.replaceAll(":", "_");
        new_name = new_name.replaceAll(";", "_");
        new_name = new_name.replaceAll("\\?", "_");
        new_name = new_name.replaceAll("!", "_");
        new_name = new_name.replaceAll("\\.", "_");
        new_name = new_name.replaceAll("'", "_");
        new_name = new_name.replaceAll(",", "_");
        new_name = new_name.replaceAll(" ", "_");
        new_name = new_name.replaceAll("_+", "_");
        new_name = new_name.toLowerCase();
        new_name = new_name + "." + Static_files_and_paths_utilities.get_extension(file_name);


        if (new_name.equals(file_name))
        {
            return song;
        }
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(song.toPath(),
                                   Path.of(parent, new_name),
                                   Command_old_and_new_Path.command_rename,
                                   Status_old_and_new_Path.before_command,
                                   false));
        Moving_files.actual_safe_moves(owner, 100, 100, l, true, aborter, logger);
        File dest = new File(parent, new_name);
        try
        {
            song.renameTo(dest);
            logger.log("renamed " + song.getAbsolutePath() + " to " + dest.getAbsolutePath());
        }
        catch (Exception e)
        {
            logger.log("" + e);
            return song;
        }
        return dest;
    }

    public void init()
    {
        if (playlist_file == null)
        {
            playlist_file = get_playlist_file();
        }
        load_playlist(playlist_file);
    }


    //**********************************************************
    public static File get_playlist_file()
    //**********************************************************
    {
        String playlist_file_name = Non_booleans.get_main_properties_manager().get(PLAYLIST_FILE_NAME);
        if (playlist_file_name != null)
        {
            Path p = Path.of(playlist_file_name);
            if (p.isAbsolute())
            {
                if (p.toFile().exists())
                {
                    return p.toFile(); // OK, loading recorded playlist after checking
                }
            }
        }

        // new empty playlist with default name
        playlist_file_name = "playlist." + Guess_file_type.KLIK_AUDIO_PLAYLIST_EXTENSION;
        Non_booleans.get_main_properties_manager().add(PLAYLIST_FILE_NAME, playlist_file_name);
        String home = System.getProperty(Non_booleans.USER_HOME);
        Path p = Paths.get(home, Non_booleans.CONF_DIR, playlist_file_name);
        return p.toFile();
    }


    public String get_playlist_name()
    {
        playlist_file = get_playlist_file();
        //logger.log("playlist_file="+playlist_file.getAbsolutePath());
        String playlist_name_s = extract_playlist_name();
        logger.log("playlist_name=" + playlist_name_s);
        return playlist_name_s;
    }


    //**********************************************************
    private String extract_playlist_name()
    //**********************************************************
    {
        return Static_files_and_paths_utilities.get_base_name(playlist_file.getName());
    }


    //**********************************************************
    void jump_to_next()
    //**********************************************************
    {
        logger.log("jumping to next song");

        if (observable_playlist.isEmpty())
        {
            logger.log("empty playlist");
            return;
        }

        for (int i = 0; i < observable_playlist.size(); i++)
        {
            File file = observable_playlist.get(i);
            if (file.getAbsolutePath().equals(the_song_file.getAbsolutePath()))
            {
                logger.log("found current song in playlist as #" + i);

                int k = i + 1;
                if (k >= observable_playlist.size()) k = 0;
                File target = observable_playlist.get(k);
                change_song(target);
                return;
            }
        }
        logger.log("jumping to next song ... ??? current song not found");

    }

    //**********************************************************
    void jump_to_previous()
    //**********************************************************
    {
        if (observable_playlist.isEmpty()) return;
        for (int i = 0; i < observable_playlist.size(); i++)
        {
            File file = observable_playlist.get(i);
            if (file.getAbsolutePath().equals(the_song_file.getAbsolutePath()))
            {
                int k = i - 1;
                if (k < 0) k = observable_playlist.size() - 1;
                File target = observable_playlist.get(k);
                change_song(target);
                return;
            }
        }
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
                double returned = (double) i / (double) (observable_playlist.size() - 1);
                logger.log(" scroll to " + i + " => " + returned);
                return returned;
            }
        }
        return 1.0;
    }


    //**********************************************************
    void choose_playlist_file_name(Window owner)
    //**********************************************************
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileHidingEnabled(false); // reason to use SWING !!!
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (saving_dir == null)
        {
            String home = System.getProperty(Non_booleans.USER_HOME);
            saving_dir = new File(home, "playlists");
            if (!saving_dir.exists())
            {
                if (!saving_dir.mkdir())
                {
                    logger.log("WARNING: creating directory failed for: " + saving_dir.getAbsolutePath());
                }
            }
        }
        chooser.setCurrentDirectory(saving_dir.getParentFile());
        chooser.setSelectedFile(saving_dir);
        int status = chooser.showOpenDialog(null);
        if (status != JFileChooser.APPROVE_OPTION) return;
        saving_dir = chooser.getSelectedFile();
        Platform.runLater(() -> choose_playlist_name(owner));
    }

    //**********************************************************
    private void choose_playlist_name(Window owner)
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog("playlistname");
        Look_and_feel_manager.set_dialog_look(dialog);
        dialog.initOwner(owner);
        dialog.setTitle("Choose a name for the playlist");
        dialog.setContentText("playlistname");

        // The Java 8 way to get the response value (with lambda expression).
        //result.ifPresent(name -> logger.log("Your name: " + name));
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty())
        {
            logger.log("playlist not saved");
            Popups.popup_warning(owner, "Not saved ", "plylist not saved", true, logger);
            return;
        }

        String new_playlist_name = result.get();

        if (!new_playlist_name.endsWith(Guess_file_type.KLIK_AUDIO_PLAYLIST_EXTENSION))
            new_playlist_name += "." + Guess_file_type.KLIK_AUDIO_PLAYLIST_EXTENSION;

        change_play_list_name(new_playlist_name);

    }


    //**********************************************************
    public void change_play_list_name(String new_playlist_name)
    //**********************************************************
    {
        playlist_file = new File(saving_dir, new_playlist_name);
        save_observable_playlist();
        the_song_adding_receiver.set_playlist_name_display(extract_playlist_name());
    }

    //**********************************************************
    public boolean is_empty()
    //**********************************************************
    {
        return observable_playlist.isEmpty();
    }

    //**********************************************************
    public void play_fist_song()
    //**********************************************************
    {
        File first = observable_playlist.get(0);
        change_song(first);
    }


    //**********************************************************
    void load_playlist(File playlist_file_)//, Window owner, Aborter local_aborter)
    //**********************************************************
    {
        if (playlist_file_ == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return;
        }
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(playlist_file_));
            observable_playlist.clear();
            for (; ; )
            {
                String song_path = br.readLine();
                if (song_path == null) break;
                File song = new File(song_path);
                //song = sanitize(song, owner, local_aborter, logger);
                if (song != null) add_if_not_already_there(song);
            }
            playlist_file = playlist_file_;
        }
        catch (FileNotFoundException e)
        {
            try
            {
                playlist_file.createNewFile();
            }
            catch (IOException ex)
            {
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            }
            if (playlist_file.canWrite())
            {
                the_song_adding_receiver.set_playlist_name_display(extract_playlist_name());
                return;
            }
            playlist_file = null;
            logger.log(Stack_trace_getter.get_stack_trace("cannot write" + e.toString()));
        }
        catch (IOException e)
        {
            playlist_file = null;
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
    }


    //**********************************************************
    void remove_from_playlist_and_jump_to_next()
    //**********************************************************
    {
        File to_be_removed = the_song_file;
        jump_to_next(); // will change the song
        logger.log("removing from playlist: " + to_be_removed);
        observable_playlist.remove(to_be_removed); // will also update file_to_button in the event handler
    }



    //**********************************************************
    void remove_from_playlist_actual(File to_be_removed)
    //**********************************************************
    {
        the_song_adding_receiver.remove(file_to_button.get(to_be_removed));
        file_to_button.remove(to_be_removed);

        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(to_be_removed.toPath(),
                                   null,
                                   Command_old_and_new_Path.command_remove_for_playlist,
                                   Status_old_and_new_Path.before_command,
                                   false));
        Undo_item ui = new Undo_item(l, LocalDateTime.now(), UUID.randomUUID(), logger);
        undo_core.add(ui);
        save_observable_playlist();

    }


    //**********************************************************
    public void undo_remove()
    //**********************************************************
    {
        Undo_item last = undo_core.get_most_recent();
        if (last == null)
        {
            logger.log("nothing to undo");
            return;
        }
        List<Old_and_new_Path> l = last.oans;
        for (Old_and_new_Path o : l)
        {
            //if ( o.cmd != Command_old_and_new_Path.command_remove_for_playlist) continue;
            //if ( o.status != Status_old_and_new_Path.before_command) continue;
            if (o.old_Path == null) continue;
            logger.log("undo remove from play list for" + o.old_Path);
            File f = o.old_Path.toFile();
            logger.log("undo remove " + f);
            observable_playlist.add(f); // the listener will do everything
        }

        //save_observable_playlist();

        undo_core.remove_undo_item(last);

    }



    //**********************************************************
    private void add_if_not_already_there(File added_song)
    //**********************************************************
    {
        for (File file : observable_playlist)
        {
            if (file.getAbsolutePath().equals(added_song.getAbsolutePath()))
            {
                // that song is ALREADY in the list
                return;
            }
        }
        observable_playlist.add(added_song);
    }

}
