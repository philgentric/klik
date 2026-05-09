package klikr.audio.player;

import javafx.application.Platform;
import javafx.stage.Window;
import klikr.change.undo.Undo_core;
import klikr.change.undo.Undo_item;
import klikr.look.my_i18n.My_I18n;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.files_and_paths.*;
import klikr.change.old_and_new.Command;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.old_and_new.Status;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

//**********************************************************
public class Song_playlist
//**********************************************************
{
    private final static boolean dbg = false;
    private final Aborter aborter;
    private final Window owner;
    private final Logger logger;
    //private final Application application;
    private final Path playlist_path;
    private final Undo_core undo_core;

    List<String> the_playlist =  new ArrayList<>();

    public static Klikr_cache<Path,Double> duration_cache;
    public static Klikr_cache<Path,Double> bitrate_cache;

    //**********************************************************
    public Song_playlist(Path playlist_path,
                         //Application application,
                         Aborter aborter, Window owner, Logger logger)
    //**********************************************************
    {
        this.playlist_path = playlist_path;
        //this.application = application;
        this.aborter = aborter;
        this.owner = owner;
        this.logger = logger;
        this.undo_core = new Undo_core("undos_for_music_playlist.properties", owner,logger);

    }

    //**********************************************************
    public void user_wants_to_add_songs(
            List<String> the_list_of_new_songs,
            Aborter youtube_abort)
    //**********************************************************
    {
        long start = System.currentTimeMillis();
        Runnable r = () ->
        {
            List<Old_and_new_Path> to_be_renamed_first = new ArrayList<>();
            List<String> oks = new ArrayList<>();
            for (String path : the_list_of_new_songs)
            {
                if ( youtube_abort != null) if ( youtube_abort.should_abort()) return;
                if ( aborter.should_abort()) return;
                File f = new File(path);
                if (f.isDirectory())
                {
                    load_folder(f, oks,to_be_renamed_first, aborter);
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
            the_playlist.addAll(finaly);
            if ( last != null)
            {
                save_playlist();
                //change_song(last, start,true);
            }
            update_playlist_size_info();
        };
        Actor_engine.execute(r, "Adding multiple songs to playlist",logger);

    }

    //**********************************************************
    private void save_playlist()
    //**********************************************************
    {
        if ( !Platform.isFxApplicationThread())
        {
            logger.log("HAPPENS1 save_playlist");
            Platform.runLater(this::save_playlist);
        }
        if (playlist_path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return;
        }
        logger.log("Saving playlist as:" + playlist_path.toAbsolutePath().toString());
        try
        {
            int count = 0;
            FileWriter fw = new FileWriter(playlist_path.toFile());
            for (String f : the_playlist)
            {
                fw.write(f + "\n");
                count++;
            }
            fw.close();
            logger.log("Saved "+count+" songs in playlist file named:" + playlist_path.toAbsolutePath());
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("not saved" + e.toString()));
        }
    }

    //**********************************************************
    private void update_playlist_size_info()
    //**********************************************************
    {
        Runnable r = () -> update_playlist_size_info_in_a_thread();
        Actor_engine.execute(r,"Update playlist length info",logger);
    }
    //**********************************************************
    private void update_playlist_size_info_in_a_thread()
    //**********************************************************
    {
        Aborter cleanup_aborter = new Aborter("update_playlist_size_info_in_a_thread",logger);

        double seconds[] = {0.0};
        for ( String path: the_playlist)
        {
            bitrate_cache.prefill_cache(Path.of(path),true,cleanup_aborter,owner);
            duration_cache.prefill_cache(Path.of(path),true,cleanup_aborter,owner);
        }

        for ( String path: the_playlist)
        {
            seconds[0] += duration_cache.get(Path.of(path),cleanup_aborter,null,owner);
        }

        duration_cache.save_whole_cache_to_disk();
        bitrate_cache.save_whole_cache_to_disk();

        /*
        final String text = My_I18n.get_I18n_string("Songs",owner,logger);
        Runnable r = () -> the_music_ui.set_total_duration(the_playlist.size() + " "+text+", " + get_nice_string_for_duration(seconds[0],owner,logger));
        Platform.runLater(r);
           */
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


    //**********************************************************
    private void load_folder(File folder, List<String> oks, List<Old_and_new_Path> out, Aborter aborter)
    //**********************************************************
    {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File ff : files)
        {
            if ( aborter.should_abort()) return;

            if (ff.isDirectory()) load_folder(ff, oks, out, aborter);
            else
            {
                sanitize(ff.getAbsolutePath(), oks, out,logger);
            }
        }
    }

    //**********************************************************
    static void sanitize(String song, List<String> oks, List<Old_and_new_Path> out, Logger logger)
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

    /*
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

        Progress progress = Progress.start(vbox,owner,logger);

        Map<String,List<String>> matched_keywords_in_full_path = new HashMap<>();
        Map<String,List<String>> matched_keywords_in_name = new HashMap<>();
        for (String path: the_playlist)
        {
            String full_path = path.toLowerCase();
            String name = Path.of(path).getFileName().toString().toLowerCase();
            for(String key : keys)
            {
                if (full_path.contains(key))
                {
                    List<String> l = matched_keywords_in_full_path.get(path);
                    if ( l == null)
                    {
                        l = new ArrayList<>();
                        matched_keywords_in_full_path.put(path, l);
                    }
                    l.add(key);
                }
                if (name.contains(key))
                {
                    List<String> l = matched_keywords_in_name.get(path);
                    if ( l == null)
                    {
                        l = new ArrayList<>();
                        matched_keywords_in_name.put(name, l);
                    }
                    l.add(key);
                }
            }
        }
        for (String path : the_playlist)
        {
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
            if ( found) the_result_vbox.getChildren().add(make_button_for_found_song(path, display, search_stage,is_max));

        }

        progress.stop();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.log(""+e);
                }
                Platform.runLater(progress::remove);
            }
        };
        Actor_engine.execute(r,"set end of search icon",logger);
    }

    //**********************************************************
    private Node make_button_for_found_song(String path_s , String search_text, Stage search_stage, boolean is_max)
    //**********************************************************
    {
        Path p = Path.of(path_s);
        Button b = new Button(search_text +" => "+ p.getFileName());

        if(is_max)
        {
            b.setGraphic(new Circle(10, Color.RED));
        }
        b.setMnemonicParsing(false); // avoid removal of first underscore
        Look_and_feel_manager.set_button_look(b, true,owner,logger);

        b.setOnAction((ActionEvent e) -> {
            long start = System.currentTimeMillis();
            change_song(path_s, start,false);
        });


        b.setOnContextMenuRequested((ContextMenuEvent event) -> {
            //logger.log("show context menu of button:"+ song.path);
            ContextMenu context_menu = get_context_menu_for_a_song(path_s);
            context_menu.show(b, event.getScreenX(), event.getScreenY());
        });


        Drag_and_drop.init_drag_and_drop_sender_side(b, null,Path.of(path_s),logger);

        return b;
    }

    //**********************************************************
    public ContextMenu get_context_menu_for_a_song(String full_path)
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu, owner, logger);

        Menu_items.add_menu_item_for_context_menu(
                "Play_Similar_Song",null,
                (ActionEvent e) ->
                        play_similar(Path.of(full_path), owner, logger),
                context_menu,
                owner, logger);


        Menu_items.add_menu_item_for_context_menu(
                "Browse_in_new_window",
                null,//(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN)).getDisplayText(),
                (ActionEvent e) ->
                        Window_builder.additional_no_past(application, Window_type.File_system_2D, new Path_list_provider_for_file_system(Path.of(full_path).getParent(), owner, logger), owner, logger),
                context_menu,
                owner, logger);

        Menu_items.add_menu_item_for_context_menu(
                "Rename",
                null,//(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN)).getDisplayText(),

                (ActionEvent e) ->
                {

                    Path new_path =  Static_files_and_paths_utilities.ask_user_for_new_file_name(owner, Path.of(full_path), logger);
                    if ( new_path == null) return;

                    List<Old_and_new_Path> l = new ArrayList<>();
                    Old_and_new_Path oandn = new Old_and_new_Path(Path.of(full_path), new_path, Command.command_rename, Status.before_command, false);
                    l.add(oandn);
                    Moving_files.perform_safe_moves_in_a_thread( l, true, owner.getX()+100, owner.getY()+100,owner, new Aborter("dummy", logger), logger);

                    remove_from_playlist(full_path);
                    the_playlist.add(new_path.toAbsolutePath().toString());
                    //if ( the_song_path.equals(full_path)) the_song_path = new_path.toAbsolutePath().toString();
                },
                context_menu,
                owner, logger);

        Menu_items.add_menu_item_for_context_menu(
                "Remove_From_Playlist",null,
                (ActionEvent e) ->
                        remove_from_playlist(full_path),
                context_menu, owner, logger);


        {
            String info_string = "Info: ";
            Double dur = duration_cache.get(Path.of(full_path), aborter,null, owner);
            if ( dur != null) info_string += String.format("Duration %.1f s ", dur);
            double bitrate = bitrate_cache.get(Path.of(full_path), aborter,null, owner);
            if ( bitrate > 0) info_string += String.format(" Bitrate %.0f kb/s", bitrate);
            MenuItem the_menu_item = new MenuItem(info_string);
            Look_and_feel_manager.set_menu_item_look(the_menu_item,owner,logger);
            context_menu.getItems().add(the_menu_item);
            the_menu_item.setOnAction(e-> Audio_info_frame.show(Path.of(full_path),owner,logger));
        }

        Menu_items.add_menu_item_for_context_menu(
                "Edit_Song_Metadata",null,
                (ActionEvent e) -> Ffmpeg_metadata_editor.edit_metadata_of_a_file_in_a_thread(Path.of(full_path), owner, logger),
                context_menu, owner, logger);

        return context_menu;
    }
*/

    //**********************************************************
    void remove_from_playlist(String to_be_removed)
    //**********************************************************
    {
        the_playlist.remove(to_be_removed);

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

/*
    //**********************************************************
    private void play_similar(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        Actor_engine.execute(()->
                play_similar_in_thread(path, owner, logger),"Find and play similar song",logger
        );
    }

    //**********************************************************
    private void play_similar_in_thread(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        long start = System.currentTimeMillis();

        Aborter aborter = new Aborter("dummy",logger);

        Path_comparator_source path_comparator_source = new Path_comparator_source() {
            @Override
            public Comparator<Path> get_path_comparator() {
                return new Comparator<Path>() {
                    @Override
                    public int compare(Path o1, Path o2) {
                        return o1.compareTo(o2);
                    }
                };
            }
        };
        Path_list_provider path_list_provider = new Path_list_provider_for_playlist(String_constants.get_playlist_path(owner),owner, aborter,logger);
        Similarity_engine similarity_engine = new Similarity_engine(
                path_list_provider.only_song_paths(true,false,aborter),
                path_list_provider,
                path_comparator_source,
                owner,
                aborter,
                logger);
        Feature_vector_source fvs = new Feature_vector_source_for_song_similarity(aborter);
        Feature_vector_cache fvc = new Feature_vector_cache("audio_feature_vector_cache", fvs, logger);
        Supplier<Feature_vector_cache> fv_cache_supplier = () -> fvc;
        LongAdder count_pairs_examined = new LongAdder();
        List<Most_similar> similars = similarity_engine.find_similars_generic(
                path,
                new ArrayList<>(),
                3,
                100000000.0,// MAGIC
                fv_cache_supplier,
                owner, 100,100,
                count_pairs_examined,
                aborter);

        if ( similars.size() == 0)
        {
            logger.log("no similar song found");
            return;
        }
        Path similar = similars.get(0).path();
        logger.log("similar song found : "+ similar);

        Platform.runLater(()-> change_song(similar.toAbsolutePath().toString(), start,false));

        aborter.abort("end of similar search");
    }
*/

/*

    Aborter previous;
    //**********************************************************
    public void change_song(String new_song, long start, boolean first_time)
    //**********************************************************
    {
        if ( previous != null)
        {
            //logger.log("change_song aborting previous");
            previous.abort("changing song");
        }
        if (new_song == null)
        {
            logger.log("❗Warning new_song == null in change_song");
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

    //**********************************************************
    private double get_bitrate(String new_song)
    //**********************************************************
    {
        double bitrate;
        bitrate = bitrate_cache.get(Path.of(new_song),aborter, null,owner);
        if ( dbg) logger.log(  (new File(new_song)).getName() + " (bitrate= " + bitrate + " kb/s)");
        //the_music_ui.set_status("✅ Status: OK for:" + (new File(new_song)).getName() + " (bitrate= " + bitrate + " kb/s)");
        return bitrate;
    }

 */
}
