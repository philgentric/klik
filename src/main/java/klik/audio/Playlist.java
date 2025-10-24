package klik.audio;
//SOURCES ../change/undo/Undo_core.java

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.Shared_services;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor;
import klik.util.execute.actor.Actor_engine;
import klik.util.execute.actor.Message;
import klik.util.execute.actor.workers.Actor_engine_based_on_workers;
import klik.browser.Drag_and_drop;
import klik.util.animated_gifs.Ffmpeg_utils;
import klik.change.undo.Undo_core;
import klik.change.undo.Undo_item;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans_properties;
import klik.util.files_and_paths.*;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Folder_chooser;
import klik.util.ui.Popups;

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
    private final static boolean dbg = false;
     private final Logger logger;
    static final String PLAYLIST_FILE_NAME = "PLAYLIST_FILE_NAME";

    private List<String> the_playlist = new ArrayList<>();
    private Map<String, Song> path_to_Song = new HashMap<>();
    Song selected = null;

    private static File playlist_file = null;
    String the_song_path;
    private final Audio_player_FX_UI the_music_ui;
    File saving_dir = null;
    private final Undo_core undo_core;
    private final Window owner;


    //**********************************************************
    public Playlist(Audio_player_FX_UI the_music_ui, Window owner, Logger logger)
    //**********************************************************
    {
        this.the_music_ui = the_music_ui;
        this.owner = owner;
        this.logger = logger;
        this.undo_core = new Undo_core("undos_for_music_playlist.properties", owner,logger);
    }


    //**********************************************************
    void add_to_playlist(String file_path)
    //**********************************************************
    {
        the_playlist.add(file_path);
        Node node = define_node_for_a_song(file_path);
        Song local = new Song(file_path,node);
        local.init(this,owner,logger);
        path_to_Song.put(file_path, local);
        update_display();
    }

    //**********************************************************
    private void add_all_to_playlist(List<String> file_paths)
    //**********************************************************
    {
        the_playlist.clear();
        the_playlist.addAll(file_paths);
        path_to_Song.clear();
        for (String file_path : file_paths)
        {
            Node node = define_node_for_a_song(file_path);
            Song local = new Song(file_path, node);
            local.init(this,owner,logger);
            path_to_Song.put(file_path, local);
            //logger.log("added "+file_path);
        }
        update_display();
    }


    //**********************************************************
    private Node define_node_for_a_song(String file_path)
    //**********************************************************
    {
        File f = new File(file_path);
        //Label node = new Label(f.getParentFile().getName() + "    /    " + f.getName());
        //Look_and_feel_manager.set_label_look(node,owner,logger);

        Button node = new Button(f.getParentFile().getName() + "    /    " + f.getName());
        node.setMnemonicParsing(false);
        Look_and_feel_manager.set_button_look(node,false,owner,logger);
        node.setPrefWidth(2000);
        node.getStyleClass().add("unselected_song");
        // the node active part is set when is becomes visible
        Song local = new Song(file_path, node);
        local.init(this,owner,logger);
        path_to_Song.put(file_path, local);
        return node;
    }



    //**********************************************************
    void remove_from_playlist(String to_be_removed)
    //**********************************************************
    {
        the_playlist.remove(to_be_removed);
        {
            Song local = path_to_Song.get(to_be_removed);
            if ( local != null)
            {
                the_music_ui.remove_song(local);
                path_to_Song.remove(to_be_removed);
            }
        }

        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(Path.of(to_be_removed),
                Path.of(to_be_removed),
                Command.command_remove_for_playlist,
                Status.before_command,
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
        long start = System.currentTimeMillis();
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
            Moving_files.actual_safe_moves(to_be_renamed_first, true,  owner.getX()+100, owner.getY()+ 100, owner, new Aborter("dummy",logger), logger);
            logger.log(to_be_renamed_first.size()+ " files RENAMED to be accepted as possible songs");

            String last = null;
            List<String> finaly = new ArrayList<>();
            for ( Old_and_new_Path o : to_be_renamed_first)
            {
                if ( !the_playlist.contains(o.new_Path.toAbsolutePath().toString()))
                {
                    finaly.add(o.new_Path.toAbsolutePath().toString());
                }
                else
                {
                    logger.log(o.new_Path.toAbsolutePath().toString()+" not added = already there!");
                }
                last = o.new_Path.toAbsolutePath().toString();
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
                change_song(last, start,true);
            }
            update_playlist_size_info();
        };
        Actor_engine.execute(r, "Adding multiple songs to playlist",logger);

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
    void set_selected(long start)
    //**********************************************************
    {
        if ( the_song_path == null) return;

        if (dbg) logger.log("set_selected " + the_song_path);

        if ( (new File(the_song_path)).exists() == false)
        {
            logger.log("this file is gone: " + the_song_path);
            if ( the_playlist.isEmpty())
            {
                // nothing to play
                return;
            }
            the_song_path = the_playlist.get(0);
        }

        Song future = path_to_Song.get(the_song_path);
        if ( future == null)
        {
            if ( dbg) logger.log("❌ WARNING: this file is not mapped: " + the_song_path);
            return;
        }
        if ( selected != null)
        {
            if (selected.path.equals(future.path))
            {
                // already selected
                if (dbg) logger.log("❗ already selected " + the_song_path);
                return;
            }
            selected.set_background_to_unselected();
        }
        selected = future;

        logger.log("✅ SELECTED = "+selected.path);
        selected.set_background_to_selected();

        the_music_ui.scroll_to(the_song_path);
        Non_booleans_properties.save_current_song(the_song_path,owner);

        logger.log("✅ elapsed = "+(System.currentTimeMillis()-start)+" ms");
    }

    //**********************************************************
    private double get_bitrate(String new_song)
    //**********************************************************
    {
        double bitrate;
        bitrate = Ffmpeg_utils.get_audio_bitrate(Path.of(new_song), null,logger);
        if ( dbg) logger.log(  (new File(new_song)).getName() + " (bitrate= " + bitrate + " kb/s)");
        the_music_ui.set_status("✅ Status: OK for:" + (new File(new_song)).getName() + " (bitrate= " + bitrate + " kb/s)");
        return bitrate;
    }


    //**********************************************************
    static void sanitize(String song, List<String> oks, List<Old_and_new_Path> out,Logger logger)
    //**********************************************************
    {
        if (!Guess_file_type.is_this_extension_an_audio(Extensions.get_extension((new File(song)).getName())))
        {
            if ( dbg) logger.log("❗ Rejected as a possible song due to extension: "+(new File(song)).getName());
            return;
        }
        String parent = (new File(song)).getParent();
        String file_name = (new File(song)).getName();
        String new_name = Extensions.get_base_name(file_name);

        new_name = Filename_sanitizer.sanitize(new_name,logger);

        new_name = Extensions.add(new_name,Extensions.get_extension(file_name));

        if (new_name.equals(file_name))
        {
            oks.add(song);
            return;
        }

        out.add(new Old_and_new_Path(Path.of(song), Path.of(parent, new_name), Command.command_rename, Status.before_command,false));

    }


    //**********************************************************
    public void init()
    //**********************************************************
    {
        long start = System.currentTimeMillis();
        if (playlist_file == null)
        {
            playlist_file = get_playlist_file(owner);
        }
        load_playlist(playlist_file);
        String song = Non_booleans_properties.get_current_song(owner);
        if (song == null) {
            song = the_playlist.get(0);
            if (song == null) return;
        }
        change_song(song,start, true);
    }


    //**********************************************************
    public static File get_playlist_file(Window owner)
    //**********************************************************
    {
        String playlist_file_name = Shared_services.main_properties().get(PLAYLIST_FILE_NAME);
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
        Shared_services.main_properties().set(PLAYLIST_FILE_NAME, playlist_file_name);
        String home = System.getProperty(Non_booleans_properties.USER_HOME);
        Path p = Paths.get(home, Non_booleans_properties.CONF_DIR, playlist_file_name);
        return p.toFile();
    }


    //**********************************************************
    public String get_playlist_name()
    //**********************************************************
    {
        playlist_file = get_playlist_file(owner);
        if ( dbg) logger.log("playlist_file="+playlist_file.getAbsolutePath());
        String playlist_name_s = extract_playlist_name();
        if ( dbg) logger.log("playlist_name=" + playlist_name_s);
        return playlist_name_s;
    }


    //**********************************************************
    String extract_playlist_name()
    //**********************************************************
    {
        return Extensions.get_base_name(playlist_file.getName());
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
         if (saving_dir == null)
        {
            String home = System.getProperty(Non_booleans_properties.USER_HOME);
            saving_dir = new File(home, "playlists");
            if (!saving_dir.exists())
            {
                if (!saving_dir.mkdir())
                {
                    logger.log("❗ WARNING: creating directory failed for: " + saving_dir.getAbsolutePath());
                }
            }
        }

        saving_dir = Folder_chooser.show_dialog_for_folder_selection("Choose folder for playlist",saving_dir.toPath(), owner, logger).toFile();
        Platform.runLater(() -> choose_playlist_name());
    }

    //**********************************************************
    private void choose_playlist_name()
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog("playlistname");
        Look_and_feel_manager.set_dialog_look(dialog,owner,logger);
        dialog.initOwner(owner);
        dialog.setTitle("Choose a name for the playlist");
        dialog.setContentText("playlistname");

        // The Java 8 way to get the response value (with lambda expression).
        //result.ifPresent(name -> logger.log("Your name: " + name));
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty())
        {
            logger.log("❗ playlist not saved");
            Popups.popup_warning("❗ Not saved ", "playlist not saved", true, owner,logger);
            return;
        }

        String new_playlist_name = result.get();

        if (!new_playlist_name.endsWith(Guess_file_type.KLIK_AUDIO_PLAYLIST_EXTENSION))
            new_playlist_name = Extensions.add(new_playlist_name, Guess_file_type.KLIK_AUDIO_PLAYLIST_EXTENSION);

        change_play_list_name(new_playlist_name);

    }


    //**********************************************************
    public void change_play_list_name(String new_playlist_name)
    //**********************************************************
    {
        Shared_services.main_properties().set(PLAYLIST_FILE_NAME, new_playlist_name);
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
        long start = System.currentTimeMillis();

        if ( the_playlist.isEmpty()) return;
        String first = the_playlist.get(0);
        change_song(first,start, true);
    }


    //**********************************************************
    synchronized void  load_playlist(File playlist_file_)
    //**********************************************************
    {
        logger.log("✅ Loading playlist as:" + playlist_file_.getAbsolutePath());
        if (playlist_file_ == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ SHOULD NOT HAPPEN"));
            return;
        }
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(playlist_file_));
            the_playlist.clear();
            the_music_ui.remove_all_songs();
            List<String> to_be_loaded = new ArrayList<>();
            for (;;)
            {
                String song_path = br.readLine();
                if (song_path == null) break;
                if ( (new File(song_path)).exists())
                {
                    to_be_loaded.add(song_path);
                }
            }
            add_all_to_playlist(to_be_loaded);
            playlist_file = playlist_file_;
            Shared_services.main_properties().set(PLAYLIST_FILE_NAME, playlist_file.getAbsolutePath());

            logger.log("\n\n✅ loaded " + the_playlist.size() + " songs from file:" + playlist_file.getAbsolutePath() + "\n\n");
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
            logger.log(Stack_trace_getter.get_stack_trace("❌ cannot write" + e.toString()));
        }
        catch (IOException e)
        {
            playlist_file = null;
            logger.log("❌ "+Stack_trace_getter.get_stack_trace(e.toString()));
        }

    }

    //**********************************************************
    private void update_playlist_size_info()
    //**********************************************************
    {
        Runnable r = () -> update_playlist_size_info_in_a_thread();
        Actor_engine.execute(r,"Update playlist size info",logger);
    }

    //**********************************************************
    class Duration_message implements Message
    //**********************************************************
    {
        public final String path;
        public final AtomicLong seconds;
        public final CountDownLatch cdl;
        public final Aborter aborter;
        //**********************************************************
        public Duration_message(String path, AtomicLong seconds, CountDownLatch cdl, Aborter aborter)
        //**********************************************************
        {
            this.path = path;
            this.seconds = seconds;
            this.cdl = cdl;
            this.aborter = aborter;
        }

        //**********************************************************
        @Override
        public String to_string()
        //**********************************************************
        {
            StringBuilder sb = new StringBuilder();
            sb.append(" Duration_message: ");
            sb.append(" path: ").append(path);
            return sb.toString();
        }

        //**********************************************************
        @Override
        public Aborter get_aborter()
        //**********************************************************
        {
            return aborter;
        }
    }
    //**********************************************************
    private void update_playlist_size_info_in_a_thread()
    //**********************************************************
    {
        Actor actor = new Actor()
        {
            @Override
            public String run(Message m)
            {
                Duration_message dm = (Duration_message) m;
                get_media_duration(dm.path, dm.seconds, dm.cdl);
                return "OK";
            }
            @Override
            public String name()
            {
                return "Duration_finder_actor";
            }
        };
        Aborter cleanup_aborter = new Aborter("actor engine cleanup",logger);
        Actor_engine_based_on_workers local = new Actor_engine_based_on_workers("playlist size",cleanup_aborter, logger);
        AtomicLong seconds = new AtomicLong(0);
        CountDownLatch cdl = new CountDownLatch(the_playlist.size());
        for ( String path: the_playlist)
        {
            Message m = new Duration_message(path,seconds,cdl,new Aborter("dummy",logger));
            local.run(actor,m,null,logger);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log(e.toString());
        }
        cleanup_aborter.abort("kill actor engine threads");
        final String text = My_I18n.get_I18n_string("Songs",owner,logger);
        Runnable r = () -> the_music_ui.set_total_duration(the_playlist.size() + " "+text+", " + Audio_player_FX_UI.get_nice_string_for_duration(seconds.get(),owner,logger));
        Platform.runLater(r);

    }

    //**********************************************************
    private void get_media_duration(String path, AtomicLong seconds, CountDownLatch cdl)
    //**********************************************************
    {
        Double dur = Ffmpeg_utils.get_media_duration(Path.of(path), null, logger);
        if ( dur != null) seconds.addAndGet((long) (double)dur);
        cdl.countDown();
    }


    //**********************************************************
    void remove_from_playlist_and_jump_to_next()
    //**********************************************************
    {
        String to_be_removed = the_song_path;
        jump_to_next_from_user(); // will change the song
        if ( dbg) logger.log("✅ removing from playlist: " + to_be_removed);
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
            if ( dbg) logger.log("✅ nothing to undo");
            return;
        }
        List<Old_and_new_Path> l = last.oans;
        for (Old_and_new_Path o : l)
        {
            //if ( o.cmd != Command.command_remove_for_playlist) continue;
            //if ( o.status != Status.before_command) continue;
            if (o.old_Path == null) continue;
            if ( dbg) logger.log("✅ undo remove from play list for" + o.old_Path);
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
            if ( dbg) logger.log("✅ Song already listed: "+added_song);
            return false;
        }
        if ( dbg) logger.log("✅ Added song: "+added_song);
        add_to_playlist(added_song);
        return true;
    }

    //**********************************************************
    public void shuffle()
    //**********************************************************
    {
        Collections.shuffle(the_playlist);
        update_display();
    }

    //**********************************************************
    public void update_display()
    //**********************************************************
    {
        the_music_ui.remove_all_songs();
        List<Song> local_songs = new ArrayList<>();
        String selected_path = null;
        for ( String path : the_playlist)
        {
            Song local_song = path_to_Song.get(path);
            if ( !local_songs.contains(local_song)) local_songs.add(local_song);
            if ( selected !=null) if ( local_song.path.equals(selected.path)) selected_path = path;
        }
        //logger.log("adding all songs in UI");
        the_music_ui.add_all_songs(local_songs);
        //logger.log("added all songs in UI");
        the_music_ui.scroll_to(selected_path);

    }

    //**********************************************************
    public void remove(String s)
    //**********************************************************
    {
        remove_from_playlist(s);
    }

    //**********************************************************
    public void search()
    //**********************************************************
    {
        Stage search_stage = new Stage();
        search_stage.initOwner(owner);
        VBox vbox = new VBox();
        Look_and_feel_manager.set_region_look(vbox,search_stage,logger);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);


        VBox the_result_vbox = new VBox();

        TextField search_field = new TextField();
        vbox.getChildren().add(search_field);
        search_field.setPromptText("Search for a song...");
        search_field.setOnAction(event -> {
            perform_search(search_field, search_stage, vbox, the_result_vbox);
        });

        ScrollPane scroll_pane = new ScrollPane(the_result_vbox);
        vbox.getChildren().add(scroll_pane);



        search_stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (key_event.getCode() == KeyCode.ESCAPE) {
                        search_stage.close();
                        key_event.consume();
                    }
                });

        scroll_pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll_pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll_pane.setFitToWidth(true);
        scroll_pane.setFitToHeight(true);
        the_result_vbox.getChildren().clear();

        Scene scene = new Scene(vbox, 600, 400);
        search_stage.setScene(scene);
        search_stage.setTitle(My_I18n.get_I18n_string("Search_Results", search_stage,logger));

        search_stage.show();
        search_stage.sizeToScene();
    }

    //**********************************************************
    private void perform_search(TextField search_field, Stage search_stage, VBox vbox, VBox the_result_vbox)
    //**********************************************************
    {
        the_result_vbox.getChildren().clear();
        String search_text = search_field.getText().toLowerCase();
        if (search_text.trim().isEmpty()) return;
        String[] keys = search_text.split("\\s");
        ImageView iv = new ImageView(Look_and_feel_manager.get_running_film_icon(search_stage,logger));
        iv.setFitHeight(100);
        iv.setPreserveRatio(true);
        vbox.getChildren().add(iv);

        Map<String,List<String>> matched_keywords_in_full_path = new HashMap<>();
        Map<String,List<String>> matched_keywords_in_name = new HashMap<>();
        for (Song song : path_to_Song.values())
        {
            String full_path = song.path.toLowerCase();
            String name = Path.of(song.path).getFileName().toString().toLowerCase();
            for(String key : keys)
            {
                if (full_path.contains(key))
                {
                    List<String> l = matched_keywords_in_full_path.get(song.path);
                    if ( l == null)
                    {
                        l = new ArrayList<>();
                        matched_keywords_in_full_path.put(song.path, l);
                    }
                    l.add(key);
                }
                if (name.contains(key))
                {
                    List<String> l = matched_keywords_in_name.get(song.path);
                    if ( l == null)
                    {
                        l = new ArrayList<>();
                        matched_keywords_in_name.put(name, l);
                    }
                    l.add(key);
                }
            }
        }
        for (String path : path_to_Song.keySet())
        {
            Song song = path_to_Song.get(path);
            boolean found = false;
            boolean show_full_path = true;
            boolean is_max = false;
            if ( matched_keywords_in_full_path.get(path) != null)
            {
                if (matched_keywords_in_full_path.get(path).size() > 0)
                {
                    found = true;
                }
                if (matched_keywords_in_full_path.get(path).size() == keys.length)
                {
                    is_max = true; // all keywords matched
                }
            }
            if ( matched_keywords_in_name.get(path) != null)
            {
                if (matched_keywords_in_name.get(path).size() > 0)
                {
                    found = true;
                }
                if (matched_keywords_in_name.get(path).size() == keys.length)
                {
                    is_max = true; // all keywords matched
                    show_full_path = false;
                }
            }
            List<String> matched = new ArrayList<>();
            if( show_full_path)
            {
                if ( matched_keywords_in_full_path.get(path) != null)
                {
                    for (String key : matched_keywords_in_full_path.get(path))
                    {
                        if ( !matched.contains(key)) matched.add(key);
                    }
                }
            }
            else
            {
                if ( matched_keywords_in_name.get(path) != null)
                {
                    for (String key : matched_keywords_in_name.get(path))
                    {
                        if ( !matched.contains(key)) matched.add(key);
                    }
                }
            }

            String display = "";
            for ( String m : matched) display += m + " ";
            if ( found) the_result_vbox.getChildren().add(make_button_for_found_song(song, display, search_stage,is_max));

        }

        iv.setImage(Look_and_feel_manager.get_the_end_icon(search_stage,logger));
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.log(""+e);
                }
                Platform.runLater(() -> {
                    iv.setImage(null);
                    vbox.getChildren().remove(iv);
                });
            }
        };
        Actor_engine.execute(r,"set end of search icon",logger);
    }

    //**********************************************************
    private Node make_button_for_found_song(Song song, String search_text, Stage search_stage, boolean is_max)
    //**********************************************************
    {
        Path p = Path.of(song.path);
        Button b = new Button(search_text +" => "+ p.getFileName());

        if(is_max)
        {
            b.setGraphic(new Circle(10, Color.RED));
        }
        b.setMnemonicParsing(false); // avoid removal of first underscore
        Look_and_feel_manager.set_button_look(b, true,owner,logger);

        b.setOnAction((ActionEvent e) -> {
            long start = System.currentTimeMillis();
            change_song(song.path, start,false);
        });

        // add a menu to the button!
        ContextMenu context_menu = Song.get_context_menu_for_a_song(this, song.path, owner, logger);

        b.setOnContextMenuRequested((ContextMenuEvent event) -> {
            //logger.log("show context menu of button:"+ song.path);
            context_menu.show(b, event.getScreenX(), event.getScreenY());
        });


        Drag_and_drop.init_drag_and_drop_sender_side(b, null,Path.of(song.path),logger);

        return b;
    }

    // warning the ORDER is wrong
    //**********************************************************
    public List<Song> get_a_copy_of_all_songs()
    //**********************************************************
    {
        return new ArrayList<>(path_to_Song.values());
    }

    //**********************************************************
    synchronized void jump_to_next_on_end_of_media()
    //**********************************************************
    {
        jump_to_next();
    }


    //**********************************************************
    public void jump_to_next_from_user()
    //**********************************************************
    {
        jump_to_next();
    }

    //**********************************************************
    synchronized void jump_to_next()
    //**********************************************************
    {
        long start = System.currentTimeMillis();

        if ( dbg) logger.log("✅ jumping to next song");

        if (the_playlist.isEmpty())
        {
            logger.log("❗ Warning: empty playlist");
            return;
        }

        // the playlist may have been changed...
        for (int i = 0; i < the_playlist.size(); i++)
        {
            String file = the_playlist.get(i);
            if (file.equals(the_song_path))
            {
                if ( dbg) logger.log("✅ found current song in playlist as #" + i);

                int k = i + 1;
                if (k >= the_playlist.size()) k = 0;
                String target = the_playlist.get(k);
                change_song(target,start, false);
                return;
            }
        }

        logger.log("❗ jumping to next song but current song not found in playlist???");
        change_song(null,start, true);

    }




    //**********************************************************
    synchronized void jump_to_previous_from_user()
    //**********************************************************
    {
        long start = System.currentTimeMillis();

        if (the_playlist.isEmpty()) return;

        // the playlist may have been changed...
        for (int i = 0; i < the_playlist.size(); i++)
        {
            String file = the_playlist.get(i);
            if (file.equals(the_song_path))
            {
                int k = i - 1;
                if (k < 0) k = the_playlist.size() - 1;
                String target = the_playlist.get(k);
                change_song(target, start, false);
                return;
            }
        }
        logger.log("jumping to previous song but current song not found in playlist???");
        change_song(null,start, true);
    }



    Aborter previous;
    //**********************************************************
    void change_song(String new_song, long start, boolean first_time)
    //**********************************************************
    {
        if ( previous != null)
        {
            //logger.log("change_song aborting previous");
            previous.abort("changing song");
        }
        if (new_song == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL new_song == null"));
            return;
        }
        String finalNew_song = new_song;
        Aborter aborter = new Aborter(new_song,logger);
        Actor_engine.execute(() -> change_song_in_a_thread(finalNew_song, start, first_time,aborter),"change song",logger);

    }


    //**********************************************************
    void change_song_in_a_thread(String new_song, long start, boolean first_time, Aborter aborter)
    //**********************************************************
    {

        previous = aborter;
        the_music_ui.stop_current_media();

        if ( aborter.should_abort())
        {
            logger.log("change_song_in_a_thread aborting1");
            return;
        }

        Actor_engine.execute(()->launch_bitrate_in_a_thread(new_song),"Find and display bitrate",logger);

        the_song_path = new_song;
        the_music_ui.set_title((new File(new_song)).getName());
        if ( aborter.should_abort())
        {
            logger.log("change_song_in_a_thread aborting2");
            return;
        }
        add_one_song_to_playlist_if_not_already_there(the_song_path);
        if ( aborter.should_abort())
        {
            logger.log("change_song_in_a_thread aborting3");
            return;
        }
        the_music_ui.play_song_with_new_media_player(the_song_path, first_time);
        if ( aborter.should_abort())
        {
            logger.log("change_song_in_a_thread aborting4");
            return;
        }
        set_selected(start);
    }

    //**********************************************************
    private void launch_bitrate_in_a_thread(String new_song)
    //**********************************************************
    {
        double bitrate = get_bitrate(new_song);
        the_song_path = new_song;
        Platform.runLater(() -> the_music_ui.set_title((new File(new_song)).getName() + "       bitrate= " + bitrate + " kb/s"));

    }

}
