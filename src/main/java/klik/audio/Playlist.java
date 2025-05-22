package klik.audio;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.workers.Actor_engine_based_on_workers;
import klik.browser.icons.animated_gifs.Ffmpeg_utils;
import klik.change.undo.Undo_core;
import klik.change.undo.Undo_item;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans;
import klik.util.files_and_paths.*;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Popups;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;


//**********************************************************
public class Playlist
//**********************************************************
{
    private final static boolean dbg = true;
    private final Logger logger;
    static final String PLAYLIST_FILE_NAME = "PLAYLIST_FILE_NAME";

    List<String> the_playlist = new ArrayList<>();
    //private ObservableList<String> observable_playlist = FXCollections.observableArrayList();
    Map<String, Button> file_to_button = new HashMap<>();
    Button selected = null;

    private static File playlist_file = null;
    String the_song_path;
    private final Music_UI the_music_ui;
    File saving_dir = null;
    private final Undo_core undo_core;
    private final Window owner;


    //**********************************************************
    public Playlist(Music_UI the_music_ui, Window owner, Logger logger)
    //**********************************************************
    {
        this.the_music_ui = the_music_ui;
        this.owner = owner;
        this.logger = logger;
        this.undo_core = new Undo_core("undos_for_music_playlist.properties", logger);
    }


    //**********************************************************
    private Button add_to_playlist(String file_path)
    //**********************************************************
    {
        the_playlist.add(file_path);
        Button local_button = define_button_for_a_song(file_path);
        the_music_ui.add_song(local_button);
        return local_button;
    }

    //**********************************************************
    private void add_all_to_playlist(List<String> file_paths)
    //**********************************************************
    {
        the_playlist.addAll(file_paths);
        List<Button> local_buttons = new ArrayList<>();
        for ( String file_path : file_paths)
        {
            Button local_button = define_button_for_a_song(file_path);
            local_buttons.add(local_button);
        }
        the_music_ui.add_songs(local_buttons);
    }


    //**********************************************************
    private Button define_button_for_a_song(String file_path)
    //**********************************************************
    {
        File f = new File(file_path);
        Button local_button = new Button(f.getParentFile().getName() + "    /    " + f.getName());
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
                MenuItem the_menu_item = new MenuItem(My_I18n.get_I18n_string("Rename", logger));
                the_menu_item.setOnAction(_ -> {

                    Path new_path =  Static_files_and_paths_utilities.ask_user_for_new_file_name(owner,Path.of(file_path),logger);
                    if ( new_path == null) return;

                    List<Old_and_new_Path> l = new ArrayList<>();
                    Old_and_new_Path oandn = new Old_and_new_Path(Path.of(file_path), new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command,false);
                    l.add(oandn);
                    Moving_files.perform_safe_moves_in_a_thread(owner, owner.getX()+100, owner.getY()+100,l, true, new Aborter("rename in playlist",logger), logger);

                    remove_from_playlist(file_path);
                    add_to_playlist(new_path.toAbsolutePath().toString());
                    if ( the_song_path.equals(file_path)) the_song_path = new_path.toAbsolutePath().toString();
                });
                the_context_menu.getItems().add(the_menu_item);
            }
            {
                MenuItem the_menu_item = new MenuItem("Remove from list");
                the_menu_item.setOnAction(_ -> remove_from_playlist(file_path));
                the_context_menu.getItems().add(the_menu_item);
            }
            local_button.setOnContextMenuRequested((ContextMenuEvent event) -> the_context_menu.show(local_button, event.getScreenX(), event.getScreenY()));

        }
        local_button.setPrefWidth(2000);
        Look_and_feel_manager.set_button_look(local_button, false);
        file_to_button.put(file_path, local_button);
        local_button.setOnAction(_ -> change_song(file_path));
        return local_button;
    }


    //**********************************************************
    private void remove_from_playlist(String to_be_removed)
    //**********************************************************
    {
        the_playlist.remove(to_be_removed);
        the_music_ui.remove_song(file_to_button.get(to_be_removed));
        file_to_button.remove(to_be_removed);

        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(Path.of(to_be_removed),
                null,
                Command_old_and_new_Path.command_remove_for_playlist,
                Status_old_and_new_Path.before_command,
                false));
        Undo_item ui = new Undo_item(l, LocalDateTime.now(), UUID.randomUUID(), logger);
        undo_core.add(ui);
        save_playlist();

    }


    
    
    
    
    
    

    //**********************************************************
    private synchronized void save_playlist()
    //**********************************************************
    {
        if (playlist_file == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return;
        }
        logger.log("Saving playlist as:" + playlist_file.getAbsolutePath());
        try
        {
            int count = 0;
            FileWriter fw = new FileWriter(playlist_file);
            for (String f : the_playlist)
            {
                fw.write(f + "\n");
                count++;
            }
            fw.close();
            logger.log("Saved "+count+" songs in playlist file named:" + playlist_file.getAbsolutePath());
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("not saved" + e.toString()));
        }
    }

    //**********************************************************
    public void user_wants_to_add_songs(List<String> the_list)
    //**********************************************************
    {
        Runnable r = () ->
        {
            List<Old_and_new_Path> to_be_renamed_first = new ArrayList<>();
            List<String> oks = new ArrayList<>();
            for (String path : the_list)
            {
                File f = new File(path);
                if (f.isDirectory())
                {
                    load_folder(f, oks,to_be_renamed_first);
                }
                else
                {
                    sanitize(path,  oks,to_be_renamed_first,logger);
                }
            }
            Moving_files.actual_safe_moves(owner, owner.getX()+100, owner.getY()+ 100, to_be_renamed_first, true, new Aborter("actual_safe_moves",logger), logger);
            logger.log(to_be_renamed_first.size()+ " files RENAMED to be accepted as possible songs");

            String last = null;
            List<String> finaly = new ArrayList<>();
            for ( Old_and_new_Path o : to_be_renamed_first)
            {
                if ( !the_playlist.contains(o.new_Path.toAbsolutePath().toString()))
                {
                    finaly.add(o.new_Path.toAbsolutePath().toString());
                    last = o.new_Path.toAbsolutePath().toString();
                }
            }
            for ( String f : oks)
            {
                if ( !the_playlist.contains(f))
                {
                    finaly.add(f);
                    last = f;
                }
            }
            logger.log(finaly.size()+ " files accepted as possible songs");
            add_all_to_playlist(finaly);
            if ( last != null)
            {
                save_playlist();
                change_song(last);
            }
            update_playlist_size_info();
        };
        Actor_engine.execute(r, logger);

    }


    //**********************************************************
    private void load_folder(File folder, List<String> oks, List<Old_and_new_Path> out)
    //**********************************************************
    {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File ff : files)
        {
            if (ff.isDirectory()) load_folder(ff, oks, out);
            else
            {
                sanitize(ff.getAbsolutePath(), oks, out,logger);
            }
        }
    }



    //**********************************************************
    private void set_selected(String f)
    //**********************************************************
    {

        if (dbg) logger.log("set_selected " + f);
        Button future = file_to_button.get(f);
        if ( future == null)
        {
            if ( dbg) logger.log("WARNING: this file is not mapped: " + f);
            return;
        }
        if ( selected != null)
        {
            if (selected == future)
            {
                // already selected
                if (dbg) logger.log("already selected " + f);
                return;
            }
            set_background_to(future, "#90D5FF");
            reset_background_to_default(selected);
        }
        selected = future;
        the_music_ui.scroll_to(f);

    }

    //**********************************************************
    private void reset_background_to_default(Button button)
    //**********************************************************
    {
        //if (dbg) logger.log("resetting background for previously selected");
        String s = button.getStyle();
        //if (dbg) logger.log("style before = " + s);
        s = change_background_color(s, "#ffffff");
        //if (dbg) logger.log("style after = " + s);
        button.setStyle(s);
    }

    //**********************************************************
    private void set_background_to(Button future, String color)
    //**********************************************************
    {
        String s = future.getStyle();
        //if (dbg) logger.log("style before = " + s);
        s = change_background_color(s, color);
        //if (dbg) logger.log("style after = " + s);
        future.setStyle(s);
    }


    //**********************************************************
    void change_song(String new_song)
    //**********************************************************
    {
        Integer current_time_s;
        if (new_song == null)
        {
            String path = Non_booleans.get_current_song();
            if (path == null)
            {
                current_time_s = null;
                if (the_playlist == null) return;
                if (the_playlist.isEmpty()) return;
                new_song = the_playlist.get(0);
            }
            else
            {
                new_song = path;
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
            if ((new File(new_song)).exists() == false)
            {
                if ( dbg) logger.log(("warning: " + new_song + " does not exist"));
                the_music_ui.set_status("File not found: " + new_song);
                remove_from_playlist(new_song);
                save_playlist();
                return;
            }
            current_time_s = 0;
        }



        double bitrate = Ffmpeg_utils.get_audio_bitrate(null, Path.of(new_song), logger);
        if ( dbg) logger.log(  (new File(new_song)).getName() + " (bitrate= " + bitrate + " kb/s)");
        the_music_ui.set_status("Status: OK for:" + (new File(new_song)).getName() + " (bitrate= " + bitrate + " kb/s)");


        the_song_path = new_song;
        the_music_ui.set_title((new File(new_song)).getName() + "       bitrate= " + bitrate + " kb/s");

        the_music_ui.stop_current_media();
        add_one_song_to_playlist_if_not_already_there(the_song_path);

        the_music_ui.play_song_with_new_media_player(new_song, current_time_s);
        set_selected(the_song_path);
        Non_booleans.save_current_song(new_song);

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
    static void sanitize(String song, List<String> oks, List<Old_and_new_Path> out,Logger logger)
    //**********************************************************
    {
        if (!Guess_file_type.is_this_extension_an_audio(Static_files_and_paths_utilities.get_extension((new File(song)).getName())))
        {
            if ( dbg) logger.log("Rejected as a possible song due to extension: "+(new File(song)).getName());
            return;
        }
        String parent = (new File(song)).getParent();
        String file_name = (new File(song)).getName();
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
        new_name = new_name.replaceAll("\\{", "_");
        new_name = new_name.replaceAll("\\}", "_");
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
            oks.add(song);
            return;
        }

        out.add(new Old_and_new_Path(Path.of(song), Path.of(parent, new_name),Command_old_and_new_Path.command_rename,Status_old_and_new_Path.before_command,false));

    }

    //**********************************************************
    public void init()
    //**********************************************************
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
        Non_booleans.get_main_properties_manager().set(PLAYLIST_FILE_NAME, playlist_file_name);
        String home = System.getProperty(Non_booleans.USER_HOME);
        Path p = Paths.get(home, Non_booleans.CONF_DIR, playlist_file_name);
        return p.toFile();
    }


    //**********************************************************
    public String get_playlist_name()
    //**********************************************************
    {
        playlist_file = get_playlist_file();
        if ( dbg) logger.log("playlist_file="+playlist_file.getAbsolutePath());
        String playlist_name_s = extract_playlist_name();
        if ( dbg) logger.log("playlist_name=" + playlist_name_s);
        return playlist_name_s;
    }


    //**********************************************************
    String extract_playlist_name()
    //**********************************************************
    {
        return Static_files_and_paths_utilities.get_base_name(playlist_file.getName());
    }


    //**********************************************************
    void jump_to_next()
    //**********************************************************
    {
        if ( dbg) logger.log("jumping to next song");

        if (the_playlist.isEmpty())
        {
            logger.log("Warning: empty playlist");
            return;
        }

        for (int i = 0; i < the_playlist.size(); i++)
        {
            String file = the_playlist.get(i);
            if (file.equals(the_song_path))
            {
                if ( dbg) logger.log("found current song in playlist as #" + i);

                int k = i + 1;
                if (k >= the_playlist.size()) k = 0;
                String target = the_playlist.get(k);
                change_song(target);
                return;
            }
        }
        logger.log("FATAL: jumping to next song ... ??? current song not found");

    }

    //**********************************************************
    void jump_to_previous()
    //**********************************************************
    {
        if (the_playlist.isEmpty()) return;
        for (int i = 0; i < the_playlist.size(); i++)
        {
            String file = the_playlist.get(i);
            if (file.equals(the_song_path))
            {
                int k = i - 1;
                if (k < 0) k = the_playlist.size() - 1;
                String target = the_playlist.get(k);
                change_song(target);
                return;
            }
        }
    }


    //**********************************************************
    double get_scroll_for(String target)
    //**********************************************************
    {
        for (int i = 0; i < the_playlist.size(); i++)
        {
            String file = the_playlist.get(i);
            if (file.equals(target))
            {
                double returned = (double) i / (double) (the_playlist.size() - 1);
                if ( dbg) logger.log(" scroll to " + i + " => " + returned);
                return returned;
            }
        }
        return 1.0;
    }


    //**********************************************************
    void choose_playlist_file_name()
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
        Platform.runLater(() -> choose_playlist_name());
    }

    //**********************************************************
    private void choose_playlist_name()
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
        Non_booleans.get_main_properties_manager().set(PLAYLIST_FILE_NAME, new_playlist_name);
        playlist_file = new File(saving_dir, new_playlist_name);
        save_playlist();
        the_music_ui.set_playlist_name_display(extract_playlist_name());
    }

    //**********************************************************
    public boolean is_empty()
    //**********************************************************
    {
        return the_playlist.isEmpty();
    }

    //**********************************************************
    public void play_fist_song()
    //**********************************************************
    {
        if ( the_playlist.isEmpty()) return;
        String first = the_playlist.get(0);
        change_song(first);
    }


    //**********************************************************
    synchronized void  load_playlist(File playlist_file_)
    //**********************************************************
    {
        logger.log("Loading playlist as:" + playlist_file_.getAbsolutePath());
        if (playlist_file_ == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return;
        }
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(playlist_file_));
            the_playlist.clear();
            the_music_ui.remove_all_songs();
            for (;;)
            {
                String song_path = br.readLine();
                if (song_path == null) break;
                add_to_playlist(song_path);
            }
            playlist_file = playlist_file_;
            Non_booleans.get_main_properties_manager().set(PLAYLIST_FILE_NAME, playlist_file.getAbsolutePath());

            logger.log("\n\nloaded " + the_playlist.size() + " songs from file:" + playlist_file.getAbsolutePath() + "\n\n");
            update_playlist_size_info();

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
                the_music_ui.set_playlist_name_display(extract_playlist_name());
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
    private void update_playlist_size_info()
    //**********************************************************
    {
        Runnable r = () -> update_playlist_size_info_in_a_thread();
        Actor_engine.execute(r,logger);
    }

    //**********************************************************
    private void update_playlist_size_info_in_a_thread()
    //**********************************************************
    {
        Actor_engine_based_on_workers local = new Actor_engine_based_on_workers(logger);
        AtomicLong seconds = new AtomicLong(0);
        CountDownLatch cdl = new CountDownLatch(the_playlist.size());
        for ( String path: the_playlist)
        {
            Runnable r = () -> get_media_duration(path, seconds, cdl);
            local.execute_internal(r,logger);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log(e.toString());
        }

        Runnable r = () -> the_music_ui.set_total_duration("Playlist: " + the_playlist.size() + " songs, " + Audio_player_FX_UI.get_nice_string_for_duration(seconds.get()));
        Platform.runLater(r);

    }

    //**********************************************************
    private void get_media_duration(String path, AtomicLong seconds, CountDownLatch cdl)
    //**********************************************************
    {
        double dur = Ffmpeg_utils.get_video_duration(null, Path.of(path), logger);
        seconds.addAndGet((long) dur);
        cdl.countDown();
    }


    //**********************************************************
    void remove_from_playlist_and_jump_to_next()
    //**********************************************************
    {
        String to_be_removed = the_song_path;
        jump_to_next(); // will change the song
        if ( dbg) logger.log("removing from playlist: " + to_be_removed);
        remove_from_playlist(to_be_removed);
        save_playlist();
    }


    //**********************************************************
    public void undo_remove()
    //**********************************************************
    {
        Undo_item last = undo_core.get_most_recent();
        if (last == null)
        {
            if ( dbg) logger.log("nothing to undo");
            return;
        }
        List<Old_and_new_Path> l = last.oans;
        for (Old_and_new_Path o : l)
        {
            //if ( o.cmd != Command_old_and_new_Path.command_remove_for_playlist) continue;
            //if ( o.status != Status_old_and_new_Path.before_command) continue;
            if (o.old_Path == null) continue;
            if ( dbg) logger.log("undo remove from play list for" + o.old_Path);
            add_to_playlist(o.old_Path.toAbsolutePath().toString());
        }

        save_playlist();

        undo_core.remove_undo_item(last);

    }



    //**********************************************************
    private boolean add_one_song_to_playlist_if_not_already_there(String added_song)
    //**********************************************************
    {
        if ( the_playlist.contains(added_song))
        {
            // that song is ALREADY in the list
            if ( dbg) logger.log("Song already listed: "+added_song);
            return false;
        }
        if ( dbg) logger.log("Added song: "+added_song);
        add_to_playlist(added_song);
        return true;
    }

    //**********************************************************
    public void shuffle()
    //**********************************************************
    {
        the_music_ui.remove_all_songs();
        Collections.shuffle(the_playlist);
        List<Button> local_buttons = new ArrayList<>();
        String selected_path = null;
        for ( String path : the_playlist)
        {
            Button local_button = file_to_button.get(path);
            local_buttons.add(local_button);
            if ( local_button == selected) selected_path = path;
        }
        the_music_ui.add_songs(local_buttons);
        the_music_ui.scroll_to(selected_path);



    }

    //**********************************************************
    public void remove(String s)
    //**********************************************************
    {
        remove_from_playlist(s);
    }
}
